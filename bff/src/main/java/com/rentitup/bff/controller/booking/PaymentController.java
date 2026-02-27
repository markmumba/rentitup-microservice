package com.rentitup.bff.controller.booking;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.common.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

	@GrpcClient(value = "BOOKING-SERVICE", forwardToken = true)
	private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

	@PostMapping
	@Operation(summary = "Create a payment", description = "Initiate a payment for a booking")
	public ResponseEntity<?> createPayment(@RequestBody CreatePaymentDto request) {
		log.info("Creating payment for booking: {}", request.bookingId());

		CreatePaymentRequest grpcRequest = CreatePaymentRequest.newBuilder()
				.setBookingId(request.bookingId())
				.setAmount(Money.newBuilder()
						.setAmount(request.amount())
						.setCurrency(request.currency() != null ? request.currency() : "KES")
						.build())
				.setPaymentType(mapPaymentType(request.paymentType()))
				.setTransactionId(request.transactionId() != null ? request.transactionId() : "")
				.build();

		PaymentResponse response = bookingStub.createPayment(grpcRequest);
		return ResponseBuilder.created("Payment initiated successfully", response.getPayment());
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
	@Operation(summary = "Update payment status", description = "Update the status of a payment (Admin/System only)")
	public ResponseEntity<?> updatePaymentStatus(
			@Parameter(description = "Payment ID") @PathVariable String id,
			@RequestBody UpdatePaymentStatusDto request) {
		log.info("Updating payment status: {} -> {}", id, request.status());

		UpdatePaymentStatusRequest grpcRequest = UpdatePaymentStatusRequest.newBuilder()
				.setId(id)
				.setStatus(mapPaymentStatus(request.status()))
				.build();

		PaymentResponse response = bookingStub.updatePaymentStatus(grpcRequest);
		return ResponseBuilder.success("Payment status updated successfully", response.getPayment());
	}

	@GetMapping("/booking/{bookingId}")
	@Operation(summary = "Get payments by booking", description = "Retrieve all payments for a specific booking")
	public ResponseEntity<?> getPaymentsByBooking(
			@Parameter(description = "Booking ID") @PathVariable String bookingId) {
		log.info("Getting payments for booking: {}", bookingId);

		GetPaymentsByBookingRequest request = GetPaymentsByBookingRequest.newBuilder()
				.setBookingId(bookingId)
				.build();

		PaymentsResponse response = bookingStub.getPaymentsByBooking(request);
		return ResponseBuilder.success("Payments retrieved successfully", response.getPaymentsList());
	}

	private PaymentType mapPaymentType(String type) {
		if (type == null) return PaymentType.PAYMENT_TYPE_UNSPECIFIED;
		return switch (type.toUpperCase()) {
			case "DEPOSIT" -> PaymentType.DEPOSIT;
			case "FULL_PAYMENT", "FULL" -> PaymentType.FULL_PAYMENT;
			case "SECURITY_DEPOSIT", "SECURITY" -> PaymentType.SECURITY_DEPOSIT;
			default -> PaymentType.PAYMENT_TYPE_UNSPECIFIED;
		};
	}

	private PaymentStatus mapPaymentStatus(String status) {
		if (status == null) return PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
		return switch (status.toUpperCase()) {
			case "PENDING" -> PaymentStatus.PAYMENT_PENDING;
			case "COMPLETED" -> PaymentStatus.PAYMENT_COMPLETED;
			case "FAILED" -> PaymentStatus.PAYMENT_FAILED;
			case "REFUNDED" -> PaymentStatus.PAYMENT_REFUNDED;
			default -> PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
		};
	}

	// DTOs as records
	public record CreatePaymentDto(
			String bookingId,
			String amount,
			String currency,
			String paymentType,
			String transactionId
	) {}

	public record UpdatePaymentStatusDto(String status) {}
}
