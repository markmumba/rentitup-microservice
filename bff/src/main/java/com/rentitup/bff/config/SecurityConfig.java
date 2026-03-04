package com.rentitup.bff.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${app.frontend-url}")
	private String frontendUrl;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
												   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/error").permitAll()
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/api/v1/auth/register", "/api/v1/auth/check-email").permitAll()
						.requestMatchers("/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
						.requestMatchers("/api/v1/auth/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/machines/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/machine/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/owner/**").permitAll()
						.anyRequest().authenticated()
				)
				// OAuth2 Login (for browser-based login with sessions)
				.oauth2Login(oauth2 -> oauth2
						.successHandler(oauth2AuthenticationSuccessHandler())
						.failureUrl(frontendUrl + "/login?error=true")
				)
				// Logout configuration
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
				)
				// Resource server for API calls with Bearer token (mobile apps, etc.)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				);

		return http.build();
	}

	@Bean
	public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
		return new AuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
												@NonNull HttpServletResponse response,
												@NonNull Authentication authentication) throws IOException {
				// Redirect to frontend after successful login
				response.sendRedirect(frontendUrl + "/auth/callback?success=true");
			}
		};
	}

	private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
		OidcClientInitiatedLogoutSuccessHandler handler =
				new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
		handler.setPostLogoutRedirectUri(frontendUrl);
		return handler;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendUrl));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}
}
