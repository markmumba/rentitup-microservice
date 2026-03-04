plugins {
	id("buildlogic.spring-boot-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Backend for Frontend (BFF) for Rentitup"

dependencies {
	// Spring Security (only BFF needs full security for HTTP endpoints)
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.session:spring-session-core")
	implementation(project(":shared-libs"))
	implementation(project(":common"))
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
}

application {
	mainClass = "com.rentitup.bff.BffApplication"
}

