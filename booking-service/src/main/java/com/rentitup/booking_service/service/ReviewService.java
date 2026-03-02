package com.rentitup.booking_service.service;

import com.rentitup.booking_service.entities.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public interface ReviewService {
	ReviewEntity createReview(UUID bookingId, UUID reviewerId, int machineRating, int ownerRating, String comment);

	Page<ReviewEntity> getReviewsByMachine(UUID machineId, Pageable pageable);

	Page<ReviewEntity> getReviewsByOwner(UUID ownerId, Pageable pageable);

	List<ReviewEntity> getAllReviews();

	Stream<ReviewEntity> streamUnsyncedReviews();

	int markReviewsAsSynced(List<UUID> reviewIds);
}
