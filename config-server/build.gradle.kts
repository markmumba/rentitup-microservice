plugins {
	id("buildlogic.spring-boot-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "config server for rentitup"


dependencies {
	implementation("org.springframework.cloud:spring-cloud-config-server")
	// Registers with Eureka so services can find it via discovery-first config
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
}

