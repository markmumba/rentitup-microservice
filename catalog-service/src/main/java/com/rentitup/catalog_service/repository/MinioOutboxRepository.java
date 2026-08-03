package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MinioOutbox;
import com.rentitup.catalog_service.enums.MinioOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MinioOutboxRepository extends JpaRepository<MinioOutbox, UUID> {

	List<MinioOutbox> findTop100ByStatusOrderByCreatedAtAsc(MinioOutboxStatus status);
}
