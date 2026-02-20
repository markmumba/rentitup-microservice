package com.rentitup.user_service.service.impl;

import com.rentitup.shared_libs.exceptions.BadRequestException;
import com.rentitup.user_service.entities.RefreshTokenEntity;
import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import com.rentitup.user_service.repository.RefreshTokenRepository;
import com.rentitup.user_service.repository.UserRepository;
import com.rentitup.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthenticationManager authenticationManager;
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
				.enabled(true)
				.locked(false)
				.build();

		return userRepository.save(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserEntity authenticate(String email, String password) {

		log.info("Authenticating user: {}", email);

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, password)
			);

			return (UserEntity) authentication.getPrincipal();
		} catch (DisabledException e) {
			log.warn("User account is disabled: {}", email);
			throw new BadRequestException("Account is disabled");
		} catch (LockedException e) {
			log.warn("User account is locked: {}", email);
			throw new BadRequestException("Account is locked");
		} catch (BadCredentialsException e) {
			log.warn("Invalid credentials for user: {}", email);
			throw new BadRequestException("Invalid email or password");
		}
	}

	@Override
	@Transactional
	public RefreshTokenEntity createRefreshToken(UserEntity user, String token, long expirationMs) {
		RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
				.token(token)
				.user(user)
				.expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000))
				.revoked(false)
				.build();

		return refreshTokenRepository.save(refreshToken);
	}

	@Override
	@Transactional(readOnly = true)
	public UserEntity validateRefreshToken(String token) {
		RefreshTokenEntity refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
				.orElseThrow(() -> new BadRequestException("Invalid or revoked refresh token"));

		if (refreshToken.isExpired()) {
			throw new BadRequestException("Refresh token has expired");
		}

		return refreshToken.getUser();
	}

	@Override
	@Transactional
	public void revokeRefreshToken(String token) {
		refreshTokenRepository.revokeByToken(token);
	}

	@Override
	@Transactional
	public void revokeAllUserTokens(UserEntity user) {
		refreshTokenRepository.revokeAllByUser(user);
	}
}
