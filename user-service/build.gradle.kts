plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "microservice for user data "



dependencies {
	implementation(project(":shared-libs"))
	implementation(project(":common"))
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
}

application {
	mainClass = "com.rentitup.user_service.UserServiceApplication"
}



