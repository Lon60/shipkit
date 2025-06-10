package io.shipkit.gatewayapi.gatewayapi.core.exceptions;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, @NonNull DataFetchingEnvironment env) {
        return switch (ex) {
            case AlreadyExistsException ignored ->
                    toGraphQLError(HttpStatus.CONFLICT, ex.getMessage(), env);
            case ResourceNotFoundException ignored ->
                    toGraphQLError(HttpStatus.NOT_FOUND, ex.getMessage(), env);
            case BadRequestException ignored ->
                    toGraphQLError(HttpStatus.BAD_REQUEST, ex.getMessage(), env);
            case UnauthorizedException ignored ->
                    toGraphQLError(HttpStatus.UNAUTHORIZED, ex.getMessage(), env);
            default -> toGraphQLError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage(), env);
        };

    }

    private GraphQLError toGraphQLError(HttpStatus status, String message, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of(
                        "statusCode", status.value(),
                        "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
