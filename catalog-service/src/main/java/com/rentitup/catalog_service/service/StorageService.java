package com.rentitup.catalog_service.service;

import java.util.UUID;

public interface StorageService {

	record UploadUrlResult(
		String uploadUrl,
		String objectKey,
		String publicUrl,
		long expiresInSeconds
	) {}

	UploadUrlResult generateUploadUrl(UUID machineId, String filename, String contentType);

	void deleteObject(String objectKey);
}