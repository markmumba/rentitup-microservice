package com.rentitup.shared_libs.security;

public class JwtProperties {
	private String secretKey;
	private long accessTokenExpiration = 3600000; // 1 hour in ms
	private long refreshTokenExpiration = 604800000; // 7 days in ms
	private String issuer = "rentitup";

	public JwtProperties() {
	}

	public JwtProperties(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public long getAccessTokenExpiration() {
		return accessTokenExpiration;
	}

	public void setAccessTokenExpiration(long accessTokenExpiration) {
		this.accessTokenExpiration = accessTokenExpiration;
	}

	public long getRefreshTokenExpiration() {
		return refreshTokenExpiration;
	}

	public void setRefreshTokenExpiration(long refreshTokenExpiration) {
		this.refreshTokenExpiration = refreshTokenExpiration;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}
