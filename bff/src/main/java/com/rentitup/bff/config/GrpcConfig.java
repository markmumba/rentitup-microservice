package com.rentitup.bff.config;

import com.netflix.discovery.EurekaClient;
import com.rentitup.bff.grpc.nameResolver.EurekaNameResolverProvider;
import io.grpc.NameResolverRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GrpcConfig {
	private final EurekaClient eurekaClient;

	@PostConstruct
	public void registerResolver() {
		NameResolverRegistry.getDefaultRegistry()
				.register(new EurekaNameResolverProvider(eurekaClient));
	}
}

