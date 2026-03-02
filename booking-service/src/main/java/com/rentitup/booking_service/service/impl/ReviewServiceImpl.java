package com.rentitup.booking_service.service.impl;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.entities.ReviewEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import com.rentitup.booking_service.grpc.client.CatalogGrpcClient;
import com.rentitup.booking_service.repository.BookingRepository;
import com.rentitup.booking_service.repository.ReviewRepository;
import com.rentitup.booking_service.service.ReviewService;
import com.rentitup.common.exceptions.BadRequestException;
import com.rentitup.common.exceptions.ConflictException;
import com.rentitup.common.exceptions.NotFoundException;
import com.rentitup.shared.proto.catalog.Machine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
	private final ReviewRepository reviewRepository;
	private final BookingRepository bookingRepository;
	private final CatalogGrpcClient catalogGrpcClient;

	@Override
	public ReviewEntity createReview(UUID bookingId, UUID reviewerId, int machineRating, int ownerRating, String comment) {
		log.info("Creating review for booking: {}", bookingId);

		BookingEntity booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

		if (booking.getStatus() != BookingStatus.COMPLETED) {
			throw new BadRequestException("Can only review completed bookings");
		}

		if (!booking.getCustomerId().equals(reviewerId)) {
			throw new BadRequestException("Only the customer who made the booking can leave a review");
		}

		if (reviewRepository.existsByBookingId(bookingId)) {
			throw new ConflictException("A review already exists for this booking");
		}

		if (machineRating < 1 || machineRating > 5 || ownerRating < 1 || ownerRating > 5) {
			throw new BadRequestException("Ratings must be between 1 and 5");
		}

		Machine machine = catalogGrpcClient.getMachine(booking.getMachineId().toString());
		UUID ownerId = UUID.fromString(machine.getOwnerId());

		ReviewEntity review = ReviewEntity.builder()
				.booking(booking)
				.reviewerId(reviewerId)
				.machineId(booking.getMachineId())
				.ownerId(ownerId)
				.machineRating(machineRating)
				.ownerRating(ownerRating)
				.comment(comment)
				.build();

		ReviewEntity savedReview = reviewRepository.save(review);
		log.info("Review created with id: {}", savedReview.getId());

		return savedReview;
	}

	@Override
	public Page<ReviewEntity> getReviewsByMachine(UUID machineId, Pageable pageable) {
		log.info("Getting reviews for machine: {}", machineId);
		return reviewRepository.findByMachineId(machineId, pageable);
	}

	@Override
	public Page<ReviewEntity> getReviewsByOwner(UUID ownerId, Pageable pageable) {
		log.info("Getting reviews for owner: {}", ownerId);
		return reviewRepository.findByOwnerId(ownerId, pageable);
	}

	@Override
	public List<ReviewEntity> getAllReviews() {
		return reviewRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Stream<ReviewEntity> streamUnsyncedReviews() {
		log.info("Streaming unsynced reviews");
		return reviewRepository.streamUnsyncedReviews();
	}

	@Override
	@Transactional
	public int markReviewsAsSynced(List<UUID> reviewIds) {
		if (reviewIds == null || reviewIds.isEmpty()) {
			return 0;
		}
		log.info("Marking {} reviews as synced", reviewIds.size());
		return reviewRepository.markReviewsAsSynced(reviewIds, Instant.now());
	}
}
