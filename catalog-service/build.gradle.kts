plugins {
    id("buildlogic.spring-service-conventions")
}

group = "com.rentitup"
version = "0.0.1-SNAPSHOT"
description = "Catalog service for Rentitup"

dependencies {

    implementation(project(":shared-libs"))

}

application {
    mainClass = "com.rentitup.catalog_service.CatalogServiceApplication"
}
