package com.rentitup.auth_server.config;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.GetUserByEmailRequest;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.stream.Collectors;

@Configuration
@Slf4j
public class TokenCustomizer {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
		return context -> {
			String tokenType = context.getTokenType().getValue();
			boolean accessToken = "access_token".equals(tokenType);
			boolean idToken = "id_token".equals(tokenType);
			if (!accessToken && !idToken) {
				return;
			}

			Authentication principal = context.getPrincipal();
			var roles = principal.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.toSet());
			context.getClaims().claim("roles", roles);

			AuthorizationGrantType grantType = context.getAuthorizationGrantType();
			if (accessToken && grantType.equals(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
				context.getClaims().claim("token_type", "SERVICE");
				context.getClaims().claim("client_id",
						context.getRegisteredClient().getClientId()
				);

				log.debug("Created service token for: {}", context.getRegisteredClient().getClientId());
				return;
			}

			if (grantType.equals(AuthorizationGrantType.AUTHORIZATION_CODE)
					|| grantType.equals(AuthorizationGrantType.REFRESH_TOKEN)) {
				if (accessToken) {
					context.getClaims().claim("token_type", "USER");
				}

				String username = principal.getName();

				try {
					var user = userServiceStub.getUserByEmail(
							GetUserByEmailRequest.newBuilder()
									.setEmail(username)
									.build()
					).getUser();

					context.getClaims().claim("user_id", user.getId());
					context.getClaims().claim("role", user.getRole().name());
					context.getClaims().claim("email", user.getEmail());

					log.debug("Created {} with user identity for: {}", tokenType, username);
				} catch (Exception e) {
					log.error("Failed to get user by email: {}", username, e);
					throw new IllegalStateException("Cannot issue a user token without identity claims", e);
				}
			}
		};
	}
}
