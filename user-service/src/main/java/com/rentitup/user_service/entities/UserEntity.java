package com.rentitup.user_service.entities;


import com.rentitup.user_service.common.entities.BaseEntity;
import com.rentitup.user_service.enums.ERole;
import com.rentitup.user_service.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity implements UserDetails {

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String password;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	private String phone;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "role", nullable = false)
	private ERole role = ERole.CUSTOMER;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "kyc_status", nullable = false)
	private KycStatus kycStatus = KycStatus.PENDING;

	@Column(name = "business_license")
	private String businessLicense;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Builder.Default
	@Column(name = "is_enabled")
	private boolean enabled = true;

	@Builder.Default
	@Column(name = "is_locked")
	private boolean locked = false;

	// ==================== UserDetails Implementation ====================

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
