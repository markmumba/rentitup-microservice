plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Backend for Frontend (BFF) for Rentitup"

dependencies {
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
	implementation("com.google.protobuf:protobuf-java-util")
	implementation(project(":shared-libs"))
}

application {
	mainClass = "com.rentitup.bff.BffApplication"
}

