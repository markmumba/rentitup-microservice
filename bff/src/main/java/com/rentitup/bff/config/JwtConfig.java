package com.rentitup.bff.config;

import com.rentitup.shared_libs.security.JwtProperties;
import com.rentitup.shared_libs.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

	@Value("${jwt.secret-key}")
	private String secretKey;

	@Bean
	public JwtProperties jwtProperties() {
		return new JwtProperties(secretKey);
	}

	@Bean
	public JwtUtil jwtUtil(JwtProperties jwtProperties) {
		return new JwtUtil(jwtProperties);
	}
}
