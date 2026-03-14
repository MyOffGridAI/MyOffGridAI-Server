# MyOffGridAI-Server — Architecture Specification

**Generated:** 2026-03-14
**Phase:** 11 — Captive Portal & Setup Wizard
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
| Vector Search | pgvector | 0.1.6 (Java) / PostgreSQL ext |
| Testing | JUnit 5, Mockito 5.21.0, Testcontainers | via Spring Boot 3.4.3 |
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
├── config/           — Application configuration, security, JWT filter, captive portal filter
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
├── system/           — System management, captive portal, AP mode, factory reset (Phase 2 + 11)
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
┌──────────────────────────┐
│ CaptivePortalRedirectFilter│  If AP mode active & not API/static,
│ (OncePerRequest)           │  redirect to /setup
└─────────┬────────────────┘
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

---

## 9. Phase 2 — Ollama Integration Architecture

### OllamaService as Sole Integration Point

```
┌────────────────────────┐
│   ChatService          │──→ OllamaService ──→ Ollama (localhost:11434)
│   AgentService         │──→ OllamaService ──→ Ollama
│   ModelController      │──→ OllamaService ──→ Ollama
│   ModelHealthCheck     │──→ OllamaService ──→ Ollama
└────────────────────────┘
         ▲
         │ No direct Ollama calls from any other class
```

### HTTP Clients
- **RestClient** (`ollamaRestClient`): Blocking calls for chat, embed, health, model listing
- **WebClient** (`ollamaWebClient`): Reactive streaming for SSE chat responses

---

## 10. Phase 2 — Chat Flow

### Synchronous Chat
```
POST /api/chat/conversations/{id}/messages (stream=false)
    │
    ▼
ChatController.sendMessage()
    │ @AuthenticationPrincipal User
    ▼
ChatService.sendMessage()
    ├── Verify conversation ownership
    ├── Persist user Message (role=USER)
    ├── SystemPromptBuilder.build() → system prompt
    ├── ContextWindowService.prepareMessages()
    │   ├── Fetch recent messages (up to 20)
    │   ├── Prepend system prompt
    │   ├── Append new user message
    │   └── TokenCounter.truncateToTokenLimit(8192)
    ├── OllamaService.chat() → synchronous response
    ├── Persist assistant Message (role=ASSISTANT)
    ├── Increment conversation messageCount
    ├── If first exchange → @Async generateTitle()
    └── Return ApiResponse<MessageDto>
```

### Streaming Chat
```
POST /api/chat/conversations/{id}/messages (stream=true)
    │
    ▼
ChatController.sendMessage()
    │ Returns SseEmitter
    ▼
ChatService.streamMessage()
    ├── Persist user Message
    ├── Build context window
    ├── OllamaService.chatStream() → Flux<OllamaChatChunk>
    ├── Each chunk → SseEmitter.send(token)
    ├── On complete:
    │   ├── Join all tokens → full response
    │   ├── Persist assistant Message
    │   ├── Update messageCount
    │   └── Send [DONE] event
    └── SseEmitter.complete()
```

---

## 11. Phase 2 — Agent Framework Foundation

### Tool-Call Pattern
```json
{"tool": "tool_name", "params": {"key": "value"}}
```

### Phase 2 Behavior
- AgentService sends step-by-step reasoning prompts to Ollama
- Parses response for JSON tool-call blocks using regex
- Detected tool calls are logged but NOT executed
- Returns `AgentTaskResult` with `detectedToolCalls` list
- Phase 5 will wire tool calls to the SkillService for execution

---

## 12. Phase 2 — Package Structure Update

```
com.myoffgridai
├── ai/
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── ModelController.java
│   ├── service/
│   │   ├── OllamaService.java
│   │   ├── ChatService.java
│   │   ├── SystemPromptBuilder.java
│   │   ├── ContextWindowService.java
│   │   ├── AgentService.java
│   │   └── ModelHealthCheckService.java
│   ├── model/
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   └── MessageRole.java
│   ├── dto/
│   │   ├── OllamaModelInfo.java
│   │   ├── OllamaMessage.java
│   │   ├── OllamaChatRequest.java
│   │   ├── OllamaChatResponse.java
│   │   ├── OllamaChatChunk.java
│   │   ├── CreateConversationRequest.java
│   │   ├── SendMessageRequest.java
│   │   ├── ConversationDto.java
│   │   ├── ConversationSummaryDto.java
│   │   ├── MessageDto.java
│   │   ├── ActiveModelDto.java
│   │   ├── OllamaHealthDto.java
│   │   └── AgentTaskResult.java
│   └── repository/
│       ├── ConversationRepository.java
│       └── MessageRepository.java
├── common/
│   ├── exception/
│   │   ├── OllamaUnavailableException.java
│   │   └── OllamaInferenceException.java
│   └── util/
│       └── TokenCounter.java
└── config/
    └── OllamaConfig.java
```

---

## 13. Phase 3 — Memory & RAG Architecture

### Memory Pipeline
```
Chat Exchange (user msg + assistant response)
    │
    ▼
MemoryExtractionService.extractAndStore()  (@Async)
    ├── Build extraction prompt
    ├── OllamaService.chat() → JSON array of facts
    ├── Parse facts (handle markdown fences, invalid JSON)
    └── For each fact:
        └── MemoryService.createMemory()
            ├── Persist Memory entity
            └── EmbeddingService.embed() → VectorDocument
```

### RAG Pipeline
```
User sends message
    │
    ▼
ChatService.sendMessage()
    ├── RagService.buildRagContext(userId, userContent)
    │   ├── EmbeddingService.embed(userContent) → query vector
    │   ├── MemoryService.findRelevantMemories()
    │   │   ├── VectorDocumentRepository.findMostSimilar() [cosine distance <=>]
    │   │   ├── Filter by SIMILARITY_THRESHOLD (0.7)
    │   │   └── Update access tracking (lastAccessedAt, accessCount)
    │   ├── VectorDocumentRepository.findMostSimilar(KNOWLEDGE_CHUNK)
    │   └── Assemble RagContext(memorySnippets, knowledgeSnippets, hasContext, tokenEstimate)
    │
    ├── SystemPromptBuilder.build(user, instanceName, ragContext)
    │   ├── Base system prompt (identity, user context)
    │   ├── Truncate context if > RAG_MAX_CONTEXT_TOKENS (knowledge first, then memories)
    │   └── RagService.formatContextBlock() → [RELEVANT MEMORIES]...[END MEMORIES]
    │
    ├── ContextWindowService.prepareMessages()
    ├── OllamaService.chat() → response
    ├── Persist assistant Message (hasRagContext = true/false)
    └── MemoryExtractionService.extractAndStore() (@Async)
```

### Nightly Summarization
```
@Scheduled(cron = "0 0 2 * * *")
SummarizationService.scheduledNightlySummarization()
    ├── Find conversations: messageCount >= 10 AND age >= 7 days
    ├── Skip already-summarized (tag = "conversation-summary")
    └── For each eligible:
        ├── Fetch messages
        ├── OllamaService.chat() → summary
        └── MemoryService.createMemory(CRITICAL, "conversation-summary")
```

### Vector Search Architecture
```
┌─────────────────────────┐
│  EmbeddingService       │  Sole entry point for embeddings
│  (wraps OllamaService)  │  embed(), embedAndFormat(), cosineSimilarity()
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐
│  VectorDocumentRepository│  Native SQL with pgvector
│  findMostSimilar()      │  ORDER BY embedding <=> CAST(:embedding AS vector)
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐
│  PostgreSQL + pgvector  │  vector(768) column type
│  Cosine distance: <=>   │  Extension: CREATE EXTENSION IF NOT EXISTS vector
└─────────────────────────┘
```

### Per-User Memory Isolation
```
MemoryService
├── createMemory()      → sets userId on Memory + VectorDocument
├── getMemory()         → assertOwnership(memory.userId, callerId)
├── updateImportance()  → assertOwnership()
├── updateTags()        → assertOwnership()
├── deleteMemory()      → assertOwnership()
├── findRelevantMemories()  → queries filtered by userId
├── exportMemories()    → queries filtered by userId
└── deleteAllMemories() → deletes by userId

VectorDocumentRepository.findMostSimilar()
    WHERE user_id = :userId   ← per-user isolation at query level
```

---

## 14. Phase 3 — Dependency Graph

```
ChatService
    ├── OllamaService
    ├── SystemPromptBuilder
    │   └── RagService
    │       ├── MemoryService
    │       │   ├── MemoryRepository
    │       │   ├── VectorDocumentRepository
    │       │   └── EmbeddingService
    │       │       └── OllamaService
    │       └── VectorDocumentRepository
    ├── ContextWindowService
    ├── MemoryExtractionService
    │   ├── OllamaService
    │   └── MemoryService
    └── ConversationRepository / MessageRepository

MemoryController → MemoryService

SummarizationService
    ├── ConversationRepository
    ├── MessageRepository
    ├── OllamaService
    ├── MemoryService
    └── MemoryRepository
```

---

## 15. Phase 11 — Captive Portal & Setup Wizard

### First-Boot Flow
```
Device powers on (system not initialized)
    │
    ▼
ApModeStartupService.onApplicationReady()
    ├── SystemConfigService.isInitialized() → false
    └── ApModeService.startApMode()
        ├── hostapd → broadcast SSID "MyOffGridAI-Setup"
        ├── dnsmasq → DHCP + DNS redirect
        └── SystemConfig.apModeEnabled = true

User connects to WiFi SSID
    │
    ▼
CaptivePortalRedirectFilter
    ├── All non-API/static requests → redirect to /setup
    └── API requests pass through normally

Setup Wizard (4-step HTML/JS)
    │
    ├── Step 1: Welcome (index.html)
    ├── Step 2: WiFi Config (wifi.html)
    │   ├── GET /api/setup/wifi/scan → ApModeService.scanWifiNetworks()
    │   └── POST /api/setup/wifi/connect → ApModeService.connectToWifi()
    ├── Step 3: Owner Account (account.html)
    │   └── POST /api/system/initialize → SystemController
    │       ├── AuthService.register(ROLE_OWNER)
    │       └── SystemConfigService.setInitialized()
    └── Step 4: Confirm (confirm.html)
        └── POST /api/system/finalize-setup
            └── NetworkTransitionService.finalizeSetup() @Async
                ├── ApModeService.stopApMode()
                ├── Start avahi-daemon (mDNS)
                └── SystemConfig.wifiConfigured = true
```

### Factory Reset Flow
```
POST /api/system/factory-reset (OWNER only)
    │ Requires confirmPhrase = "RESET MY DEVICE"
    ▼
FactoryResetService.performReset() @Async
    ├── UserRepository.deleteAll()
    ├── SystemConfigRepository.deleteAll()
    ├── ApModeService.startApMode()
    └── System returns to first-boot state

USB Reset (physical trigger)
    │
    ▼
UsbResetWatcherService @Scheduled(fixedDelay=30000)
    ├── Check /media/myoffgridai/USB/myoffgridai-reset.txt
    ├── If found → FactoryResetService.performUsbReset(path)
    │   ├── performReset()
    │   └── Delete trigger file
    └── Check for myoffgridai-update.zip (logged, not yet implemented)
```

### Dependency Graph
```
CaptivePortalController
    ├── SystemConfigService
    └── ApModeService

SystemController
    ├── SystemConfigService
    ├── AuthService
    ├── NetworkTransitionService
    │   ├── ApModeService
    │   └── SystemConfigService
    └── FactoryResetService
        ├── UserRepository
        ├── SystemConfigRepository
        └── ApModeService

ApModeStartupService
    ├── SystemConfigService
    └── ApModeService

UsbResetWatcherService
    └── FactoryResetService

CaptivePortalRedirectFilter (config package)
    └── ApModeService
```
