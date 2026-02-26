package com.rentitup.catalog_service.entities;


import com.rentitup.catalog_service.common.entites.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "machine_images", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MachineImageEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "machine_id", nullable = false)
	private MachineEntity machine;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(name = "is_primary")
	@Builder.Default
	private boolean primary = false;

	@Column(name = "display_order")
	@Builder.Default
	private Integer displayOrder = 0;

	@Column(name = "uploaded_at", updatable = false)
	@Builder.Default
	private Instant uploadedAt = Instant.now();
}