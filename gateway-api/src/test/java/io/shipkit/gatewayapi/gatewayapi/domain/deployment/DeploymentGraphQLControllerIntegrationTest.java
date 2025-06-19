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

import java.util.Map;

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

        String name = "web";
        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        GraphQlTester.Response response = graphQlTester.documentName("createDeployment")
                .variable("input", Map.of("name", name, "composeYaml", composeYaml))
                .execute();

        response.path("createDeployment.id").hasValue();

        assertEquals(1, deploymentRepository.count());
    }

    @Test
    @WithMockUser
    void shouldUpdateDeployment() {
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());
        when(grpcClient.stopApp(any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("stopped").build());

        String originalName = "app";
        String originalYaml = "version: '3'\nservices:\n  app:\n    image: nginx";
        String updatedYaml = "version: '3'\nservices:\n  app:\n    image: httpd";
        String updatedName = "app2";

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("input", Map.of("name", originalName, "composeYaml", originalYaml))
                .execute()
                .path("createDeployment.id")
                .entity(String.class)
                .get();

        GraphQlTester.Response updateResponse = graphQlTester.documentName("updateDeployment")
                .variable("id", deploymentId)
                .variable("input", Map.of("name", updatedName, "composeYaml", updatedYaml))
                .execute();

        updateResponse.path("updateDeployment.id").entity(String.class).isEqualTo(deploymentId);
        updateResponse.path("updateDeployment.composeYaml").entity(String.class).isEqualTo(updatedYaml);
        updateResponse.path("updateDeployment.name").entity(String.class).isEqualTo(updatedName);

        assertEquals(1, deploymentRepository.count());
        verify(grpcClient).stopApp(deploymentId);
        verify(grpcClient).startCompose(deploymentId, updatedYaml);
    }

    @Test
    @WithMockUser
    void shouldDeleteDeployment() {
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());
        when(grpcClient.stopApp(any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("stopped").build());

        String name = "web";
        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("input", Map.of("name", name, "composeYaml", composeYaml))
                .execute()
                .path("createDeployment.id")
                .entity(String.class)
                .get();

        assertEquals(1, deploymentRepository.count());

        GraphQlTester.Response deleteResponse = graphQlTester.documentName("deleteDeployment")
                .variable("id", deploymentId)
                .execute();

        deleteResponse.path("deleteDeployment").entity(Boolean.class).isEqualTo(true);

        assertEquals(0, deploymentRepository.count());
        verify(grpcClient).stopApp(deploymentId);
    }

    @Test
    @WithMockUser
    void shouldReturnDeploymentStatus() {
        java.util.concurrent.atomic.AtomicReference<String> uuidRef = new java.util.concurrent.atomic.AtomicReference<>();

        when(grpcClient.startCompose(any(), any())).thenAnswer(invocation -> {
            String uuid = invocation.getArgument(0);
            uuidRef.set(uuid);
            return ActionResult.newBuilder().setStatus(0).build();
        });

        String name = "web";
        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";

        String deploymentId = graphQlTester.documentName("createDeployment")
                .variable("input", Map.of("name", name, "composeYaml", composeYaml))
                .execute()
                .path("createDeployment.id")
                .entity(String.class)
                .get();

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

        graphQlTester.documentName("deploymentStatus")
                .variable("id", deploymentId)
                .execute()
                .path("deploymentStatus.uuid")
                .entity(String.class)
                .isEqualTo(uuidRef.get());
    }

    @Test
    @WithMockUser
    void shouldStartExistingDeployment() {
        when(grpcClient.startCompose(any(), any()))
                .thenReturn(ActionResult.newBuilder().setStatus(0).setMessage("started").build());
        String name = "web";
        String composeYaml = "version: '3'\nservices:\n  app:\n    image: nginx";
        Deployment deployment = deploymentRepository.save(Deployment.create(name, composeYaml));

        GraphQlTester.Response response = graphQlTester.documentName("startDeployment")
                .variable("id", deployment.getId().toString())
                .execute();

        response.path("startDeployment.id").entity(String.class).isEqualTo(deployment.getId().toString());
        verify(grpcClient).startCompose(deployment.getId().toString(), composeYaml);
    }
} 