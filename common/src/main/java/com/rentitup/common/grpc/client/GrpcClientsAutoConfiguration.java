package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.GrpcAutoConfiguration;
import com.rentitup.common.grpc.GrpcChannelFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;


@AutoConfiguration(
		after = GrpcTokenAutoConfiguration.class,
		before = GrpcAutoConfiguration.class
)
public class GrpcClientsAutoConfiguration {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrpcClientsAutoConfiguration.class);

	public GrpcClientsAutoConfiguration() {
		log.info("[gRPC-CONFIG] GrpcClientsAutoConfiguration loaded");
	}

	@Bean
	@ConditionalOnMissingBean
	public static GrpcClientBeanPostProcessor grpcClientBeanPostProcessor(ObjectProvider<GrpcChannelFactory> channelFactoryProvider) {
		return new GrpcClientBeanPostProcessor(channelFactoryProvider);
	}


	@Bean
	@ConditionalOnMissingBean(BearerTokenInterceptor.class)
	@Order(100)
	public BearerTokenInterceptor bearerTokenInterceptor(TokenResolver tokenResolver) {
		log.info("[gRPC-CONFIG] BearerTokenInterceptor registered as GlobalClientInterceptor");
		return new BearerTokenInterceptor(tokenResolver);
	}
}
