package com.rentitup.bff.controller.user;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.bff.grpc.GrpcStubFactory;
import com.rentitup.bff.security.UserContext;
import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

	private final GrpcStubFactory grpcStubFactory;

	@Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user")
	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser() {
		UserContext context = UserContext.get();
		if (context == null) {
			return ResponseBuilder.unauthorized("Not authenticated");
		}

		log.info("REST: Get current user: {}", context.getUserId());
		GetUserRequest request = GetUserRequest.newBuilder()
				.setId(context.getUserId().toString())
				.build();
		UserResponse response = grpcStubFactory.getUserStub().getUser(request);
		return ResponseBuilder.success("User retrieved", response.getUser());
	}

	@Operation(summary = "Update current user profile", description = "Updates the profile of the authenticated user")
	@PutMapping("/me")
	public ResponseEntity<?> updateCurrentUser(@RequestBody UpdateUserRequest request) {
		UserContext context = UserContext.get();
		if (context == null) {
			return ResponseBuilder.unauthorized("Not authenticated");
		}

		log.info("REST: Update current user: {}", context.getUserId());
		UpdateUserRequest.Builder builder = UpdateUserRequest.newBuilder()
				.setId(context.getUserId().toString());

		if (request.hasFullname()) builder.setFullname(request.getFullname());
		if (request.hasPhone()) builder.setPhone(request.getPhone());
		if (request.hasBusinessLicense()) builder.setBusinessLicense(request.getBusinessLicense());

		UserResponse response = grpcStubFactory.getUserStub().updateUser(builder.build());
		return ResponseBuilder.success("User updated", response.getUser());
	}

	@Operation(summary = "Get user by ID", description = "Returns a user by their ID (admin only)")
	@GetMapping("/{id}")
	public ResponseEntity<?> getUser(@Parameter(description = "User ID") @PathVariable String id) {
		log.info("REST: Get user: {}", id);
		GetUserRequest request = GetUserRequest.newBuilder()
				.setId(id)
				.build();
		UserResponse response = grpcStubFactory.getUserStub().getUser(request);
		return ResponseBuilder.success("User retrieved", response.getUser());
	}

	@Operation(summary = "List users", description = "Returns a paginated list of users (admin only)")
	@GetMapping
	public ResponseEntity<?> listUsers(
			@Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Filter by user type") @RequestParam(required = false) UserType userType,
			@Parameter(description = "Filter by KYC status") @RequestParam(required = false) KycStatus kycStatus) {
		log.info("REST: List users - page: {}, size: {}", page, size);

		ListUserRequest.Builder builder = ListUserRequest.newBuilder()
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build());

		if (userType != null) builder.setUserType(userType);
		if (kycStatus != null) builder.setKyStatus(kycStatus);

		ListUsersResponse response = grpcStubFactory.getUserStub().listUsers(builder.build());

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getUsersList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@Operation(summary = "Verify user KYC", description = "Updates a user's KYC verification status (admin only)")
	@PostMapping("/{id}/verify")
	public ResponseEntity<?> verifyUser(
			@Parameter(description = "User ID") @PathVariable String id,
			@RequestBody VerifyUserRequest request) {
		log.info("REST: Verify user: {}", id);

		VerifyUserRequest grpcRequest = VerifyUserRequest.newBuilder()
				.setId(id)
				.setKycStatus(request.getKycStatus())
				.build();

		UserResponse response = grpcStubFactory.getUserStub().verifyUser(grpcRequest);
		return ResponseBuilder.success("User verification updated", response.getUser());
	}
}
