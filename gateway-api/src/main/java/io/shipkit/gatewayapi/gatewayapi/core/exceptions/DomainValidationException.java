package io.shipkit.gatewayapi.gatewayapi.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DomainValidationException extends BadRequestException {
    public DomainValidationException(String message) {
        super(message);
    }
} 