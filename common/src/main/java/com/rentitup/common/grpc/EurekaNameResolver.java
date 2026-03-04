package com.rentitup.common.grpc;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.StatusOr;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EurekaNameResolver extends NameResolver {

	private final String serviceName;
	private final EurekaClient eurekaClient;
	private Listener2 listener;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final String PORT_METADATA_KEY = "grpc-port";

	public EurekaNameResolver(String serviceName, EurekaClient eurekaClient) {
		this.serviceName = serviceName;
		this.eurekaClient = eurekaClient;
	}

	@Override
	public String getServiceAuthority() {
		return serviceName;
	}

	@Override
	public void start(Listener2 listener) {
		this.listener = listener;
		resolver();
		scheduler.scheduleAtFixedRate(this::resolver, 10, 10, TimeUnit.SECONDS);
	}

	private void resolver() {
		List<InstanceInfo> instances = eurekaClient.getInstancesByVipAddress(serviceName, false);

		List<EquivalentAddressGroup> addressGroups = instances.stream()
				.map(instance -> {
					String host = instance.getHostName();
					int port = Integer.parseInt(
							instance.getMetadata().getOrDefault(PORT_METADATA_KEY, "9000")
					);
					return new EquivalentAddressGroup(
							new InetSocketAddress(host, port)
					);
				})
				.toList();

		if (addressGroups.isEmpty()) {
			listener.onError(Status.UNAVAILABLE.withDescription("No instances available for " + serviceName));
			return;
		}

		ResolutionResult result = ResolutionResult.newBuilder()
				.setAddressesOrError(StatusOr.fromValue(addressGroups))
				.build();
		listener.onResult(result);
	}

	@Override
	public void shutdown() {
		scheduler.shutdown();
	}
}
