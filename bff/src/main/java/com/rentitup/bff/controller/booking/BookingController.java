package com.rentitup.bff.controller.booking;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.bff.common.security.SecurityUtils;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.common.PaginationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@Slf4j
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

	@GrpcClient("BOOKING-SERVICE")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

	@PostMapping
	@Operation(summary = "Create a new booking", description = "Create a booking for a machine rental")
	public ResponseEntity<?> createBooking(@RequestBody CreateBookingRequest request) {
		log.info("Creating booking for machine: {}", request.getMachineId());

		// Override customerId with authenticated user's ID for security
		CreateBookingRequest secureRequest = request.toBuilder()
				.setCustomerId(SecurityUtils.requiredCurrentUserId())
				.build();

		BookingResponse response = bookingStub.createBooking(secureRequest);
		return ResponseBuilder.created("Booking created successfully", response.getBooking());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get booking by ID", description = "Retrieve a specific booking by its ID")
	public ResponseEntity<?> getBooking(
			@Parameter(description = "Booking ID") @PathVariable String id) {
		log.info("Getting booking: {}", id);

		GetBookingRequest request = GetBookingRequest.newBuilder()
				.setId(id)
				.build();

		BookingResponse response = bookingStub.getBooking(request);
		return ResponseBuilder.success("Booking retrieved successfully", response.getBooking());
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
	@Operation(summary = "Update booking status", description = "Update the status of a booking (Admin/Owner only)")
	public ResponseEntity<?> updateBookingStatus(
			@Parameter(description = "Booking ID") @PathVariable String id,
			@RequestBody UpdateBookingStatusRequest request) {
		log.info("Updating booking status: {} -> {}", id, request.getStatus());

		// Override ID from path parameter for security
		UpdateBookingStatusRequest secureRequest = request.toBuilder()
				.setId(id)
				.setActorId(SecurityUtils.requiredCurrentUserId())
				.setActorIsAdmin(SecurityUtils.hasRole("ADMIN"))
				.build();

		BookingResponse response = bookingStub.updateBookingStatus(secureRequest);
		return ResponseBuilder.success("Booking status updated successfully", response.getBooking());
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel a booking", description = "Cancel an existing booking with a reason")
	public ResponseEntity<?> cancelBooking(
			@Parameter(description = "Booking ID") @PathVariable String id,
			@RequestBody CancelBookingRequest request) {
		log.info("Cancelling booking: {}", id);

		// Override ID from path parameter
		CancelBookingRequest secureRequest = request.toBuilder()
				.setId(id)
				.build();

		BookingResponse response = bookingStub.cancelBooking(secureRequest);
		return ResponseBuilder.success("Booking cancelled successfully", response.getBooking());
	}

	@GetMapping("/my-bookings")
	@Operation(summary = "Get current user's bookings", description = "List all bookings for the authenticated customer")
	public ResponseEntity<?> getMyBookings(
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
		log.info("Getting bookings for current user");

		ListBookingsByCustomerRequest.Builder builder = ListBookingsByCustomerRequest.newBuilder()
				.setCustomerId(SecurityUtils.requiredCurrentUserId())
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build());

		if (status != null && !status.isEmpty()) {
			builder.setStatus(mapBookingStatus(status));
		}

		ListBookingsResponse response = bookingStub.listBookingsByCustomer(builder.build());

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getBookingsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@GetMapping("/machine/{machineId}")
	@Operation(summary = "Get bookings by machine", description = "List all bookings for a specific machine")
	public ResponseEntity<?> getBookingsByMachine(
			@Parameter(description = "Machine ID") @PathVariable String machineId,
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
		log.info("Getting bookings for machine: {}", machineId);

		ListBookingsByMachineRequest request = ListBookingsByMachineRequest.newBuilder()
				.setMachineId(machineId)
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build())
				.build();

		ListBookingsResponse response = bookingStub.listBookingsByMachine(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getBookingsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@GetMapping("/owner")
	@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
	@Operation(summary = "Get bookings for owner", description = "List all bookings for machines owned by the current user")
	public ResponseEntity<?> getOwnerBookings(
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
			@Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
		log.info("Getting bookings for owner");

		ListBookingsForOwnerRequest.Builder builder = ListBookingsForOwnerRequest.newBuilder()
				.setOwnerId(SecurityUtils.requiredCurrentUserId())
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build());

		if (status != null && !status.isEmpty()) {
			builder.setStatus(mapBookingStatus(status));
		}

		ListBookingsResponse response = bookingStub.listBookingsForOwner(builder.build());

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getBookingsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	private BookingStatus mapBookingStatus(String status) {
		if (status == null) return BookingStatus.BOOKING_STATUS_UNSPECIFIED;
		return switch (status.toUpperCase()) {
			case "PENDING" -> BookingStatus.BOOKING_PENDING;
			case "CONFIRMED" -> BookingStatus.BOOKING_CONFIRMED;
			case "PAID" -> BookingStatus.BOOKING_PAID;
			case "ONGOING" -> BookingStatus.BOOKING_ONGOING;
			case "COMPLETED" -> BookingStatus.BOOKING_COMPLETED;
			case "CANCELLED" -> BookingStatus.BOOKING_CANCELLED;
			case "REJECTED" -> BookingStatus.BOOKING_REJECTED;
			default -> BookingStatus.BOOKING_STATUS_UNSPECIFIED;
		};
	}
}
