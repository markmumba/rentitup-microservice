package com.rentitup.bff.controller.booking;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.common.PaginationRequest;
import com.rentitup.shared.proto.common.Location;
import com.rentitup.shared.proto.common.Timestamp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/bookings")
@Slf4j
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

	@GrpcClient(value = "BOOKING-SERVICE", forwardToken = true)
	private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

	@PostMapping
	@Operation(summary = "Create a new booking", description = "Create a booking for a machine rental")
	public ResponseEntity<?> createBooking(@RequestBody CreateBookingDto request) {
		log.info("Creating booking for machine: {}", request.machineId());

		CreateBookingRequest.Builder builder = CreateBookingRequest.newBuilder()
				.setMachineId(request.machineId())
				.setCustomerId(getCurrentUserId())
				.setStartDate(toTimestamp(request.startDate()))
				.setEndDate(toTimestamp(request.endDate()));

		if (request.pickupLatitude() != null && request.pickupLongitude() != null) {
			Location.Builder locationBuilder = Location.newBuilder()
					.setLatitude(request.pickupLatitude())
					.setLongitude(request.pickupLongitude());
			if (request.pickupAddress() != null) {
				locationBuilder.setAddress(request.pickupAddress());
			}
			builder.setPickupLocation(locationBuilder.build());
		}

		if (request.specialRequirements() != null) {
			builder.setSpecialRequirements(request.specialRequirements());
		}

		if (request.hours() != null) {
			builder.setHours(request.hours());
		}
		if (request.weeks() != null) {
			builder.setWeeks(request.weeks());
		}
		if (request.distance() != null) {
			builder.setDistance(request.distance());
		}

		BookingResponse response = bookingStub.createBooking(builder.build());
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
			@RequestBody UpdateBookingStatusDto request) {
		log.info("Updating booking status: {} -> {}", id, request.status());

		UpdateBookingStatusRequest grpcRequest = UpdateBookingStatusRequest.newBuilder()
				.setId(id)
				.setStatus(mapBookingStatus(request.status()))
				.build();

		BookingResponse response = bookingStub.updateBookingStatus(grpcRequest);
		return ResponseBuilder.success("Booking status updated successfully", response.getBooking());
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel a booking", description = "Cancel an existing booking with a reason")
	public ResponseEntity<?> cancelBooking(
			@Parameter(description = "Booking ID") @PathVariable String id,
			@RequestBody CancelBookingDto request) {
		log.info("Cancelling booking: {}", id);

		CancelBookingRequest grpcRequest = CancelBookingRequest.newBuilder()
				.setId(id)
				.setReason(request.reason() != null ? request.reason() : "")
				.build();

		BookingResponse response = bookingStub.cancelBooking(grpcRequest);
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
				.setCustomerId(getCurrentUserId())
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
				.setOwnerId(getCurrentUserId())
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

	private String getCurrentUserId() {
		JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = auth.getToken();
		return jwt.getClaimAsString("user_id");
	}

	private Timestamp toTimestamp(LocalDate date) {
		if (date == null) return Timestamp.getDefaultInstance();
		Instant instant = date.atStartOfDay().toInstant(ZoneOffset.UTC);
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}

	private BookingStatus mapBookingStatus(String status) {
		if (status == null) return BookingStatus.BOOKING_STATUS_UNSPECIFIED;
		return switch (status.toUpperCase()) {
			case "PENDING" -> BookingStatus.BOOKING_PENDING;
			case "CONFIRMED" -> BookingStatus.BOOKING_CONFIRMED;
			case "ONGOING" -> BookingStatus.BOOKING_ONGOING;
			case "COMPLETED" -> BookingStatus.BOOKING_COMPLETED;
			case "CANCELLED" -> BookingStatus.BOOKING_CANCELLED;
			default -> BookingStatus.BOOKING_STATUS_UNSPECIFIED;
		};
	}

	// DTOs as records
	public record CreateBookingDto(
			String machineId,
			LocalDate startDate,
			LocalDate endDate,
			Double pickupLatitude,
			Double pickupLongitude,
			String pickupAddress,
			String specialRequirements,
			Integer hours,
			Integer weeks,
			Integer distance
	) {}

	public record UpdateBookingStatusDto(String status) {}

	public record CancelBookingDto(String reason) {}
}
