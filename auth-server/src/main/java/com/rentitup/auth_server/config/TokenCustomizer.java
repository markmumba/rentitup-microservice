package com.rentitup.auth_server.config;

import com.rentitup.shared.proto.user.GetUserByEmailRequest;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import com.rentitup.common.grpc.GrpcChannelFactory;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TokenCustomizer {
	private static final String USER_SERVICE = "USER-SERVICE";

	private final GrpcChannelFactory channelFactory;

	private UserServiceGrpc.UserServiceBlockingStub getUserClient() {
		ManagedChannel channel = channelFactory.getChannel(USER_SERVICE);
		return UserServiceGrpc.newBlockingStub(channel);
	}

	@Bean
	public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
		return context -> {
			if(context.getTokenType().getValue().equals("access_token")) {
				Authentication principal = context.getPrincipal();

				var roles = principal.getAuthorities().stream()
						.map(GrantedAuthority::getAuthority)
						.collect(Collectors.toSet());

				context.getClaims().claim("roles", roles);
				AuthorizationGrantType grantType = context.getAuthorizationGrantType();

				if (grantType.equals(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
					context.getClaims().claim("token_type", "SERVICE");
					context.getClaims().claim("client_id",
							context.getRegisteredClient().getClientId()
					);

					log.debug("created service token for: {}",context.getRegisteredClient().getClientId());

				}else if (grantType.equals(AuthorizationGrantType.AUTHORIZATION_CODE)||
						grantType.equals(AuthorizationGrantType.REFRESH_TOKEN)) {
					context.getClaims().claim("token_type", "USER");

					String username = principal.getName();

					try {
						var user = getUserClient().getUserByEmail(GetUserByEmailRequest.newBuilder()
										.setEmail(username)
								.build()).getUser();

						context.getClaims().claim("user_id", user.getId());
						context.getClaims().claim("user_role", user.getUserType().name());
						context.getClaims().claim("email", user.getEmail());

						log.debug("Created user token for : {} ({})", username, user.getEmail());
					}catch (Exception e) {
						log.error("Failed to get user by email: {}",username, e);
					}
				}
			}
		};
	}
}
