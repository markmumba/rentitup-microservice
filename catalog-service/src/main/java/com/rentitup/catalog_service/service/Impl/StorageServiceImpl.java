package com.rentitup.catalog_service.service.Impl;

import com.rentitup.catalog_service.config.StorageProperties;
import com.rentitup.catalog_service.service.StorageService;
import com.rentitup.common.exceptions.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

	private final S3Presigner s3Presigner;
	private final S3Client s3Client;
	private final StorageProperties storageProperties;

	@Override
	public UploadUrlResult generateUploadUrl(UUID machineId, String filename, String contentType) {
		String extension = getExtension(filename);
		String objectKey = "machines/%s/%s%s".formatted(
			machineId,
			UUID.randomUUID(),
			extension
		);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(storageProperties.bucket())
			.key(objectKey)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.putObjectRequest(putObjectRequest)
			.signatureDuration(Duration.ofSeconds(storageProperties.uploadUrlExpirationSeconds()))
			.build();

		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
		String publicUrl = "%s/%s/%s".formatted(
			storageProperties.endpoint(),
			storageProperties.bucket(),
			objectKey
		);

		log.debug("Generated upload URL for machine {}: {}", machineId, objectKey);

		return new UploadUrlResult(
			uploadUrl,
			objectKey,
			publicUrl,
			storageProperties.uploadUrlExpirationSeconds()
		);
	}

	@Override
	public void deleteObject(String objectKey) {
		try {

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
					.bucket(storageProperties.bucket())
					.key(objectKey)
					.build();

			s3Client.deleteObject(deleteRequest);
			log.debug("Deleted object: {}", objectKey);
		} catch (SdkException e) {
			log.error("Failed to delete storage object {}", objectKey, e);
			throw new ServiceUnavailableException("Could not delete storage object " + objectKey, e);
		}

	}

	private String getExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf("."));
	}
}
