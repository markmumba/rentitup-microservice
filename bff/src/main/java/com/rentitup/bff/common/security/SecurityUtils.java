package com.rentitup.bff.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

	private SecurityUtils() {}

	public static String getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			Jwt jwt = jwtAuth.getToken();
			return jwt.getClaimAsString("user_id");
		}

		if (auth instanceof OAuth2AuthenticationToken oauthAuth) {
			OAuth2User principal = oauthAuth.getPrincipal();
			if (principal.getAttribute("user_id") != null) {
				return principal.getAttribute("user_id");
			}
			return principal.getName();
		}

		throw new IllegalStateException("Unsupported authentication type: " + auth.getClass().getName());
	}
}
