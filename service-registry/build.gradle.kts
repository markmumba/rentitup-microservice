plugins {
	id("buildlogic.spring-boot-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Service registry for Rentitup (Eureka Server)"

dependencies {
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// Spring Security for web security
	implementation("org.springframework.boot:spring-boot-starter-security")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
