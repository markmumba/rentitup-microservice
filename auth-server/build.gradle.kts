plugins {
	id("buildlogic.cloud-service-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Authentication server to control security on the platform"


dependencies {
	implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-h2console")
	runtimeOnly("com.h2database:h2")
	implementation(project(":shared-libs"))
	implementation(project(":common"))
}

