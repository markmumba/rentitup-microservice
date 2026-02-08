/*
 * Convention plugin for Spring Boot microservices.
 * Extends spring-boot-conventions with: Eureka Client, gRPC, Database (JPA, Flyway, PostgreSQL)
 * Use this for microservices that register with Eureka and communicate via gRPC.
 */

import com.google.protobuf.gradle.id

plugins {
    id("buildlogic.spring-boot-conventions")
    id("com.google.protobuf")
}

val springGrpcVersion = "1.0.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
    }
}

dependencies {
    // Eureka Client for service discovery
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // gRPC
    implementation("io.grpc:grpc-services")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")

    // Database & Migrations
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}
