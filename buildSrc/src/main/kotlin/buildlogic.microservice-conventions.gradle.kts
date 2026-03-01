/*
 * Convention plugin for Spring Boot microservices with database.
 * Extends spring-boot-conventions with: Database (JPA, Flyway, PostgreSQL)
 */

plugins {
    id("buildlogic.spring-boot-conventions")
}

dependencies {
    // Database & Migrations
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}