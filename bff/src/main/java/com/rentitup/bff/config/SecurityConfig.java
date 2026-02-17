package com.rentitup.bff.config;

import com.rentitup.bff.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter() {
		FilterRegistrationBean<JwtAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(jwtAuthenticationFilter);
		registrationBean.addUrlPatterns("/api/*");
		registrationBean.setOrder(1);
		return registrationBean;
	}
}
