/*
 * Convention plugin for Java libraries with gRPC support.
 * Provides: java-library, gRPC/protobuf code generation
 */

import com.google.protobuf.gradle.id

plugins {
    id("buildlogic.java-common-conventions")
    id("io.spring.dependency-management")
    id("com.google.protobuf")
    `java-library`
}

val springGrpcVersion = "1.0.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:$springGrpcVersion")
    }
}

dependencies {
    api("io.grpc:grpc-services")
    api("org.springframework.grpc:spring-grpc-core")
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
