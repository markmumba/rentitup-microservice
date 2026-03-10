/*
 * Convention plugin for Spring Boot microservices with a PostgreSQL database.
 * Extends cloud-service-conventions with: JPA, Flyway, PostgreSQL.
 * Use this for: user-service, booking-service, catalog-service.
 */

plugins {
    id("buildlogic.cloud-service-conventions")
}

dependencies {
    // Database & Migrations
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}