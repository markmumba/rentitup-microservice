package com.rentitup.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Deprecated
@RequiredArgsConstructor
@Slf4j
public class TokenResolver {

	private final Supplier<Optional<String>> userTokenSupplier;
	private final ServiceTokenProvider serviceTokenProvider;


	public String resolveToken(Set<String> requiredScopes) {
		Optional<String> userToken = userTokenSupplier.get();
		if (userToken.isPresent()) {
			log.debug("Using user token for service call");
			return userToken.get();
		}
		log.debug("No user token available, using service token");
		return serviceTokenProvider.getToken(requiredScopes);

	}

	public String resolveToken() {
		return resolveToken(Set.of("internal"));
	}

	public boolean hasUserToken() {
		return userTokenSupplier.get().isPresent();
	}
}
