plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Authentication server to control security on the platform"


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
}

