package com.rentitup.booking_service.entities;

import com.rentitup.booking_service.common.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews")
@SuperBuilder
public class ReviewEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id", nullable = false)
	private BookingEntity booking;

	@Column(nullable = false)
	private UUID reviewerId;

	@Column(nullable = false)
	private UUID machineId;

	@Column(nullable = false)
	private UUID ownerId;

	@Column(nullable = false)
	private int machineRating;

	@Column(nullable = false)
	private int ownerRating;

	private String comment;

	private String ownerResponse;

	private Instant ownerResponseAt;

	@Column(nullable = false)
	private boolean ratingSynced = false;

	private Instant syncedAt;

}
