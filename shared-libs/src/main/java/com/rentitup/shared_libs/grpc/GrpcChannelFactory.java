package com.rentitup.shared_libs.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcChannelFactory {

	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

	public ManagedChannel getChannel(String serviceName) {
		return channels.computeIfAbsent(serviceName, this::createChannel);
	}

	public ManagedChannel createChannel(String serviceName) {
		return ManagedChannelBuilder
				.forTarget("eureka:///" + serviceName)
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
