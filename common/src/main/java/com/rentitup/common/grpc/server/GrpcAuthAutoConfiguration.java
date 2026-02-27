package com.rentitup.common.grpc.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

	@Bean
	@ConditionalOnBean(JwtDecoder.class)
	@ConditionalOnMissingBean(GrpcAuthInterceptor.class)
	@GlobalServerInterceptor
	@Order(100)
	public GrpcAuthInterceptor grpcAuthInterceptor(JwtDecoder jwtDecoder, GrpcAuthProperties properties) {
		return new GrpcAuthInterceptor(jwtDecoder, properties.getPublicMethods());
	}
}
