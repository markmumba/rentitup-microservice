package com.rentitup.bff.controller.auth;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	@PostMapping("/register")
	@Operation(summary = "Register a new user", description = "Creates a new user account")
	public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
		log.info("REST: Register user: {}", request.getEmail());

		CreateUserRequest.Builder builder = request.toBuilder();
		if (request.getRole() == Role.ROLE_UNSPECIFIED) {
			builder.setRole(Role.CUSTOMER);
		}

		UserResponse response = userServiceStub.createUser(builder.build());
		return ResponseBuilder.created("User registered successfully", response.getUser());
	}

	@GetMapping("/check-email")
	@Operation(summary = "Check if email exists", description = "Check if an email is already registered")
	public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
		log.info("REST: Check email exists: {}", email);

		try {
			userServiceStub.getUserByEmail(
					GetUserByEmailRequest.newBuilder()
							.setEmail(email)
							.build()
			);
			return ResponseBuilder.success("Email check completed",
					new EmailCheckResponse(true, "Email is already registered"));
		} catch (Exception e) {
			return ResponseBuilder.success("Email check completed",
					new EmailCheckResponse(false, "Email is available"));
		}
	}

	public record EmailCheckResponse(boolean exists, String message) {}
}
