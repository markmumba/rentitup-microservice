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

					String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
					if (tokenResolver.hasUserToken()) {
						log.info("[gRPC-CLIENT] Forwarding USER token to: {} (token: {})",
								method.getFullMethodName(), tokenPreview);
					} else {
						log.info("[gRPC-CLIENT] Using SERVICE token for: {} (token: {})",
								method.getFullMethodName(), tokenPreview);
					}
				} else {
					log.info("[gRPC-CLIENT] No token available for: {}", method.getFullMethodName());
				}

				super.start(responseListener, headers);
			}
		};
	}
}
