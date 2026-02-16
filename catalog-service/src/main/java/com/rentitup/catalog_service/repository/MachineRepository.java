package com.rentitup.catalog_service.repository;

import com.rentitup.catalog_service.entities.MachineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MachineRepository extends JpaRepository<MachineEntity, UUID>, JpaSpecificationExecutor<MachineEntity> {
}
