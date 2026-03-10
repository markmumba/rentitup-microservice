package com.rentitup.user_service.repository;

import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	@Query("SELECT u FROM UserEntity u WHERE " +
			"(:role IS NULL OR u.role = :role) AND " +
			"(:kycStatus IS NULL OR u.kycStatus = :kycStatus)")
	Page<UserEntity> findAllWithFilters(
			@Param("role") ERole role,
			@Param("kycStatus") KycStatus kycStatus,
			Pageable pageable
	);
}
