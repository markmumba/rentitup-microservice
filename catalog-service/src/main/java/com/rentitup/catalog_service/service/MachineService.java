package com.rentitup.catalog_service.service;

import com.rentitup.catalog_service.entities.MachineEntity;
import com.rentitup.catalog_service.entities.MaintenanceRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface MachineService {
	MachineEntity createMachine(MachineEntity machine, UUID categoryId);
	MachineEntity getMachine(UUID id);
	MachineEntity updateMachine(UUID id, MachineEntity updates, UUID categoryId);
	String deleteMachine(UUID id);
	Page<MachineEntity> findAll(Specification<MachineEntity> spec, Pageable pageable);
	List<MachineEntity> findAllByIds(List<UUID> ids);
	MachineEntity addImage(UUID machineId, String url, boolean isPrimary);
	MachineEntity removeImage(UUID machineId, UUID imageId);
	MachineEntity setPrimaryImage(UUID machineId, UUID imageId);
	MaintenanceRecordEntity createMaintenanceRecord(MaintenanceRecordEntity maintenanceRecord, UUID machineId);
	Page<MaintenanceRecordEntity> getMaintenanceHistory(UUID machineId, Pageable pageable);
	Page<MaintenanceRecordEntity> getUpcomingMaintenances(UUID ownerId,int daysAhead, Pageable pageable);

	List<UUID> getMachineIdsByOwner(UUID ownerId);
}
