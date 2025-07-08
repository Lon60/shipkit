package io.shipkit.gatewayapi.gatewayapi.core.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class ErrorObject {
    private Integer statusCode;
    private String message;
    private Date timestamp;
    private Map<String, String> errors;
    private ErrorCode code;
}