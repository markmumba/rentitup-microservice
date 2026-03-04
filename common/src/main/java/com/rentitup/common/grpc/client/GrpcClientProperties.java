package com.rentitup.common.grpc.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grpc.client")
public class GrpcClientProperties {

	/**
	 * The name of the OAuth2 client registration to use for service-to-service tokens.
	 * This should match a registration under spring.security.oauth2.client.registration.*
	 * Default: "auth-server"
	 */
	private String oauth2Registration = "auth-server";

	public String getOauth2Registration() {
		return oauth2Registration;
	}

	public void setOauth2Registration(String oauth2Registration) {
		this.oauth2Registration = oauth2Registration;
	}
}
