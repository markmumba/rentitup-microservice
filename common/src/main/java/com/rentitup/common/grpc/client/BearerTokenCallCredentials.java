package com.rentitup.common.grpc.client;

import com.rentitup.common.grpc.server.GrpcAuthContext;
import com.rentitup.common.security.SecurityUtils;
import io.grpc.CallCredentials;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

import static com.rentitup.common.grpc.server.GrpcAuthContext.TOKEN;

public class BearerTokenCallCredentials extends CallCredentials {

	private static final Metadata.Key<String> AUTHORIZATION_KEY =
			Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

	@Override
	public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier applier) {
		String grpcToken = GrpcAuthContext.getToken();
		executor.execute(() -> {
			try {
				Metadata headers = new Metadata();

				var springToken = SecurityUtils.getCurrentTokenValue();
				if (springToken.isPresent()) {
					headers.put(AUTHORIZATION_KEY, "Bearer " + springToken.get());
				}
				else if (grpcToken != null) {
					headers.put(AUTHORIZATION_KEY, "Bearer " + grpcToken);
				}

				applier.apply(headers);
			} catch (Exception e) {
				applier.fail(Status.UNAUTHENTICATED.withCause(e));
			}
		});
	}
}
