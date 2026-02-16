package com.rentitup.bff.grpc;

import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcStubFactory {

	private final GrpcChannelFactory channelFactory;

	private static final String CATALOG_SERVICE = "CATALOG-SERVICE";

	public CatalogServiceGrpc.CatalogServiceBlockingStub getCatalogStub() {
		ManagedChannel channel = channelFactory.getChannel(CATALOG_SERVICE);
		return CatalogServiceGrpc.newBlockingStub(channel);
	}
}
