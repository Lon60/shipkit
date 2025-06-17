package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountGraphQLControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    private GraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        WebTestClient client = MockMvcWebTestClient
                .bindTo(mockMvc)
                .baseUrl("/graphql")
                .build();
        graphQlTester = HttpGraphQlTester.create(client);
    }

    @Test
    void shouldReturnStatusWithNoAdminInitialized() {
        graphQlTester.documentName("status")
                .execute()
                .path("status.status").entity(String.class).isEqualTo("healthy")
                .path("status.adminInitialized").entity(Boolean.class).isEqualTo(false);
    }

    @Test
    void shouldReturnStatusWithAdminInitialized() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "admin@example.com",
                        "password", "password123"))
                .execute();

        graphQlTester.documentName("status")
                .execute()
                .path("status.status").entity(String.class).isEqualTo("healthy")
                .path("status.adminInitialized").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void shouldRegisterFirstAccount() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "test@example.com",
                        "password", "password123"))
                .execute()
                .path("register.token").hasValue();

        assertEquals(1, accountRepository.count());
        assertTrue(accountRepository.findByEmail("test@example.com").isPresent());
    }

    @Test
    void shouldNotAllowRegistrationAfterFirstAccount() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "first@example.com",
                        "password", "password123"))
                .execute();

        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "second@example.com",
                        "password", "password456"))
                .execute()
                .errors().satisfy(err -> {
                    assertFalse(err.isEmpty());
                    assertTrue(err.get(0).getMessage().contains("Registration is disabled"));
                });

        assertEquals(1, accountRepository.count());
        assertTrue(accountRepository.findByEmail("first@example.com").isPresent());
        assertFalse(accountRepository.findByEmail("second@example.com").isPresent());
    }

    @Test
    void shouldNotRegisterAccountWithExistingEmail() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "test@example.com",
                        "password", "password123"))
                .execute();

        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "test@example.com",
                        "password", "password456"))
                .execute()
                .errors().satisfy(err -> assertFalse(err.isEmpty()));

        assertEquals(1, accountRepository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@domain.com",
            "test.email@example.org",
            "user+tag@domain.co.uk"
    })
    void shouldRegisterWithValidEmailFormats(String email) {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", email,
                        "password", "password123"))
                .execute()
                .path("register.token").hasValue();

        assertTrue(accountRepository.findByEmail(email).isPresent());
    }

    @Test
    void shouldHandleEmptyEmailAndPassword() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "",
                        "password", ""))
                .execute()
                .errors().satisfy(err -> assertFalse(err.isEmpty()));

        assertEquals(0, accountRepository.count());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "login@example.com",
                        "password", "password123"))
                .execute();

        String token = graphQlTester.documentName("login")
                .variable("email", "login@example.com")
                .variable("password", "password123")
                .execute()
                .path("login.token")
                .entity(String.class)
                .get();

        assertNotNull(token);
        assertFalse(token.isBlank(), "JWT should not be blank");
    }

    @Test
    void shouldNotLoginWithInvalidCredentials() {
        graphQlTester.documentName("register")
                .variable("input", Map.of(
                        "email", "login@example.com",
                        "password", "password123"))
                .execute();

        graphQlTester.documentName("login")
                .variable("email", "login@example.com")
                .variable("password", "wrongpassword")
                .execute()
                .errors().satisfy(err -> assertFalse(err.isEmpty()));
    }

    @Test
    void shouldNotLoginWithNonExistentUser() {
        graphQlTester.documentName("login")
                .variable("email", "ghost@example.com")
                .variable("password", "password123")
                .execute()
                .errors().satisfy(err -> assertFalse(err.isEmpty()));
    }
}