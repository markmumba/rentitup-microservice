plugins {
	id("buildlogic.java-library-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Shared libs for the Rentitup microservices"

val jjwtVersion = "0.12.6"

dependencies {
	// JWT
	api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
}
