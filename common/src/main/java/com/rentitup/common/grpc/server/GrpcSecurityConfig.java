package com.rentitup.common.grpc.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;


@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.grpc.server.security.GrpcSecurity")
@ConditionalOnProperty(prefix = "grpc.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GrpcSecurityConfig {

	@Bean
	@GlobalServerInterceptor
	public AuthenticationProcessInterceptor grpcSecurityFilterChain(GrpcSecurity grpc) throws Exception {
		return grpc
				.authorizeRequests(requests -> requests
						.allRequests().permitAll())
				.build();
	}
}
