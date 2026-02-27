package com.rentitup.common.security;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves authentication tokens for outgoing gRPC calls.
 *
 * <p>Priority:
 * <ol>
 *   <li>User token (forwarded from incoming request)</li>
 *   <li>Service token (from OAuth2 client credentials)</li>
 * </ol>
 */
@Slf4j
public class TokenResolver {

	private final ServiceTokenProvider serviceTokenProvider; // nullable
	private final Supplier<Optional<String>> userTokenSupplier;

	public TokenResolver(ServiceTokenProvider serviceTokenProvider,
						 Supplier<Optional<String>> userTokenSupplier) {
		this.serviceTokenProvider = serviceTokenProvider;
		this.userTokenSupplier = userTokenSupplier;
	}

	/**
	 * Creates a TokenResolver that only forwards user tokens (no service token fallback).
	 */
	public TokenResolver(Supplier<Optional<String>> userTokenSupplier) {
		this(null, userTokenSupplier);
	}

	public String resolveToken(Set<String> requiredScopes) {
		Optional<String> userToken = userTokenSupplier.get();
		if (userToken.isPresent()) {
			log.debug("Using user token for service call");
			return userToken.get();
		}

		if (serviceTokenProvider != null) {
			log.debug("No user token available, using service token");
			return serviceTokenProvider.getToken(requiredScopes);
		}

		log.warn("No user token and no service token provider configured");
		return null;
	}

	public String resolveToken() {
		return resolveToken(Set.of("internal"));
	}

	public boolean hasUserToken() {
		return userTokenSupplier.get().isPresent();
	}

	public boolean hasServiceTokenProvider() {
		return serviceTokenProvider != null;
	}
}
