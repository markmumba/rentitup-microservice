package com.rentitup.shared_libs.security;

import lombok.*;

import java.util.UUID;


public class JwtClaims {
	private final UUID userId;
	private final String email;
	private final String role;
	private final String tokenType;

	public JwtClaims(UUID userId, String email, String role, String tokenType) {
		this.userId = userId;
		this.email = email;
		this.role = role;
		this.tokenType = tokenType;
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

	public String getTokenType() {
		return tokenType;
	}

	public boolean isAccessToken() {
		return "access".equals(tokenType);
	}

	public boolean isRefreshToken() {
		return "refresh".equals(tokenType);
	}
}
