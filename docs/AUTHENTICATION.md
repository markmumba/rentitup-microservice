# RentItUp Authentication System

## Overview

RentItUp uses a **JWT-based stateless authentication** system with a **BFF (Backend for Frontend) pattern**. Authentication is handled by the User Service, while the BFF validates tokens using Spring Security.

## Architecture

```
┌─────────────┐     REST      ┌─────────────┐     gRPC      ┌──────────────┐
│   Client    │ ────────────► │     BFF     │ ────────────► │ User Service │
│  (Mobile/   │               │  (Gateway)  │               │              │
│   Web App)  │ ◄──────────── │             │ ◄──────────── │              │
└─────────────┘    JWT Token  └─────────────┘   User Data   └──────────────┘
                                    │
                                    │ gRPC (with userId in request)
                                    ▼
                              ┌──────────────┐
                              │   Catalog    │
                              │   Service    │
                              └──────────────┘
```

---

## Spring Security Setup (BFF)

### How It Works

The BFF uses Spring Security with a custom JWT filter. Here's what each component does:

### 1. SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", ...).permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**What each part does:**

| Configuration | Purpose |
|---------------|---------|
| `@EnableWebSecurity` | Enables Spring Security's web security support |
| `@EnableMethodSecurity` | Enables `@PreAuthorize`, `@PostAuthorize` annotations on methods |
| `.csrf(disable)` | Disables CSRF protection (not needed for stateless JWT APIs) |
| `.sessionManagement(STATELESS)` | No HTTP sessions - each request must include JWT |
| `.requestMatchers(...).permitAll()` | These paths don't require authentication |
| `.anyRequest().authenticated()` | All other paths require valid JWT |
| `.addFilterBefore(...)` | Adds our JWT filter before Spring's username/password filter |

### 2. JwtAuthenticationFilter.java

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // No token, let Spring Security handle it
            return;
        }

        String token = authHeader.substring(7);

        try {
            JwtClaims claims = jwtUtil.validateAccessToken(token);

            // Create Authentication object and set in SecurityContext
            JwtAuthentication authentication = new JwtAuthentication(
                claims.getUserId(),
                claims.getEmail(),
                claims.getRole()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();  // Invalid token
        }

        filterChain.doFilter(request, response);
    }
}
```

**What it does:**

1. **Extracts token** from `Authorization: Bearer <token>` header
2. **Validates JWT** using `JwtUtil` (checks signature, expiration)
3. **Creates Authentication** object with user details and roles
4. **Sets SecurityContext** so Spring Security knows the user is authenticated
5. **Continues filter chain** - Spring Security then decides access based on rules

### 3. JwtAuthentication.java

```java
public class JwtAuthentication extends AbstractAuthenticationToken {
    private final UUID userId;
    private final String email;
    private final String role;

    public JwtAuthentication(UUID userId, String email, String role) {
        super(buildAuthorities(role));  // Creates ROLE_ADMIN, ROLE_OWNER, etc.
        this.userId = userId;
        this.email = email;
        this.role = role;
        super.setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(String role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
```

**What it does:**

- Extends `AbstractAuthenticationToken` - Spring Security's base auth class
- Holds user info: `userId`, `email`, `role`
- Creates **GrantedAuthority** objects (e.g., `ROLE_ADMIN`) for access control
- Marked as authenticated so Spring Security trusts it

### 4. Using @PreAuthorize in Controllers

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        // Any authenticated user can access
        JwtAuthentication auth = (JwtAuthentication)
            SecurityContextHolder.getContext().getAuthentication();
        // Use auth.getUserId(), auth.getEmail(), etc.
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // Only ADMIN can access
    public ResponseEntity<?> getUser(@PathVariable String id) {
        // ...
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")  // ADMIN or OWNER
    public ResponseEntity<?> listUsers() {
        // ...
    }
}
```

---

## Security Context in Microservice Architecture

### Does SecurityContext propagate to downstream services?

**No, it does not.** And this is by design.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              HTTP REQUEST                                │
│                     Authorization: Bearer <JWT>                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                                 BFF                                      │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  JwtAuthenticationFilter                                         │   │
│  │  - Validates JWT                                                 │   │
│  │  - Sets SecurityContext (ThreadLocal, request-scoped)           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Controller                                                      │   │
│  │  - Has access to SecurityContextHolder.getContext()              │   │
│  │  - @PreAuthorize works here                                      │   │
│  │  - Extracts userId, passes it in gRPC request                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ gRPC call (userId passed in request message)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           CATALOG SERVICE                                │
│                                                                          │
│  - NO SecurityContext here                                               │
│  - Receives userId as part of gRPC request                              │
│  - Trusts the BFF (internal network)                                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Why doesn't SecurityContext propagate?

1. **ThreadLocal scope**: `SecurityContextHolder` uses `ThreadLocal` - it's bound to the current thread/request
2. **Different process**: Downstream services run in separate JVM processes
3. **Network boundary**: gRPC calls are network calls, not method calls

### How do we handle this?

**Pattern 1: Pass user ID in request messages (Current Approach)**

```java
// BFF Controller
@GetMapping("/machines")
public ResponseEntity<?> createMachine(@RequestBody CreateMachineRequest request) {
    JwtAuthentication auth = getAuthentication();

    // Pass owner ID in the gRPC request
    CreateMachineRequest grpcRequest = CreateMachineRequest.newBuilder()
        .setOwnerId(auth.getUserId().toString())  // <-- User ID from SecurityContext
        .setName(request.getName())
        .build();

    grpcStubFactory.getCatalogStub().createMachine(grpcRequest);
}
```

**Pattern 2: Pass user info in gRPC metadata (Alternative)**

```java
// Create metadata with user info
Metadata metadata = new Metadata();
metadata.put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER),
    auth.getUserId().toString());
metadata.put(Metadata.Key.of("x-user-role", Metadata.ASCII_STRING_MARSHALLER),
    auth.getRole());

// Attach to stub
CatalogServiceGrpc.CatalogServiceBlockingStub stubWithMetadata =
    MetadataUtils.attachHeaders(catalogStub, metadata);
```

### Trust Model

```
┌──────────────────┐         ┌──────────────────┐
│    INTERNET      │         │  INTERNAL NETWORK │
│   (Untrusted)    │         │    (Trusted)      │
│                  │         │                   │
│   ┌──────────┐   │         │  ┌─────────────┐  │
│   │  Client  │   │  HTTPS  │  │     BFF     │  │
│   │  (JWT)   │───┼────────►│  │ (Validates) │  │
│   └──────────┘   │         │  └─────────────┘  │
│                  │         │         │         │
└──────────────────┘         │         │ gRPC    │
                             │         ▼         │
                             │  ┌─────────────┐  │
                             │  │  Services   │  │
                             │  │  (Trust BFF)│  │
                             │  └─────────────┘  │
                             │                   │
                             └───────────────────┘
```

- **Client → BFF**: Must provide valid JWT (validated by Spring Security)
- **BFF → Services**: Internal network, services trust the BFF
- **Services don't validate JWT**: They trust user info passed by BFF

---

## Complete Request Flow

### 1. Registration Flow

```
Client                          BFF                         User Service
  │                              │                              │
  │  POST /api/v1/auth/register  │                              │
  │  {email, password, ...}      │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │              [No auth needed - permitAll()]                 │
  │                              │                              │
  │                              │  gRPC: Register()            │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │     - Hash password (BCrypt) │
  │                              │     - Create user            │
  │                              │     - Generate JWT tokens    │
  │                              │                              │
  │                              │  AuthResponse                │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  201 Created                 │                              │
  │  {user, access_token, ...}   │                              │
  │ ◄─────────────────────────────                              │
```

### 2. Authenticated Request Flow

```
Client                          BFF                         Catalog Service
  │                              │                              │
  │  POST /api/v1/machines       │                              │
  │  Authorization: Bearer xxx   │                              │
  │  {name, price, ...}          │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │              ┌───────────────┴───────────────┐              │
  │              │   JwtAuthenticationFilter     │              │
  │              │   1. Extract Bearer token     │              │
  │              │   2. Validate JWT signature   │              │
  │              │   3. Check expiration         │              │
  │              │   4. Create JwtAuthentication │              │
  │              │   5. Set SecurityContext      │              │
  │              └───────────────┬───────────────┘              │
  │                              │                              │
  │              ┌───────────────┴───────────────┐              │
  │              │   Spring Security             │              │
  │              │   - Check .authenticated()    │              │
  │              │   - Check @PreAuthorize       │              │
  │              └───────────────┬───────────────┘              │
  │                              │                              │
  │              ┌───────────────┴───────────────┐              │
  │              │   MachineController           │              │
  │              │   - Get userId from context   │              │
  │              │   - Build gRPC request        │              │
  │              └───────────────┬───────────────┘              │
  │                              │                              │
  │                              │  gRPC: CreateMachine()       │
  │                              │  {ownerId: "user-uuid", ...} │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │  MachineResponse             │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  201 Created                 │                              │
  │  {machine: {...}}            │                              │
  │ ◄─────────────────────────────                              │
```

### 3. Unauthorized Request Flow

```
Client                          BFF
  │                              │
  │  GET /api/v1/users           │
  │  Authorization: Bearer xxx   │
  │ ─────────────────────────────►
  │                              │
  │              ┌───────────────┴───────────────┐
  │              │   JwtAuthenticationFilter     │
  │              │   - Token valid ✓             │
  │              │   - Role: CUSTOMER            │
  │              └───────────────┬───────────────┘
  │                              │
  │              ┌───────────────┴───────────────┐
  │              │   @PreAuthorize("hasRole      │
  │              │     ('ADMIN')")               │
  │              │   - User role: CUSTOMER       │
  │              │   - Required: ADMIN           │
  │              │   - ACCESS DENIED ✗           │
  │              └───────────────┬───────────────┘
  │                              │
  │  403 Forbidden               │
  │  {error: "Access Denied"}    │
  │ ◄─────────────────────────────
```

---

## JWT Token Structure

### Access Token Claims

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  // User ID
  "email": "user@example.com",
  "role": "CUSTOMER",                              // ADMIN, OWNER, CUSTOMER
  "type": "access",
  "iss": "rentitup",
  "iat": 1699999999,
  "exp": 1700003599                                // 1 hour from iat
}
```

### Refresh Token Claims

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "CUSTOMER",
  "type": "refresh",                               // Different type
  "iss": "rentitup",
  "iat": 1699999999,
  "exp": 1700604799                                // 7 days from iat
}
```

---

## API Endpoints

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh tokens |
| GET | `/swagger-ui.html` | API documentation |

### Authenticated Endpoints (Any Role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users/me` | Get current user profile |
| PUT | `/api/v1/users/me` | Update current user profile |
| GET | `/api/v1/machines` | List machines |
| GET | `/api/v1/machines/{id}` | Get machine details |

### Admin Only Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users` | List all users |
| GET | `/api/v1/users/{id}` | Get user by ID |
| POST | `/api/v1/users/{id}/verify` | Verify user KYC |

### Owner Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/machines` | Create machine (sets ownerId from token) |
| PUT | `/api/v1/machines/{id}` | Update own machine |
| DELETE | `/api/v1/machines/{id}` | Delete own machine |

---

## Configuration

### Environment Variables

```bash
# Required - Must be identical across BFF and User Service
JWT_SECRET_KEY=your-256-bit-secret-key-here-must-be-at-least-32-characters
```

### User Service (`application.yml`)

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token-expiration: 3600000      # 1 hour
  refresh-token-expiration: 604800000   # 7 days
  issuer: rentitup
```

### BFF (`application.yml`)

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}   # Must match User Service
```

---

## Security Best Practices

### 1. Token Security
- Access tokens expire in 1 hour
- Refresh tokens expire in 7 days
- Tokens are signed with HMAC-SHA256

### 2. Password Security
- Passwords hashed with BCrypt (never stored plain)
- BCrypt includes salt automatically

### 3. Transport Security
- Always use HTTPS in production
- Internal gRPC can use plaintext (internal network)

### 4. Secret Key Management
- Use environment variables
- Never commit to source control
- Same key across all services that validate JWT

### 5. Client-Side Token Storage
```
✓ Access token  → Memory (JavaScript variable)
✓ Refresh token → HttpOnly cookie or secure storage
✗ localStorage  → Vulnerable to XSS
```

---

## Error Responses

| HTTP | Scenario | Response |
|------|----------|----------|
| 401 | Missing token | `{"status":401,"message":"Full authentication is required"}` |
| 401 | Invalid/expired token | `{"status":401,"message":"Invalid or expired token"}` |
| 403 | Insufficient role | `{"status":403,"message":"Access Denied"}` |

---

## Testing

### cURL Examples

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123","full_name":"Test","user_type":"CUSTOMER"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123"}'

# Authenticated request
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Admin-only request (will fail with non-admin token)
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```
