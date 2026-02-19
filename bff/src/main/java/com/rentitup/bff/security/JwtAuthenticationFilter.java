package com.rentitup.bff.security;

import com.rentitup.shared_libs.security.JwtClaims;
import com.rentitup.shared_libs.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);

		try {
			JwtClaims claims = jwtUtil.validateAccessToken(token);

			JwtAuthentication authentication = new JwtAuthentication(
					claims.getUserId(),
					claims.getEmail(),
					claims.getRole()
			);

			SecurityContextHolder.getContext().setAuthentication(authentication);

			// Also set request attributes for convenience
			request.setAttribute("userId", claims.getUserId().toString());
			request.setAttribute("userEmail", claims.getEmail());
			request.setAttribute("userRole", claims.getRole());

		} catch (Exception e) {
			log.warn("JWT validation failed: {}", e.getMessage());
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}
}
