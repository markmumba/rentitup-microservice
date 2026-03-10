package com.rentitup.bff.common.exception;

import com.rentitup.bff.common.response.ResponseBuilder;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GrpcExceptionAdvice {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<?> handleGrpcException(StatusRuntimeException ex) {
        String message = ex.getStatus().getDescription() != null
                ? ex.getStatus().getDescription()
                : ex.getStatus().getCode().name();

        return switch (ex.getStatus().getCode()) {
            case NOT_FOUND          -> ResponseBuilder.notFound(message);
            case INVALID_ARGUMENT   -> ResponseBuilder.badRequest(message);
            case ALREADY_EXISTS     -> ResponseBuilder.error(HttpStatus.CONFLICT, message);
            case UNAUTHENTICATED    -> ResponseBuilder.unauthorized(message);
            case PERMISSION_DENIED  -> ResponseBuilder.forbidden(message);
            case UNAVAILABLE        -> ResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE, message);
            default                 -> ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR, message);
        };
    }
}
