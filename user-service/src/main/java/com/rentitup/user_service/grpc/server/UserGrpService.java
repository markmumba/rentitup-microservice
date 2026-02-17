package com.rentitup.user_service.grpc.server;

import com.rentitup.shared.proto.user.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpService extends UserServiceGrpc.UserServiceImplBase {

	@Override
	public void createUser(CreateUserRequest request, StreamObserver<UserResponse> responseObserver) {
		super.createUser(request, responseObserver);
	}

	@Override
	public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
		super.getUser(request, responseObserver);
	}

	@Override
	public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<UserResponse> responseObserver) {
		super.getUserByEmail(request, responseObserver);
	}

	@Override
	public void listUserResponse(ListUserRequest request, StreamObserver<ListUsersResponse> responseObserver) {
		super.listUserResponse(request, responseObserver);
	}

	@Override
	public void verifyUser(VerifyUserRequest request, StreamObserver<UserResponse> responseObserver) {
		super.verifyUser(request, responseObserver);
	}
}
