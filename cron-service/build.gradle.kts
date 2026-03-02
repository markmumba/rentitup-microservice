
plugins {
	id("buildlogic.spring-boot-conventions")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "service to handle all cron jobs for rentitup"



dependencies {
	implementation(project(":common"))
	implementation(project(":shared-libs"))

}

