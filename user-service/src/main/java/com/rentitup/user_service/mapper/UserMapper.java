package com.rentitup.user_service.mapper;

import com.rentitup.shared.proto.common.Timestamp;
import com.rentitup.shared.proto.user.*;
import com.rentitup.user_service.entities.UserEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Mapper(
		componentModel = MappingConstants.ComponentModel.SPRING,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserMapper {

	default User toProto(UserEntity entity) {
		if (entity == null) return null;

		User.Builder builder = User.newBuilder()
				.setId(entity.getId().toString())
				.setEmail(entity.getEmail())
				.setFullName(entity.getFullName())
				.setUserType(mapRole(entity.getRole()))
				.setKycStatus(mapKycStatus(entity.getKycStatus()))
				.setIsActive(entity.isEnabled());

		if (entity.getPhone() != null) {
			builder.setPhone(entity.getPhone());
		}
		if (entity.getProfileImageUrl() != null) {
			builder.setProfileImageUrl(entity.getProfileImageUrl());
		}
		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(mapTimestamp(entity.getCreatedAt()));
		}
		if (entity.getUpdatedAt() != null) {
			builder.setUpdatedAt(mapTimestamp(entity.getUpdatedAt()));
		}
		if (entity.getVerifiedAt() != null) {
			builder.setVerifiedAt(mapTimestamp(entity.getVerifiedAt()));
		}

		return builder.build();
	}

	/**
	 * Converts UserEntity to Proto User including password hash.
	 * Use this only for auth server requests where password verification is needed.
	 */
	default User toProtoWithHash(UserEntity entity) {
		if (entity == null) return null;

		User.Builder builder = User.newBuilder()
				.setId(entity.getId().toString())
				.setEmail(entity.getEmail())
				.setFullName(entity.getFullName())
				.setUserType(mapRole(entity.getRole()))
				.setKycStatus(mapKycStatus(entity.getKycStatus()))
				.setIsActive(entity.isEnabled())
				.setPasswordHash(entity.getPassword());

		if (entity.getPhone() != null) {
			builder.setPhone(entity.getPhone());
		}
		if (entity.getProfileImageUrl() != null) {
			builder.setProfileImageUrl(entity.getProfileImageUrl());
		}
		if (entity.getCreatedAt() != null) {
			builder.setCreatedAt(mapTimestamp(entity.getCreatedAt()));
		}
		if (entity.getUpdatedAt() != null) {
			builder.setUpdatedAt(mapTimestamp(entity.getUpdatedAt()));
		}
		if (entity.getVerifiedAt() != null) {
			builder.setVerifiedAt(mapTimestamp(entity.getVerifiedAt()));
		}

		return builder.build();
	}

	default UserType mapRole(ERole role) {
		if (role == null) return UserType.USER_TYPE_UNSPECIFIED;
		return switch (role) {
			case ADMIN -> UserType.ADMIN;
			case OWNER -> UserType.OWNER;
			case CUSTOMER -> UserType.CUSTOMER;
		};
	}

	default ERole mapUserType(UserType userType) {
		if (userType == null) return ERole.CUSTOMER;
		return switch (userType) {
			case ADMIN -> ERole.ADMIN;
			case OWNER -> ERole.OWNER;
			case CUSTOMER -> ERole.CUSTOMER;
			case USER_TYPE_UNSPECIFIED, UNRECOGNIZED -> ERole.CUSTOMER;
		};
	}

	default com.rentitup.shared.proto.user.KycStatus mapKycStatus(KycStatus status) {
		if (status == null) return com.rentitup.shared.proto.user.KycStatus.KYC_STATUS_UNSPECIFIED;
		return switch (status) {
			case PENDING -> com.rentitup.shared.proto.user.KycStatus.PENDING;
			case VERIFIED -> com.rentitup.shared.proto.user.KycStatus.VERIFIED;
			case REJECTED -> com.rentitup.shared.proto.user.KycStatus.REJECTED;
		};
	}

	default KycStatus mapProtoKycStatus(com.rentitup.shared.proto.user.KycStatus status) {
		if (status == null) return KycStatus.PENDING;
		return switch (status) {
			case PENDING -> KycStatus.PENDING;
			case VERIFIED -> KycStatus.VERIFIED;
			case REJECTED -> KycStatus.REJECTED;
			case KYC_STATUS_UNSPECIFIED, UNRECOGNIZED -> KycStatus.PENDING;
		};
	}

	default Timestamp mapTimestamp(LocalDateTime dateTime) {
		if (dateTime == null) return Timestamp.getDefaultInstance();
		Instant instant = dateTime.toInstant(ZoneOffset.UTC);
		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}
}
