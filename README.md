# Rentitup Backend

A Spring Boot 4.x microservices monorepo for the Rentitup equipment rental platform.

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.2**
- **Spring Cloud 2025.1.0** (Eureka for service discovery)
- **Spring gRPC 1.0.2**
- **PostgreSQL 18**
- **Flyway** (database migrations)
- **Gradle 9.3** (with convention plugins)

## Project Structure

```
backend/
├── buildSrc/                    # Gradle convention plugins
│   ├── build.gradle.kts         # Plugin version definitions
│   └── src/main/kotlin/         # Convention plugin implementations
├── shared-libs/                 # Shared protobuf definitions & utilities
├── catalog-service/             # Equipment catalog microservice
├── service-registry/            # Eureka Server for service discovery
├── docker-compose.yml           # PostgreSQL container
└── .env                         # Environment variables
```

## Modules

| Module | Port | Description |
|--------|------|-------------|
| **service-registry** | 8081 | Eureka Server for service discovery |
| **catalog-service** | 8082 (HTTP), 9090 (gRPC) | Equipment catalog management |
| **shared-libs** | N/A | Shared protobuf definitions |

---

## Build System - Convention Plugins

This project uses **Gradle convention plugins** in `buildSrc/` for DRY build configuration.

### Plugin Versions (Single Source of Truth)

Defined in `buildSrc/build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.2")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.5")
}
```

### Dependency Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           buildSrc/build.gradle.kts                             │
│                         (Plugin Versions - Single Source)                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│  • spring-boot-gradle-plugin:4.0.2                                              │
│  • dependency-management-plugin:1.1.7                                           │
│  • protobuf-gradle-plugin:0.9.5                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      buildlogic.java-common-conventions                         │
│                              (BASE LAYER)                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Plugins: java                                                                  │
│  Provides: Java 21, Maven Central, JUnit 5                                      │
└─────────────────────────────────────────────────────────────────────────────────┘
                          │                              │
                          ▼                              ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│ buildlogic.java-application-conventions │ │ buildlogic.java-library-conventions │
│           (FOR APPS)                    │ │         (FOR LIBRARIES)             │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ Extends: java-common-conventions     │  │ Extends: java-common-conventions     │
│ Plugins: application                 │  │ Plugins: java-library, protobuf,     │
│                                      │  │          dependency-management       │
│                                      │  │ Provides: gRPC core, protobuf codegen│
│                                      │  │ BOM: spring-grpc 1.0.2               │
└──────────────────────────────────────┘  └──────────────────────────────────────┘
                          │                              │
                          ▼                              │
┌──────────────────────────────────────┐                 │
│  buildlogic.spring-boot-conventions  │                 │
│       (FOR SPRING BOOT APPS)         │                 │
├──────────────────────────────────────┤                 │
│ Extends: java-application-conventions│                 │
│ Plugins: spring-boot,                │                 │
│          dependency-management       │                 │
│ Provides:                            │                 │
│  • Lombok                            │                 │
│  • MapStruct 1.6.3                   │                 │
│  • spring-boot-starter-validation    │                 │
│  • spring-boot-starter-test          │                 │
│ BOM: spring-cloud 2025.1.0           │                 │
└──────────────────────────────────────┘                 │
           │                    │                        │
           ▼                    ▼                        ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│  microservice-      │ │  service-registry   │ │    shared-libs      │
│  conventions        │ │  (build.gradle.kts) │ │  (build.gradle.kts) │
│  (FOR MICROSERVICES)│ │                     │ │                     │
├─────────────────────┤ ├─────────────────────┤ ├─────────────────────┤
│Extends: spring-boot-│ │Uses: spring-boot-   │ │Uses: java-library-  │
│        conventions  │ │      conventions    │ │      conventions    │
│Plugins: protobuf    │ │                     │ │                     │
│Provides:            │ │Adds:                │ │Provides:            │
│ • webmvc            │ │ • eureka-server     │ │ • gRPC services API │
│ • eureka-client     │ │ • webmvc            │ │ • protobuf codegen  │
│ • grpc-server-web   │ │                     │ │                     │
│ • data-jpa          │ │                     │ │                     │
│ • flyway            │ │                     │ │                     │
│ • postgresql        │ │                     │ │                     │
│BOM: spring-grpc 1.0.2│ │                     │ │                     │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘
           │                                             │
           ▼                                             │
┌─────────────────────────────────────────────────────────────────────┐
│                    catalog-service (build.gradle.kts)               │
├─────────────────────────────────────────────────────────────────────┤
│ Uses: microservice-conventions                                      │
│ Dependencies: project(":shared-libs")  ◄────────────────────────────┘
│                                                                     │
│ INHERITS ALL OF:                                                    │
│  • Java 21, JUnit 5                    (from java-common)           │
│  • application plugin                  (from java-application)      │
│  • Spring Boot 4.0.2                   (from spring-boot)           │
│  • Lombok, MapStruct                   (from spring-boot)           │
│  • Spring Cloud BOM 2025.1.0           (from spring-boot)           │
│  • Eureka Client, gRPC, JPA, Flyway    (from microservice)          │
│  • gRPC proto definitions              (from shared-libs)           │
└─────────────────────────────────────────────────────────────────────┘
```

### Module to Plugin Mapping

| Module | Convention Plugin | What It Gets |
|--------|------------------|--------------|
| **shared-libs** | `java-library-conventions` | gRPC/protobuf codegen, spring-grpc-core |
| **service-registry** | `spring-boot-conventions` | Spring Boot, Lombok, MapStruct, Spring Cloud BOM + Eureka Server |
| **catalog-service** | `microservice-conventions` | Everything above + Eureka Client, gRPC, JPA, Flyway, PostgreSQL |

### Inheritance Chain

```
catalog-service
    └── microservice-conventions
            └── spring-boot-conventions
                    └── java-application-conventions
                            └── java-common-conventions

service-registry
    └── spring-boot-conventions
            └── java-application-conventions
                    └── java-common-conventions

shared-libs
    └── java-library-conventions
            └── java-common-conventions
```

---

## Getting Started

### Prerequisites

- Java 21
- Docker (for PostgreSQL)

### 1. Start PostgreSQL

```bash
docker-compose up -d
```

### 2. Configure Environment

Create a `.env` file (or use the existing one):

```env
POSTGRES_USER=administrator
POSTGRES_PASSWORD=qwerty123
POSTGRES_DB=rentitup
DB_PORT=5433

DB_URL=jdbc:postgresql://localhost:5433/rentitup
DB_USERNAME=administrator
DB_PASSWORD=qwerty123
```

### 3. Run Services

**Terminal 1 - Start Eureka Server:**
```bash
./gradlew :service-registry:bootRun
```

**Terminal 2 - Start Catalog Service:**
```bash
./gradlew :catalog-service:bootRun
```

### 4. Verify

- **Eureka Dashboard:** http://localhost:8081
- **Catalog Service:** http://localhost:8082
- **gRPC:** localhost:9090

---

## Common Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :catalog-service:build

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# Check dependencies
./gradlew :catalog-service:dependencies --configuration runtimeClasspath
```

---

## Adding a New Microservice

1. Create a new module directory (e.g., `order-service/`)

2. Add to `settings.gradle.kts`:
   ```kotlin
   include("order-service")
   ```

3. Create `order-service/build.gradle.kts`:
   ```kotlin
   plugins {
       id("buildlogic.microservice-conventions")
   }

   group = "com.rentitup"
   version = "0.0.1-SNAPSHOT"

   dependencies {
       implementation(project(":shared-libs"))
   }

   application {
       mainClass = "com.rentitup.order_service.OrderServiceApplication"
   }
   ```

4. The new service automatically gets:
   - Spring Boot 4.0.2
   - Eureka Client (service discovery)
   - gRPC support
   - JPA + Flyway + PostgreSQL
   - Lombok + MapStruct
## Notification Delivery Reference

The current SSE/gRPC notification flow and the proposed multi-instance
RabbitMQ fanout architecture are documented in
[NOTIFICATION_DELIVERY_ARCHITECTURE.md](NOTIFICATION_DELIVERY_ARCHITECTURE.md).
