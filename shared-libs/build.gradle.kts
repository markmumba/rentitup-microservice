plugins {
	id("buildlogic.java-library-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Shared libs for the Rentitup microservices"

val jjwtVersion = "0.12.6"
val grpcVersion = "1.71.0"
val springCloudVersion = "2025.1.0"

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
	}
}

dependencies {
	// Lombok
	compileOnly("org.projectlombok:lombok:1.18.36")
	annotationProcessor("org.projectlombok:lombok:1.18.36")
	// JWT
	api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	// gRPC for inter-service communication
	api("io.grpc:grpc-api:$grpcVersion")

	// Spring Boot autoconfigure for @AutoConfiguration, @ConditionalOn* annotations
	compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.5.0")

	// Spring context for @PostConstruct, etc.
	compileOnly("org.springframework:spring-context")

	// Eureka client for service discovery (compileOnly - services provide it)
	compileOnly("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
}
