package com.rentitup.bff.security;

import java.util.UUID;

public class UserContext {

	private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

	private final UUID userId;
	private final String email;
	private final String role;

	public UserContext(UUID userId, String email, String role) {
		this.userId = userId;
		this.email = email;
		this.role = role;
	}

	public static void set(UserContext context) {
		CONTEXT.set(context);
	}

	public static UserContext get() {
		return CONTEXT.get();
	}

	public static void clear() {
		CONTEXT.remove();
	}

	public static boolean isAuthenticated() {
		return CONTEXT.get() != null;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public boolean hasRole(String role) {
		return this.role != null && this.role.equals(role);
	}

	public boolean isAdmin() {
		return hasRole("ADMIN");
	}

	public boolean isOwner() {
		return hasRole("OWNER");
	}

	public boolean isCustomer() {
		return hasRole("CUSTOMER");
	}
}
