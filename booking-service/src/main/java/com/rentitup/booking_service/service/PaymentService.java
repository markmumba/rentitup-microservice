package com.rentitup.booking_service.service;

import com.rentitup.booking_service.entities.PaymentEntity;
import com.rentitup.booking_service.enums.PaymentStatus;
import com.rentitup.booking_service.enums.PaymentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {
	PaymentEntity createPaymentEntry(UUID bookingId, BigDecimal amount, String currency, PaymentType type, String transactionId);

	PaymentEntity updatePaymentStatus(UUID paymentId, PaymentStatus status);

	List<PaymentEntity> getPaymentsByBooking(UUID bookingId);

	PaymentEntity getPaymentById(UUID paymentId);
}
