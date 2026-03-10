package com.rentitup.bff.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			@NonNull AccessDeniedException accessDeniedException) throws IOException, ServletException {

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);

		Map<String, Object> body = new HashMap<>();
		body.put("status", HttpServletResponse.SC_FORBIDDEN);
		body.put("error", "Forbidden");
		body.put("message", "You don't have permission to access this resource.");
		body.put("path", request.getServletPath());
		body.put("timestamp", Instant.now().toString());

		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
