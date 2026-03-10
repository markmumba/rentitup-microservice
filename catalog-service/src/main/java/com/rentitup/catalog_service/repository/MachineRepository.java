package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MachineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<MachineEntity, UUID>, JpaSpecificationExecutor<MachineEntity> {
	Page<MachineEntity> findAllByOwnerId(UUID ownerId,Pageable pageable);
	List<MachineEntity> findAllByOwnerId(UUID ownerId);
}
