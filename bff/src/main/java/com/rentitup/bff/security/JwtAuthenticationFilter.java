package com.rentitup.bff.security;

import com.rentitup.shared_libs.security.JwtClaims;
import com.rentitup.shared_libs.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	// Paths that don't require authentication
	private static final List<String> PUBLIC_PATHS = List.of(
			"/api/v1/auth/login",
			"/api/v1/auth/register",
			"/api/v1/auth/refresh",
			"/api-docs",
			"/swagger-ui",
			"/swagger-resources",
			"/v3/api-docs",
			"/actuator"
	);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String path = request.getRequestURI();

			// Skip authentication for public paths
			if (isPublicPath(path)) {
				filterChain.doFilter(request, response);
				return;
			}

			String authHeader = request.getHeader("Authorization");

			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				sendUnauthorized(response, "Missing or invalid Authorization header");
				return;
			}

			String token = authHeader.substring(7);

			try {
				JwtClaims claims = jwtUtil.validateAccessToken(token);

				// Set user context for the request
				UserContext context = new UserContext(
						claims.getUserId(),
						claims.getEmail(),
						claims.getRole()
				);
				UserContext.set(context);

				// Add user info to request attributes for downstream use
				request.setAttribute("userId", claims.getUserId().toString());
				request.setAttribute("userEmail", claims.getEmail());
				request.setAttribute("userRole", claims.getRole());

				filterChain.doFilter(request, response);
			} catch (Exception e) {
				log.warn("JWT validation failed: {}", e.getMessage());
				sendUnauthorized(response, "Invalid or expired token");
			}
		} finally {
			// Always clear the context after the request
			UserContext.clear();
		}
	}

	private boolean isPublicPath(String path) {
		return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
	}

	private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write(String.format(
				"{\"status\":401,\"message\":\"%s\",\"data\":null}", message
		));
	}
}
