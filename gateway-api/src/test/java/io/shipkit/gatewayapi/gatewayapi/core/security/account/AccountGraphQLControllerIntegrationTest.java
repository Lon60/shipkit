package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@Transactional
class AccountGraphQLControllerIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        accountRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewAccount() throws Exception {
        String query = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "test@example.com",
                        "password": "password123"
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.register.token").exists())
                .andExpect(jsonPath("$.data.register.token").isString())
                .andExpect(jsonPath("$.errors").doesNotExist());

        assertEquals(1, accountRepository.count());
        assertTrue(accountRepository.findByEmail("test@example.com").isPresent());
    }

    @Test
    void shouldNotRegisterAccountWithExistingEmail() throws Exception {
        String firstRegister = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "test@example.com",
                        "password": "password123"
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstRegister))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.register.token").exists());

        String secondRegister = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "test@example.com",
                        "password": "password456"
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondRegister))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());

        assertEquals(1, accountRepository.count());
    }

    @Test
    void shouldRegisterWithValidEmailFormats() throws Exception {
        String[] validEmails = {
                "user@domain.com",
                "test.email@example.org",
                "user+tag@domain.co.uk"
        };

        for (String email : validEmails) {
            String query = String.format("""
                {
                    "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                    "variables": {
                        "input": {
                            "email": "%s",
                            "password": "password123"
                        }
                    }
                }
                """, email);

            mockMvc.perform(post("/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(query))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.register.token").exists());
        }

        assertEquals(validEmails.length, accountRepository.count());
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        String registerQuery = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "login@example.com",
                        "password": "password123"
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerQuery))
                .andExpect(status().isOk());

        String loginQuery = """
            {
                "query": "mutation login($email: String!, $password: String!) { login(email: $email, password: $password) { token } }",
                "variables": {
                    "email": "login@example.com",
                    "password": "password123"
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.token").exists())
                .andExpect(jsonPath("$.data.login.token").isString());
    }

    @Test
    void shouldNotLoginWithInvalidCredentials() throws Exception {
        String registerQuery = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "login@example.com",
                        "password": "password123"
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerQuery))
                .andExpect(status().isOk());

        String loginQuery = """
            {
                "query": "mutation login($email: String!, $password: String!) { login(email: $email, password: $password) { token } }",
                "variables": {
                    "email": "login@example.com",
                    "password": "wrongpassword"
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldNotLoginWithNonExistentUser() throws Exception {
        String loginQuery = """
            {
                "query": "mutation login($email: String!, $password: String!) { login(email: $email, password: $password) { token } }",
                "variables": {
                    "email": "nonexistent@example.com",
                    "password": "password123"
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldHandleEmptyEmailAndPassword() throws Exception {
        String query = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "",
                        "password": ""
                    }
                }
            }
            """;

        mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());

        assertEquals(0, accountRepository.count());
    }

    @Test
    void shouldReturnDifferentTokensForSameUser() throws Exception {
        String registerQuery = """
            {
                "query": "mutation register($input: CreateAccountInput!) { register(input: $input) { token } }",
                "variables": {
                    "input": {
                        "email": "multilogin@example.com",
                        "password": "password123"
                    }
                }
            }
            """;

        String registerResponse = mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerQuery))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String loginQuery = """
            {
                "query": "mutation login($email: String!, $password: String!) { login(email: $email, password: $password) { token } }",
                "variables": {
                    "email": "multilogin@example.com",
                    "password": "password123"
                }
            }
            """;

        String loginResponse = mockMvc.perform(post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginQuery))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotEquals(registerResponse, loginResponse);
    }
}