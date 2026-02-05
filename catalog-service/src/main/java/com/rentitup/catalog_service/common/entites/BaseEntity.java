package com.rentitup.catalog_service.common.entites;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@CreatedDate
	@Column(name = "created_at",updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name ="updated_at")
	private LocalDateTime updatedAt;
}
