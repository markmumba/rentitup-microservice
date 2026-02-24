package com.rentitup.common.grpc.server;

import io.grpc.Context;

import java.util.Set;

/**
 * Context keys for accessing authenticated user information in gRPC handlers.
 *
 * <p>Usage in gRPC service:
 * <pre>
 * {@code
 * String userId = GrpcAuthContext.USER_ID.get();
 * if (GrpcAuthContext.isAuthenticated()) {
 *     // handle authenticated request
 * }
 * }
 * </pre>
 */
public final class GrpcAuthContext {

	private GrpcAuthContext() {
	}

	public static final Context.Key<String> USER_ID = Context.key("user-id");
	public static final Context.Key<String> USER_TYPE = Context.key("user-type");
	public static final Context.Key<String> USER_EMAIL = Context.key("user-email");
	public static final Context.Key<String> TOKEN_TYPE = Context.key("token-type");
	public static final Context.Key<Boolean> IS_AUTHENTICATED = Context.key("is-authenticated");
	public static final Context.Key<Set<String>> ROLES = Context.key("roles");

	/**
	 * Check if the current request is authenticated.
	 */
	public static boolean isAuthenticated() {
		Boolean authenticated = IS_AUTHENTICATED.get();
		return authenticated != null && authenticated;
	}

	/**
	 * Get the current user ID, or null if not authenticated.
	 */
	public static String getUserId() {
		return USER_ID.get();
	}

	/**
	 * Get the current user ID, throwing if not authenticated.
	 */
	public static String requireUserId() {
		String userId = USER_ID.get();
		if (userId == null) {
			throw new IllegalStateException("User ID not available - request not authenticated");
		}
		return userId;
	}

	/**
	 * Get the current user email, or null if not authenticated.
	 */
	public static String getUserEmail() {
		return USER_EMAIL.get();
	}

	/**
	 * Get the current user type, or null if not authenticated.
	 */
	public static String getUserType() {
		return USER_TYPE.get();
	}

	/**
	 * Get the token type (USER or SERVICE).
	 */
	public static String getTokenType() {
		return TOKEN_TYPE.get();
	}

	/**
	 * Check if the current token is a service token.
	 */
	public static boolean isServiceToken() {
		return "SERVICE".equals(TOKEN_TYPE.get());
	}

	/**
	 * Check if the current token is a user token.
	 */
	public static boolean isUserToken() {
		return "USER".equals(TOKEN_TYPE.get());
	}

	/**
	 * Get the roles from the current token.
	 */
	public static Set<String> getRoles() {
		Set<String> roles = ROLES.get();
		return roles != null ? roles : Set.of();
	}

	/**
	 * Check if the current user has a specific role.
	 */
	public static boolean hasRole(String role) {
		return getRoles().contains(role);
	}
}
