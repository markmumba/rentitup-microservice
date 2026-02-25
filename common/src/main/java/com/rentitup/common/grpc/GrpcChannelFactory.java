package com.rentitup.common.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcChannelFactory {

	private static final Logger log = LoggerFactory.getLogger(GrpcChannelFactory.class);
	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
	private static final String EUREKA_DNS="eureka:///";

	public ManagedChannel getChannel(String serviceName) {
		return channels.computeIfAbsent(serviceName, this::createChannel);
	}

	public ManagedChannel createChannel(String serviceName) {
		log.debug("Creating gRPC channel for service: {}", serviceName);
		return ManagedChannelBuilder
				.forTarget(EUREKA_DNS + serviceName)
				.defaultLoadBalancingPolicy("round_robin")
				.usePlaintext()
				.build();
	}

	public void refreshChannel(String serviceName) {
		ManagedChannel oldChannel = channels.remove(serviceName);
		if (oldChannel != null) {
			oldChannel.shutdown();
		}
	}

	@PreDestroy
	public void shutdown() {
		channels.values().forEach(channel -> {
			try {
				channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				channel.shutdownNow();
			}
		});
	}
}
