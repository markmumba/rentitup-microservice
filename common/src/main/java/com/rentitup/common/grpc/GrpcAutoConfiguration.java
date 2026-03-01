package com.rentitup.common.grpc;

import com.netflix.discovery.EurekaClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(EurekaClient.class)
public class GrpcAutoConfiguration {

	@Bean
	@ConditionalOnBean(EurekaClient.class)
	@ConditionalOnMissingBean
	public GrpcChannelFactory grpcChannelFactory(EurekaClient eurekaClient) {
		return new GrpcChannelFactory(eurekaClient);
	}
}
