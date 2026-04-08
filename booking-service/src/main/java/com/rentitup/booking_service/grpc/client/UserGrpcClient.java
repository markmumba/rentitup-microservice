package com.rentitup.booking_service.grpc.client;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.GetUserRequest;
import com.rentitup.shared.proto.user.User;
import com.rentitup.shared.proto.user.UserResponse;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import org.springframework.stereotype.Service;


@Service

public class UserGrpcClient {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub client;



	public User getUser(String userId) {
		GetUserRequest request = GetUserRequest.newBuilder()
				.setId(userId)
				.build();
		UserResponse response = client.getUser(request);
		return response.getUser();
	}
}

