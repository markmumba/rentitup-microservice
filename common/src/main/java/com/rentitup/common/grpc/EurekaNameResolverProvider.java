package com.rentitup.common.grpc;

import com.netflix.discovery.EurekaClient;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import java.net.URI;

public class EurekaNameResolverProvider extends NameResolverProvider {

	private final EurekaClient eurekaClient;

	public EurekaNameResolverProvider(EurekaClient eurekaClient) {
		this.eurekaClient = eurekaClient;
	}

	@Override
	protected boolean isAvailable() {
		return true;
	}

	@Override
	protected int priority() {
		return 5;
	}

	@Override
	public NameResolver newNameResolver(URI uri, NameResolver.Args args) {
		if (!"eureka".equals(uri.getScheme())) {
			return null;
		}
		String serviceName = uri.getPath().substring(1);
		return new EurekaNameResolver(serviceName, eurekaClient);
	}

	@Override
	public String getDefaultScheme() {
		return "eureka";
	}
}
