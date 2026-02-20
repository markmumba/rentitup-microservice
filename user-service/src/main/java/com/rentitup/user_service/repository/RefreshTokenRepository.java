package com.rentitup.user_service.repository;

import com.rentitup.user_service.entities.RefreshTokenEntity;
import com.rentitup.user_service.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

	Optional<RefreshTokenEntity> findByToken(String token);

	Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(String token);

	@Modifying
	@Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
	void revokeAllByUser(@Param("user") UserEntity user);

	@Modifying
	@Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true WHERE rt.token = :token")
	void revokeByToken(@Param("token") String token);

	@Modifying
	@Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
	void deleteExpiredTokens();
}
