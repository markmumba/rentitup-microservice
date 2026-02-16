package com.rentitup.catalog_service.service;

import com.rentitup.catalog_service.entities.MachineEntity;
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

	// Single generic query method - specification built by gRPC layer
	Page<MachineEntity> findAll(Specification<MachineEntity> spec, Pageable pageable);

	// Batch fetch for inter-service calls
	List<MachineEntity> findAllByIds(List<UUID> ids);
}
