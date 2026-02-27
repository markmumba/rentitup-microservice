package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.entities.PaymentEntity;
import com.rentitup.booking_service.enums.PaymentMethod;
import com.rentitup.booking_service.enums.PaymentStatus;
import com.rentitup.booking_service.enums.PaymentType;
import com.rentitup.booking_service.grpc.client.UserGrpcClient;
import com.rentitup.booking_service.repository.BookingRepository;
import com.rentitup.booking_service.repository.PaymentRepository;
import com.rentitup.booking_service.service.PaymentService;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.shared.proto.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
	private final PaymentRepository paymentRepository;
	private final BookingRepository bookingRepository;
	private final UserGrpcClient userGrpcClient;

	@Override
	public PaymentEntity createPaymentEntry(UUID bookingId, BigDecimal amount, String currency, PaymentType type, String transactionId) {
		log.info("Creating payment entry for booking: {}", bookingId);

		BookingEntity booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException("Booking of id " + bookingId + " not found"));

		PaymentEntity paymentEntity = PaymentEntity.builder()
				.booking(booking)
				.amount(amount)
				.currency(currency != null ? currency : "KES")
				.paymentType(type)
				.status(PaymentStatus.PENDING)
				.method(PaymentMethod.MPESA)
				.transactionId(transactionId)
				.build();

		PaymentEntity savedPayment = paymentRepository.save(paymentEntity);
		log.info("Payment entry created with id: {}", savedPayment.getId());

		User user = userGrpcClient.getUser(booking.getCustomerId().toString());

		try {
			initiatePayment(savedPayment,
					user.getEmail(),
					user.getPhone(),
					user.getKycStatus().toString()
			);
		} catch (Exception e) {
			log.error("Error while initiating payment: {}", e.getMessage());
			throw new RuntimeException("Error while initiating payment", e);
		}

		return savedPayment;
	}

	@Override
	public PaymentEntity updatePaymentStatus(UUID paymentId, PaymentStatus status) {
		log.info("Updating payment status: {} -> {}", paymentId, status);

		PaymentEntity payment = getPaymentById(paymentId);
		payment.setStatus(status);

		if (status == PaymentStatus.COMPLETED) {
			payment.setCompletedAt(Instant.now());
		}

		PaymentEntity savedPayment = paymentRepository.save(payment);
		log.info("Payment status updated successfully: {}", savedPayment.getId());

		return savedPayment;
	}

	@Override
	public List<PaymentEntity> getPaymentsByBooking(UUID bookingId) {
		log.info("Getting payments for booking: {}", bookingId);
		return paymentRepository.findByBookingId(bookingId);
	}

	@Override
	public PaymentEntity getPaymentById(UUID paymentId) {
		return paymentRepository.findById(paymentId)
				.orElseThrow(() -> new NotFoundException("Payment of id " + paymentId + " not found"));
	}

	private void initiatePayment(
			PaymentEntity payment,
			String email,
			String phone,
			String kycStatus
	) {
		// TODO: Implement actual payment initiation logic (e.g., M-Pesa STK push)
		log.info("Initiating payment for amount: {} {} to phone: {}",
				payment.getAmount(), payment.getCurrency(), phone);
	}
}
