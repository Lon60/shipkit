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
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.mockito.Mockito.verify;

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
    @WithMockUser
    void shouldCreateDeployment() {
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());

        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        GraphQlTester.Response response = graphQlTester.documentName("createDeployment")
                .variable("yaml", composeYaml)
                .execute();

        response.path("createDeployment.id").hasValue();

        assertEquals(1, deploymentRepository.count());
    }

    @Test
    @WithMockUser
    void shouldUpdateDeployment() {
        // First create a deployment
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());
        when(grpcClient.stopApp(any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("stopped").build());

        String originalYaml = "version: '3'\nservices:\n  app:\n    image: nginx";
        String updatedYaml = "version: '3'\nservices:\n  app:\n    image: httpd";

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("yaml", originalYaml)
                .execute()
                .path("createDeployment.id")
                .entity(String.class)
                .get();

        // Now update the deployment
        GraphQlTester.Response updateResponse = graphQlTester.documentName("updateDeployment")
                .variable("id", deploymentId)
                .variable("yaml", updatedYaml)
                .execute();

        updateResponse.path("updateDeployment.id").entity(String.class).isEqualTo(deploymentId);
        updateResponse.path("updateDeployment.composeYaml").entity(String.class).isEqualTo(updatedYaml);

        assertEquals(1, deploymentRepository.count()); // Should still have only one deployment
        verify(grpcClient).stopApp(deploymentId); // Should have stopped the old deployment
        verify(grpcClient).startCompose(deploymentId, updatedYaml); // Should have started with new yaml
    }

    @Test
    @WithMockUser
    void shouldDeleteDeployment() {
        // First create a deployment
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());
        when(grpcClient.stopApp(any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("stopped").build());

        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("yaml", composeYaml)
                .execute()
                .path("createDeployment.id")
                .entity(String.class)
                .get();

        assertEquals(1, deploymentRepository.count());

        // Now delete the deployment
        GraphQlTester.Response deleteResponse = graphQlTester.documentName("deleteDeployment")
                .variable("id", deploymentId)
                .execute();

        deleteResponse.path("deleteDeployment").entity(Boolean.class).isEqualTo(true);

        assertEquals(0, deploymentRepository.count()); // Should be deleted from database
        verify(grpcClient).stopApp(deploymentId); // Should have stopped the deployment first
    }

    @Test
    @WithMockUser
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

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("yaml", composeYaml)
                .execute()
                .path("createDeployment.id")
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