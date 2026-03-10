package com.rentitup.catalog_service.entities;

import com.rentitup.catalog_service.common.entites.BaseEntity;
import com.rentitup.catalog_service.enums.MachineCondition;
import com.rentitup.catalog_service.enums.MachineStatus;
import com.rentitup.catalog_service.enums.PriceCalculationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;


@Entity
@Table(name = "machines", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MachineEntity extends BaseEntity {

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	private CategoryEntity category;

	@Column(nullable = false)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(name = "base_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal basePrice;

	@Column(length = 3)
	@Builder.Default
	private String currency = "KES";

	@Enumerated(EnumType.STRING)
	@Column(name = "price_type", length = 20)
	@Builder.Default
	private PriceCalculationType priceType = PriceCalculationType.DAILY;

	@Enumerated(EnumType.STRING)
	@Column(name = "condition", nullable = false, length = 20)
	private MachineCondition condition;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	@Builder.Default
	private MachineStatus status = MachineStatus.AVAILABLE;

	@Column(precision = 10, scale = 8)
	private BigDecimal latitude;

	@Column(precision = 11, scale = 8)
	private BigDecimal longitude;

	@Column(length = 500)
	private String address;

	@Column(length = 100)
	private String city;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	@Builder.Default
	private Map<String, String> specifications = new HashMap<>();

	@Column(name = "is_available")
	@Builder.Default
	private boolean available = true;

	@Column(name = "average_rating", precision = 2, scale = 1)
	@Builder.Default
	private BigDecimal averageRating = BigDecimal.ZERO;

	@Column(name = "total_reviews")
	@Builder.Default
	private Integer totalReviews = 0;

	@Column(name = "total_rentals")
	@Builder.Default
	private Integer totalRentals = 0;

	@Builder.Default
	private boolean deleted = false;


	// Relationships within same service
	@OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<MachineImageEntity> images = new ArrayList<>();

	@OneToMany(mappedBy = "machine", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<MaintenanceRecordEntity> maintenanceRecords = new ArrayList<>();

	// Helper methods
	public void addImage(MachineImageEntity image) {
		images.add(image);
		image.setMachine(this);
	}

	public void removeImage(MachineImageEntity image) {
		images.remove(image);
		image.setMachine(null);
	}

	public void addMaintenanceRecord(MaintenanceRecordEntity record) {
		maintenanceRecords.add(record);
		record.setMachine(this);
	}

	public String getPrimaryImageUrl() {
		return images.stream()
				.filter(MachineImageEntity::isPrimary)
				.findFirst()
				.map(MachineImageEntity::getUrl)
				.orElse(images.isEmpty() ? null : images.get(0).getUrl());
	}


}