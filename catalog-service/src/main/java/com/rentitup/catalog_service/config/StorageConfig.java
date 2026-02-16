package com.rentitup.catalog_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	@Bean
	public S3Client s3Client(StorageProperties properties) {
		return S3Client.builder()
			.endpointOverride(URI.create(properties.endpoint()))
			.region(Region.of(properties.region()))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
			))
			.forcePathStyle(true) // Required for MinIO
			.build();
	}

	@Bean
	public S3Presigner s3Presigner(StorageProperties properties) {
		return S3Presigner.builder()
			.endpointOverride(URI.create(properties.endpoint()))
			.region(Region.of(properties.region()))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
			))
			.build();
	}
}