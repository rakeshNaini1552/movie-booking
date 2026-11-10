package com.movie.booking.catalog_service.advice;

import com.movie.booking.catalog_service.exception.InvalidRequestException;
import com.movie.booking.catalog_service.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

// GraphQL counterpart of CatalogApiExceptionHandler: a missing id becomes a NOT_FOUND error instead of INTERNAL_ERROR.
@ControllerAdvice
public class CatalogGraphQlExceptionHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBadRequest(InvalidRequestException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
    }
}
