package com.rentitup.bff.grpc;

import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcStubFactory {

	private final GrpcChannelFactory channelFactory;

	private static final String CATALOG_SERVICE = "CATALOG-SERVICE";
	private static final String USER_SERVICE = "USER-SERVICE";

	public CatalogServiceGrpc.CatalogServiceBlockingStub getCatalogStub() {
		ManagedChannel channel = channelFactory.getChannel(CATALOG_SERVICE);
		return CatalogServiceGrpc.newBlockingStub(channel);
	}

	public UserServiceGrpc.UserServiceBlockingStub getUserStub() {
		ManagedChannel channel = channelFactory.getChannel(USER_SERVICE);
		return UserServiceGrpc.newBlockingStub(channel);
	}
}
