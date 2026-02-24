package com.rentitup.bff.intercpetor;

import com.rentitup.common.security.SecurityUtils;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenForwardingClient implements ClientInterceptor {
	private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key
			.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);


	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		return new ForwardingClientCall.SimpleForwardingClientCall<>(
				next.newCall(method, callOptions)) {
						public void start(Listener<RespT> responseListener, Metadata headers) {
								SecurityUtils.getCurrentTokenValue().ifPresent(token -> {
								headers.put(AUTHORIZATION_HEADER, "Bearer " + token);
								log.debug("Forwarding to token: {}", method.getFullMethodName());
						});
						super.start(responseListener, headers);
				}
		};
	}
}
