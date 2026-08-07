package com.rentitup.booking_service.entities;

import com.rentitup.booking_service.common.entities.BaseEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bookings")
@SuperBuilder
public class BookingEntity extends BaseEntity {

	@Column(name = "machine_id",nullable = false)
	private UUID machineId;

	@Column(name = "customer_id",nullable = false)
	private UUID customerId;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "start_date",nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date",nullable = false)
	private LocalDate endDate;

	@Column(name = "duration_days",nullable = false)
	private int durationDays;

	@Column(name = "pickup_latitude")
	private BigDecimal pickupLatitude;

	@Column(name = "pickup_longitude")
	private BigDecimal pickupLongitude;

	@Column(name = "pickup_address")
	private String pickupAddress;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private BookingStatus status = BookingStatus.PENDING;

	@Column(name = "cancellation_reason")
	private String cancellationReason;

	@Column(nullable = false)
	private BigDecimal dailyRate;

	@Column(nullable = false)
	private BigDecimal totalAmount;

	private BigDecimal securityDeposit;

	private BigDecimal amountPaid;

	@Builder.Default
	private String currency = "KES";

	private String specialRequirements;

	private Instant confirmedAt;

	private Instant completedAt;

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<PaymentEntity> payments;


	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ReviewEntity> reviews;

}
