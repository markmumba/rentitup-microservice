package com.rentitup.bff.grpc;

import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import com.rentitup.common.grpc.GrpcChannelFactory;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcClientFactory {

	private final GrpcChannelFactory channelFactory;

	private static final String CATALOG_SERVICE = "CATALOG-SERVICE";
	private static final String USER_SERVICE = "USER-SERVICE";

	public CatalogServiceGrpc.CatalogServiceBlockingStub getCatalogClient() {
		ManagedChannel channel = channelFactory.getChannel(CATALOG_SERVICE);
		return CatalogServiceGrpc.newBlockingStub(channel);
	}

	public UserServiceGrpc.UserServiceBlockingStub getUserClient() {
		ManagedChannel channel = channelFactory.getChannel(USER_SERVICE);
		return UserServiceGrpc.newBlockingStub(channel);
	}
}
