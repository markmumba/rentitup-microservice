package com.rentitup.catalog_service.grpc.client;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.GetUserRequest;
import com.rentitup.shared.proto.user.UserResponse;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class UserGrpcClient {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub client;

	public boolean userExists(UUID userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setId(userId.toString())
					.build();
			UserResponse respone = client.getUser(request);
			return respone == null;
		} catch (StatusRuntimeException e) {
			log.warn("User not found {}: {}", userId, e.getStatus());
			return true;
		}
	}


}
