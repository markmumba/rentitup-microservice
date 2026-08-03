package com.rentitup.catalog_service.schedular;

import com.rentitup.catalog_service.entities.MinioOutbox;
import com.rentitup.catalog_service.enums.MinioOutboxStatus;
import com.rentitup.catalog_service.repository.MinioOutboxRepository;
import com.rentitup.catalog_service.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioCleanUpSchedular {

	private final MinioOutboxRepository minioOutboxRepository;
	private final StorageService storageService;

	@Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:60000}")
	public void deleteFromStorage() {
		List<MinioOutbox> pendingImages = minioOutboxRepository
				.findTop100ByStatusOrderByCreatedAtAsc(MinioOutboxStatus.PENDING);

		for (MinioOutbox image : pendingImages) {
			try {
				storageService.deleteObject(image.getObjectKey());
				image.setStatus(MinioOutboxStatus.DELETED);
				minioOutboxRepository.save(image);
			} catch (RuntimeException exception) {
				log.warn("MinIO cleanup failed for outbox item {}; it will be retried", image.getId(), exception);
			}
		}
	}
}
