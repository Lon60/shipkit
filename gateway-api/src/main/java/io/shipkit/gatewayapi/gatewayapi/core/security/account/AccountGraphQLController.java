package io.shipkit.gatewayapi.gatewayapi.core.security.account;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.AuthPayloadDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.CreateAccountDTO;
import io.shipkit.gatewayapi.gatewayapi.core.security.account.dto.ChangePasswordDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    
    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AuthPayloadDTO changePassword(@Valid @Argument("input") ChangePasswordDTO input, Authentication authentication) {
        String email = authentication.getName();
        return accountService.changePassword(email, input.getOldPassword(), input.getNewPassword());
    }
}