package com.rentitup.user_service.grpc.server;

import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.common.PaginationResponse;
import com.rentitup.shared.proto.user.*;
import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.mapper.UserMapper;
import com.rentitup.user_service.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpService extends UserServiceGrpc.UserServiceImplBase {

	private final UserService userService;
	private final UserMapper userMapper;

	// ==================== User Management ====================

	@Override
	public void createUser(CreateUserRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			log.info("gRPC: Create user: {}", request.getEmail());

			UserEntity user = UserEntity.builder()
					.email(request.getEmail())
					.password(request.getPassword())
					.fullName(request.getFullName())
					.phone(request.getPhone())
					.role(userMapper.mapUserType(request.getUserType()))
					.build();

			UserEntity created = userService.createUser(user);

			UserResponse response = UserResponse.newBuilder()
					.setUser(userMapper.toProto(created))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to create user", ex);
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			log.info("gRPC: Get user: {}", request.getId());

			UUID userId = UUID.fromString(request.getId());
			UserEntity user = userService.getUserById(userId);

			UserResponse response = UserResponse.newBuilder()
					.setUser(userMapper.toProto(user))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to get user", ex);
			responseObserver.onError(Status.NOT_FOUND
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			log.info("gRPC: Get user by email: {}", request.getEmail());

			UserEntity user = userService.getUserByEmail(request.getEmail());

			// Use toProtoWithHash for auth server requests that need password verification
			UserResponse response = UserResponse.newBuilder()
					.setUser(userMapper.toProtoWithHash(user))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to get user by email", ex);
			responseObserver.onError(Status.NOT_FOUND
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	public void updateUser(UpdateUserRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			log.info("gRPC: Update user: {}", request.getId());

			UUID userId = UUID.fromString(request.getId());

			UserEntity updates = UserEntity.builder()
					.fullName(request.hasFullname() ? request.getFullname() : null)
					.phone(request.hasPhone() ? request.getPhone() : null)
					.businessLicense(request.hasBusinessLicense() ? request.getBusinessLicense() : null)
					.build();

			UserEntity updated = userService.updateUser(userId, updates);

			UserResponse response = UserResponse.newBuilder()
					.setUser(userMapper.toProto(updated))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to update user", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	public void verifyUser(VerifyUserRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			log.info("gRPC: Verify user: {}", request.getId());

			UUID userId = UUID.fromString(request.getId());
			UserEntity user = userService.verifyUser(userId, userMapper.mapProtoKycStatus(request.getKycStatus()));

			UserResponse response = UserResponse.newBuilder()
					.setUser(userMapper.toProto(user))
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to verify user", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	public void listUsers(ListUserRequest request, StreamObserver<ListUsersResponse> responseObserver) {
		try {
			log.info("gRPC: List users");

			PaginationRequest pagination = request.getPagination();
			Pageable pageable = PageRequest.of(
					pagination.getPage() > 0 ? pagination.getPage() - 1 : 0,
					pagination.getSize() > 0 ? pagination.getSize() : 10
			);

			Page<UserEntity> page = userService.listUsers(
					request.hasUserType() ? userMapper.mapUserType(request.getUserType()) : null,
					request.hasKyStatus() ? userMapper.mapProtoKycStatus(request.getKyStatus()) : null,
					pageable
			);

			ListUsersResponse.Builder responseBuilder = ListUsersResponse.newBuilder()
					.setPagination(PaginationResponse.newBuilder()
							.setCurrentPage(page.getNumber() + 1)
							.setTotalPages(page.getTotalPages())
							.setTotalElements(page.getTotalElements())
							.setPageSize(page.getSize())
							.setHasNext(page.hasNext())
							.setHasPrevious(page.hasPrevious())
							.build());

			page.getContent().forEach(user ->
					responseBuilder.addUsers(userMapper.toProto(user)));

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();
		} catch (Exception ex) {
			log.error("Failed to list users", ex);
			responseObserver.onError(Status.INTERNAL
					.withDescription(ex.getMessage())
					.asRuntimeException());
		}
	}

}
