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
	private static final String EUREKA_SCHEME = "eureka:///";
	private static final String XDS_SCHEME    = "xds:///";

	private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
	private final EurekaClient eurekaClient;
	private final ObjectProvider<ClientInterceptor> interceptorProvider;
	private final boolean xdsEnabled;
	private final AtomicBoolean nameResolverRegistered = new AtomicBoolean(false);
	private volatile List<ClientInterceptor> resolvedInterceptors;

	public GrpcChannelFactory(EurekaClient eurekaClient,
	                          ObjectProvider<ClientInterceptor> interceptorProvider,
	                          boolean xdsEnabled) {
		this.eurekaClient        = eurekaClient;
		this.interceptorProvider = interceptorProvider;
		this.xdsEnabled          = xdsEnabled;
		log.info("GrpcChannelFactory initialized  resolver={}", xdsEnabled ? "xds" : "eureka");
	}

	private List<ClientInterceptor> getInterceptors() {
		if (resolvedInterceptors == null) {
			synchronized (this) {
				if (resolvedInterceptors == null) {
					resolvedInterceptors = interceptorProvider.orderedStream().toList();
					log.info("Resolved {} gRPC client interceptors", resolvedInterceptors.size());
				}
			}
		}
		return resolvedInterceptors;
	}

	public ManagedChannel getChannel(String serviceName) {
		if (!xdsEnabled) {
			ensureEurekaResolverRegistered();
		}
		return channels.computeIfAbsent(serviceName, this::createChannel);
	}

	private void ensureEurekaResolverRegistered() {
		if (nameResolverRegistered.compareAndSet(false, true)) {
			log.info("Registering EurekaNameResolverProvider");
			NameResolverRegistry.getDefaultRegistry()
					.register(new EurekaNameResolverProvider(eurekaClient));
		}
	}

	private ManagedChannel createChannel(String serviceName) {
		List<ClientInterceptor> interceptors = getInterceptors();
		String target = xdsEnabled
				? XDS_SCHEME + serviceName.toLowerCase()
				: EUREKA_SCHEME + serviceName;

		log.debug("Creating gRPC channel  target={}  interceptors={}", target, interceptors.size());

		ManagedChannelBuilder<?> builder = ManagedChannelBuilder
				.forTarget(target)
				.defaultLoadBalancingPolicy(xdsEnabled ? "xds_wrr_locality" : "round_robin")
				.usePlaintext();

		if (!interceptors.isEmpty()) {
			builder.intercept(interceptors);
		}

		return builder.build();
	}

	public void refreshChannel(String serviceName) {
		ManagedChannel old = channels.remove(serviceName);
		if (old != null) {
			old.shutdown();
		}
	}

	@PreDestroy
	public void shutdown() {
		channels.values().forEach(ch -> {
			try {
				ch.shutdown().awaitTermination(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				ch.shutdownNow();
			}
		});
	}
}
