package com.rentitup.user_service.service;

import com.rentitup.user_service.entities.RefreshTokenEntity;
import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;

public interface AuthService {

	/**
	 * Register a new user and return the created user
	 */
	UserEntity register(String email, String password, String fullName, String phone, ERole role);

	/**
	 * Authenticate a user and return the user entity
	 */
	UserEntity authenticate(String email, String password);

	/**
	 * Create and store a refresh token for a user
	 */
	RefreshTokenEntity createRefreshToken(UserEntity user, String token, long expirationMs);

	/**
	 * Validate a refresh token and return the associated user
	 */
	UserEntity validateRefreshToken(String token);

	/**
	 * Revoke a specific refresh token
	 */
	void revokeRefreshToken(String token);

	/**
	 * Revoke all refresh tokens for a user (logout from all devices)
	 */
	void revokeAllUserTokens(UserEntity user);
}
