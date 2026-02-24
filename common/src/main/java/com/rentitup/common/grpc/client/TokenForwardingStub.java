package com.rentitup.common.grpc.client;

import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;

/**
 * Utility for wrapping gRPC stubs with bearer token forwarding.
 * Forwards the current user's JWT token from Spring Security context to downstream services.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * // Wrap a stub for a single call
 * var response = TokenForwardingStub.withToken(userServiceStub).getUser(request);
 * }
 * </pre>
 */
public final class TokenForwardingStub {

	private static final BearerTokenCallCredentials CREDENTIALS = new BearerTokenCallCredentials();

	private TokenForwardingStub() {
	}

	/**
	 * Wraps a blocking stub with bearer token forwarding credentials.
	 */
	public static <S extends AbstractBlockingStub<S>> S withToken(S stub) {
		return stub.withCallCredentials(CREDENTIALS);
	}

	/**
	 * Wraps a future stub with bearer token forwarding credentials.
	 */
	public static <S extends AbstractFutureStub<S>> S withToken(S stub) {
		return stub.withCallCredentials(CREDENTIALS);
	}

	/**
	 * Wraps any stub with bearer token forwarding credentials.
	 */
	@SuppressWarnings("unchecked")
	public static <S extends AbstractStub<S>> S withTokenGeneric(S stub) {
		return (S) stub.withCallCredentials(CREDENTIALS);
	}
}
