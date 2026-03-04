plugins {
	id("buildlogic.java-library-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Common utilities and cross-cutting concerns for Rentitup microservices"

val jjwtVersion = "0.12.6"
val springBootVersion = "4.0.2"
val springCloudVersion = "2025.1.0"

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
	}
}

dependencies {
	// Spring Security for JWT validation and SecurityUtils
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation(project(":shared-libs"))

	// Spring Boot autoconfigure
	compileOnly("org.springframework.boot:spring-boot-autoconfigure")

	// Eureka client for service discovery (compileOnly - services provide it)
	compileOnly("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

	// Spring Data Commons for Page, Pageable, PageRequest (compileOnly - services provide it)
	compileOnly("org.springframework.data:spring-data-commons")




	// Lombok
	compileOnly("org.projectlombok:lombok:1.18.36")
	annotationProcessor("org.projectlombok:lombok:1.18.36")

	// Logging
	compileOnly("org.slf4j:slf4j-api")
}
