package com.rentitup.bff.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class JwtAuthentication extends AbstractAuthenticationToken {

	private final UUID userId;
	private final String email;
	private final String role;

	public JwtAuthentication(UUID userId, String email, String role) {
		super(buildAuthorities(role));
		this.userId = userId;
		this.email = email;
		this.role = role;
		// Cal super.setAuthenticated(true) to bypass the deprecated warning
		// This is safe because we're setting it in the constructor after validation
		super.setAuthenticated(true);
	}

	@Override
	public void setAuthenticated(boolean authenticated) {
		if (authenticated) {
			throw new IllegalArgumentException("Cannot set this token to trusted - use constructor instead");
		}
		super.setAuthenticated(false);
	}

	private static Collection<? extends GrantedAuthority> buildAuthorities(String role) {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return userId;
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
}
