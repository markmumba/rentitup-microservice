package com.rentitup.common.grpc.client;

import com.rentitup.common.security.TokenResolver;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;


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
				Set<String> scopes = determineScopesForMethod(method);
				String token = tokenResolver.resolveToken(scopes);

				if (token != null && !token.isEmpty()) {
					String authValue = token.startsWith("Bearer ") ? token : "Bearer " + token;
					headers.put(AUTHORIZATION_KEY, authValue);

					if (tokenResolver.hasUserToken()) {
						log.debug("Forwarding user token to: {}", method.getFullMethodName());
					} else {
						log.debug("Using service token for: {}", method.getFullMethodName());
					}
				}

				super.start(responseListener, headers);
			}
		};
	}


	private Set<String> determineScopesForMethod(MethodDescriptor<?, ?> method) {
		String serviceName = method.getServiceName();

		if (serviceName.contains("Catalog")) {
			return Set.of("internal", "machine:read");
		} else if (serviceName.contains("User")) {
			return Set.of("internal", "user:read");
		} else if (serviceName.contains("Booking")) {
			return Set.of("internal", "booking:write");
		}

		return Set.of("internal");
	}

}
