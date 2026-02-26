package com.rentitup.common.grpc.server;

import io.grpc.Context;

import java.util.Set;


public final class GrpcAuthContext {

	private GrpcAuthContext() {
	}

	public static final Context.Key<String> USER_ID = Context.key("user-id");
	public static final Context.Key<String> ROLE = Context.key("role");
	public static final Context.Key<String> USER_EMAIL = Context.key("user-email");
	public static final Context.Key<String> TOKEN_TYPE = Context.key("token-type");
	public static final Context.Key<Boolean> IS_AUTHENTICATED = Context.key("is-authenticated");
	public static final Context.Key<Set<String>> ROLES = Context.key("roles");
	public static final Context.Key<String> TOKEN = Context.key("token");

	public static boolean isAuthenticated() {
		Boolean authenticated = IS_AUTHENTICATED.get();
		return authenticated != null && authenticated;
	}

	public static String getUserId() {
		return USER_ID.get();
	}

	public static String requireUserId() {
		String userId = USER_ID.get();
		if (userId == null) {
			throw new IllegalStateException("User ID not available - request not authenticated");
		}
		return userId;
	}

	public static String getUserEmail() {
		return USER_EMAIL.get();
	}

	public static String getRole() {
		return ROLE.get();
	}

	public static String getTokenType() {
		return TOKEN_TYPE.get();
	}

	public static String getToken() {
		return TOKEN.get();
	}

	public static boolean isServiceToken() {
		return "SERVICE".equals(TOKEN_TYPE.get());
	}

	public static boolean isUserToken() {
		return "USER".equals(TOKEN_TYPE.get());
	}

	public static Set<String> getRoles() {
		Set<String> roles = ROLES.get();
		return roles != null ? roles : Set.of();
	}

	public static boolean hasRole(String role) {
		return getRoles().contains(role);
	}

}
