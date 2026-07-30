package com.rentitup.common.grpc.client;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class BearerTokenInterceptor implements ClientInterceptor {

	private final TokenResolver tokenResolver;

	static final Metadata.Key<String> AUTHORIZATION_KEY =
			Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);



	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
			MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
			Channel next) {
		return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
			@Override
			public void start(Listener<RespT> responseListener, Metadata headers) {
				String token = tokenResolver.resolveToken();

				if (token != null && !token.isEmpty()) {
					String authValue = token.startsWith("Bearer ") ? token : "Bearer " + token;
					headers.put(AUTHORIZATION_KEY, authValue);

					if (tokenResolver.hasUserToken()) {
						log.debug("[gRPC-CLIENT] Forwarding USER token to: {}",
								method.getFullMethodName());
					} else {
						log.debug("[gRPC-CLIENT] Using SERVICE token for: {}",
								method.getFullMethodName());
					}
				} else {
					log.info("[gRPC-CLIENT] No token available for: {}", method.getFullMethodName());
				}

				super.start(responseListener, headers);
			}
		};
	}
}
