package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.shipkit.gatewayapi.gatewayapi.core.exceptions.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlatformSettingGraphQLErrorIntegrationTest {

    @Autowired MockMvc mockMvc;
    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        WebTestClient client = MockMvcWebTestClient
                .bindTo(mockMvc)
                .baseUrl("/graphql")
                .build();
        graphQlTester = HttpGraphQlTester.create(client);
    }

    @Test
    @WithMockUser
    void shouldReturnDomainValidationErrorCodeInGraphQlExtensions() {
        graphQlTester.documentName("setupDomain")
                .variable("domain", "invalid domain")
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors.get(0).getExtensions())
                            .extracting("error", as(MAP))
                            .containsEntry("code", ErrorCode.DOMAIN_VALIDATION_ERROR.name());
                });
    }
} 