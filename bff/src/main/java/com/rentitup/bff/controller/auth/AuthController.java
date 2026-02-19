package com.rentitup.bff.controller.auth;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.bff.grpc.GrpcClientFactory;
import com.rentitup.shared.proto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

	private final GrpcClientFactory grpcClient;

	@Operation(summary = "Register a new user", description = "Creates a new user account and returns authentication tokens")
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
		log.info("REST: Register user: {}", request.getEmail());
		AuthResponse response = grpcClient.getUserClient().register(request);
		return ResponseBuilder.created("Registration successful", response);
	}

	@Operation(summary = "Login", description = "Authenticates a user and returns authentication tokens")
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
		log.info("REST: Login user: {}", request.getEmail());
		AuthResponse response = grpcClient.getUserClient().login(request);
		return ResponseBuilder.success("Login successful", response);
	}

	@Operation(summary = "Refresh token", description = "Exchanges a refresh token for new authentication tokens")
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
		log.info("REST: Refresh token");
		AuthResponse response = grpcClient.getUserClient().refreshToken(request);
		return ResponseBuilder.success("Token refreshed", response);
	}

	@Operation(summary = "Logout", description = "Invalidates the user's refresh token")
	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
		log.info("REST: Logout");
		LogoutResponse response = grpcClient.getUserClient().logout(request);
		return ResponseBuilder.success(response.getMessage(), null);
	}
}
