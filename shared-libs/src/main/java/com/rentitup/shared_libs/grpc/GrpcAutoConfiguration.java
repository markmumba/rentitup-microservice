package com.rentitup.shared_libs.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.NameResolverRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(EurekaClient.class)
public class GrpcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GrpcChannelFactory grpcChannelFactory() {
		return new GrpcChannelFactory();
	}

	@Bean
	@ConditionalOnBean(EurekaClient.class)
	public EurekaNameResolverRegistrar eurekaNameResolverRegistrar(EurekaClient eurekaClient) {
		return new EurekaNameResolverRegistrar(eurekaClient);
	}


	public static class EurekaNameResolverRegistrar {

		private final EurekaClient eurekaClient;

		public EurekaNameResolverRegistrar(EurekaClient eurekaClient) {
			this.eurekaClient = eurekaClient;
		}

		@PostConstruct
		public void register() {
			NameResolverRegistry.getDefaultRegistry()
					.register(new EurekaNameResolverProvider(eurekaClient));
		}
	}
}
