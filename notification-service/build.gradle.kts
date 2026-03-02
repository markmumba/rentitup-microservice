plugins {
    id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Notification service for Rentitup - handles email, SMS, and push notifications"

dependencies {
    implementation(project(":shared-libs"))
    implementation(project(":common"))

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // For async processing
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}

application {
    mainClass = "com.rentitup.notification_service.NotificationServiceApplication"
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                environment(key.trim(), value.trim())
            }
    }
}
