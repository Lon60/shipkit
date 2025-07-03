package io.shipkit.gatewayapi.gatewayapi.core.security.account.dto;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountInfoDTO {
    private UUID id;
    private String email;
    private List<String> authorities;

    public static AccountInfoDTO from(Account account) {
        List<String> authorities = account.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return new AccountInfoDTO(account.getId(), account.getEmail(), authorities);
    }
} 