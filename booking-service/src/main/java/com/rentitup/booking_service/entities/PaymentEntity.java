package com.rentitup.booking_service.entities;

import com.google.type.Decimal;
import com.rentitup.booking_service.common.entities.BaseEntity;
import com.rentitup.booking_service.enums.PaymentMethod;
import com.rentitup.booking_service.enums.PaymentStatus;
import com.rentitup.booking_service.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
@SuperBuilder
public class PaymentEntity extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "booking_id",nullable = false)
	private BookingEntity booking;

	@Column(nullable = false)
	private BigDecimal amount;

	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentType paymentType ;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private PaymentStatus status= PaymentStatus.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod method;

	private String transactionId;

	private String mpesaReceipt;

	private Instant completedAt;
}

