package com.rentitup.api_gateway.grpc;

import com.netflix.appinfo.InstanceInfo;
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

	private final EurekaClient eurekaClient;
	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

	public ManagedChannel getChannel(String serviceName) {
		return channels.computeIfAbsent(serviceName,this::createChannel);
	}

	public ManagedChannel createChannel(String serviceName) {
		InstanceInfo instance = eurekaClient.getNextServerFromEureka(serviceName, false);
		if  (instance == null) {
			throw new IllegalStateException("No instance found for service: " + serviceName);
		}
		String grpcPortStr = instance.getMetadata().get("grpcPort");
		int grpcPort = grpcPortStr != null ?Integer.parseInt(grpcPortStr) : 9000;

		String  host = instance.getHostName();

		log.info("Creating gRPC channel for {} at {}:{}", serviceName, host, grpcPort);
		return ManagedChannelBuilder
				.forAddress(host,grpcPort)
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
