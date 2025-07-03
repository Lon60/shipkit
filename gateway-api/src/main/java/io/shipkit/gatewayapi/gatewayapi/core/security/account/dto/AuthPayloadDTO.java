package io.shipkit.gatewayapi.gatewayapi.core.security.account.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthPayloadDTO {
    private String token;
    private AccountInfoDTO account;
}