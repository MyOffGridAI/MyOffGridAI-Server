# MyOffGridAI-Server — Codebase Audit

**Generated:** 2026-03-14
**Phase:** 1 — Foundation
**Version:** 0.1.0-SNAPSHOT

---

## 1. Entity Inventory

### User (`com.myoffgridai.auth.model.User`)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | `@GeneratedValue(strategy = UUID)` |
| username | String | unique, not null | Login identifier |
| email | String | unique, nullable | Optional contact email |
| displayName | String | not null | Human-readable name |
| passwordHash | String | not null | BCrypt-encoded password |
| role | Role (enum) | not null | `@Enumerated(STRING)` |
| isActive | boolean | not null, default true | Account status |
| createdAt | Instant | not null, auto | `@CreatedDate` |
| updatedAt | Instant | auto | `@LastModifiedDate` |
| lastLoginAt | Instant | nullable | Updated on login |

**Implements:** `UserDetails` (Spring Security)
**Table:** `users`
**Auditing:** `@EntityListeners(AuditingEntityListener.class)`, `@PrePersist`, `@PreUpdate`

### Role (`com.myoffgridai.auth.model.Role`)
| Value | Description |
|-------|-------------|
| ROLE_OWNER | Full system access, created at first boot |
| ROLE_ADMIN | All features, user management |
| ROLE_MEMBER | Full AI features, own data only |
| ROLE_VIEWER | Read-only access |
| ROLE_CHILD | Safe mode, filtered responses, no vault access |

---

## 2. Repository Method Inventory

### UserRepository (`com.myoffgridai.auth.repository.UserRepository`)
Extends: `JpaRepository<User, UUID>`

| Method | Return Type |
|--------|-------------|
| `findByUsername(String)` | `Optional<User>` |
| `findByEmail(String)` | `Optional<User>` |
| `existsByUsername(String)` | `boolean` |
| `existsByEmail(String)` | `boolean` |
| `findAllByRole(Role)` | `List<User>` |
| `countByIsActiveTrue()` | `long` |

---

## 3. Service Method Inventory

### JwtService (`com.myoffgridai.auth.service.JwtService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `generateAccessToken(UserDetails)` | `String` | Short-lived access JWT |
| `generateRefreshToken(UserDetails)` | `String` | Long-lived refresh JWT |
| `extractUsername(String)` | `String` | Extract subject from token |
| `extractExpiration(String)` | `Date` | Extract expiry from token |
| `isTokenValid(String, UserDetails)` | `boolean` | Validate token ownership + expiry |
| `isTokenExpired(String)` | `boolean` | Check if token is expired |
| `getAccessExpirationMs()` | `long` | Get configured access expiration |

### AuthService (`com.myoffgridai.auth.service.AuthService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `register(RegisterRequest)` | `AuthResponse` | Create user, return JWT pair |
| `login(LoginRequest)` | `AuthResponse` | Authenticate, return JWT pair |
| `refresh(String)` | `AuthResponse` | Issue new access token |
| `logout(String)` | `void` | Blacklist token |
| `isTokenBlacklisted(String)` | `boolean` | Check blacklist status |
| `changePassword(UUID, ChangePasswordRequest)` | `void` | Change user password |

### UserService (`com.myoffgridai.auth.service.UserService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `listUsers(int, int)` | `Page<UserSummaryDto>` | Paginated user list |
| `getUserById(UUID)` | `UserDetailDto` | Get user details |
| `updateUser(UUID, String, String, Role)` | `UserDetailDto` | Update user profile |
| `deactivateUser(UUID)` | `void` | Set isActive=false |
| `deleteUser(UUID)` | `void` | Hard delete user |

---

## 4. Security Matrix

| Method | Endpoint | Auth Required | Required Role |
|--------|----------|---------------|---------------|
| POST | `/api/auth/register` | No | Public |
| POST | `/api/auth/login` | No | Public |
| POST | `/api/auth/refresh` | No | Public (valid refresh token) |
| POST | `/api/auth/logout` | Yes | Any authenticated |
| GET | `/api/users` | Yes | OWNER or ADMIN |
| GET | `/api/users/{id}` | Yes | OWNER/ADMIN (any), MEMBER/VIEWER (own only) |
| PUT | `/api/users/{id}` | Yes | OWNER or ADMIN |
| PUT | `/api/users/{id}/deactivate` | Yes | OWNER only |
| DELETE | `/api/users/{id}` | Yes | OWNER only |
| GET | `/api/system/status` | No | Public |
| POST | `/api/system/initialize` | No | Public |
| GET | `/setup/**` | No | Public |
| GET | `/actuator/health` | No | Public |
| GET | `/v3/api-docs/**` | No | Public |
| GET | `/swagger-ui/**` | No | Public |

---

## 5. Constants Inventory (`AppConstants.java`)

### Server
- `SERVER_PORT` = 8080
- `OLLAMA_PORT` = 11434
- `POSTGRES_PORT` = 5432

### JWT
- `JWT_EXPIRATION_MS` = 86,400,000 (24h)
- `JWT_REFRESH_EXPIRATION_MS` = 604,800,000 (7d)
- `TOKEN_TYPE_BEARER` = "Bearer"
- `AUTHORIZATION_HEADER` = "Authorization"
- `BEARER_PREFIX` = "Bearer "

### API Paths
- `API_AUTH` = "/api/auth"
- `API_USERS` = "/api/users"
- `API_AI` = "/api/ai"
- `API_MEMORY` = "/api/memory"
- `API_KNOWLEDGE` = "/api/knowledge"
- `API_SKILLS` = "/api/skills"
- `API_SENSORS` = "/api/sensors"
- `API_PROACTIVE` = "/api/proactive"
- `API_PRIVACY` = "/api/privacy"
- `API_SYSTEM` = "/api/system"

### Pagination
- `DEFAULT_PAGE_SIZE` = 20
- `MAX_PAGE_SIZE` = 100

### File Upload
- `MAX_FILE_SIZE_BYTES` = 104,857,600 (100MB)

### Sensors
- `SENSOR_POLLING_INTERVAL_SECONDS` = 30

### RAG
- `RAG_TOP_K_DEFAULT` = 5
- `CHUNK_SIZE_TOKENS` = 512
- `CHUNK_OVERLAP_TOKENS` = 64

### Memory Importance
- `MEMORY_IMPORTANCE_LOW` = 1
- `MEMORY_IMPORTANCE_MEDIUM` = 5
- `MEMORY_IMPORTANCE_HIGH` = 10

### Roles
- `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER`, `ROLE_CHILD`

### Device & System
- `DEVICE_KEY_PATH` = "/etc/myoffgridai/.device.key"
- `AP_MODE_SSID` = "MyOffGridAI-Setup"
- `AP_MODE_IP` = "192.168.4.1"
- `UPDATE_ZIP_FILENAME` = "myoffgridai-update.zip"
- `FACTORY_RESET_TRIGGER_FILENAME` = "factory-reset.trigger"

### Password Validation
- `PASSWORD_MIN_LENGTH_DEV` = 4
- `PASSWORD_MIN_LENGTH_PROD` = 12

---

## 6. Known Constraints and Validation Rules

### RegisterRequest
- `username`: @NotBlank, @Size(min=3, max=50)
- `email`: @Email (optional)
- `displayName`: @NotBlank, @Size(min=1, max=100)
- `password`: @NotBlank, runtime min length 4 (dev) / 12 (prod)
- `role`: optional, defaults to ROLE_MEMBER

### LoginRequest
- `username`: @NotBlank
- `password`: @NotBlank

### ChangePasswordRequest
- `currentPassword`: @NotBlank
- `newPassword`: @NotBlank, runtime min length 4 (dev) / 12 (prod)

### RefreshRequest
- `refreshToken`: @NotBlank

### UpdateUserRequest
- `displayName`: @Size(min=1, max=100) (optional)
- `email`: @Email (optional)
- `role`: optional

---

## 7. Package Structure

```
com.myoffgridai
├── MyOffGridAiApplication.java
├── config/
│   ├── AppConstants.java
│   ├── JpaConfig.java
│   ├── JwtAuthFilter.java
│   └── SecurityConfig.java
├── auth/
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── UserController.java
│   ├── service/
│   │   ├── JwtService.java
│   │   ├── AuthService.java
│   │   └── UserService.java
│   ├── model/
│   │   ├── Role.java
│   │   └── User.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── RefreshRequest.java
│   │   ├── AuthResponse.java
│   │   ├── UserSummaryDto.java
│   │   ├── UserDetailDto.java
│   │   └── UpdateUserRequest.java
│   └── repository/
│       └── UserRepository.java
├── ai/                    (empty — Phase 2+)
├── memory/                (empty — Phase 2+)
├── knowledge/             (empty — Phase 2+)
├── skills/                (empty — Phase 2+)
├── sensors/               (empty — Phase 2+)
├── proactive/             (empty — Phase 2+)
├── privacy/               (empty — Phase 2+)
├── system/                (empty — Phase 2+)
└── common/
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── DuplicateResourceException.java
    │   ├── EntityNotFoundException.java
    │   ├── InitializationException.java
    │   └── FortressActiveException.java
    ├── response/
    │   └── ApiResponse.java
    └── util/               (empty — Phase 2+)
```
