import com.google.protobuf.gradle.id

plugins {
	id("buildlogic.java-library-conventions")
	id("io.spring.dependency-management")
	id("com.google.protobuf")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "shared libs for the rentitup "


extra["springGrpcVersion"] = "1.0.1"

dependencies {
	implementation("io.grpc:grpc-services")
	implementation("org.springframework.grpc:spring-grpc-core")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
	}
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
