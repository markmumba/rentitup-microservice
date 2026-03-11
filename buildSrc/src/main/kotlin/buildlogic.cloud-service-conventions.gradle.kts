/*
 * Convention plugin for Spring Boot services that participate in the cloud (Eureka + Config Server).
 * Extends spring-boot-conventions with: Eureka Client, Config Client (discovery-first).
 * Use this for: bff, auth-server, cron-service, notification-service.
 * For services with a database, use microservice-conventions instead.
 */

plugins {
    id("buildlogic.spring-boot-conventions")
}

dependencies {
    // Eureka Client for service discovery
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Config Client — fetches config from config-server via Eureka (discovery-first)
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // xDS support — enables xds:/// channel scheme for proxyless gRPC service mesh
    implementation("io.grpc:grpc-xds")
}