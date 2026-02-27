package com.rentitup.common.grpc.client;

import com.rentitup.common.security.ServiceTokenProvider;
import com.rentitup.common.security.TokenResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class GrpcTokenAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GrpcContextTokenSupplier grpcContextTokenSupplier() {
		return new GrpcContextTokenSupplier();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.auth-server", name = "client-id")
	public ServiceTokenProvider serviceTokenProvider(
			@Value("${spring.security.oauth2.client.registration.auth-server.client-id}") String clientId,
			@Value("${spring.security.oauth2.client.registration.auth-server.client-secret}") String clientSecret,
			@Value("${spring.security.oauth2.client.provider.auth-server.token-uri}") String tokenUri) {
		return new ServiceTokenProvider(clientId, clientSecret, tokenUri);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.auth-server", name = "client-id")
	public TokenResolver tokenResolverWithServiceProvider(
			ServiceTokenProvider serviceTokenProvider,
			GrpcContextTokenSupplier tokenSupplier) {
		return new TokenResolver(serviceTokenProvider, tokenSupplier::getCurrentToken);
	}


	@Bean
	@ConditionalOnMissingBean(TokenResolver.class)
	public TokenResolver tokenResolverUserOnly(GrpcContextTokenSupplier tokenSupplier) {
		return new TokenResolver(tokenSupplier::getCurrentToken);
	}
}
