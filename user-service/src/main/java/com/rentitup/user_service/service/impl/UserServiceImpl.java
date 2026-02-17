package com.rentitup.user_service.service.impl;

import com.rentitup.shared_libs.exceptions.BadRequestException;
import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import com.rentitup.user_service.repository.UserRepository;
import com.rentitup.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public UserEntity register(String email, String password, String fullName, String phone, ERole role) {
		log.info("Registering new user with email: {}", email);

		if (userRepository.existsByEmail(email)) {
			throw new BadRequestException("Email already registered: " + email);
		}

		UserEntity user = UserEntity.builder()
				.email(email)
				.password(passwordEncoder.encode(password))
				.fullName(fullName)
				.phone(phone)
				.role(role != null ? role : ERole.CUSTOMER)
				.kycStatus(KycStatus.PENDING)
				.build();

		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserEntity authenticate(String email, String password) {
		log.info("Authenticating user: {}", email);

		UserEntity user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BadRequestException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new BadRequestException("Invalid email or password");
		}

		return user;
	}

	@Override
	@Transactional
	public UserEntity createUser(UserEntity user) {
		log.info("Creating user with email: {}", user.getEmail());

		if (userRepository.existsByEmail(user.getEmail())) {
			throw new BadRequestException("Email already registered: " + user.getEmail());
		}

		// Hash password if provided
		if (user.getPassword() != null) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}

		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserEntity getUserById(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new BadRequestException("User not found: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public UserEntity getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new BadRequestException("User not found: " + email));
	}

	@Override
	@Transactional
	public UserEntity updateUser(UUID id, UserEntity updates) {
		log.info("Updating user: {}", id);

		UserEntity existing = getUserById(id);

		if (updates.getFullName() != null) {
			existing.setFullName(updates.getFullName());
		}
		if (updates.getPhone() != null) {
			existing.setPhone(updates.getPhone());
		}
		if (updates.getBusinessLicense() != null) {
			existing.setBusinessLicense(updates.getBusinessLicense());
		}
		if (updates.getProfileImageUrl() != null) {
			existing.setProfileImageUrl(updates.getProfileImageUrl());
		}

		return userRepository.save(existing);
	}

	@Override
	@Transactional
	public UserEntity verifyUser(UUID id, KycStatus kycStatus) {
		log.info("Verifying user: {} with status: {}", id, kycStatus);

		UserEntity user = getUserById(id);
		user.setKycStatus(kycStatus);

		if (kycStatus == KycStatus.VERIFIED) {
			user.setVerifiedAt(LocalDateTime.now());
		}

		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<UserEntity> listUsers(ERole role, KycStatus kycStatus, Pageable pageable) {
		return userRepository.findAllWithFilters(role, kycStatus, pageable);
	}
}
