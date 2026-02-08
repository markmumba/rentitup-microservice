plugins {
	id("buildlogic.spring-boot-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Service registry for Rentitup (Eureka Server)"

extra["springCloudVersion"] = "2025.1.0"

dependencies {
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}
