package com.rentitup.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRedirectController {

	@Value("${app.frontend-url}")
	private String frontendUrl;

	@GetMapping("/")
	public String redirectToFrontend() {
		return "redirect:" + frontendUrl;
	}
}
