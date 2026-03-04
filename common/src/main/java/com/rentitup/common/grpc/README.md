# gRPC Auto-Configuration

This package provides:
- Automatic injection of gRPC client stubs using `@GrpcClient`
- JWT token propagation for gRPC client calls (`BearerTokenInterceptor`)
- JWT authentication for gRPC server calls (`GrpcAuthInterceptor`)

## Usage

```java
@RestController
public class MyController {

    @GrpcClient("USER-SERVICE")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return userServiceStub.getUser(GetUserRequest.newBuilder().setId(id).build());
    }
}
```

The service name (e.g., `"USER-SERVICE"`) must match the application name registered in Eureka.

## How It Works

### Components

| Component | Purpose |
|-----------|---------|
| `GrpcClient` | Annotation to mark fields for gRPC stub injection |
| `GrpcClientBeanPostProcessor` | Processes beans and injects stubs via reflection |
| `GrpcChannelFactory` | Creates and caches gRPC channels using Eureka for service discovery |
| `GrpcAutoConfiguration` | Creates the `GrpcChannelFactory` bean |
| `GrpcClientsAutoConfiguration` | Creates the `BeanPostProcessor` and `BearerTokenInterceptor` beans |

### Channel Interceptors

The `GrpcChannelFactory` applies client interceptors (like `BearerTokenInterceptor`) to all channels it creates. This ensures that every gRPC call automatically includes authentication tokens.

```java
// GrpcChannelFactory applies interceptors when creating channels
ManagedChannelBuilder<?> builder = ManagedChannelBuilder
    .forTarget("eureka:///" + serviceName)
    .usePlaintext();

if (!interceptors.isEmpty()) {
    builder.intercept(interceptors);  // BearerTokenInterceptor applied here
}

return builder.build();
```

**Key point:** Interceptors are resolved lazily via `ObjectProvider<ClientInterceptor>` to avoid circular dependencies. The interceptors are only resolved when the first channel is created, not when the factory is instantiated.

```java
// Lazy resolution prevents circular dependency issues
private List<ClientInterceptor> getInterceptors() {
    if (resolvedInterceptors == null) {
        resolvedInterceptors = interceptorProvider.orderedStream().toList();
    }
    return resolvedInterceptors;
}
```

This is important because:
1. `GrpcChannelFactory` is created early (needed by `GrpcClientBeanPostProcessor`)
2. `BearerTokenInterceptor` depends on `TokenResolver` which depends on `ServiceTokenProvider`
3. Lazy resolution ensures all beans are available before interceptors are applied

### Auto-Configuration Order

The auto-configurations run in this order:

1. `EurekaClientAutoConfiguration` - Creates the Eureka client
2. `GrpcAutoConfiguration` - Creates `GrpcChannelFactory` (requires Eureka client)
3. `GrpcClientsAutoConfiguration` - Creates `GrpcClientBeanPostProcessor`

This ordering is enforced using `@AutoConfiguration(after = ...)`.

### Lazy Initialization

The `GrpcClientBeanPostProcessor` uses lazy initialization via `ObjectProvider<GrpcChannelFactory>` because:

- `BeanPostProcessor` beans are created very early in Spring's lifecycle
- The `GrpcChannelFactory` (which depends on Eureka) may not be available yet
- Using `ObjectProvider` defers the lookup until the factory is actually needed

```java
// Instead of direct injection (fails if factory not ready):
public GrpcClientBeanPostProcessor(GrpcChannelFactory channelFactory)

// We use ObjectProvider for lazy lookup:
public GrpcClientBeanPostProcessor(ObjectProvider<GrpcChannelFactory> channelFactoryProvider)
```

## Supported Stub Types

The processor automatically detects and creates the appropriate stub type:

| Stub Class | Factory Method |
|------------|----------------|
| `*BlockingStub` | `newBlockingStub(channel)` |
| `*FutureStub` | `newFutureStub(channel)` |
| `*Stub` (async) | `newStub(channel)` |

## Requirements

Your service must have:

1. **Eureka client dependency:**
   ```kotlin
   implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
   ```

2. **Eureka configuration in `application.yml`:**
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8081/eureka
       fetch-registry: true
   ```

3. **The `common` module as a dependency:**
   ```kotlin
   implementation(project(":common"))
   ```

## Troubleshooting

### Stub is null at runtime

If your `@GrpcClient` field is null:

1. **Check logs for errors** - Look for messages like:
   - `"GrpcChannelFactory not available"` - Eureka client isn't configured
   - `"Failed to create gRPC stub"` - Channel creation failed

2. **Verify Eureka is running** - The service registry must be up and the target service registered

3. **Don't use `@RequiredArgsConstructor`** - This annotation can interfere with field injection. Use it only if you have actual constructor dependencies.

4. **Check service name** - The name in `@GrpcClient("SERVICE-NAME")` must match the target service's `spring.application.name` in Eureka (case-sensitive)

### No token being sent (UNAUTHENTICATED errors)

If you see `UNAUTHENTICATED: Authentication required` errors:

1. **Check interceptor count on startup:**
   ```
   GrpcChannelFactory initialized
   Resolved 1 client interceptors for gRPC channels
   ```
   If you see `0 client interceptors`, the `BearerTokenInterceptor` isn't being created.

2. **Verify TokenResolver has ServiceTokenProvider:**
   ```
   [gRPC-CONFIG] TokenResolver created WITH ServiceTokenProvider
   ```
   If you see `user-only mode`, check your OAuth2 client configuration.

3. **Check the actual token resolution:**
   ```
   [gRPC-CLIENT] Using SERVICE token for: rentitup.user.UserService/CreateUser
   ```
   If you see `No token available`, the ServiceTokenProvider failed to get a token.

4. **Verify OAuth2 client credentials config:**
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             bff-service:
               client-id: bff-service
               client-secret: bff-secret
               authorization-grant-type: client_credentials
           provider:
             bff-service:
               token-uri: http://localhost:9000/oauth2/token

   grpc:
     client:
       oauth2-registration: bff-service
   ```

### Debug logging

Enable debug logging to see stub injection:

```yaml
logging:
  level:
    com.rentitup.common.grpc: DEBUG
```

## OAuth2 Client Registration Names

When a service needs to make authenticated calls to another service (service-to-service), it needs OAuth2 **client credentials**. These are configured in `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          <REGISTRATION_NAME>:    # <-- This is the key name
            client-id: my-service
            client-secret: secret
            authorization-grant-type: client_credentials
            scope: internal,user:read
        provider:
          <REGISTRATION_NAME>:
            token-uri: http://localhost:9000/oauth2/token
```

### Why Different Services Use Different Names

| Service | Registration Name | Reason |
|---------|------------------|--------|
| **BFF** | `bff-service` | BFF has TWO registrations: `bff-gateway` (user login) and `bff-service` (service-to-service) |
| **catalog-service** | `auth-server` | Only needs one registration for service-to-service calls |
| **booking-service** | `auth-server` | Same pattern as catalog-service |
| **user-service** | *(none)* | Only receives calls, doesn't make outgoing authenticated calls |

### BFF Example (Two Registrations)

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          # Registration 1: For user login via browser (authorization_code flow)
          bff-gateway:
            client-id: bff-gateway
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: openid,profile,read,write

          # Registration 2: For service-to-service calls (client_credentials flow)
          bff-service:
            client-id: bff-service
            client-secret: bff-secret
            authorization-grant-type: client_credentials
            scope: internal,user:write,user:read
```

### Other Services Example (One Registration)

```yaml
# catalog-service
spring:
  security:
    oauth2:
      client:
        registration:
          auth-server:    # Only service-to-service
            client-id: catalog-service
            client-secret: catalog-secret
            authorization-grant-type: client_credentials
            scope: internal,user:read
```

### Configuring Which Registration to Use

The `ServiceTokenProvider` needs to know which registration to use for getting tokens. Configure it with:

```yaml
grpc:
  client:
    oauth2-registration: bff-service  # Default is "auth-server"
```

**Examples:**
- BFF: `oauth2-registration: bff-service` (because it uses `bff-service` for client credentials)
- catalog-service: no config needed (uses default `auth-server`)

### How It Works

1. **No user token available** (e.g., scheduled job, startup task)
2. `TokenResolver` checks for service token provider
3. `ServiceTokenProvider` reads the configured registration name
4. Makes a client_credentials request to auth server
5. Returns the service token for the gRPC call

```
BFF Controller → No user logged in
     ↓
TokenResolver.resolveToken()
     ↓
ServiceTokenProvider.getToken()
     ↓
POST http://auth-server:9000/oauth2/token
  grant_type=client_credentials
  client_id=bff-service
  client_secret=bff-secret
  scope=internal,user:read
     ↓
Returns JWT service token
     ↓
BearerTokenInterceptor adds to gRPC call
```

## Token Propagation (Client Side)

The `BearerTokenInterceptor` automatically attaches JWTs to outgoing gRPC calls:

1. **User Token** - If a user is authenticated (via HTTP request), their JWT is forwarded
2. **Service Token** - If no user token, a service-to-service token is obtained via client credentials

```
[gRPC-CLIENT] Forwarding USER token to: rentitup.user.UserService/GetUser
[gRPC-CLIENT] Using SERVICE token for: rentitup.catalog.CatalogService/GetMachine
```

## JWT Authentication (Server Side)

The `GrpcAuthInterceptor` validates incoming JWTs on gRPC server calls:

1. Extracts the `Authorization: Bearer <token>` header
2. Validates the JWT using `JwtDecoder`
3. Populates `GrpcAuthContext` with user info (userId, email, role, etc.)
4. Rejects unauthenticated requests to protected methods

### Public Methods

Configure public methods (no auth required) in `application.yml`:

```yaml
grpc:
  auth:
    public-methods:
      - "rentitup.user.UserService/CreateUser"
      - "rentitup.user.UserService/GetUserByEmail"
```

## Auto-Configuration Order

The auto-configurations are registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and run in this order:

```
1. GrpcAutoConfiguration          → Creates GrpcChannelFactory (requires Eureka)
2. GrpcTokenAutoConfiguration     → Creates TokenResolver, ServiceTokenProvider
3. GrpcClientsAutoConfiguration   → Creates BearerTokenInterceptor (requires TokenResolver)
4. GrpcSecurityConfig             → Configures Spring Security for gRPC (permitAll)
5. GrpcAuthAutoConfiguration      → Creates GrpcAuthInterceptor (requires JwtDecoder)
```

### Why Order Matters

Spring's `@ConditionalOnBean` checks happen **before** bean creation. This caused interceptors to silently not register because their dependencies didn't exist yet.

**Solution:**
- Use `@AutoConfiguration(after = ...)` to ensure dependencies are created first
- Rely on Spring's dependency injection (constructor/method parameters) instead of `@ConditionalOnBean`
- Spring will naturally wait for dependencies to be available

```java
// WRONG - @ConditionalOnBean checks too early, may skip the bean
@Bean
@ConditionalOnBean(TokenResolver.class)
public BearerTokenInterceptor interceptor(TokenResolver resolver) { ... }

// RIGHT - Spring DI handles the dependency naturally
@Bean
public BearerTokenInterceptor interceptor(TokenResolver resolver) { ... }
```

### Interceptor Registration

Interceptors are registered using Spring gRPC annotations:

| Annotation | Purpose |
|------------|---------|
| `@GlobalClientInterceptor` | Applies to all outgoing gRPC calls |
| `@GlobalServerInterceptor` | Applies to all incoming gRPC calls |

The `@Order` annotation controls execution order (lower = earlier):

```java
@Bean
@GlobalServerInterceptor
@Order(50)  // Run before Spring Security's default interceptor
public GrpcAuthInterceptor grpcAuthInterceptor(...) { ... }
```

## Debugging

Enable debug logging to trace token propagation:

```yaml
logging:
  level:
    com.rentitup.common.grpc: DEBUG
```

### Startup Logs

On startup, you should see:
```
GrpcChannelFactory initialized
[gRPC-CONFIG] GrpcContextTokenSupplier created
[gRPC-CONFIG] ServiceTokenProvider created for client: bff-service (registration: bff-service)
[gRPC-CONFIG] TokenResolver created WITH ServiceTokenProvider
[gRPC-CONFIG] GrpcClientsAutoConfiguration loaded
[gRPC-CONFIG] BearerTokenInterceptor registered as GlobalClientInterceptor
[gRPC-CONFIG] GrpcAuthAutoConfiguration loaded
[gRPC-CONFIG] GrpcAuthInterceptor registered with public methods: [...]
```

On first gRPC call:
```
Resolved 1 client interceptors for gRPC channels
Creating gRPC channel for service: USER-SERVICE with 1 interceptors
```

### Request Logs

On each gRPC call:
```
[gRPC-CLIENT] Forwarding USER token to: rentitup.user.UserService/GetUser (token: eyJhbGciOiJS...)
[gRPC-SERVER] Incoming call: rentitup.user.UserService/GetUser (public: false)
[gRPC-SERVER] Token received: eyJhbGciOiJS...
[gRPC-SERVER] Authenticated: email=user@example.com, type=USER, userId=123
```
