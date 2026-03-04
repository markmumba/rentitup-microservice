package com.rentitup.common.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.ClientInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = EurekaClientAutoConfiguration.class)
@ConditionalOnClass(EurekaClient.class)
public class GrpcAutoConfiguration {

	@Bean
	@ConditionalOnBean(EurekaClient.class)
	@ConditionalOnMissingBean
	public GrpcChannelFactory grpcChannelFactory(EurekaClient eurekaClient,
												  ObjectProvider<ClientInterceptor> interceptorProvider) {
		return new GrpcChannelFactory(eurekaClient, interceptorProvider);
	}
}
