
plugins {
	id("buildlogic.microservice-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "booking service microservice"



dependencies {
	implementation(project(":shared-libs"))
	implementation(project(":common"))
}

