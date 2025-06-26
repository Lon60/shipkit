package io.shipkit.gatewayapi.gatewayapi.core.settings;

import docker_control.ActionResult;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DockerControlGrpcClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DomainSetupServiceTest {

    private PlatformSettingRepository repository;
    private DockerControlGrpcClient dockerClient;
    private Configuration fmConfig;
    private DomainSetupService service;

    private Path tempDir;

    private static final String DOMAIN = "example.com";

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(PlatformSettingRepository.class);
        dockerClient = mock(DockerControlGrpcClient.class);

        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate("nginx_vhost.ftl", "server { server_name ${domain}; }");
        fmConfig = new Configuration(Configuration.VERSION_2_3_32);
        fmConfig.setTemplateLoader(loader);

        service = new DomainSetupService(repository, dockerClient, fmConfig);

        tempDir = Files.createTempDirectory("nginx-test");
        ReflectionTestUtils.setField(service, "nginxOutputDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "nginxContainerName", "nginx");

        when(repository.findByFqdn(DOMAIN)).thenReturn(Optional.of(new PlatformSetting()));
        ActionResult ok = ActionResult.newBuilder().setStatus(0).setMessage("OK").build();
        when(dockerClient.issueCertificate(DOMAIN)).thenReturn(ok);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void rollsBackExistingVhostFileOnFailure() throws Exception {
        Path vhostPath = tempDir.resolve(DOMAIN + ".conf");
        Files.writeString(vhostPath, "previous");

        ActionResult fail = ActionResult.newBuilder().setStatus(1).setMessage("fail").build();
        ActionResult ok = ActionResult.newBuilder().setStatus(0).setMessage("OK").build();
        when(dockerClient.reloadNginx(anyString())).thenReturn(fail, ok);

        assertThrows(RuntimeException.class, () ->
                service.configureDomain(DOMAIN, true, false, false));

        String content = Files.readString(vhostPath);
        assertThat(content).isEqualTo("previous");

        verify(dockerClient, times(2)).reloadNginx("nginx");
    }

    @Test
    void deletesNewVhostFileOnFailureWhenNoPreviousFile() throws Exception {
        Path vhostPath = tempDir.resolve(DOMAIN + ".conf");
        Files.deleteIfExists(vhostPath);

        ActionResult fail = ActionResult.newBuilder().setStatus(1).setMessage("fail").build();
        when(dockerClient.reloadNginx(anyString())).thenReturn(fail);
        
        assertThrows(RuntimeException.class, () ->
                service.configureDomain(DOMAIN, true, false, false));

        assertThat(Files.exists(vhostPath)).isFalse();
    }
} 