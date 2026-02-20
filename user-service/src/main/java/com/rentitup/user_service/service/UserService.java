package com.rentitup.user_service.service;

import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

	UserEntity createUser(UserEntity user);
	UserEntity getUserById(UUID id);
	UserEntity getUserByEmail(String email);
	UserEntity updateUser(UUID id, UserEntity updates);
	UserEntity verifyUser(UUID id, KycStatus kycStatus);
	Page<UserEntity> listUsers(ERole role, KycStatus kycStatus, Pageable pageable);
}
