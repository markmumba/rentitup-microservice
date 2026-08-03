package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<MachineEntity, UUID>, JpaSpecificationExecutor<MachineEntity> {
	Optional<MachineEntity> findByIdAndDeletedFalse(UUID id);
	List<MachineEntity> findAllByIdInAndDeletedFalse(List<UUID> ids);
	List<MachineEntity> findAllByOwnerIdAndDeletedFalse(UUID ownerId);
}
