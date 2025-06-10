package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.AuthPayloadDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.CreateAccountDTO;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class AccountGraphQLController {

    private final AccountService accountService;

    @MutationMapping
    public AuthPayloadDTO register(@Argument("input") CreateAccountDTO input) {
        return accountService.register(input.email(), input.password());
    }

    @MutationMapping
    public AuthPayloadDTO login(@Argument String email, @Argument String password) {
        return accountService.login(email, password);
    }
}