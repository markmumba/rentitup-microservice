package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
	Page<ReviewEntity> findByMachineId(UUID machineId, Pageable pageable);

	Page<ReviewEntity> findByOwnerId(UUID ownerId, Pageable pageable);

	Optional<ReviewEntity> findByBookingId(UUID bookingId);

	boolean existsByBookingId(UUID bookingId);

	@Query("SELECT r FROM ReviewEntity r WHERE r.ratingSynced = false")
	Stream<ReviewEntity> streamUnsyncedReviews();

	@Modifying
	@Query("UPDATE ReviewEntity r SET r.ratingSynced = true, r.syncedAt = :syncedAt WHERE r.id IN :ids")
	int markReviewsAsSynced(@Param("ids") List<UUID> ids, @Param("syncedAt") Instant syncedAt);
}
