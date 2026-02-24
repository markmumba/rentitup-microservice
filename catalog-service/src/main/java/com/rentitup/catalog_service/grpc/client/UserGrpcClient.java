package com.rentitup.catalog_service.grpc.client;

import com.rentitup.shared.proto.user.GetUserRequest;
import com.rentitup.shared.proto.user.UserResponse;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import com.rentitup.common.grpc.GrpcChannelFactory;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserGrpcClient {

	private static final String USER_SERVICE = "USER-SERVICE";

	private final GrpcChannelFactory channelFactory;

	private UserServiceGrpc.UserServiceBlockingStub getUserClient() {
		ManagedChannel channel = channelFactory.getChannel(USER_SERVICE);
		return UserServiceGrpc.newBlockingStub(channel);
	}

	public Optional<UserResponse> getUserById(UUID userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setId(userId.toString())
					.build();

			UserResponse response = getUserClient().getUser(request);
			return Optional.of(response);
		} catch (StatusRuntimeException e) {
			log.warn("Failed to get user {}: {}", userId, e.getStatus());
			return Optional.empty();
		}
	}

	public boolean userExists(UUID userId) {
		return getUserById(userId).isPresent();
	}
}
