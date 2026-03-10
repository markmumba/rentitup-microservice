package com.rentitup.common.grpc;

import com.rentitup.common.exceptions.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrpcExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GrpcExceptionHandler.class);

	private GrpcExceptionHandler() {
	}

	public static <T> void handleException(Exception ex, StreamObserver<T> responseObserver, String operation) {
		Status status = mapExceptionToStatus(ex);

		if (status.getCode() == Status.Code.INTERNAL) {
			log.error("Internal error during {}: {}", operation, ex.getMessage(), ex);
		} else {
			log.warn("{} failed: {} - {}", operation, status.getCode(), ex.getMessage());
		}

		responseObserver.onError(status.withDescription(ex.getMessage()).asRuntimeException());
	}

	public static Status mapExceptionToStatus(Exception ex) {
		return switch (ex) {
			case NotFoundException ignored ->  Status.NOT_FOUND;
			case BadRequestException ignored -> Status.INVALID_ARGUMENT;
			case ConflictException ignored -> Status.ALREADY_EXISTS;
			case UnauthorizedException ignored -> Status.UNAUTHENTICATED;
			case ForbiddenException ignored -> Status.PERMISSION_DENIED;
			case ServiceUnavailableException ignored -> Status.UNAVAILABLE;
			case IllegalArgumentException ignored -> Status.INVALID_ARGUMENT;
			default -> Status.INTERNAL;
		};
	}
}
