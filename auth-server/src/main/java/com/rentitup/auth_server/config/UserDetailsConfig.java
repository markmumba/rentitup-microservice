package com.rentitup.auth_server.config;

import com.rentitup.common.grpc.client.GrpcClient;
import com.rentitup.shared.proto.user.GetUserByEmailRequest;
import com.rentitup.shared.proto.user.UserResponse;
import com.rentitup.shared.proto.user.UserServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

@Configuration
@Slf4j
public class UserDetailsConfig {

	@GrpcClient("USER-SERVICE")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {
			log.info("Getting user details for {}", username);

			try {
				UserResponse response = userServiceStub.getUserByEmail(
						GetUserByEmailRequest.newBuilder()
								.setEmail(username)
								.build()
				);

				var user = response.getUser();

				log.info("Found user:{} with type {} ", user.getEmail(), user.getUserType());

				return User.builder()
						.username(user.getEmail())
						.password(user.getPasswordHash())
						.authorities(List.of(
								new SimpleGrantedAuthority("ROLE_" + user.getUserType().name())
						))
						.accountExpired(false)
						.accountLocked(false)
						.disabled(!user.getIsActive())
						.build();
			}catch (Exception e) {
				log.error("Failed to get user details for {}", username, e);
				throw new UsernameNotFoundException("Failed to get user details for " + username, e);
			}
		};
	}
}
