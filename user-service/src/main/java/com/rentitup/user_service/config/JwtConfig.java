package com.rentitup.user_service.config;

import com.rentitup.common.security.JwtProperties;
import com.rentitup.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

	@Value("${jwt.secret-key}")
	private String secretKey;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpiration;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	@Value("${jwt.issuer}")
	private String issuer;

	@Bean
	public JwtProperties jwtProperties() {
		JwtProperties properties = new JwtProperties(secretKey);
		properties.setAccessTokenExpiration(accessTokenExpiration);
		properties.setRefreshTokenExpiration(refreshTokenExpiration);
		properties.setIssuer(issuer);
		return properties;
	}

	@Bean
	public JwtUtil jwtUtil(JwtProperties jwtProperties) {
		return new JwtUtil(jwtProperties);
	}
}
