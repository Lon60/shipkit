package io.shipkit.gatewayapi.gatewayapi.core.settings;

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

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlatformSettingGraphQLControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DomainSetupService domainSetupService;
    @Autowired PlatformSettingRepository repository;

    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        WebTestClient client = MockMvcWebTestClient
                .bindTo(mockMvc)
                .baseUrl("/graphql")
                .build();
        graphQlTester = HttpGraphQlTester.create(client);
    }

    @Test
    @WithMockUser
    void shouldSetupDomain() {
        String domain = "example.com";
        boolean sslEnabled = true;
        boolean forceSsl = true;

        doNothing().when(domainSetupService).configureDomain(domain, false, sslEnabled, forceSsl);

        graphQlTester.documentName("setupDomain")
                .variable("domain", domain)
                .variable("sslEnabled", sslEnabled)
                .variable("forceSsl", forceSsl)
                .execute()
                .path("setupDomain").entity(Boolean.class).isEqualTo(true);

        verify(domainSetupService).configureDomain(domain, false, sslEnabled, forceSsl);
    }
}