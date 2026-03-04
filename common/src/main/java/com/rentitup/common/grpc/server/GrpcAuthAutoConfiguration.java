package com.rentitup.common.grpc.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;


@AutoConfiguration
@EnableConfigurationProperties(GrpcAuthProperties.class)
@ConditionalOnClass(name = "org.springframework.grpc.server.GlobalServerInterceptor")
@ConditionalOnProperty(prefix = "grpc.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GrpcAuthAutoConfiguration {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrpcAuthAutoConfiguration.class);

	public GrpcAuthAutoConfiguration() {
		log.info("[gRPC-CONFIG] GrpcAuthAutoConfiguration loaded");
	}

	@Bean
	@ConditionalOnMissingBean(GrpcAuthInterceptor.class)
	@GlobalServerInterceptor
	@Order(50)
	public GrpcAuthInterceptor grpcAuthInterceptor(JwtDecoder jwtDecoder, GrpcAuthProperties properties) {
		log.info("[gRPC-CONFIG] GrpcAuthInterceptor registered with public methods: {}", properties.getPublicMethods());
		return new GrpcAuthInterceptor(jwtDecoder, properties.getPublicMethods());
	}
}
