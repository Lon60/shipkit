package io.shipkit.gatewayapi.gatewayapi.core.exceptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @GraphQlExceptionHandler(MethodArgumentNotValidException.class)
    public GraphQLError handleMethodArgNotValid(
            MethodArgumentNotValidException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((FieldError) err).getField();
            String msg = err.getDefaultMessage();
            fieldErrors.put(field, msg);
        });
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.BAD_REQUEST.value());
        error.setMessage("Input validation error");
        error.setTimestamp(new Date());
        error.setErrors(fieldErrors);
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(ConstraintViolationException.class)
    public GraphQLError handleConstraintViolation(
            ConstraintViolationException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            violations.put(cv.getPropertyPath().toString(), cv.getMessage());
        }
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.BAD_REQUEST.value());
        error.setMessage("Validation failed");
        error.setTimestamp(new Date());
        error.setErrors(violations);
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(AlreadyExistsException.class)
    public GraphQLError handleAlreadyExists(
            AlreadyExistsException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.CONFLICT.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(InternalServerException.class)
    public GraphQLError handleInternalServer(
            InternalServerException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(ResourceNotFoundException.class)
    public GraphQLError handleResourceNotFound(
            ResourceNotFoundException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.NOT_FOUND.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(NoResourceFoundException.class)
    public GraphQLError handleNoResourceFound(
            NoResourceFoundException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.NOT_FOUND.value());
        error.setMessage("Resource not found");
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(BadRequestException.class)
    public GraphQLError handleBadRequest(
            BadRequestException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.BAD_REQUEST.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(UnauthorizedException.class)
    public GraphQLError handleUnauthorized(
            UnauthorizedException ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message(error.getMessage())
                .errorType(ErrorType.UNAUTHORIZED)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }

    @GraphQlExceptionHandler(Exception.class)
    public GraphQLError handleGeneric(
            Exception ex,
            DataFetchingEnvironment env,
            GraphqlErrorBuilder<?> builder) {
        ErrorObject error = new ErrorObject();
        error.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setMessage(ex.getMessage());
        error.setTimestamp(new Date());
        return builder
                .message("An unexpected error occurred: " + error.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .extensions(Map.of(ERROR_KEY, error))
                .path(env.getExecutionStepInfo().getPath())
                .build();
    }
}