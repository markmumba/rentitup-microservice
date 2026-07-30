package com.rentitup.bff.config;

import com.rentitup.common.grpc.client.GrpcContextTokenSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.Optional;

@Configuration
public class GrpcUserTokenConfig {

	@Bean
	public GrpcContextTokenSupplier grpcContextTokenSupplier(
			OAuth2AuthorizedClientService authorizedClientService) {
		return new GrpcContextTokenSupplier() {
			@Override
			public Optional<String> getCurrentToken() {
				Optional<String> resourceServerToken = super.getCurrentToken();
				if (resourceServerToken.isPresent()) {
					return resourceServerToken;
				}

				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
					return Optional.empty();
				}

				var authorizedClient = authorizedClientService.loadAuthorizedClient(
						oauthToken.getAuthorizedClientRegistrationId(),
						oauthToken.getName());
				if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
					return Optional.empty();
				}

				return Optional.of(authorizedClient.getAccessToken().getTokenValue());
			}
		};
	}
}
