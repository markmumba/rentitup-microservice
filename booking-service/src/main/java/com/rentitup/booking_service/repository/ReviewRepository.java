package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
	Page<ReviewEntity> findByMachineId(UUID machineId, Pageable pageable);

	Page<ReviewEntity> findByOwnerId(UUID ownerId, Pageable pageable);

	Optional<ReviewEntity> findByBookingId(UUID bookingId);

	boolean existsByBookingId(UUID bookingId);
}
