package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.server.GrpcAuthContext;
import com.rentitup.common.security.SecurityUtils;

import java.util.Optional;


public class GrpcContextTokenSupplier {

	public Optional<String> getCurrentToken() {

		Optional<String> springToken = SecurityUtils.getCurrentTokenValue();
		if (springToken.isPresent()) {
			return springToken;
		}

		String grpcToken = GrpcAuthContext.getToken();
		return Optional.ofNullable(grpcToken);
	}
}
