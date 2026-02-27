package com.rentitup.booking_service.repository;

import com.rentitup.booking_service.entities.BookingEntity;
import com.rentitup.booking_service.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

	@Query("""
		SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
		FROM BookingEntity b
		WHERE b.machineId = :machineId
		AND b.status NOT IN (:excludedStatuses)
		AND b.startDate < :endDate
		AND b.endDate > :startDate
		""")
	boolean existsOverlappingBooking(
			@Param("machineId") UUID machineId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("excludedStatuses") List<BookingStatus> excludedStatuses
	);


	default boolean existsOverlappingBookingForMachine(UUID machineId, LocalDate startDate, LocalDate endDate) {
		return existsOverlappingBooking(
				machineId,
				startDate,
				endDate,
				List.of(BookingStatus.CANCELLED, BookingStatus.REJECTED)
		);
	}

	Page<BookingEntity> findByMachineIdIn(Collection<UUID> machineIds, Pageable pageable);
	
	Page<BookingEntity> findByCustomerIdAndStatus(UUID customerId, BookingStatus status, Pageable pageable);

	Page<BookingEntity> findByCustomerId(UUID customerId, Pageable pageable);

	Page<BookingEntity> findByMachineId(UUID machineId, Pageable pageable);

	Page<BookingEntity> findByOwnerIdAndStatus(UUID ownerId, Pageable pageable, BookingStatus status);
}
