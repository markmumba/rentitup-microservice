plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Backend for Frontend (BFF) for Rentitup"

dependencies {
	implementation("com.google.protobuf:protobuf-java-util")
	implementation(project(":shared-libs"))

	// Spring Security
	implementation("org.springframework.boot:spring-boot-starter-security")

	// Swagger/OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
}

application {
	mainClass = "com.rentitup.bff.BffApplication"
}

