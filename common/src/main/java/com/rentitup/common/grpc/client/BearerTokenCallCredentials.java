package com.rentitup.common.grpc.client;

import com.rentitup.common.security.SecurityUtils;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.Executor;

/**
 * CallCredentials implementation that forwards the current user's bearer token
 * from the Spring Security context to downstream gRPC services.
 */
public class BearerTokenCallCredentials extends CallCredentials {

	private static final Metadata.Key<String> AUTHORIZATION_KEY =
			Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

	@Override
	public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier applier) {
		executor.execute(() -> {
			try {
				Metadata headers = new Metadata();
				SecurityUtils.getCurrentTokenValue().ifPresent(token ->
						headers.put(AUTHORIZATION_KEY, "Bearer " + token)
				);
				applier.apply(headers);
			} catch (Exception e) {
				applier.fail(Status.UNAUTHENTICATED.withCause(e));
			}
		});
	}
}
