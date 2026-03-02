package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public interface MaintenanceRecordRepository extends  JpaRepository<MaintenanceRecordEntity, UUID> {
	@Query("SELECT mr FROM MaintenanceRecordEntity mr " +
			"WHERE mr.machine.ownerId = :ownerId " +
			"AND mr.nextServiceDate >= :startDate " +
			"AND mr.nextServiceDate <= :endDate")
	Page<MaintenanceRecordEntity> findUpcomingByOwnerId(@Param("ownerId") UUID ownerId,
										  @Param("startDate") LocalDate startDate,
										  @Param("endDate") LocalDate endDate,
										  Pageable pageable);

	Page<MaintenanceRecordEntity> findAllByMachine(MachineEntity machine, Pageable pageable);

	boolean existsByMachineAndNextServiceDateBetween(MachineEntity machine, LocalDate startDate, LocalDate endDate);

	@Query("SELECT mr FROM MaintenanceRecordEntity mr " +
			"WHERE mr.nextServiceDate IS NOT NULL " +
			"AND mr.nextServiceDate >= CURRENT_DATE " +
			"AND mr.nextServiceDate <= :endDate " +
			"AND (mr.reminderSentAt IS NULL OR mr.reminderSentAt < :reminderCutoff)")
	Stream<MaintenanceRecordEntity> streamRecordsNeedingReminder(
			@Param("endDate") LocalDate endDate,
			@Param("reminderCutoff") Instant reminderCutoff);

	@Modifying
	@Query("UPDATE MaintenanceRecordEntity mr SET mr.reminderSentAt = :sentAt WHERE mr.id IN :ids")
	int markAsReminded(@Param("ids") List<UUID> ids, @Param("sentAt") Instant sentAt);
}
