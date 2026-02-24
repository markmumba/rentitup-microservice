package com.rentitup.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

	private final SecretKey secretKey;
	private final JwtProperties properties;

	public JwtUtil(JwtProperties properties) {
		this.properties = properties;
		this.secretKey = Keys.hmacShaKeyFor(properties.getSecretKey().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(UUID userId, String email, String role) {
		return generateToken(userId, email, role, "access", properties.getAccessTokenExpiration());
	}

	public String generateRefreshToken(UUID userId, String email, String role) {
		return generateToken(userId, email, role, "refresh", properties.getRefreshTokenExpiration());
	}

	private String generateToken(UUID userId, String email, String role, String tokenType, long expiration) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.claim("role", role)
				.claim("type", tokenType)
				.issuer(properties.getIssuer())
				.issuedAt(now)
				.expiration(expiryDate)
				.signWith(secretKey)
				.compact();
	}

	public JwtClaims validateToken(String token) throws JwtException {
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		return new JwtClaims(
				UUID.fromString(claims.getSubject()),
				claims.get("email", String.class),
				claims.get("role", String.class),
				claims.get("type", String.class)
		);
	}

	public JwtClaims validateAccessToken(String token) throws JwtException {
		JwtClaims claims = validateToken(token);
		if (!claims.isAccessToken()) {
			throw new JwtException("Invalid token type: expected access token");
		}
		return claims;
	}

	public JwtClaims validateRefreshToken(String token) throws JwtException {
		JwtClaims claims = validateToken(token);
		if (!claims.isRefreshToken()) {
			throw new JwtException("Invalid token type: expected refresh token");
		}
		return claims;
	}

	public boolean isTokenExpired(String token) {
		try {
			validateToken(token);
			return false;
		} catch (ExpiredJwtException e) {
			return true;
		} catch (JwtException e) {
			return true;
		}
	}

	public long getAccessTokenExpirationMs() {
		return properties.getAccessTokenExpiration();
	}

	public long getRefreshTokenExpirationMs() {
		return properties.getRefreshTokenExpiration();
	}
}
