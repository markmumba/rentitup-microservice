package com.rentitup.bff.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Locale;

public final class SecurityUtils {

	private SecurityUtils() {}

	public static String getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new IllegalStateException("No authenticated user found in security context");
		}

		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return requireUserId(jwtAuth.getToken().getClaimAsString("user_id"));
		}

		if (auth instanceof OAuth2AuthenticationToken oauthAuth) {
			OAuth2User principal = oauthAuth.getPrincipal();
			return requireUserId(principal.getAttribute("user_id"));
		}

		throw new IllegalStateException("Unsupported authentication type: " + auth.getClass().getName());
	}

	public static String requiredCurrentUserId() {
		return getCurrentUserId();
	}

	public static boolean hasRole(String role) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return false;
		}
		String authority = "ROLE_" + role.toUpperCase(Locale.ROOT);
		return auth.getAuthorities().stream()
				.anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
	}

	private static String requireUserId(String userId) {
		if (userId == null || userId.isBlank()) {
			throw new IllegalStateException("No user id found in security context");
		}
		return userId;
	}
}
