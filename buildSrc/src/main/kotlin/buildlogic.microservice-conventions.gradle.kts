/*
 * Convention plugin for Spring Boot microservices.
 * Extends spring-boot-conventions with: Eureka Client, gRPC, Database (JPA, Flyway, PostgreSQL)
 */

import com.google.protobuf.gradle.id

plugins {
    id("buildlogic.spring-boot-conventions")
    id("com.google.protobuf")
}

val springGrpcVersion = "1.0.2"
val protobufVersion = "4.33.5"

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
    }
}

dependencies {
    // Protobuf version constraints
    constraints {
        implementation("com.google.protobuf:protobuf-java:$protobufVersion")
        implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    }

    // Web server (Spring Boot 4.x uses webmvc naming)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Eureka Client for service discovery
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // gRPC (web-based starter for Spring Boot 4.x compatibility)
    implementation("io.grpc:grpc-services")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")


    // Database & Migrations
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
