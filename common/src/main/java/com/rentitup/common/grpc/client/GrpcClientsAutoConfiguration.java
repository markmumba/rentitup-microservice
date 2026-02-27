package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.GrpcChannelFactory;
import com.rentitup.common.security.TokenResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;


@AutoConfiguration
@ConditionalOnBean(GrpcChannelFactory.class)
public class GrpcClientsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public GrpcClientBeanPostProcessor grpcClientBeanPostProcessor(GrpcChannelFactory channelFactory) {
		return new GrpcClientBeanPostProcessor(channelFactory);
	}


	@Bean
	@ConditionalOnBean(TokenResolver.class)
	@ConditionalOnMissingBean(BearerTokenInterceptor.class)
	@GlobalClientInterceptor
	@Order(100)
	public BearerTokenInterceptor bearerTokenInterceptor(TokenResolver tokenResolver) {
		return new BearerTokenInterceptor(tokenResolver);
	}
}
