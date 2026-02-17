package com.rentitup.user_service.entities;


import com.rentitup.user_service.common.entities.BaseEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash")
	private String password;

	@Column(name = "full_name")
	private String fullName;

	private String phone;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private ERole role = ERole.CUSTOMER;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "kyc_status")
	private KycStatus kycStatus = KycStatus.PENDING;

	@Column(name = "business_license")
	private String businessLicense;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

}
