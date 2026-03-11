package com.rentitup.common.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.ClientInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration(after = EurekaClientAutoConfiguration.class)
@ConditionalOnClass(EurekaClient.class)
@EnableConfigurationProperties(GrpcAutoConfiguration.GrpcResolverProperties.class)
public class GrpcAutoConfiguration {

	@Bean
	@ConditionalOnBean(EurekaClient.class)
	@ConditionalOnMissingBean
	public GrpcChannelFactory grpcChannelFactory(EurekaClient eurekaClient,
	                                              ObjectProvider<ClientInterceptor> interceptorProvider,
	                                              GrpcResolverProperties props) {
		return new GrpcChannelFactory(eurekaClient, interceptorProvider, props.isXdsEnabled());
	}

	@ConfigurationProperties(prefix = "grpc.resolver.type")
	public static class GrpcResolverProperties {

		/**
		 * When true, channels use xds:/// and the gRPC xDS bootstrap file.
		 * Set GRPC_XDS_BOOTSTRAP env var to the path of bootstrap.json.
		 * When false (default), the existing Eureka name resolver is used.
		 */
		private boolean xdsEnabled = false;

		public boolean isXdsEnabled() { return xdsEnabled; }
		public void setXdsEnabled(boolean xdsEnabled) { this.xdsEnabled = xdsEnabled; }
	}
}
