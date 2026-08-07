package com.rentitup.bff.controller.booking;

import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.bff.common.security.SecurityUtils;
import com.rentitup.shared.proto.booking.*;
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

	@GrpcClient("BOOKING-SERVICE")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

	@PostMapping
	@Operation(summary = "Create a payment", description = "Initiate a payment for a booking")
	public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
		log.info("Creating payment for booking: {}", request.getBookingId());

		CreatePaymentRequest secureRequest = request.toBuilder()
				.setCustomerId(SecurityUtils.requiredCurrentUserId())
				.build();
		PaymentResponse response = bookingStub.createPayment(secureRequest);
		return ResponseBuilder.created("Payment initiated successfully", response.getPayment());
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
	@Operation(summary = "Update payment status", description = "Update the status of a payment (Admin/System only)")
	public ResponseEntity<?> updatePaymentStatus(
			@Parameter(description = "Payment ID") @PathVariable String id,
			@RequestBody UpdatePaymentStatusRequest request) {
		log.info("Updating payment status: {} -> {}", id, request.getStatus());

		// Override ID from path parameter for security
		UpdatePaymentStatusRequest secureRequest = request.toBuilder()
				.setId(id)
				.build();

		PaymentResponse response = bookingStub.updatePaymentStatus(secureRequest);
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
}
