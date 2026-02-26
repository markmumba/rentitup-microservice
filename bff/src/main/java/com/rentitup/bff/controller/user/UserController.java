package com.rentitup.bff.controller.user;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

	@GrpcClient(value = "USER-SERVICE", forwardToken = true)
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	@Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user")
	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser() {
		String userId = getCurrentUserId();

		log.info("REST: Get current user: {}", userId);
		GetUserRequest request = GetUserRequest.newBuilder()
				.setId(userId)
				.build();
		UserResponse response = userServiceStub.getUser(request);
		return ResponseBuilder.success("User retrieved", response.getUser());
	}

	@Operation(summary = "Update current user profile", description = "Updates the profile of the authenticated user")
	@PutMapping("/me")
	public ResponseEntity<?> updateCurrentUser(@RequestBody UpdateUserRequest request) {
		String userId = getCurrentUserId();

		log.info("REST: Update current user: {}", userId);
		UpdateUserRequest.Builder builder = UpdateUserRequest.newBuilder()
				.setId(userId);

		if (request.hasFullname()) builder.setFullname(request.getFullname());
		if (request.hasPhone()) builder.setPhone(request.getPhone());
		if (request.hasBusinessLicense()) builder.setBusinessLicense(request.getBusinessLicense());

		UserResponse response = userServiceStub.updateUser(builder.build());
		return ResponseBuilder.success("User updated", response.getUser());
	}

	@Operation(summary = "Get user by ID", description = "Returns a user by their ID (admin only)")
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getUser(@Parameter(description = "User ID") @PathVariable String id) {
		log.info("REST: Get user: {}", id);
		GetUserRequest request = GetUserRequest.newBuilder()
				.setId(id)
				.build();
		UserResponse response = userServiceStub.getUser(request);
		return ResponseBuilder.success("User retrieved", response.getUser());
	}

	@Operation(summary = "List users", description = "Returns a paginated list of users (admin only)")
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> listUsers(
			@Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Filter by role") @RequestParam(required = false) Role role,
			@Parameter(description = "Filter by KYC status") @RequestParam(required = false) KycStatus kycStatus) {
		log.info("REST: List users - page: {}, size: {}", page, size);

		ListUserRequest.Builder builder = ListUserRequest.newBuilder()
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build());

		if (role != null) builder.setRole(role);
		if (kycStatus != null) builder.setKyStatus(kycStatus);

		ListUsersResponse response = userServiceStub.listUsers(builder.build());

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
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> verifyUser(
			@Parameter(description = "User ID") @PathVariable String id,
			@RequestBody VerifyUserRequest request) {
		log.info("REST: Verify user: {}", id);

		VerifyUserRequest grpcRequest = VerifyUserRequest.newBuilder()
				.setId(id)
				.setKycStatus(request.getKycStatus())
				.build();

		UserResponse response = userServiceStub.verifyUser(grpcRequest);
		return ResponseBuilder.success("User verification updated", response.getUser());
	}

	private String getCurrentUserId() {
		JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		assert auth != null;
		Jwt jwt = auth.getToken();
		return jwt.getClaimAsString("user_id");
	}
}
