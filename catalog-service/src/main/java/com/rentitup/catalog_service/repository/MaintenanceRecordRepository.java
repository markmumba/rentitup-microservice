package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

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
}
