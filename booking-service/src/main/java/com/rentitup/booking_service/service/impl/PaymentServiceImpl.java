package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.entities.PaymentEntity;
import com.rentitup.booking_service.enums.PaymentMethod;
import com.rentitup.booking_service.enums.PaymentStatus;
import com.rentitup.booking_service.enums.PaymentType;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.grpc.client.UserGrpcClient;
import com.rentitup.booking_service.repository.BookingRepository;
import com.rentitup.booking_service.repository.PaymentRepository;
import com.rentitup.booking_service.service.PaymentService;
import com.rentitup.common.exceptions.BadRequestException;
import com.rentitup.common.exceptions.ConflictException;
import com.rentitup.common.exceptions.ForbiddenException;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.shared.proto.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional
	public PaymentEntity createPaymentEntry(UUID bookingId, UUID customerId, BigDecimal amount, String currency,
											PaymentType type, String transactionId) {
		log.info("Creating payment entry for booking: {}", bookingId);

		BookingEntity booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException("Booking of id " + bookingId + " not found"));

		if (!booking.getCustomerId().equals(customerId)) {
			throw new ForbiddenException("This booking does not belong to the authenticated customer");
		}
		if (booking.getStatus() != BookingStatus.CONFIRMED) {
			throw new ConflictException("Payments can only be initiated for a confirmed booking");
		}
		if (amount == null || amount.signum() <= 0) {
			throw new BadRequestException("Payment amount must be greater than zero");
		}

		String paymentCurrency = currency == null || currency.isBlank() ? booking.getCurrency() : currency;
		if (!booking.getCurrency().equalsIgnoreCase(paymentCurrency)) {
			throw new BadRequestException("Payment currency must match the booking currency");
		}

		BigDecimal completedAmount = completedAmountFor(bookingId);
		BigDecimal outstandingAmount = booking.getTotalAmount().subtract(completedAmount);
		if (amount.compareTo(outstandingAmount) > 0) {
			throw new BadRequestException("Payment amount exceeds the outstanding booking amount");
		}

		String resolvedTransactionId = transactionId == null || transactionId.isBlank()
				? "DUMMY-" + UUID.randomUUID()
				: transactionId;
		if (paymentRepository.existsByTransactionId(resolvedTransactionId)) {
			throw new ConflictException("A payment with this transaction ID already exists");
		}

		PaymentEntity paymentEntity = PaymentEntity.builder()
				.booking(booking)
				.amount(amount)
				.currency(paymentCurrency)
				.paymentType(type)
				.status(PaymentStatus.PENDING)
				.method(PaymentMethod.MPESA)
				.transactionId(resolvedTransactionId)
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
	@Transactional
	public PaymentEntity updatePaymentStatus(UUID paymentId, PaymentStatus status) {
		log.info("Updating payment status: {} -> {}", paymentId, status);

		PaymentEntity payment = getPaymentById(paymentId);
		if (payment.getStatus() == status) {
			return payment;
		}
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new ConflictException("A finalized payment cannot change status");
		}
		if (status == PaymentStatus.PENDING) {
			throw new BadRequestException("Payment is already pending");
		}

		payment.setStatus(status);

		if (status == PaymentStatus.COMPLETED) {
			payment.setCompletedAt(Instant.now());
			BookingEntity booking = payment.getBooking();
			BigDecimal amountPaid = completedAmountFor(booking.getId()).add(payment.getAmount());
			booking.setAmountPaid(amountPaid);
			if (amountPaid.compareTo(booking.getTotalAmount()) >= 0) {
				if (booking.getStatus() != BookingStatus.CONFIRMED) {
					throw new ConflictException("Only a confirmed booking can become paid");
				}
				booking.setStatus(BookingStatus.PAID);
			}
			bookingRepository.save(booking);
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

	private BigDecimal completedAmountFor(UUID bookingId) {
		return paymentRepository.findByBookingId(bookingId).stream()
				.filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
				.map(PaymentEntity::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
