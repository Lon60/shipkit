package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.AuthPayloadDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.CreateAccountDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.StatusDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class AccountGraphQLController {

    private final AccountService accountService;

    @MutationMapping
    @PreAuthorize("permitAll()")
    public AuthPayloadDTO register(@Valid @Argument("input") CreateAccountDTO input) {
        return accountService.register(input.getEmail(), input.getPassword());
    }

    @MutationMapping
    @PreAuthorize("permitAll()")
    public AuthPayloadDTO login(@Argument String email, @Argument String password) {
        return accountService.login(email, password);
    }

    @QueryMapping
    @PreAuthorize("permitAll()")
    public StatusDTO status() {
        return new StatusDTO("healthy", accountService.isAdminInitialized());
    }
}