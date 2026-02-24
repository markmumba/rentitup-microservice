package com.rentitup.bff.grpc;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class GrpcClientFactory {

	@GrpcClient("CATALOG-SERVICE")
	private CatalogServiceGrpc.CatalogServiceBlockingStub catalogServiceStub;

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	public CatalogServiceGrpc.CatalogServiceBlockingStub getCatalogClient() {
		return catalogServiceStub;
	}

	public UserServiceGrpc.UserServiceBlockingStub getUserClient() {
		return userServiceStub;
	}
}
