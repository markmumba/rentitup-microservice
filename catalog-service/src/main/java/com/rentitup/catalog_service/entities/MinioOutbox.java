package com.rentitup.catalog_service.entities;

import com.rentitup.catalog_service.common.entites.BaseEntity;
import com.rentitup.catalog_service.enums.MinioOutboxStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "minio_outbox",schema = "catalog")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MinioOutbox extends BaseEntity {
	@Column(name = "object_key", nullable = false, length = 500)
	private String objectKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MinioOutboxStatus status;
}
