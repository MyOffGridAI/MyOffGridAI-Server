# MyOffGridAI-Server — Architecture Specification

**Generated:** 2026-03-14
**Phase:** 1 — Foundation
**Version:** 0.1.0-SNAPSHOT

---

## 1. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.3 |
| Build Tool | Maven | 3.9.9 (wrapper) |
| Database | PostgreSQL + pgvector | 16 |
| ORM | Hibernate (JPA) | 6.6.x (via Spring Boot) |
| Auth | JWT (JJWT) | 0.12.6 |
| Password Hashing | BCrypt | via Spring Security |
| API Documentation | SpringDoc OpenAPI | 2.8.4 |
| Testing | JUnit 5, Mockito, Testcontainers | via Spring Boot 3.4.3 |
| Logging | SLF4J + Logback | via Spring Boot |

### Future Dependencies (scaffolded, not yet used)
- Apache PDFBox 3.0.4 (document processing)
- Tess4j 5.13.0 (OCR)
- jSerialComm 2.11.0 (sensor communication)
- Spring WebFlux (non-blocking HTTP client for Ollama)

---

## 2. Package Structure

```
com.myoffgridai
├── config/           — Application configuration, security, JWT filter
├── auth/             — Authentication and user management
│   ├── controller/   — REST endpoints (AuthController, UserController)
│   ├── service/      — Business logic (AuthService, UserService, JwtService)
│   ├── model/        — JPA entities (User) and enums (Role)
│   ├── dto/          — Request/response data transfer objects
│   └── repository/   — Spring Data JPA repositories
├── ai/               — AI conversation orchestration (Phase 2)
├── memory/           — Memory management and recall (Phase 2)
├── knowledge/        — Knowledge base / RAG pipeline (Phase 2)
├── skills/           — Skill execution framework (Phase 3)
├── sensors/          — Sensor data ingestion (Phase 3)
├── proactive/        — Proactive intelligence engine (Phase 3)
├── privacy/          — Vault and data privacy (Phase 3)
├── system/           — System management, updates, setup (Phase 2)
└── common/           — Cross-cutting concerns
    ├── exception/    — Global exception handler, custom exceptions
    ├── response/     — ApiResponse wrapper
    └── util/         — Shared utilities (Phase 2+)
```

---

## 3. Request Lifecycle

```
HTTP Request
    │
    ▼
┌─────────────────────┐
│   JwtAuthFilter      │  Extract Bearer token, validate JWT,
│   (OncePerRequest)   │  set SecurityContext authentication
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   SecurityFilterChain│  Check endpoint permissions
│   (SecurityConfig)   │  Public endpoints → pass through
│                      │  Protected → require authentication
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   @PreAuthorize      │  Method-level role checks
│   (Controller)       │  hasRole('OWNER'), hasRole('ADMIN')
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   Controller         │  Request validation (@Valid)
│                      │  Delegate to Service
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   Service            │  Business logic, transactions
│                      │  Delegate to Repository
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   Repository         │  JPA data access
│   (Spring Data)      │  PostgreSQL queries
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   ApiResponse<T>     │  Wrap result in standard envelope
│   (Controller)       │  Return to client
└─────────────────────┘

Exceptions at any layer → GlobalExceptionHandler → ApiResponse.error(...)
```

---

## 4. Authentication Flows

### Register Flow
```
POST /api/auth/register
    │
    ▼
AuthController.register()
    │ @Valid RegisterRequest
    ▼
AuthService.register()
    ├── Check username uniqueness (UserRepository.existsByUsername)
    ├── Check email uniqueness (UserRepository.existsByEmail)
    ├── Validate password length (min 4 dev / 12 prod)
    ├── Hash password (BCrypt)
    ├── Create User entity (default role: ROLE_MEMBER)
    ├── Save to database
    ├── Generate access token (JwtService)
    ├── Generate refresh token (JwtService)
    └── Return AuthResponse (tokens + UserSummaryDto)
```

### Login Flow
```
POST /api/auth/login
    │
    ▼
AuthController.login()
    │ @Valid LoginRequest
    ▼
AuthService.login()
    ├── Lookup user by username (UserRepository.findByUsername)
    ├── Verify password (BCrypt.matches)
    ├── Check isActive flag
    ├── Update lastLoginAt timestamp
    ├── Generate access token (JwtService)
    ├── Generate refresh token (JwtService)
    └── Return AuthResponse
```

### Refresh Flow
```
POST /api/auth/refresh
    │
    ▼
AuthController.refresh()
    │ @Valid RefreshRequest
    ▼
AuthService.refresh()
    ├── Check blacklist (in-memory Set)
    ├── Extract username from refresh token
    ├── Verify token not expired
    ├── Lookup user (UserRepository.findByUsername)
    ├── Generate new access token
    └── Return AuthResponse (new access + same refresh)
```

---

## 5. Role Hierarchy and Permission Matrix

### Role Hierarchy (highest → lowest)
```
ROLE_OWNER > ROLE_ADMIN > ROLE_MEMBER > ROLE_VIEWER > ROLE_CHILD
```

### Permission Matrix
| Operation | OWNER | ADMIN | MEMBER | VIEWER | CHILD |
|-----------|-------|-------|--------|--------|-------|
| Register users | ✅ | ✅ | ❌ | ❌ | ❌ |
| Login | ✅ | ✅ | ✅ | ✅ | ✅ |
| List all users | ✅ | ✅ | ❌ | ❌ | ❌ |
| View any user | ✅ | ✅ | ❌ | ❌ | ❌ |
| View own profile | ✅ | ✅ | ✅ | ✅ | ✅ |
| Update any user | ✅ | ✅ | ❌ | ❌ | ❌ |
| Deactivate user | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete user | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 6. Configuration Profiles

### Dev Profile (`spring.profiles.active=dev`)
- **Database:** `jdbc:postgresql://localhost:5432/myoffgridai`
- **DDL:** `hibernate.ddl-auto=update`
- **Flyway:** disabled
- **Password min length:** 4 characters
- **CORS:** all origins allowed
- **JWT secret:** dev-secret-key (hardcoded in config)
- **Token blacklist:** in-memory ConcurrentHashMap

### Prod Profile (`spring.profiles.active=prod`)
- **DDL:** `hibernate.ddl-auto=validate`
- **Flyway:** enabled
- **Password min length:** 12 characters + uppercase + lowercase + digit + special
- **CORS:** restricted origins
- **JWT secret:** externalized (env var / vault)
- **Token blacklist:** Redis (future)

### Test Profile (`spring.profiles.active=test`)
- **DDL:** `hibernate.ddl-auto=create-drop`
- **Database:** either Testcontainers Postgres or Docker Compose Postgres
- **JWT:** test-specific secret and shorter expirations

---

## 7. Docker Compose Services

**Location:** `~/Documents/Github/MyOffGridAI-Infra/docker-compose.yml`

| Service | Image | Port | Container |
|---------|-------|------|-----------|
| PostgreSQL | pgvector/pgvector:pg16 | 5432 | myoffgridai-db |

### Startup Order
1. `docker compose -f ~/Documents/Github/MyOffGridAI-Infra/docker-compose.yml up -d`
2. Wait for Postgres to accept connections
3. Start Spring Boot (`./mvnw spring-boot:run`)
4. Hibernate DDL auto creates/updates tables

### Credentials
- Database: `myoffgridai`
- Username: `myoffgridai`
- Password: `myoffgridai`

---

## 8. API Base URL

- **Dev:** `http://localhost:8080`
- **API prefix:** No global prefix (endpoints start with `/api/`)
- **OpenAPI UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI spec:** `http://localhost:8080/v3/api-docs`
