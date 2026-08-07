package com.rentitup.booking_service.grpc.client;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.common.exceptions.BadRequestException;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.common.exceptions.ServiceUnavailableException;
import com.rentitup.shared.proto.user.GetUserRequest;
import com.rentitup.shared.proto.user.User;
import com.rentitup.shared.proto.user.UserResponse;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub client;



	public User getUser(String userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setId(userId)
					.build();
			UserResponse response = client.getUser(request);
			return response.getUser();
		} catch (StatusRuntimeException exception) {
			Status status = exception.getStatus();
			log.error("User service call failed for user {}: {}", userId, status);
			throw switch (status.getCode()) {
				case NOT_FOUND -> new NotFoundException("Customer not found: " + userId);
				case INVALID_ARGUMENT -> new BadRequestException("Invalid customer id: " + userId);
				case PERMISSION_DENIED, UNAUTHENTICATED ->
						new ForbiddenException("User service rejected access to customer: " + userId);
				default -> new ServiceUnavailableException("User service unavailable", exception);
			};
		}
	}
}
