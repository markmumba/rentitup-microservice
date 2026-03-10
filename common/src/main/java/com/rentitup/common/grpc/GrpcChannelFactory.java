package com.rentitup.common.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrpcChannelFactory {

	private static final Logger log = LoggerFactory.getLogger(GrpcChannelFactory.class);
	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
	private static final String EUREKA_DNS = "eureka:///";

	private final EurekaClient eurekaClient;
	private final ObjectProvider<ClientInterceptor> interceptorProvider;
	private final AtomicBoolean nameResolverRegistered = new AtomicBoolean(false);
	private volatile List<ClientInterceptor> resolvedInterceptors;

	public GrpcChannelFactory(EurekaClient eurekaClient, ObjectProvider<ClientInterceptor> interceptorProvider) {
		this.eurekaClient = eurekaClient;
		this.interceptorProvider = interceptorProvider;
		log.info("GrpcChannelFactory initialized");
	}

	private List<ClientInterceptor> getInterceptors() {
		if (resolvedInterceptors == null) {
			synchronized (this) {
				if (resolvedInterceptors == null) {
					resolvedInterceptors = interceptorProvider.orderedStream().toList();
					log.info("Resolved {} client interceptors for gRPC channels", resolvedInterceptors.size());
				}
			}
		}
		return resolvedInterceptors;
	}

	public ManagedChannel getChannel(String serviceName) {
		ensureNameResolverRegistered();
		return channels.computeIfAbsent(serviceName, this::createChannel);
	}

	private void ensureNameResolverRegistered() {
		if (nameResolverRegistered.compareAndSet(false, true)) {
			log.info("Registering Eureka NameResolverProvider with gRPC");
			NameResolverRegistry.getDefaultRegistry()
					.register(new EurekaNameResolverProvider(eurekaClient));
		}
	}

	public ManagedChannel createChannel(String serviceName) {
		List<ClientInterceptor> interceptors = getInterceptors();
		log.debug("Creating gRPC channel for service: {} with {} interceptors", serviceName, interceptors.size());
		ManagedChannelBuilder<?> builder = ManagedChannelBuilder
				.forTarget(EUREKA_DNS + serviceName)
				.defaultLoadBalancingPolicy("round_robin")
				.usePlaintext();

		if (!interceptors.isEmpty()) {
			builder.intercept(interceptors);
		}

		return builder.build();
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
