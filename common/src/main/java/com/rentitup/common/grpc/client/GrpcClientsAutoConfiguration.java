package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.GrpcChannelFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;


@AutoConfiguration
@ConditionalOnBean(GrpcChannelFactory.class)
public class GrpcClientsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GrpcClientBeanPostProcessor grpcClientBeanPostProcessor(GrpcChannelFactory channelFactory) {
		return new GrpcClientBeanPostProcessor(channelFactory);
	}
}
