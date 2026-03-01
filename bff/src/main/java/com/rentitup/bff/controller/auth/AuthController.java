package com.rentitup.bff.controller.auth;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {

	@Value("${app.frontend-url}")
	private String frontendUrl;

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	/**
	 * Initiates OAuth2 login flow.
	 * Frontend should redirect user to this endpoint to start login.
	 * Spring Security will handle the redirect to the auth server.
	 */
	@GetMapping("/login")
	@Operation(summary = "Initiate login", description = "Redirects to OAuth2 authorization server for login")
	public ResponseEntity<Void> login() {
		log.info("REST: Initiating OAuth2 login flow");
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create("/oauth2/authorization/bff-gateway"))
				.build();
	}

	/**
	 * Get current authenticated user info from session.
	 * This endpoint is used by the frontend to check if user is logged in
	 * and get user details without exposing the JWT.
	 */
	@GetMapping("/me")
	@Operation(summary = "Get current user", description = "Returns the currently authenticated user's information")
	public ResponseEntity<?> getCurrentUser(
			@AuthenticationPrincipal OAuth2User principal,
			@RegisteredOAuth2AuthorizedClient("bff-gateway") OAuth2AuthorizedClient authorizedClient) {

		if (principal == null) {
			return ResponseBuilder.unauthorized("Not authenticated");
		}

		log.info("REST: Getting current user info for: {}", principal.getName());

		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("id", principal.getName());
		userInfo.put("authenticated", true);

		// Extract claims from the OAuth2 user
		Map<String, Object> attributes = principal.getAttributes();
		if (attributes.containsKey("email")) {
			userInfo.put("email", attributes.get("email"));
		}
		if (attributes.containsKey("name")) {
			userInfo.put("name", attributes.get("name"));
		}
		if (attributes.containsKey("roles")) {
			userInfo.put("roles", attributes.get("roles"));
		}

		// If it's an OIDC user, we can get more info
		if (principal instanceof OidcUser oidcUser) {
			if (oidcUser.getEmail() != null) {
				userInfo.put("email", oidcUser.getEmail());
			}
			if (oidcUser.getFullName() != null) {
				userInfo.put("name", oidcUser.getFullName());
			}
		}

		// Include token expiry info (useful for frontend to know when to refresh)
		if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
			var expiresAt = authorizedClient.getAccessToken().getExpiresAt();
			if (expiresAt != null) {
				userInfo.put("tokenExpiresAt", expiresAt.toString());
			}
		}

		return ResponseBuilder.success("User info retrieved", userInfo);
	}

	/**
	 * Check authentication status without full user info.
	 * Lightweight endpoint for quick auth checks.
	 */
	@GetMapping("/status")
	@Operation(summary = "Check auth status", description = "Returns whether the user is authenticated")
	public ResponseEntity<?> getAuthStatus(@AuthenticationPrincipal OAuth2User principal) {
		Map<String, Object> status = new HashMap<>();
		status.put("authenticated", principal != null);
		if (principal != null) {
			status.put("userId", principal.getName());
		}
		return ResponseBuilder.success("Auth status", status);
	}

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
