package com.rentitup.common.grpc.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServiceTokenProvider {

	private final String clientId;
	private final String clientSecret;
	private final String tokenUri;
	private final RestTemplate restTemplate;


	private final Map<String,CachedToken> tokenCache = new ConcurrentHashMap<>();

	private static final long REFRESH_BUFFER_SECONDS = 60;

	public ServiceTokenProvider(String clientId, String clientSecret, String tokenUri) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tokenUri = tokenUri;
		this.restTemplate = new RestTemplate();
	}


	public String getToken(Set<String> scopes) {
		String scopeKey = String.join(" ", scopes);

		CachedToken cached = tokenCache.get(scopeKey);
		if (cached != null && !cached.isExpired()) {
			log.debug("Using cached service token for scope: {}", scopeKey);
			return cached.token;
		}
		log.info("Requesting new service token for scope: {}", scopeKey);

		return requestNewToken(scopes,scopeKey);
	}

	public String getToken() {
		return getToken(Set.of("internal"));
	}

	private synchronized String requestNewToken(Set<String> scopes,String scopeKey) {
		CachedToken cached = tokenCache.get(scopeKey);
		if (cached != null && !cached.isExpired()) {
			return cached.token;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		String auth = clientId + ":" + clientSecret;
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		headers.add("Authorization", "Basic " + encodedAuth);

		MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "client_credentials");
		body.add("scope", String.join(" ", scopes));

		HttpEntity<MultiValueMap<String,String>> request = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new RuntimeException("Failed to request service token: " + response.getStatusCode());
			}

			Map<String,Object> responseBody = response.getBody();
			String token = (String) responseBody.get("access_token");
			int expiresIn = (Integer) responseBody.get("expires_in");

			Instant expires = Instant.now().plusSeconds(expiresIn - REFRESH_BUFFER_SECONDS);
			tokenCache.put(scopeKey, new CachedToken(token, expires));
			return token;
		} catch (Exception e) {
			log.error("Failed to obtain service token",e);
			throw new RuntimeException("Failed to obtain service token", e);
		}

	}

	public void clearTokenCache() {
		tokenCache.clear();
	}

	private static class CachedToken {
		final String token;
		final Instant expiration;

		CachedToken(String token, Instant expiration) {
			this.token = token;
			this.expiration = expiration;
		}
		boolean isExpired() {
			return Instant.now().isAfter(expiration);
		}
	}
}
