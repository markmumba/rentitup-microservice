plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "microservice for user data "



dependencies {
	implementation(project(":shared-libs"))
}

application {
	mainClass = "com.rentitup.user_service.UserServiceApplication"
}



