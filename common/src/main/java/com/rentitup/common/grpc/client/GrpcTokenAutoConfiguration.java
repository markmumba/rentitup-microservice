package com.rentitup.common.grpc.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(GrpcClientProperties.class)
public class GrpcTokenAutoConfiguration {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrpcTokenAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public GrpcContextTokenSupplier grpcContextTokenSupplier() {
		log.info("[gRPC-CONFIG] GrpcContextTokenSupplier created");
		return new GrpcContextTokenSupplier();
	}

	/**
	 * Creates ServiceTokenProvider only if OAuth2 client config exists for either
	 * the default 'auth-server' registration OR a custom registration.
	 */
	@Bean
	@ConditionalOnMissingBean
	@Conditional(OAuth2ClientConfiguredCondition.class)
	public ServiceTokenProvider serviceTokenProvider(Environment env, GrpcClientProperties properties) {
		String registration = properties.getOauth2Registration();

		String clientId = env.getProperty("spring.security.oauth2.client.registration." + registration + ".client-id");
		String clientSecret = env.getProperty("spring.security.oauth2.client.registration." + registration + ".client-secret");
		String tokenUri = env.getProperty("spring.security.oauth2.client.provider." + registration + ".token-uri");

		log.info("[gRPC-CONFIG] ServiceTokenProvider created for client: {} (registration: {})", clientId, registration);
		return new ServiceTokenProvider(clientId, clientSecret, tokenUri);
	}

	@Bean
	@ConditionalOnMissingBean
	public TokenResolver tokenResolver(
			GrpcContextTokenSupplier tokenSupplier,
			ObjectProvider<ServiceTokenProvider> serviceTokenProviderProvider) {

		ServiceTokenProvider serviceTokenProvider = serviceTokenProviderProvider.getIfAvailable();

		if (serviceTokenProvider != null) {
			log.info("[gRPC-CONFIG] TokenResolver created WITH ServiceTokenProvider");
			return new TokenResolver(serviceTokenProvider, tokenSupplier::getCurrentToken);
		} else {
			log.info("[gRPC-CONFIG] TokenResolver created (user-only mode, no service token provider)");
			return new TokenResolver(tokenSupplier::getCurrentToken);
		}
	}

	/**
	 * Condition that checks if OAuth2 client credentials are configured.
	 * Matches if EITHER 'auth-server' OR custom registration has client-id configured.
	 */
	static class OAuth2ClientConfiguredCondition extends AnyNestedCondition {

		OAuth2ClientConfiguredCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.auth-server", name = "client-id")
		static class AuthServerConfigured {}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.bff-service", name = "client-id")
		static class BffServiceConfigured {}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.catalog-service", name = "client-id")
		static class CatalogServiceConfigured {}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.booking-service", name = "client-id")
		static class BookingServiceConfigured {}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.notification-service", name = "client-id")
		static class NotificationServiceConfigured {}

		@ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.cron-service", name = "client-id")
		static class CronServiceConfigured {}
	}
}
