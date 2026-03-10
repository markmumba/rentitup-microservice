/*
 * Base convention plugin for all Spring Boot applications.
 * Provides: Spring Boot, Lombok, MapStruct, gRPC, Zipkin, Actuator, WebMVC, Validation
 * Does NOT include Eureka client or Config client — use cloud-service-conventions for that.
 * Use directly only for infrastructure services that boot before the cloud (service-registry, config-server).
 */

import com.google.protobuf.gradle.id

plugins {
    id("buildlogic.java-application-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.protobuf")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val mapstructVersion = "1.6.3"
val springCloudVersion = "2025.1.0"
val springGrpcVersion = "1.0.2"
val protobufVersion = "4.33.5"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
    }
}

dependencies {
    // Protobuf
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")

    implementation("org.springframework.boot:spring-boot-micrometer-tracing-brave")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-zipkin")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")


    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Web server
    implementation("org.springframework.boot:spring-boot-starter-webmvc")



    // gRPC
    implementation("io.grpc:grpc-services")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
    testImplementation("org.springframework.boot:spring-boot-starter-zipkin-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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