package io.shipkit.gatewayapi.gatewayapi.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CertificateIssuanceException extends BadRequestException {
    public CertificateIssuanceException(String message) {
        super(message);
    }
} 