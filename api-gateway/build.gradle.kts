plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "API Gateway for Rentitup"

dependencies {
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
	implementation(project(":shared-libs"))
}

application {
	mainClass = "com.rentitup.api_gateway.ApiGatewayApplication"
}

