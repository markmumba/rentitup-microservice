package com.rentitup.bff.controller.booking;

import com.rentitup.bff.common.pagination.PaginationDto;
import com.rentitup.bff.common.response.ResponseBuilder;
import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.common.security.SecurityUtils;
import com.rentitup.shared.proto.booking.*;
import com.rentitup.shared.proto.common.PaginationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@Slf4j
@Tag(name = "Reviews", description = "Review management endpoints")
public class ReviewController {

	@GrpcClient("BOOKING-SERVICE")
	private BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

	@PostMapping
	@Operation(summary = "Create a review", description = "Submit a review for a completed booking")
	public ResponseEntity<?> createReview(@RequestBody CreateReviewRequest request) {
		log.info("Creating review for booking: {}", request.getBookingId());

		// Override reviewerId with authenticated user's ID for security
		CreateReviewRequest secureRequest = request.toBuilder()
				.setReviewerId(SecurityUtils.requiredCurrentUserId())
				.build();

		ReviewResponse response = bookingStub.createReview(secureRequest);
		return ResponseBuilder.created("Review submitted successfully", response.getReview());
	}

	@GetMapping("/machine/{machineId}")
	@Operation(summary = "Get reviews by machine", description = "Retrieve all reviews for a specific machine")
	public ResponseEntity<?> getReviewsByMachine(
			@Parameter(description = "Machine ID") @PathVariable String machineId,
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
		log.info("Getting reviews for machine: {}", machineId);

		GetReviewsByMachineRequest request = GetReviewsByMachineRequest.newBuilder()
				.setMachineId(machineId)
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build())
				.build();

		ReviewsResponse response = bookingStub.getReviewsByMachine(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getReviewsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@GetMapping("/owner/{ownerId}")
	@Operation(summary = "Get reviews by owner", description = "Retrieve all reviews for a specific owner's machines")
	public ResponseEntity<?> getReviewsByOwner(
			@Parameter(description = "Owner ID") @PathVariable String ownerId,
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
		log.info("Getting reviews for owner: {}", ownerId);

		GetReviewsByOwnerRequest request = GetReviewsByOwnerRequest.newBuilder()
				.setOwnerId(ownerId)
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build())
				.build();

		ReviewsResponse response = bookingStub.getReviewsByOwner(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getReviewsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}

	@GetMapping("/my-reviews")
	@Operation(summary = "Get current owner's reviews", description = "Retrieve all reviews for the authenticated owner's machines")
	public ResponseEntity<?> getMyReviews(
			@Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
		log.info("Getting reviews for current owner");

		GetReviewsByOwnerRequest request = GetReviewsByOwnerRequest.newBuilder()
				.setOwnerId(SecurityUtils.requiredCurrentUserId())
				.setPagination(PaginationRequest.newBuilder()
						.setPage(page)
						.setSize(size)
						.build())
				.build();

		ReviewsResponse response = bookingStub.getReviewsByOwner(request);

		PaginationDto paginationDto = PaginationDto.builder()
				.page(response.getPagination().getCurrentPage())
				.limit(response.getPagination().getPageSize())
				.build();

		return ResponseBuilder.successPageResponse(
				response.getReviewsList(),
				paginationDto,
				response.getPagination().getTotalElements()
		);
	}
}
