package com.rentitup.bff.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrpcChannelFactory {

	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

	public ManagedChannel getChannel(String serviceName) {
		return channels.computeIfAbsent(serviceName,this::createChannel);
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
		channels.values().forEach(channels -> {
			try {
				channels.shutdown().awaitTermination(5, TimeUnit.SECONDS);
			}catch (Exception e) {
				channels.shutdownNow();
			}
		});
	}
}
