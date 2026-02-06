package com.rentitup.catalog_service.repository;


import com.rentitup.catalog_service.entities.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
	Optional<CategoryEntity> findByName(String name);
	boolean existsByName(String name);
	@Query("SELECT c FROM CategoryEntity c WHERE SIZE(c.machines) > 0")
	Page<CategoryEntity> findAllWithMachines(Pageable pageable);

}
