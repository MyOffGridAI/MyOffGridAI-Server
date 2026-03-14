# MyOffGridAI-Server — Codebase Audit

**Generated:** 2026-03-14
**Phase:** 3 — Memory & RAG
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
    └── util/
        └── TokenCounter.java
```

---

## 8. Phase 2 — AI Core Entities

### Conversation (`com.myoffgridai.ai.model.Conversation`)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | `@GeneratedValue(strategy = UUID)` |
| user | User | ManyToOne, not null | FK to users table |
| title | String | nullable | Auto-generated after first message |
| isArchived | boolean | not null, default false | Archive status |
| createdAt | Instant | not null, auto | `@CreatedDate` |
| updatedAt | Instant | auto | `@LastModifiedDate` |
| messageCount | int | not null, default 0 | Maintained by service |

**Table:** `conversations`

### Message (`com.myoffgridai.ai.model.Message`)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | `@GeneratedValue(strategy = UUID)` |
| conversation | Conversation | ManyToOne, not null | FK to conversations table |
| role | MessageRole | not null | `@Enumerated(STRING)` |
| content | String (TEXT) | not null | Full message text |
| tokenCount | Integer | nullable | Populated after inference |
| hasRagContext | boolean | not null, default false | True when RAG context injected |
| createdAt | Instant | not null, auto | `@CreatedDate` |

**Table:** `messages`

### MessageRole (`com.myoffgridai.ai.model.MessageRole`)
| Value | Description |
|-------|-------------|
| USER | User-submitted message |
| ASSISTANT | AI-generated response |
| SYSTEM | System prompt |

---

## 9. Phase 2 — Repository Method Inventory

### ConversationRepository (`com.myoffgridai.ai.repository.ConversationRepository`)
Extends: `JpaRepository<Conversation, UUID>`

| Method | Return Type |
|--------|-------------|
| `findByUserIdOrderByUpdatedAtDesc(UUID, Pageable)` | `Page<Conversation>` |
| `findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID, boolean, Pageable)` | `Page<Conversation>` |
| `findByIdAndUserId(UUID, UUID)` | `Optional<Conversation>` |
| `countByUserId(UUID)` | `long` |

### MessageRepository (`com.myoffgridai.ai.repository.MessageRepository`)
Extends: `JpaRepository<Message, UUID>`

| Method | Return Type |
|--------|-------------|
| `findByConversationIdOrderByCreatedAtAsc(UUID)` | `List<Message>` |
| `findByConversationIdOrderByCreatedAtAsc(UUID, Pageable)` | `Page<Message>` |
| `findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable)` | `List<Message>` |
| `countByConversationId(UUID)` | `long` |
| `deleteByConversationId(UUID)` | `void` |

---

## 10. Phase 2 — Service Method Inventory

### OllamaService (`com.myoffgridai.ai.service.OllamaService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `isAvailable()` | `boolean` | Checks if Ollama is responding |
| `listModels()` | `List<OllamaModelInfo>` | Lists loaded models |
| `chat(OllamaChatRequest)` | `OllamaChatResponse` | Synchronous chat |
| `chatStream(OllamaChatRequest)` | `Flux<OllamaChatChunk>` | Streaming chat |
| `embed(String)` | `float[]` | Generate embedding |
| `embedBatch(List<String>)` | `List<float[]>` | Batch embeddings |

### ChatService (`com.myoffgridai.ai.service.ChatService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `createConversation(UUID, String)` | `Conversation` | Create new conversation |
| `getConversations(UUID, boolean, Pageable)` | `Page<Conversation>` | List conversations |
| `getConversation(UUID, UUID)` | `Conversation` | Get with ownership check |
| `archiveConversation(UUID, UUID)` | `void` | Archive conversation |
| `deleteConversation(UUID, UUID)` | `void` | Delete conversation + messages |
| `sendMessage(UUID, UUID, String)` | `Message` | Sync message exchange |
| `streamMessage(UUID, UUID, String)` | `Flux<String>` | Streaming message exchange |
| `generateTitle(UUID, String)` | `void` | Async title generation |

### SystemPromptBuilder (`com.myoffgridai.ai.service.SystemPromptBuilder`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `build(User, String)` | `String` | Assembles system prompt |

### ContextWindowService (`com.myoffgridai.ai.service.ContextWindowService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `prepareMessages(UUID, String, String)` | `List<OllamaMessage>` | Builds context window |

### AgentService (`com.myoffgridai.ai.service.AgentService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `executeTask(UUID, UUID, String)` | `AgentTaskResult` | Execute agent task |

### ModelHealthCheckService (`com.myoffgridai.ai.service.ModelHealthCheckService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `checkOllamaOnStartup()` | `void` | Startup health check |

---

## 11. Phase 2 — Security Matrix (New Endpoints)

| Method | Endpoint | Auth Required | Notes |
|--------|----------|---------------|-------|
| POST | `/api/chat/conversations` | Yes | Any authenticated |
| GET | `/api/chat/conversations` | Yes | Own conversations only |
| GET | `/api/chat/conversations/{id}` | Yes | Own conversations only |
| DELETE | `/api/chat/conversations/{id}` | Yes | Own conversations only |
| PUT | `/api/chat/conversations/{id}/archive` | Yes | Own conversations only |
| POST | `/api/chat/conversations/{id}/messages` | Yes | Own conversations only |
| GET | `/api/chat/conversations/{id}/messages` | Yes | Own conversations only |
| GET | `/api/models` | No | Public |
| GET | `/api/models/active` | Yes | Any authenticated |
| GET | `/api/models/health` | No | Public |

---

## 12. Phase 2 — Constants Added to AppConstants

### Ollama
- `OLLAMA_BASE_URL` = "http://localhost:11434"
- `OLLAMA_MODEL` = "qwen3:32b"
- `OLLAMA_EMBED_MODEL` = "nomic-embed-text"
- `OLLAMA_CONNECT_TIMEOUT_SECONDS` = 10
- `OLLAMA_READ_TIMEOUT_SECONDS` = 120
- `OLLAMA_MAX_CONTEXT_TOKENS` = 8192
- `OLLAMA_CONTEXT_WINDOW_MESSAGES` = 20

### Chat
- `MAX_MESSAGE_LENGTH` = 32000
- `CHAT_API_PATH` = "/api/chat"
- `MODELS_API_PATH` = "/api/models"
- `TITLE_GENERATION_MAX_TOKENS` = 20

---

## 13. Phase 2 — Custom Exceptions

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `OllamaUnavailableException` | 503 | Ollama service unreachable |
| `OllamaInferenceException` | 502 | Ollama inference error |
| `EmbeddingException` | 503 | Embedding generation failure |

---

## 14. Phase 3 — Memory & RAG Entities

### VectorDocument (`com.myoffgridai.memory.model.VectorDocument`)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | `@GeneratedValue(strategy = UUID)` |
| userId | UUID | not null, indexed | Owner user ID |
| content | String (TEXT) | not null | Original text content |
| embedding | float[] | nullable | pgvector `vector(768)` via `@Type(VectorType.class)` |
| sourceType | VectorSourceType | not null | `@Enumerated(STRING)` |
| sourceId | UUID | nullable | FK to source entity (Memory, Conversation) |
| metadata | String (TEXT) | nullable | JSON metadata |
| createdAt | Instant | not null, auto | `@CreatedDate` |

**Table:** `vector_document`
**Index:** `(userId, sourceType)`

### Memory (`com.myoffgridai.memory.model.Memory`)
| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | PK, generated | `@GeneratedValue(strategy = UUID)` |
| userId | UUID | not null, indexed | Owner user ID |
| content | String (TEXT) | not null | Memory text |
| importance | MemoryImportance | not null, default MEDIUM | `@Enumerated(STRING)` |
| tags | String | nullable | Comma-separated tags |
| sourceConversationId | UUID | nullable | FK to originating conversation |
| createdAt | Instant | not null, auto | `@CreatedDate` |
| updatedAt | Instant | auto | `@LastModifiedDate` |
| lastAccessedAt | Instant | nullable | Updated on RAG access |
| accessCount | int | not null, default 0 | Incremented on RAG access |

**Table:** `memories`

### VectorSourceType (`com.myoffgridai.memory.model.VectorSourceType`)
| Value | Description |
|-------|-------------|
| MEMORY | User memory fact |
| CONVERSATION | Conversation summary |
| KNOWLEDGE_CHUNK | Knowledge base chunk |

### MemoryImportance (`com.myoffgridai.memory.model.MemoryImportance`)
| Value | Description |
|-------|-------------|
| LOW | Low priority |
| MEDIUM | Default priority |
| HIGH | High priority |
| CRITICAL | Critical (e.g., conversation summaries) |

---

## 15. Phase 3 — Repository Method Inventory

### VectorDocumentRepository (`com.myoffgridai.memory.repository.VectorDocumentRepository`)
Extends: `JpaRepository<VectorDocument, UUID>`

| Method | Return Type | Notes |
|--------|-------------|-------|
| `findMostSimilar(UUID, String, String, int)` | `List<VectorDocument>` | Native SQL: cosine distance `<=>` |
| `findByUserIdAndSourceType(UUID, VectorSourceType)` | `List<VectorDocument>` | Filter by source |
| `deleteBySourceIdAndSourceType(UUID, VectorSourceType)` | `void` | Cascade cleanup |
| `deleteByUserId(UUID)` | `void` | Privacy wipe |

### MemoryRepository (`com.myoffgridai.memory.repository.MemoryRepository`)
Extends: `JpaRepository<Memory, UUID>`

| Method | Return Type |
|--------|-------------|
| `findByUserIdOrderByCreatedAtDesc(UUID, Pageable)` | `Page<Memory>` |
| `findByUserIdAndImportance(UUID, MemoryImportance, Pageable)` | `Page<Memory>` |
| `findByUserIdAndTagsContaining(UUID, String, Pageable)` | `Page<Memory>` |
| `findByUserId(UUID)` | `List<Memory>` |
| `deleteByUserId(UUID)` | `void` |
| `countByUserId(UUID)` | `long` |

---

## 16. Phase 3 — Service Method Inventory

### EmbeddingService (`com.myoffgridai.memory.service.EmbeddingService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `embed(String)` | `float[]` | Single text embedding via OllamaService |
| `embedAndFormat(String)` | `String` | Embed and format as pgvector string |
| `embedBatch(List<String>)` | `List<float[]>` | Batch embeddings |
| `cosineSimilarity(float[], float[])` | `float` | Compute cosine similarity |
| `formatEmbedding(float[])` | `String` | Static: format as pgvector string |

### MemoryService (`com.myoffgridai.memory.service.MemoryService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `createMemory(UUID, String, MemoryImportance, String, UUID)` | `Memory` | Create memory + vector doc |
| `findRelevantMemories(UUID, String, int)` | `List<Memory>` | Vector similarity search |
| `searchMemoriesWithScores(UUID, String, int)` | `List<MemorySearchResultDto>` | Search with scores for API |
| `getMemory(UUID, UUID)` | `Memory` | Get with ownership check |
| `updateImportance(UUID, UUID, MemoryImportance)` | `Memory` | Update importance |
| `updateTags(UUID, UUID, String)` | `Memory` | Update tags |
| `deleteMemory(UUID, UUID)` | `void` | Delete memory + vector doc |
| `deleteAllMemoriesForUser(UUID)` | `void` | Privacy wipe |
| `exportMemories(UUID)` | `List<Memory>` | Data export |
| `getMemories(UUID, MemoryImportance, String, Pageable)` | `Page<Memory>` | Paginated list with filters |
| `toDto(Memory)` | `MemoryDto` | Convert to DTO |

### RagService (`com.myoffgridai.memory.service.RagService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `buildRagContext(UUID, String)` | `RagContext` | Build RAG context from memories + knowledge |
| `formatContextBlock(RagContext)` | `String` | Format context for system prompt |

### MemoryExtractionService (`com.myoffgridai.memory.service.MemoryExtractionService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `extractAndStore(UUID, UUID, String, String)` | `void` | `@Async` extract facts from chat exchange |

### SummarizationService (`com.myoffgridai.memory.service.SummarizationService`)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `summarizeConversation(UUID, UUID)` | `Memory` | Summarize conversation as CRITICAL memory |
| `scheduledNightlySummarization()` | `void` | `@Scheduled(cron)` nightly batch summarization |

### SystemPromptBuilder (Updated)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `build(User, String)` | `String` | Base system prompt (no RAG) |
| `build(User, String, RagContext)` | `String` | System prompt with RAG context |

### ChatService (Updated)
| Method | Return Type | Description |
|--------|-------------|-------------|
| `sendMessage(UUID, UUID, String)` | `Message` | Now includes RAG context + async memory extraction |
| `streamMessage(UUID, UUID, String)` | `Flux<String>` | Now includes RAG context + async memory extraction |

---

## 17. Phase 3 — Security Matrix (New Endpoints)

| Method | Endpoint | Auth Required | Notes |
|--------|----------|---------------|-------|
| GET | `/api/memory` | Yes | Own memories only, paginated |
| GET | `/api/memory/{id}` | Yes | Own memory only |
| DELETE | `/api/memory/{id}` | Yes | Own memory only |
| PUT | `/api/memory/{id}/importance` | Yes | Own memory only |
| PUT | `/api/memory/{id}/tags` | Yes | Own memory only |
| POST | `/api/memory/search` | Yes | Vector similarity search |
| GET | `/api/memory/export` | Yes | Export all own memories |

---

## 18. Phase 3 — Constants Added to AppConstants

### Memory & RAG
- `EMBEDDING_DIMENSIONS` = 768
- `RAG_TOP_K` = 5
- `MEMORY_TOP_K` = 5
- `SIMILARITY_THRESHOLD` = 0.7f
- `RAG_MAX_CONTEXT_TOKENS` = 2048
- `MEMORY_EXTRACTION_MAX_FACTS` = 3
- `MEMORY_SUMMARIZATION_TAG` = "conversation-summary"
- `SUMMARIZATION_MIN_MESSAGES` = 10
- `SUMMARIZATION_AGE_DAYS` = 7
- `MEMORY_API_PATH` = "/api/memory"

---

## 19. Phase 3 — Package Structure (Updated)

```
com.myoffgridai
├── MyOffGridAiApplication.java    (@EnableAsync, @EnableScheduling)
├── config/
│   ├── AppConstants.java
│   ├── JpaConfig.java
│   ├── JwtAuthFilter.java
│   ├── OllamaConfig.java
│   ├── SecurityConfig.java
│   ├── VectorStoreConfig.java     (NEW — pgvector extension check)
│   └── VectorType.java            (NEW — Hibernate UserType for vector)
├── auth/
│   └── ... (unchanged)
├── ai/
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── ModelController.java
│   ├── service/
│   │   ├── ChatService.java       (UPDATED — RAG + memory extraction)
│   │   ├── OllamaService.java
│   │   ├── SystemPromptBuilder.java (UPDATED — RagContext overload)
│   │   ├── ContextWindowService.java
│   │   ├── AgentService.java
│   │   └── ModelHealthCheckService.java
│   ├── model/
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   └── MessageRole.java
│   ├── dto/
│   │   └── ... (unchanged)
│   └── repository/
│       ├── ConversationRepository.java
│       └── MessageRepository.java
├── memory/                         (NEW — Phase 3)
│   ├── controller/
│   │   └── MemoryController.java
│   ├── service/
│   │   ├── EmbeddingService.java
│   │   ├── MemoryService.java
│   │   ├── RagService.java
│   │   ├── MemoryExtractionService.java
│   │   └── SummarizationService.java
│   ├── model/
│   │   ├── Memory.java
│   │   ├── MemoryImportance.java
│   │   ├── VectorDocument.java
│   │   └── VectorSourceType.java
│   ├── dto/
│   │   ├── MemoryDto.java
│   │   ├── MemorySearchRequest.java
│   │   ├── MemorySearchResultDto.java
│   │   ├── RagContext.java
│   │   ├── UpdateImportanceRequest.java
│   │   └── UpdateTagsRequest.java
│   └── repository/
│       ├── MemoryRepository.java
│       └── VectorDocumentRepository.java
└── common/
    ├── exception/
    │   ├── GlobalExceptionHandler.java (UPDATED — EmbeddingException handler)
    │   ├── EmbeddingException.java     (NEW)
    │   └── ... (unchanged)
    ├── response/
    │   └── ApiResponse.java
    └── util/
        └── TokenCounter.java
```
