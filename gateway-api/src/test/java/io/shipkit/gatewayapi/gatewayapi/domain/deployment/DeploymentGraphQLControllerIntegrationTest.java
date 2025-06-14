package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.ActionResult;
import docker_control.AppStatus;
import docker_control.AppState;
import docker_control.ContainerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeploymentGraphQLControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DeploymentRepository deploymentRepository;
    @MockitoBean DockerControlGrpcClient grpcClient;

    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        deploymentRepository.deleteAll();
        WebTestClient client = MockMvcWebTestClient
                .bindTo(mockMvc)
                .baseUrl("/graphql")
                .build();
        graphQlTester = HttpGraphQlTester.create(client);
    }

    @Test
    void shouldStartDeployment() {
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());

        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        GraphQlTester.Response response = graphQlTester.documentName("startDeployment")
                .variable("yaml", composeYaml)
                .execute();

        response.path("startDeployment.id").hasValue();

        assertEquals(1, deploymentRepository.count());
    }

    @Test
    void shouldReturnDeploymentStatus() {
        // capture UUID used during start
        java.util.concurrent.atomic.AtomicReference<String> uuidRef = new java.util.concurrent.atomic.AtomicReference<>();

        when(grpcClient.startCompose(any(), any())).thenAnswer(invocation -> {
            String uuid = invocation.getArgument(0);
            uuidRef.set(uuid);
            return ActionResult.newBuilder().setStatus(0).build();
        });

        // ---- create deployment via GraphQL ----
        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        String deploymentId = graphQlTester.documentName("startDeployment")
                .variable("yaml", composeYaml)
                .execute()
                .path("startDeployment.id")
                .entity(String.class)
                .get();

        // stub getStatus for captured uuid
        AppStatus appStatus = AppStatus.newBuilder()
                .setUuid(uuidRef.get())
                .setState(AppState.RUNNING)
                .setMessage("running")
                .setStatus(0)
                .addContainers(ContainerStatus.newBuilder()
                        .setName("app")
                        .setState("running")
                        .setHealth("healthy")
                        .addPorts("80:80")
                        .build())
                .build();
        when(grpcClient.getStatus(uuidRef.get())).thenReturn(appStatus);

        // ---- query status ----
        graphQlTester.documentName("deploymentStatus")
                .variable("id", deploymentId)
                .execute()
                .path("deploymentStatus.uuid")
                .entity(String.class)
                .isEqualTo(uuidRef.get());
    }
} 