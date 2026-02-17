package com.rentitup.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
	String endpoint,
	String accessKey,
	String secretKey,
	String bucket,
	String region,
	long uploadUrlExpirationSeconds
) {
	public StorageProperties {
		if (region == null) {
			region = "us-east-1";
		}
		if (uploadUrlExpirationSeconds <= 0) {
			uploadUrlExpirationSeconds = 3600; // 1 hour default
		}
	}
}