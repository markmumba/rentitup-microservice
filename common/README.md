# Common Module

Shared library auto-configured by any service that depends on it. Provides gRPC infrastructure (client stub injection, token propagation, server-side auth), exception handling, security utilities, and pagination helpers.

## Module Structure

```
common/src/main/java/com/rentitup/common/
├── grpc/
│   ├── GrpcAutoConfiguration          — Creates GrpcChannelFactory (Eureka-based)
│   ├── GrpcChannelFactory              — Creates & caches gRPC channels with service discovery
│   ├── GrpcExceptionHandler            — Maps domain exceptions → gRPC Status codes
│   ├── EurekaNameResolver              — Resolves service names via Eureka registry
│   ├── EurekaNameResolverProvider      — Registers the resolver with gRPC's registry
│   ├── client/
│   │   ├── GrpcClient                  — @GrpcClient annotation for stub injection
│   │   ├── GrpcClientBeanPostProcessor — Scans beans, injects stubs via reflection
│   │   ├── GrpcClientsAutoConfiguration
│   │   ├── GrpcClientProperties        — Per-service OAuth2 registration config
│   │   ├── BearerTokenInterceptor      — Attaches JWT to every outgoing gRPC call
│   │   ├── TokenResolver               — Decides: user token vs service token
│   │   ├── GrpcContextTokenSupplier    — Extracts user token from Security/gRPC context
│   │   ├── ServiceTokenProvider        — OAuth2 client_credentials for S2S calls
│   │   └── GrpcTokenAutoConfiguration
│   └── server/
│       ├── GrpcAuthInterceptor         — Validates JWT on incoming gRPC calls
│       ├── GrpcAuthContext             — Thread-local auth data (userId, roles, etc.)
│       ├── GrpcAuthProperties          — Configurable public methods list
│       ├── GrpcAuthAutoConfiguration
│       └── GrpcSecurityConfig          — Permits all at Spring Security level
├── exceptions/
│   ├── BadRequestException             → INVALID_ARGUMENT
│   ├── ConflictException               → ALREADY_EXISTS
│   ├── ForbiddenException              → PERMISSION_DENIED
│   ├── NotFoundException               → NOT_FOUND
│   ├── UnauthorizedException           → UNAUTHENTICATED
│   └── ServiceUnavailableException     → UNAVAILABLE
├── security/
│   └── SecurityUtils                   — Extract JWT claims from Spring Security context
└── util/
    └── PaginationHelper                — Convert between Spring Pageable and Protobuf
```

## Autoconfiguration

Five classes registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Any service with `implementation(project(":common"))` gets everything wired automatically.

| # | Class | Creates | Condition |
|---|-------|---------|-----------|
| 1 | `GrpcAutoConfiguration` | `GrpcChannelFactory` | `EurekaClient` on classpath |
| 2 | `GrpcTokenAutoConfiguration` | `TokenResolver`, `ServiceTokenProvider`, `GrpcContextTokenSupplier` | Any known OAuth2 registration exists |
| 3 | `GrpcClientsAutoConfiguration` | `GrpcClientBeanPostProcessor`, `BearerTokenInterceptor` | After token config |
| 4 | `GrpcSecurityConfig` | `AuthenticationProcessInterceptor` (permitAll) | `GrpcSecurity` on classpath |
| 5 | `GrpcAuthAutoConfiguration` | `GrpcAuthInterceptor` (`@GlobalServerInterceptor`) | `JwtDecoder` bean present |

### Why order matters

Spring's `@ConditionalOnBean` checks happen before bean creation. We use `@AutoConfiguration(after = ...)` to guarantee dependencies exist, and rely on constructor injection rather than `@ConditionalOnBean`.

---

## @GrpcClient — Stub Injection

Annotate a field with `@GrpcClient("SERVICE-NAME")` and the post-processor handles the rest:

```java
@Service
public class CatalogController {
    @GrpcClient("CATALOG-SERVICE")
    private CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;
}
```

**How it works** (`GrpcClientBeanPostProcessor`):
1. Scans every Spring bean for `@GrpcClient` fields
2. Extracts service name from annotation value
3. Gets (or creates) a channel from `GrpcChannelFactory` → target: `eureka:///CATALOG-SERVICE`, round-robin LB
4. Detects stub type from class name (`*BlockingStub` / `*FutureStub` / `*Stub`)
5. Calls the matching factory method (e.g. `newBlockingStub(channel)`) via reflection
6. Injects the stub into the field

Channels are cached per service — multiple fields pointing to the same service share one channel.

### Supported stub types

| Stub Class | Factory Method |
|------------|----------------|
| `*BlockingStub` | `newBlockingStub(channel)` |
| `*FutureStub` | `newFutureStub(channel)` |
| `*Stub` (async) | `newStub(channel)` |

---

## Token Resolution

`TokenResolver` uses a two-level strategy to decide which token to attach to outgoing calls:

```
TokenResolver.resolveToken()
│
├─ 1. Try USER token (GrpcContextTokenSupplier)
│     ├─ SecurityUtils.getCurrentTokenValue()  ← from HTTP/Spring Security context
│     └─ GrpcAuthContext.getToken()             ← from incoming gRPC context (forwarding)
│
└─ 2. Fall back to SERVICE token (ServiceTokenProvider)
      └─ OAuth2 client_credentials flow → cached with 60s refresh buffer
```

**When each is used:**
- **User token** — BFF receives an HTTP request with a user JWT, then makes gRPC calls. The user's token is propagated automatically.
- **Service token** — Background jobs, startup tasks, or service-initiated calls where no user context exists. `ServiceTokenProvider` does a `client_credentials` grant.

---

## Client Interceptor — BearerTokenInterceptor

Registered as a `@GlobalClientInterceptor`. On every outgoing gRPC call:
1. Calls `TokenResolver.resolveToken()`
2. Adds `Authorization: Bearer <token>` metadata header
3. Logs whether a USER or SERVICE token was used

---

## Server Interceptor — GrpcAuthInterceptor

Registered as a `@GlobalServerInterceptor` with `@Order(50)`. On every incoming gRPC call:

1. Extracts `Authorization: Bearer <token>` header
2. Decodes JWT via Spring's `JwtDecoder`
3. Populates `GrpcAuthContext` with:
   - **USER tokens**: `user_id`, `email`, `role`, `roles`, `token_type`
   - **SERVICE tokens**: `client_id`, `token_type`
   - **All tokens**: `IS_AUTHENTICATED`, `TOKEN`
4. If no valid token:
   - **Public methods** → allowed with unauthenticated context
   - **Protected methods** → rejected with `Status.UNAUTHENTICATED`

### Default public methods

```
grpc.health.v1.Health/Check
grpc.health.v1.Health/Watch
grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo
```

### Adding custom public methods

```yaml
grpc:
  auth:
    public-methods:
      - "rentitup.user.UserService/CreateUser"
      - "rentitup.user.UserService/GetUserByEmail"
```

### Using GrpcAuthContext in service code

```java
public void someGrpcMethod(...) {
    String userId = GrpcAuthContext.requireUserId();  // throws if not authenticated
    String email  = GrpcAuthContext.getUserEmail();
    boolean admin = GrpcAuthContext.hasRole("ADMIN");

    if (GrpcAuthContext.isServiceToken()) {
        // Called by another service, not a user
    }
}
```

---

## Per-Service OAuth2 Configuration

Different services have different needs. The key property:

```yaml
grpc:
  client:
    oauth2-registration: bff-service   # default: "auth-server"
```

### BFF (two registrations — user login + S2S)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          bff-gateway:                              # User login (authorization_code)
            client-id: bff-gateway
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,read,write
          bff-service:                              # Service-to-service (client_credentials)
            client-id: bff-service
            client-secret: bff-secret
            authorization-grant-type: client_credentials
            scope: internal,user:write,user:read
        provider:
          bff-service:
            token-uri: http://auth-server:9000/oauth2/token

grpc:
  client:
    oauth2-registration: bff-service               # Use this registration for S2S tokens
```

### Backend services (single registration)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          auth-server:                              # Only S2S
            client-id: catalog-service
            client-secret: catalog-secret
            authorization-grant-type: client_credentials
            scope: internal,user:read
        provider:
          auth-server:
            token-uri: http://auth-server:9000/oauth2/token

# grpc.client.oauth2-registration defaults to "auth-server" — no config needed
```

### Which registrations are detected

`GrpcTokenAutoConfiguration` has an `OAuth2ClientConfiguredCondition` that checks for ANY of these:

```
auth-server, bff-service, catalog-service, booking-service, notification-service, cron-service
```

If any registration exists, `ServiceTokenProvider` is created.

---

## Eureka Service Discovery for gRPC

`EurekaNameResolverProvider` and `EurekaNameResolver` bridge gRPC's name resolution with Eureka:

- Channels target `eureka:///SERVICE-NAME`
- The resolver polls Eureka every **10 seconds** for instance updates
- Reads `grpc-port` metadata from each instance (default: `9000`)
- Returns addresses as `EquivalentAddressGroup` for round-robin load balancing

---

## Exception Handling

`GrpcExceptionHandler` maps domain exceptions to gRPC status codes:

| Exception | gRPC Status |
|-----------|-------------|
| `NotFoundException` | `NOT_FOUND` |
| `BadRequestException` | `INVALID_ARGUMENT` |
| `ConflictException` | `ALREADY_EXISTS` |
| `UnauthorizedException` | `UNAUTHENTICATED` |
| `ForbiddenException` | `PERMISSION_DENIED` |
| `ServiceUnavailableException` | `UNAVAILABLE` |
| `IllegalArgumentException` | `INVALID_ARGUMENT` |
| Any other exception | `INTERNAL` |

---

## End-to-End Flows

### User request through BFF

```
HTTP Request (user JWT in Authorization header)
  → BFF Controller (SecurityUtils extracts JWT from Spring Security context)
    → @GrpcClient("CATALOG-SERVICE") stub
      → BearerTokenInterceptor: TokenResolver finds user token → attaches it
        → GrpcChannelFactory: resolves CATALOG-SERVICE via Eureka
          → Catalog Service GrpcAuthInterceptor: validates JWT
            → GrpcAuthContext populated (userId, email, role)
              → Service logic runs
```

### Service-to-service (no user context)

```
Background Job / Scheduled Task
  → @GrpcClient("NOTIFICATION-SERVICE") stub
    → BearerTokenInterceptor: TokenResolver finds no user token
      → Falls back to ServiceTokenProvider (client_credentials grant)
        → Notification Service GrpcAuthInterceptor: validates service JWT
          → GrpcAuthContext populated (clientId, tokenType=SERVER)
            → Service logic runs
```

---

## Requirements

Any service using this module needs:

1. **Dependency on common:**
   ```kotlin
   implementation(project(":common"))
   ```

2. **Eureka client** (for gRPC service discovery):
   ```kotlin
   implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
   ```

3. **Eureka config:**
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8081/eureka
       fetch-registry: true
   ```

4. **OAuth2 client config** (if making authenticated outgoing calls):
   ```yaml
   spring.security.oauth2.client.registration.<name>:
     client-id: ...
     client-secret: ...
     authorization-grant-type: client_credentials
   ```

---

## Debugging

```yaml
logging:
  level:
    com.rentitup.common.grpc: DEBUG
```

### Startup logs to look for

```
GrpcChannelFactory initialized
[gRPC-CONFIG] GrpcContextTokenSupplier created
[gRPC-CONFIG] ServiceTokenProvider created for client: bff-service (registration: bff-service)
[gRPC-CONFIG] TokenResolver created WITH ServiceTokenProvider
[gRPC-CONFIG] BearerTokenInterceptor registered as GlobalClientInterceptor
[gRPC-CONFIG] GrpcAuthInterceptor registered with public methods: [...]
```

### Per-request logs

```
[gRPC-CLIENT] Forwarding USER token to: rentitup.user.UserService/GetUser
[gRPC-SERVER] Authenticated: email=user@example.com, type=USER, userId=123
```

For detailed gRPC-specific docs, see [grpc/README.md](src/main/java/com/rentitup/common/grpc/README.md).