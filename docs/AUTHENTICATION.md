# RentItUp Authentication System

## Overview

RentItUp uses a **JWT-based stateless authentication** system with a **BFF (Backend for Frontend) pattern**. Authentication is handled by the User Service, while the BFF validates tokens and protects routes.

## Architecture

```
┌─────────────┐     REST      ┌─────────────┐     gRPC      ┌──────────────┐
│   Client    │ ────────────► │     BFF     │ ────────────► │ User Service │
│  (Mobile/   │               │  (Gateway)  │               │              │
│   Web App)  │ ◄──────────── │             │ ◄──────────── │              │
└─────────────┘    JWT Token  └─────────────┘   User Data   └──────────────┘
                                    │
                                    │ gRPC (with userId)
                                    ▼
                              ┌──────────────┐
                              │   Catalog    │
                              │   Service    │
                              └──────────────┘
```

## Components

### 1. User Service
- Handles user registration and authentication
- Stores user credentials (passwords hashed with BCrypt)
- Generates JWT tokens (access + refresh)
- Manages user profiles and KYC verification

### 2. BFF (Backend for Frontend)
- Public entry point for all client requests
- Validates JWT tokens on protected routes
- Extracts user context from tokens
- Forwards requests to microservices via gRPC

### 3. Shared Libraries
- `JwtUtil` - Token generation and validation
- `JwtProperties` - JWT configuration
- `JwtClaims` - Parsed token payload

---

## Authentication Flow

### Registration Flow

```
Client                          BFF                         User Service
  │                              │                              │
  │  POST /api/v1/auth/register  │                              │
  │  {email, password, ...}      │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │                              │  gRPC: Register()            │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │     - Validate email unique  │
  │                              │     - Hash password (BCrypt) │
  │                              │     - Create user record     │
  │                              │     - Generate JWT tokens    │
  │                              │                              │
  │                              │  AuthResponse                │
  │                              │  {user, access_token,        │
  │                              │   refresh_token, expires_in} │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  201 Created                 │                              │
  │  {user, tokens}              │                              │
  │ ◄─────────────────────────────                              │
```

### Login Flow

```
Client                          BFF                         User Service
  │                              │                              │
  │  POST /api/v1/auth/login     │                              │
  │  {email, password}           │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │                              │  gRPC: Login()               │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │     - Find user by email     │
  │                              │     - Verify password        │
  │                              │     - Generate JWT tokens    │
  │                              │                              │
  │                              │  AuthResponse                │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  200 OK                      │                              │
  │  {user, tokens}              │                              │
  │ ◄─────────────────────────────                              │
```

### Authenticated Request Flow

```
Client                          BFF                         Microservice
  │                              │                              │
  │  GET /api/v1/machines        │                              │
  │  Authorization: Bearer xxx   │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │                     ┌────────┴────────┐                     │
  │                     │ JWT Filter      │                     │
  │                     │ - Extract token │                     │
  │                     │ - Validate JWT  │                     │
  │                     │ - Set UserCtx   │                     │
  │                     └────────┬────────┘                     │
  │                              │                              │
  │                              │  gRPC call (with user ctx)   │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │  Response                    │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  200 OK                      │                              │
  │  {data}                      │                              │
  │ ◄─────────────────────────────                              │
```

### Token Refresh Flow

```
Client                          BFF                         User Service
  │                              │                              │
  │  POST /api/v1/auth/refresh   │                              │
  │  {refresh_token}             │                              │
  │ ─────────────────────────────►                              │
  │                              │                              │
  │                              │  gRPC: RefreshToken()        │
  │                              │ ─────────────────────────────►
  │                              │                              │
  │                              │     - Validate refresh token │
  │                              │     - Generate new tokens    │
  │                              │                              │
  │                              │  AuthResponse (new tokens)   │
  │                              │ ◄─────────────────────────────
  │                              │                              │
  │  200 OK                      │                              │
  │  {user, new_tokens}          │                              │
  │ ◄─────────────────────────────                              │
```

---

## JWT Token Structure

### Access Token
- **Purpose**: Authenticate API requests
- **Expiration**: 1 hour (configurable)
- **Usage**: Sent in `Authorization: Bearer <token>` header

### Refresh Token
- **Purpose**: Obtain new access tokens without re-login
- **Expiration**: 7 days (configurable)
- **Usage**: Sent to `/api/v1/auth/refresh` endpoint

### Token Payload (Claims)

```json
{
  "sub": "user-uuid",           // User ID
  "email": "user@example.com",  // User email
  "role": "CUSTOMER",           // User role (ADMIN, OWNER, CUSTOMER)
  "type": "access",             // Token type (access or refresh)
  "iss": "rentitup",            // Issuer
  "iat": 1699999999,            // Issued at
  "exp": 1700003599             // Expiration
}
```

---

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/refresh` | Refresh tokens |
| GET | `/swagger-ui.html` | API documentation |
| GET | `/api-docs` | OpenAPI spec |

### Protected Endpoints (Authentication Required)

All other `/api/*` endpoints require a valid access token.

---

## Request/Response Examples

### Register

**Request:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123",
  "full_name": "John Doe",
  "phone": "+1234567890",
  "user_type": "CUSTOMER"
}
```

**Response:**
```json
{
  "status": 201,
  "message": "Registration successful",
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "john@example.com",
      "full_name": "John Doe",
      "user_type": "CUSTOMER",
      "kyc_status": "PENDING"
    },
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 3600
  }
}
```

### Login

**Request:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "user": { ... },
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 3600
  }
}
```

### Authenticated Request

**Request:**
```http
GET /api/v1/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response:**
```json
{
  "status": 200,
  "message": "User retrieved",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john@example.com",
    "full_name": "John Doe",
    "user_type": "CUSTOMER",
    "kyc_status": "PENDING"
  }
}
```

### Unauthorized Response

```json
{
  "status": 401,
  "message": "Invalid or expired token",
  "data": null
}
```

---

## Configuration

### Environment Variables

```bash
# Required - Must be the same across all services
JWT_SECRET_KEY=your-256-bit-secret-key-here-must-be-at-least-32-characters

# Optional - User Service specific
JWT_ACCESS_TOKEN_EXPIRATION=3600000    # 1 hour in milliseconds
JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7 days in milliseconds
```

### User Service (`application.yml`)

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000
  issuer: rentitup
```

### BFF (`application.yml`)

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
```

---

## User Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| `CUSTOMER` | Regular user | Browse, rent machines |
| `OWNER` | Machine owner | List machines, manage rentals |
| `ADMIN` | Administrator | Full access, user verification |

---

## Security Considerations

1. **Password Storage**: Passwords are hashed using BCrypt (never stored in plain text)

2. **Token Security**:
   - Access tokens are short-lived (1 hour)
   - Refresh tokens are longer-lived (7 days)
   - Both are signed with HMAC-SHA256

3. **Secret Key**:
   - Must be at least 256 bits (32 characters)
   - Must be kept secret and consistent across services
   - Use environment variables, never commit to source control

4. **HTTPS**: Always use HTTPS in production to protect tokens in transit

5. **Token Storage (Client-side)**:
   - Store access token in memory (JavaScript variable)
   - Store refresh token in HttpOnly cookie or secure storage
   - Never store tokens in localStorage (XSS vulnerable)

---

## Error Handling

| HTTP Status | gRPC Status | Description |
|-------------|-------------|-------------|
| 400 | INVALID_ARGUMENT | Invalid request data |
| 401 | UNAUTHENTICATED | Missing/invalid/expired token |
| 403 | PERMISSION_DENIED | Insufficient permissions |
| 404 | NOT_FOUND | User not found |
| 409 | ALREADY_EXISTS | Email already registered |

---

## Testing Authentication

### Using cURL

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","full_name":"Test User","user_type":"CUSTOMER"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Authenticated request
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Refresh token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refresh_token":"YOUR_REFRESH_TOKEN"}'
```

---

## Client Implementation Guide

### JavaScript/TypeScript Example

```typescript
class AuthService {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  async login(email: string, password: string) {
    const response = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await response.json();
    this.accessToken = data.data.access_token;
    this.refreshToken = data.data.refresh_token;

    // Schedule token refresh before expiration
    setTimeout(() => this.refreshTokens(), (data.data.expires_in - 60) * 1000);

    return data.data.user;
  }

  async refreshTokens() {
    const response = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: this.refreshToken })
    });

    const data = await response.json();
    this.accessToken = data.data.access_token;
    this.refreshToken = data.data.refresh_token;
  }

  getAuthHeader() {
    return this.accessToken ? { Authorization: `Bearer ${this.accessToken}` } : {};
  }
}
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "Invalid or expired token" | Token expired or malformed | Refresh token or re-login |
| "Missing Authorization header" | No token in request | Add `Authorization: Bearer <token>` header |
| "Invalid email or password" | Wrong credentials | Verify email and password |
| "Email already registered" | Duplicate registration | Use login instead |
| Token validation fails across services | Different JWT secrets | Ensure same `JWT_SECRET_KEY` everywhere |
