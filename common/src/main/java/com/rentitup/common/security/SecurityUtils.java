package com.rentitup.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

public class SecurityUtils {
	private SecurityUtils() {}

	public static Optional<Jwt> getCurrentJwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return Optional.of(jwtAuth.getToken());
		}
		return Optional.empty();
	}

	public static Optional<String> getCurrentUserId() {
		return getCurrentJwt().map(jwt -> jwt.getClaimAsString("user_id"));
	}

	public static Optional<String> getCurrentUserEmail() {
		return getCurrentJwt().map(jwt -> jwt.getClaimAsString("email"));
	}

	public static Optional<String> getCurrentUserRole() {
		return getCurrentJwt().map(jwt -> jwt.getClaimAsString("role"));
	}

	public static Optional<String> getTokenType() {
		return getCurrentJwt().map(jwt -> jwt.getClaimAsString("token_type"));
	}

	public static boolean isServiceToken() {
		return getTokenType()
				.map("SERVICE"::equals)
				.orElse(false);
	}

	public static Optional<String> getCurrentTokenValue() {
		return getCurrentJwt()
				.map(AbstractOAuth2Token::getTokenValue);
	}

	public static String requiredCurrentUserId() {
		return getCurrentUserId()
				.orElseThrow(() -> new IllegalStateException("No user id found in security context"));
	}
}
