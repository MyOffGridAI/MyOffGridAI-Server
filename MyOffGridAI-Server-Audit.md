# MyOffGridAI-Server -- Codebase Audit

**Generated:** 2026-03-14
**Project:** MyOffGridAI-Server
**Root Package:** `com.myoffgridai`
**Java Version:** 21
**Framework:** Spring Boot 3.4.3
**Build Tool:** Maven (with Maven Wrapper)
**Status:** FINAL -- All 9 phases complete (Phase 11: Captive Portal)

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure Overview](#2-project-structure-overview)
3. [Configuration & Infrastructure](#3-configuration--infrastructure)
4. [Phase 1 -- Authentication & Users (`auth`)](#4-phase-1----authentication--users-auth)
5. [Common Package (`common`)](#5-common-package-common)
6. [Phase 2 -- AI & Chat (`ai`)](#6-phase-2----ai--chat-ai)
7. [Phase 3 -- Memory & RAG (`memory`)](#7-phase-3----memory--rag-memory)
8. [Phase 4 -- Knowledge Vault (`knowledge`)](#8-phase-4----knowledge-vault-knowledge)
9. [Phase 5 -- Skills & Automation (`skills`)](#9-phase-5----skills--automation-skills)
10. [Phase 6 -- Sensors (`sensors`)](#10-phase-6----sensors-sensors)
11. [Phase 7 -- Proactive Engine (`proactive`)](#11-phase-7----proactive-engine-proactive)
12. [Phase 8 -- Privacy & Fortress (`privacy`) + System (`system`)](#12-phase-8----privacy--fortress-privacy--system-system)
13. [Phase 11 -- Captive Portal & Setup Wizard (`system`)](#13-phase-11----captive-portal--setup-wizard-system)
14. [Test Suite](#14-test-suite)
15. [Database Entities & Relationships](#15-database-entities--relationships)
16. [API Endpoint Summary](#16-api-endpoint-summary)
17. [Summary Statistics](#17-summary-statistics)

---

## 1. Technology Stack

| Category | Technology | Version / Notes |
|---|---|---|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.3 |
| Build | Maven | Wrapper included |
| Database | PostgreSQL | pgvector extension for embeddings |
| ORM | Hibernate / Spring Data JPA | ddl-auto: update (dev), validate (prod) |
| Migration | Flyway | Disabled in dev, enabled in prod |
| Security | Spring Security + JJWT | 0.12.6, stateless JWT, BCrypt hashing |
| AI/LLM | Ollama | qwen3:32b (chat), nomic-embed-text (embeddings) |
| Vector Search | pgvector | 768-dimension float[] embeddings, cosine similarity |
| PDF Extraction | Apache PDFBox | 3.0.4 |
| OCR | Tess4J (Tesseract) | 5.14.0 |
| Serial Ports | jSerialComm | 2.11.0 |
| Streaming | SSE (Server-Sent Events) | SseEmitter + WebClient reactive |
| HTTP Client | RestClient (sync) + WebClient (reactive) | For Ollama communication |
| API Docs | springdoc-openapi | 2.8.4 (Swagger UI) |
| Testing | JUnit 5 + Mockito 5.21.0 + Testcontainers | ByteBuddy 1.18.4 |
| Annotations | Lombok | 1.18.42 (explicit processor paths) |
| Encryption | AES-256-GCM | PBKDF2 key derivation for data export |

### Key Dependencies (pom.xml)

```
spring-boot-starter-web, spring-boot-starter-data-jpa,
spring-boot-starter-security, spring-boot-starter-validation,
spring-boot-starter-webflux (WebClient only),
jjwt-api/impl/jackson (0.12.6),
postgresql, pgvector (0.1.6),
pdfbox (3.0.4), tess4j (5.14.0),
jSerialComm (2.11.0),
springdoc-openapi-starter-webmvc-ui (2.8.4),
spring-boot-starter-test, spring-security-test,
testcontainers (postgresql)
```

---

## 2. Project Structure Overview

```
src/main/java/com/myoffgridai/
├── MyOffGridAiApplication.java          # Entry point (@EnableAsync, @EnableScheduling)
├── config/                              # Cross-cutting configuration (8 files)
├── common/                              # Shared utilities (16 files)
│   ├── exception/                       # Custom exceptions + GlobalExceptionHandler
│   ├── response/                        # ApiResponse wrapper
│   └── util/                            # TokenCounter
├── auth/                                # Phase 1: Authentication & Users (14 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── ai/                                  # Phase 2: AI & Chat (21 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── memory/                              # Phase 3: Memory & RAG (17 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── knowledge/                           # Phase 4: Knowledge Vault (16 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── skills/                              # Phase 5: Skills & Automation (27 files)
│   ├── builtin/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── sensors/                             # Phase 6: Sensors (18 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── proactive/                           # Phase 7: Proactive Engine (16 files)
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
├── privacy/                             # Phase 8a: Privacy & Fortress (17 files)
│   ├── aspect/
│   ├── controller/
│   ├── dto/
│   ├── model/
│   ├── repository/
│   └── service/
└── system/                              # Phase 8b + Phase 11: System Management & Captive Portal (16 files)
    ├── controller/
    ├── dto/
    ├── model/
    ├── repository/
    └── service/
```

**File Counts:** 202 main source files, 89 test files (291 total Java files)

---

## 3. Configuration & Infrastructure

### 3.1 Application Entry Point

| File | Path | Description |
|---|---|---|
| `MyOffGridAiApplication` | `src/main/java/com/myoffgridai/MyOffGridAiApplication.java` | Spring Boot main class. Annotated with `@EnableAsync` and `@EnableScheduling` to support async document ingestion, memory extraction, and scheduled jobs (nightly summarization, health checks, insight generation). |

### 3.2 Configuration Package (`config`) -- 8 files

| File | Type | Description | Key Public Methods / Notes |
|---|---|---|---|
| `AppConstants` | `final class` | Centralized application constants for all 8 phases. No magic numbers or hardcoded strings elsewhere. | 90+ constants organized by domain: Server ports, JWT, API paths, pagination, file upload, sensors, RAG, memory, knowledge, skills, proactive, privacy, passwords, Ollama settings, chat limits, roles. |
| `SecurityConfig` | `@Configuration` | Stateless JWT security filter chain. CORS allows all origins. | Public endpoints: `/api/auth/login`, `/register`, `/refresh`, `/api/system/status`, `/initialize`, `/api/system/finalize-setup`, `/api/setup/**`, `/api/models/**`, `/actuator/health`, `/swagger-ui/**`. All other endpoints require authentication. |
| `JwtAuthFilter` | `OncePerRequestFilter` | Extracts Bearer token from Authorization header, checks blacklist, validates via JwtService, sets SecurityContext. | `doFilterInternal()` -- skips if no token or blacklisted, extracts username from JWT, loads UserDetails, sets authentication. |
| `CaptivePortalRedirectFilter` | `OncePerRequestFilter @Component` | Redirects non-API, non-static requests to `/setup` when AP mode is active and system is uninitialized. Skips API paths, static resources, and setup paths. | `doFilterInternal()` -- checks `ApModeService.isApModeActive()`, redirects browser requests to `/setup`. |
| `JpaConfig` | `@Configuration` | Separated `@EnableJpaAuditing` to avoid `@WebMvcTest` test conflicts. | No public methods -- annotation-only config. |
| `OllamaConfig` | `@Configuration` | Creates two HTTP client beans for Ollama. | `ollamaRestClient()` -- blocking RestClient with configurable timeout. `ollamaWebClient()` -- reactive WebClient for SSE streaming with 10MB buffer. |
| `VectorStoreConfig` | `@Component` | Startup check verifying the pgvector extension is installed in PostgreSQL. | `checkVectorExtension()` -- `@PostConstruct`, logs warning if pgvector not available. |
| `VectorType` | `UserType` (Hibernate) | Custom Hibernate type mapping `float[]` to PostgreSQL `vector(768)` columns. | `nullSafeGet()`, `nullSafeSet()` -- handles serialization between Java `float[]` and pgvector column type. |

### 3.3 Application Configuration (`application.yml`)

| Profile | Key Settings |
|---|---|
| **default** | Port 8080, multipart max 100MB, Flyway disabled |
| **dev** | PostgreSQL `localhost:5432/myoffgridai`, user/pass `myoffgridai/myoffgridai`, Hibernate ddl-auto: update, Ollama `qwen3:32b` + `nomic-embed-text`, fortress mock mode |
| **prod** | Hibernate ddl-auto: validate, Flyway enabled |

---

## 4. Phase 1 -- Authentication & Users (`auth`)

**Package:** `com.myoffgridai.auth` -- 14 files

### 4.1 Controllers

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `AuthController` | `@RestController` | `/api/auth` | `POST /register`, `POST /login`, `POST /refresh`, `POST /logout` | Authentication lifecycle. Registration creates users with BCrypt-hashed passwords. Login returns JWT access + refresh tokens. Logout blacklists current token. |
| `UserController` | `@RestController` | `/api/users` | `GET /` (paginated, OWNER/ADMIN), `GET /{id}`, `PUT /{id}` (OWNER/ADMIN), `PUT /{id}/deactivate` (OWNER), `DELETE /{id}` (OWNER) | User management. Owner-only operations: deactivate, delete. Admin+ can update profiles. All users can view by ID. |

### 4.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `AuthService` | `@Service` | Core authentication logic. In-memory token blacklist (Set). Profile-based password policy (dev: 4 chars, prod: 12 chars). | `register(RegisterRequest)`, `login(LoginRequest)`, `refresh(RefreshRequest)`, `logout(String)`, `changePassword(UUID, ChangePasswordRequest)` |
| `JwtService` | `@Service` | HMAC-SHA256 JWT signing via JJWT. Configurable expiration from `application.yml`. | `generateAccessToken(UserDetails)`, `generateRefreshToken(UserDetails)`, `extractUsername(String)`, `isTokenValid(String, UserDetails)` |
| `UserService` | `@Service` | User CRUD with pagination. | `getUser(UUID)`, `listUsers(Pageable)`, `updateUser(UUID, UpdateUserRequest)`, `deactivateUser(UUID)`, `deleteUser(UUID)` |

### 4.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `User` | `@Entity` | `users` | Implements `UserDetails`. Fields: `id` (UUID), `username`, `email`, `displayName`, `passwordHash`, `role` (enum), `isActive`, `createdAt`, `updatedAt`, `lastLoginAt`. |
| `Role` | `enum` | -- | `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER`, `ROLE_CHILD` |

### 4.4 Repository

| File | Type | Key Query Methods |
|---|---|---|
| `UserRepository` | `JpaRepository<User, UUID>` | `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`, `findAllByRole`, `countByIsActiveTrue`, `findByIsActiveTrue` |

### 4.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `AuthResponse` | JWT response: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `userId`, `username`, `role` |
| `LoginRequest` | `username`, `password` |
| `RegisterRequest` | `username`, `email`, `displayName`, `password`, `role` |
| `RefreshRequest` | `refreshToken` |
| `ChangePasswordRequest` | `currentPassword`, `newPassword` (validated) |
| `UpdateUserRequest` | `displayName`, `email` |
| `UserDetailDto` | Full user details for admin views |
| `UserSummaryDto` | Lightweight user summary |

---

## 5. Common Package (`common`)

**Package:** `com.myoffgridai.common` -- 16 files

### 5.1 Response Wrapper

| File | Type | Description |
|---|---|---|
| `ApiResponse<T>` | Generic record | Standard response wrapper: `success`, `message`, `data`, `timestamp`, `requestId`, `pagination`. Static factories: `success()`, `error()`, `paginated()`. |

### 5.2 Utility

| File | Type | Description |
|---|---|---|
| `TokenCounter` | Utility class | Token estimation (~4 chars/token). Context window truncation preserving system message and latest user message. `estimateTokens(String)`, `estimateTokens(List<OllamaMessage>)`, `truncateToFit(List<OllamaMessage>, int)`. |

### 5.3 Exception Classes (14 files)

| File | HTTP Status | Description |
|---|---|---|
| `GlobalExceptionHandler` | `@RestControllerAdvice` | Central exception handler mapping 17 exception types to HTTP responses. Handles validation (400), auth (401/403), not-found (404), conflict (409), infrastructure (500/502/503). |
| `EntityNotFoundException` | 404 | Resource not found |
| `DuplicateResourceException` | 409 | Username/email already exists |
| `OllamaUnavailableException` | 503 | Ollama LLM service unreachable |
| `OllamaInferenceException` | 502 | Ollama returned an error during inference |
| `EmbeddingException` | 503 | Embedding generation failed |
| `StorageException` | 500 | File system storage error |
| `UnsupportedFileTypeException` | 400 | Uploaded file MIME type not supported |
| `OcrException` | 500 | OCR processing failed |
| `FortressActiveException` | 403 | Operation blocked by fortress mode |
| `FortressOperationException` | 500 | iptables command failed |
| `SkillDisabledException` | 400 | Attempted to execute a disabled skill |
| `SensorConnectionException` | 502 | Serial port connection failed |
| `InitializationException` | 500 | System initialization error |

---

## 6. Phase 2 -- AI & Chat (`ai`)

**Package:** `com.myoffgridai.ai` -- 21 files

### 6.1 Controllers

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `ChatController` | `@RestController` | `/api/chat` | `POST /conversations`, `GET /conversations` (paginated), `GET /conversations/{id}`, `DELETE /conversations/{id}`, `PUT /conversations/{id}/archive`, `POST /conversations/{id}/messages` (sync + SSE streaming), `GET /conversations/{id}/messages` (paginated) | Core chat orchestration. Sync and streaming message endpoints. Streaming uses `SseEmitter` with `text/event-stream`. |
| `ModelController` | `@RestController` | `/api/models` | `GET /` (public, list available models), `GET /active` (authenticated, active model), `GET /health` (public, Ollama health check) | Model discovery and health check. |

### 6.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `ChatService` | `@Service` | Core chat orchestration. Creates conversations, handles sync/streaming message exchange with RAG context injection, triggers async title generation and memory extraction. | `createConversation(UUID, CreateConversationRequest)`, `sendMessage(UUID, UUID, SendMessageRequest)`, `sendMessageStream(UUID, UUID, SendMessageRequest)` returns `SseEmitter`, `listConversations(UUID, Pageable)`, `getConversation(UUID, UUID)`, `deleteConversation(UUID, UUID)`, `archiveConversation(UUID, UUID)`, `getMessages(UUID, UUID, Pageable)` |
| `OllamaService` | `@Service` | Sole integration point with the Ollama LLM service. Handles sync chat, streaming chat (Flux), embedding generation, model listing, and health checks. | `isAvailable()`, `listModels()`, `chat(OllamaChatRequest)`, `chatStream(OllamaChatRequest)` returns `Flux<OllamaChatChunk>`, `embed(String)`, `embedBatch(List<String>)` |
| `AgentService` | `@Service` | Tool-call agent loop. Sends prompts, parses `[TOOL_CALL]` JSON patterns from responses, dispatches to `SkillExecutorService`, loops up to `AGENT_MAX_ITERATIONS` (5). | `executeAgentTask(UUID, String, List<OllamaMessage>)` returns `AgentTaskResult` |
| `ContextWindowService` | `@Service` | Assembles ordered message list from conversation history, prepends system prompt, truncates to token limit. | `buildContextWindow(UUID, String)` returns `List<OllamaMessage>` |
| `SystemPromptBuilder` | `@Service` | Builds dynamic system prompt with user context, RAG memory/knowledge injection, token-limited truncation. | `buildSystemPrompt(UUID, String)` |
| `ModelHealthCheckService` | `@Component` | Startup Ollama availability check. Logs warning if Ollama is unreachable. | `checkOllamaHealth()` -- `@PostConstruct` |

### 6.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `Conversation` | `@Entity` | `conversations` | `@ManyToOne` to `User`. Fields: `id` (UUID), `user`, `title`, `isArchived`, `messageCount`, `createdAt`, `updatedAt`. |
| `Message` | `@Entity` | `messages` | `@ManyToOne` to `Conversation`. Fields: `id` (UUID), `conversation`, `role` (enum), `content` (TEXT), `tokenCount`, `hasRagContext`, `createdAt`. |
| `MessageRole` | `enum` | -- | `USER`, `ASSISTANT`, `SYSTEM` |

### 6.4 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `ConversationRepository` | `JpaRepository<Conversation, UUID>` | `findByUserIdOrderByUpdatedAtDesc(UUID, Pageable)`, `countByUserId`, `findByUserId`, `deleteByUserId` |
| `MessageRepository` | `JpaRepository<Message, UUID>` | `findByConversationIdOrderByCreatedAtAsc`, `countByConversationId`, `deleteByConversationId`, `countByUserId` (JPQL), `deleteByUserId` (JPQL) |

### 6.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `ConversationDto` | Full conversation with `id`, `title`, `isArchived`, `messageCount`, timestamps |
| `ConversationSummaryDto` | Lightweight conversation list item |
| `MessageDto` | Message with `id`, `role`, `content`, `tokenCount`, `hasRagContext`, `createdAt` |
| `SendMessageRequest` | `content` (validated, max 32000 chars), `stream` (boolean) |
| `CreateConversationRequest` | `title` (optional) |
| `OllamaChatRequest` | `model`, `messages`, `stream`, `options` (nullable) |
| `OllamaChatResponse` | `model`, `message`, `done`, `totalDuration`, `evalCount` |
| `OllamaChatChunk` | SSE streaming chunk: `model`, `message`, `done` |
| `OllamaMessage` | `role`, `content` |
| `OllamaModelInfo` | `name`, `size`, `modifiedAt`, `digest` |
| `ActiveModelDto` | Currently configured model name and status |
| `AgentTaskResult` | `success`, `result`, `iterations`, `toolCalls` |
| `OllamaHealthDto` | `available`, `model`, `baseUrl` |

---

## 7. Phase 3 -- Memory & RAG (`memory`)

**Package:** `com.myoffgridai.memory` -- 17 files

### 7.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `MemoryController` | `@RestController` | `/api/memory` | `GET /` (paginated, filters by importance/tag), `GET /{id}`, `DELETE /{id}`, `PUT /{id}/importance`, `PUT /{id}/tags`, `POST /search` (semantic), `GET /export` | Full memory CRUD with semantic search. Export returns all memories as JSON. |

### 7.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `MemoryService` | `@Service` | Memory CRUD with vector embeddings. Creates paired VectorDocument entries for each memory. Similarity search via pgvector. Tracks access (lastAccessedAt, accessCount). | `createMemory(UUID, String, MemoryImportance, List<String>, UUID)`, `getMemory(UUID, UUID)`, `deleteMemory(UUID, UUID)`, `updateImportance(UUID, UUID, MemoryImportance)`, `updateTags(UUID, UUID, List<String>)`, `searchMemories(UUID, String, int)`, `listMemories(UUID, MemoryImportance, String, Pageable)`, `exportMemories(UUID)`, `deleteAllMemoriesForUser(UUID)` |
| `EmbeddingService` | `@Service` | Sole embedding entry point. Delegates to `OllamaService`. Provides cosine similarity and pgvector string formatting. | `embed(String)`, `embedBatch(List<String>)`, `cosineSimilarity(float[], float[])`, `toVectorString(float[])` |
| `RagService` | `@Service` | RAG pipeline coordinator. Assembles context from memories + knowledge chunks with token estimation. | `buildRagContext(UUID, String)` returns `RagContext` |
| `MemoryExtractionService` | `@Service` | `@Async` extraction of facts from chat exchanges via Ollama. Parses JSON array of facts, stores as memories with appropriate importance. | `extractMemories(UUID, String, String, UUID)` -- async |
| `SummarizationService` | `@Service` | Conversation summarization to CRITICAL memories. Nightly `@Scheduled` job (`0 0 2 * * *`) for auto-summarization of stale conversations. | `summarizeConversation(UUID)`, `runNightlySummarization()` -- scheduled at 2am |

### 7.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `Memory` | `@Entity` | `memories` | Fields: `id` (UUID), `userId`, `content` (TEXT), `importance` (enum), `tags` (List<String>), `sourceConversationId`, `createdAt`, `updatedAt`, `lastAccessedAt`, `accessCount`. |
| `VectorDocument` | `@Entity` | `vector_document` | Fields: `id` (UUID), `userId`, `content` (TEXT), `embedding` (vector(768) via VectorType), `sourceType` (enum), `sourceId`, `metadata`, `createdAt`. |
| `MemoryImportance` | `enum` | -- | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `VectorSourceType` | `enum` | -- | `MEMORY`, `CONVERSATION`, `KNOWLEDGE_CHUNK` |

### 7.4 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `MemoryRepository` | `JpaRepository<Memory, UUID>` | `findByUserIdOrderByCreatedAtDesc(Pageable)`, `findByUserIdAndImportance(Pageable)`, `findByIdAndUserId`, `findByUserId`, `countByUserId`, `deleteByUserId` |
| `VectorDocumentRepository` | `JpaRepository<VectorDocument, UUID>` | `findSimilar(UUID, String, float, int)` -- native SQL with pgvector `<=>` cosine distance, `findSimilarBySourceType(...)`, `deleteBySourceIdAndSourceType`, `deleteByUserId` |

### 7.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `MemoryDto` | Memory with all fields including importance, tags, access tracking |
| `RagContext` | Assembled RAG context: `memoryContext`, `knowledgeContext`, `totalTokens` |
| `MemorySearchRequest` | `query`, `topK` (default 5) |
| `MemorySearchResultDto` | `memoryId`, `content`, `importance`, `similarity` |
| `UpdateImportanceRequest` | `importance` (enum) |
| `UpdateTagsRequest` | `tags` (List<String>) |

---

## 8. Phase 4 -- Knowledge Vault (`knowledge`)

**Package:** `com.myoffgridai.knowledge` -- 16 files

### 8.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `KnowledgeController` | `@RestController` | `/api/knowledge` | `POST /` (multipart upload), `GET /` (paginated), `GET /{id}`, `PUT /{id}/display-name`, `DELETE /{id}`, `POST /{id}/retry`, `POST /search` (semantic), `GET /{id}/chunks` | Document lifecycle management with semantic search. Upload triggers async ingestion pipeline. |

### 8.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `KnowledgeService` | `@Service` | Orchestrates document lifecycle: upload, async ingestion (extract -> chunk -> embed -> store vectors), retry for failed docs, bulk deletion for privacy wipe. | `upload(UUID, MultipartFile)`, `processDocumentAsync(UUID)` (`@Async`), `listDocuments(UUID, Pageable)`, `getDocument(UUID, UUID)`, `updateDisplayName(UUID, UUID, String)`, `deleteDocument(UUID, UUID)`, `retryProcessing(UUID, UUID)`, `getChunks(UUID, UUID)`, `deleteAllForUser(UUID)` |
| `IngestionService` | `@Service` | Text extraction. PDF via Apache PDFBox (page-by-page), plain text via InputStream. | `extractPdf(InputStream)` returns `ExtractionResult`, `extractText(InputStream)` returns `ExtractionResult` |
| `ChunkingService` | `@Service` | Sentence-boundary splitting with configurable size (1500 chars) and overlap (150 chars). Min chunk 50 chars, max 500 chunks per document. | `chunkText(String)` returns `List<String>` |
| `SemanticSearchService` | `@Service` | Vector similarity search across knowledge chunks with source document attribution. | `search(UUID, String, int)` returns `List<SemanticSearchResult>` |
| `FileStorageService` | `@Service` | Local filesystem storage under `/var/myoffgridai/knowledge/{userId}/`. | `store(UUID, MultipartFile, String)`, `getInputStream(String)`, `delete(String)` |
| `OcrService` | `@Service` | Tesseract OCR for image files (PNG, JPEG, TIFF, WebP). | `extractFromImage(InputStream)` returns `ExtractionResult` |
| `StorageHealthService` | `@Component` | Startup check for storage directory existence and writeability. | `checkStorageHealth()` -- `@PostConstruct` |

### 8.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `KnowledgeDocument` | `@Entity` | `knowledge_documents` | Fields: `id` (UUID), `userId`, `filename`, `displayName`, `mimeType`, `storagePath`, `fileSizeBytes`, `status` (enum), `errorMessage`, `chunkCount`, `uploadedAt`, `processedAt`. |
| `KnowledgeChunk` | `@Entity` | `knowledge_chunks` | `@ManyToOne` to `KnowledgeDocument`. Fields: `id` (UUID), `document`, `userId`, `chunkIndex`, `content` (TEXT), `pageNumber`. |
| `DocumentStatus` | `enum` | -- | `PENDING`, `PROCESSING`, `READY`, `FAILED` |

### 8.4 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `KnowledgeDocumentRepository` | `JpaRepository<KnowledgeDocument, UUID>` | `findByUserIdOrderByUploadedAtDesc(UUID, Pageable)`, `findByIdAndUserId`, `countByUserId`, `deleteByUserId` |
| `KnowledgeChunkRepository` | `JpaRepository<KnowledgeChunk, UUID>` | `findByDocumentIdOrderByChunkIndexAsc`, `deleteByDocumentId`, `deleteByUserId` |

### 8.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `KnowledgeDocumentDto` | Document with `id`, `filename`, `displayName`, `mimeType`, `fileSizeBytes`, `status`, `errorMessage`, `chunkCount`, timestamps |
| `KnowledgeSearchRequest` | `query`, `topK` (default 5) |
| `KnowledgeSearchResultDto` | `chunkId`, `documentId`, `filename`, `content`, `similarity`, `pageNumber` |
| `ExtractionResult` | `fullText`, `pages` (List<PageContent>) |
| `PageContent` | `pageNumber`, `content` |
| `SemanticSearchResult` | `vectorDocumentId`, `content`, `similarity`, `sourceId`, `metadata` |
| `UpdateDisplayNameRequest` | `displayName` (validated) |

### 8.6 Supported MIME Types

`application/pdf`, `text/plain`, `text/markdown`, `text/x-markdown`, `image/png`, `image/jpeg`, `image/tiff`, `image/webp`

---

## 9. Phase 5 -- Skills & Automation (`skills`)

**Package:** `com.myoffgridai.skills` -- 27 files

### 9.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `SkillController` | `@RestController` | `/api/skills` | `GET /` (list enabled), `GET /{id}`, `PATCH /{id}/toggle` (OWNER/ADMIN), `POST /execute`, `GET /executions` (history). Inventory sub-routes: `GET /inventory`, `POST /inventory`, `PUT /inventory/{id}`, `DELETE /inventory/{id}` | Skill management + inventory CRUD. Toggle enables/disables skills. Execute dispatches to built-in skill implementations. |

### 9.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `SkillExecutorService` | `@Service` | Central dispatcher. Resolves `BuiltInSkill` implementation by name, executes, records execution history in `SkillExecution`. | `executeSkill(UUID, SkillExecuteRequest)` returns `SkillExecutionDto` |
| `BuiltInSkill` | `interface` | Contract for built-in skill implementations. | `getSkillName()`, `execute(UUID, Map<String, Object>)` returns `String` |
| `SkillSeederService` | `@Component` | Seeds 6 built-in skills on startup if not already present. | `seedSkills()` -- `@PostConstruct` |

### 9.3 Built-In Skills (6 files)

| File | Skill Name | Category | Dependencies | Description |
|---|---|---|---|---|
| `WeatherQuerySkill` | `weather-query` | WEATHER | `SensorRepository`, `SensorReadingRepository` | Reads latest sensor data to report current conditions. Pure data retrieval, no LLM call. |
| `InventoryTrackerSkill` | `inventory-tracker` | HOMESTEAD | `InventoryItemRepository` | CRUD operations on inventory items. Parses action from parameters (list, add, remove, update). |
| `RecipeGeneratorSkill` | `recipe-generator` | HOMESTEAD | `OllamaService`, `InventoryItemRepository` | Uses Ollama to generate recipes based on available inventory items. |
| `TaskPlannerSkill` | `task-planner` | PLANNING | `OllamaService`, `PlannedTaskRepository` | Uses Ollama to break goals into actionable tasks. Persists `PlannedTask` entities. |
| `DocumentSummarizerSkill` | `document-summarizer` | KNOWLEDGE | `OllamaService`, `KnowledgeChunkRepository` | Uses Ollama + knowledge chunks to summarize uploaded documents. |
| `ResourceCalculatorSkill` | `resource-calculator` | RESOURCE | None | Pure math calculations for resource planning (water, solar, battery, food). |

### 9.4 Models

| File | Type | Table | Description |
|---|---|---|---|
| `Skill` | `@Entity` | `skills` | Fields: `id` (UUID), `name` (unique), `displayName`, `description`, `category` (enum), `isEnabled`, `isBuiltIn`, `createdAt`. |
| `SkillExecution` | `@Entity` | `skill_executions` | `@ManyToOne` to `Skill`. Fields: `id` (UUID), `skill`, `userId`, `parameters` (TEXT/JSON), `result` (TEXT), `status` (enum), `durationMs`, `createdAt`. |
| `InventoryItem` | `@Entity` | `inventory_items` | Fields: `id` (UUID), `userId`, `name`, `description`, `category` (enum), `quantity`, `unit`, `lowStockThreshold`, `location`, `expiresAt`, `createdAt`, `updatedAt`. |
| `PlannedTask` | `@Entity` | `planned_tasks` | Fields: `id` (UUID), `userId`, `title`, `description`, `status` (enum), `dueDate`, `createdAt`, `updatedAt`. |

### 9.5 Enums

| File | Values |
|---|---|
| `SkillCategory` | `HOMESTEAD`, `RESOURCE`, `PLANNING`, `KNOWLEDGE`, `WEATHER`, `CUSTOM` |
| `InventoryCategory` | `FOOD`, `TOOLS`, `MEDICAL`, `SUPPLIES`, `SEEDS`, `EQUIPMENT`, `OTHER` |
| `ExecutionStatus` | `RUNNING`, `COMPLETED`, `FAILED` |
| `TaskStatus` | `ACTIVE`, `COMPLETED`, `CANCELLED` |

### 9.6 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `SkillRepository` | `JpaRepository<Skill, UUID>` | `findByName`, `findByIsEnabledTrue` |
| `SkillExecutionRepository` | `JpaRepository<SkillExecution, UUID>` | `findByUserIdOrderByCreatedAtDesc(UUID, Pageable)`, `findBySkillIdAndUserId` |
| `InventoryItemRepository` | `JpaRepository<InventoryItem, UUID>` | `findByUserIdOrderByNameAsc`, `findByIdAndUserId`, `findByUserIdAndCategory`, `deleteByUserId` |
| `PlannedTaskRepository` | `JpaRepository<PlannedTask, UUID>` | `findByUserIdAndStatusOrderByCreatedAtDesc(UUID, TaskStatus, Pageable)`, `findByIdAndUserId`, `deleteByUserId` |

### 9.7 DTOs (all Java records)

| File | Purpose |
|---|---|
| `SkillDto` | Skill with `id`, `name`, `displayName`, `description`, `category`, `isEnabled`, `isBuiltIn` |
| `SkillExecuteRequest` | `skillName`, `parameters` (Map<String, Object>) |
| `SkillExecutionDto` | Execution with `id`, `skillName`, `status`, `result`, `durationMs`, `createdAt` |
| `InventoryItemDto` | Full inventory item with all fields |
| `CreateInventoryItemRequest` | `name`, `description`, `category`, `quantity`, `unit`, `lowStockThreshold`, `location`, `expiresAt` |
| `UpdateInventoryItemRequest` | Same fields as create (for PUT updates) |

---

## 10. Phase 6 -- Sensors (`sensors`)

**Package:** `com.myoffgridai.sensors` -- 18 files

### 10.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `SensorController` | `@RestController` | `/api/sensors` | `GET /` (list all), `GET /{sensorId}`, `POST /` (register), `DELETE /{sensorId}`, `POST /{sensorId}/start`, `POST /{sensorId}/stop`, `GET /{sensorId}/latest`, `GET /{sensorId}/history` (paginated, hours param), `PUT /{sensorId}/thresholds`, `POST /test`, `GET /ports`, `GET /{sensorId}/stream` (SSE) | Full sensor lifecycle: CRUD, start/stop polling, reading history, threshold management, connection testing, live SSE streaming. |

### 10.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `SensorService` | `@Service` | High-level sensor management. Coordinates repos, polling service, serial port service. Validates ownership on all operations. | `registerSensor(UUID, CreateSensorRequest)`, `getSensor(UUID, UUID)`, `listSensors(UUID)`, `deleteSensor(UUID, UUID)`, `startSensor(UUID, UUID)`, `stopSensor(UUID, UUID)`, `getLatestReading(UUID, UUID)`, `getReadingHistory(UUID, UUID, int, int, int)`, `updateThresholds(UUID, UUID, UpdateThresholdsRequest)`, `testSensor(String, int)`, `listAvailablePorts()`, `deleteAllForUser(UUID)` |
| `SensorPollingService` | `@Service` | Manages active polling loops per sensor with configurable intervals. Uses `ScheduledExecutorService`. Parses serial data (CSV/JSON), checks thresholds, creates memory entries for anomalies, broadcasts via SSE. | `startPolling(Sensor)`, `stopPolling(UUID)`, `isPolling(UUID)` |
| `SensorStartupService` | `@Component` | Resumes active sensors on application startup. | `resumeActiveSensors()` -- `@PostConstruct` |
| `SerialPortService` | `@Service` | Physical serial port management via jSerialComm. Opens/closes ports, reads data lines, lists available ports. | `openPort(String, int)`, `closePort(String)`, `readLine(String)`, `listPorts()`, `testPort(String, int)` |
| `SseEmitterRegistry` | `@Component` | Manages SSE connections for live sensor reading broadcast. Keyed by sensorId. Handles completion/timeout/error cleanup. | `register(UUID, SseEmitter)`, `broadcast(UUID, SensorReading)` |

### 10.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `Sensor` | `@Entity` | `sensors` | Fields: `id` (UUID), `userId`, `name`, `type` (enum), `portPath`, `baudRate` (default 9600), `dataFormat` (enum), `isActive`, `pollIntervalSeconds`, `minThreshold`, `maxThreshold`, `consecutiveFailures`, `lastReadingAt`, `createdAt`, `updatedAt`. |
| `SensorReading` | `@Entity` | `sensor_readings` | `@ManyToOne` to `Sensor`. Fields: `id` (UUID), `sensor`, `value` (Double), `rawData`, `recordedAt`. |
| `SensorType` | `enum` | -- | `TEMPERATURE`, `HUMIDITY`, `SOIL_MOISTURE`, `POWER`, `VOLTAGE`, `CUSTOM` |
| `DataFormat` | `enum` | -- | `CSV_LINE`, `JSON_LINE` |

### 10.4 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `SensorRepository` | `JpaRepository<Sensor, UUID>` | `findByUserIdOrderByNameAsc`, `findByIdAndUserId`, `findByIsActiveTrue`, `countByUserId` |
| `SensorReadingRepository` | `JpaRepository<SensorReading, UUID>` | `findBySensorIdAndRecordedAtAfterOrderByRecordedAtDesc(UUID, Instant, Pageable)`, `findTopBySensorIdOrderByRecordedAtDesc`, `findAverageValueSince(UUID, Instant)` (native query), `deleteBySensorId` |

### 10.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `CreateSensorRequest` | `name`, `type`, `portPath`, `baudRate`, `dataFormat`, `pollIntervalSeconds` (validated min/max) |
| `SensorDto` | Full sensor with all fields including thresholds and status |
| `SensorReadingDto` | `id`, `sensorId`, `value`, `rawData`, `recordedAt` |
| `SensorTestResult` | `success`, `portPath`, `baudRate`, `sampleData`, `errorMessage` |
| `TestSensorRequest` | `portPath` (required), `baudRate` (optional, default 9600) |
| `UpdateThresholdsRequest` | `minThreshold`, `maxThreshold` |

---

## 11. Phase 7 -- Proactive Engine (`proactive`)

**Package:** `com.myoffgridai.proactive` -- 16 files

### 11.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `ProactiveController` | `@RestController` | `/api/insights` + `/api/notifications` | **Insights:** `GET /api/insights` (paginated, optional category filter), `POST /api/insights/generate`, `PUT /api/insights/{insightId}/read`, `PUT /api/insights/{insightId}/dismiss`, `GET /api/insights/unread-count`. **Notifications:** `GET /api/notifications` (paginated, unreadOnly filter), `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`, `GET /api/notifications/unread-count`, `DELETE /api/notifications/{id}`, `GET /api/notifications/stream` (SSE) | Dual-resource controller. Insight management (LLM-generated recommendations) + notification management with real-time SSE push. |

### 11.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `InsightService` | `@Service` | Insight CRUD. Queries active (non-dismissed) insights with pagination and category filtering. | `getInsights(UUID, Pageable)`, `getInsightsByCategory(UUID, InsightCategory, Pageable)`, `getUnreadInsights(UUID)`, `markRead(UUID, UUID)`, `dismiss(UUID, UUID)`, `getUnreadCount(UUID)`, `deleteAllForUser(UUID)` |
| `InsightGeneratorService` | `@Service` | Generates proactive insights by analyzing user activity via `PatternAnalysisService` and prompting Ollama for actionable recommendations. Parses JSON array responses. Max 3 insights per generation. | `generateInsightForUser(UUID)` returns `List<Insight>` |
| `NightlyInsightJob` | `@Component` | Nightly scheduled job (`0 0 3 * * *`) generating insights for all active users. Each user processed independently -- one failure does not stop others. | `generateNightlyInsights()` -- `@Scheduled` at 3am |
| `NotificationService` | `@Service` | Central notification hub. All services call this to create, persist, and broadcast notifications via SSE. | `createNotification(UUID, String, String, NotificationType, String)`, `getUnreadNotifications(UUID)`, `getNotifications(UUID, Pageable)`, `markRead(UUID, UUID)`, `markAllRead(UUID)`, `getUnreadCount(UUID)`, `deleteNotification(UUID, UUID)`, `deleteAllForUser(UUID)` |
| `NotificationSseRegistry` | `@Component` | Manages SSE connections for real-time notification push. Keyed by userId (multiple clients per user supported). Broadcasts notification data and unread counts. | `register(UUID, SseEmitter)`, `broadcast(UUID, Notification)`, `broadcastUnreadCount(UUID, long)` |
| `PatternAnalysisService` | `@Service` | Analyzes a user's recent activity to build a `PatternSummary`. Aggregates data from conversations, memories, sensors, inventory, and tasks. | `buildPatternSummary(UUID)` returns `PatternSummary` |
| `SystemHealthMonitor` | `@Component` | Periodic health checks (configurable interval, default 5 minutes). Monitors disk space, Ollama availability, JVM heap usage. Rate-limits alerts (60-minute cooldown). Notifies OWNER/ADMIN users. | `checkSystemHealth()` -- `@Scheduled(fixedDelay)`. Internal: `checkDiskSpace()`, `checkOllamaAvailability()`, `checkHeapUsage()` |

### 11.3 Models

| File | Type | Table | Description |
|---|---|---|---|
| `Insight` | `@Entity` | `insights` | Fields: `id` (UUID), `userId`, `content` (TEXT), `category` (enum), `isRead`, `isDismissed`, `generatedAt`, `readAt`. Index on `user_id`. |
| `Notification` | `@Entity` | `notifications` | Fields: `id` (UUID), `userId`, `title`, `body` (TEXT), `type` (enum), `isRead`, `createdAt`, `readAt`, `metadata` (TEXT/JSON). Index on `user_id`. |
| `InsightCategory` | `enum` | -- | `HOMESTEAD`, `HEALTH`, `RESOURCE`, `GENERAL` |
| `NotificationType` | `enum` | -- | `SENSOR_ALERT`, `SYSTEM_HEALTH`, `INSIGHT_READY`, `MODEL_UPDATE`, `GENERAL` |

### 11.4 Repositories

| File | Type | Key Query Methods |
|---|---|---|
| `InsightRepository` | `JpaRepository<Insight, UUID>` | `findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(Pageable)`, `findByUserIdAndCategoryAndIsDismissedFalse(Pageable)`, `findByUserIdAndIsReadFalseAndIsDismissedFalse`, `countByUserIdAndIsReadFalseAndIsDismissedFalse`, `findByIdAndUserId`, `countByUserId`, `deleteByUserId` |
| `NotificationRepository` | `JpaRepository<Notification, UUID>` | `findByUserIdAndIsReadFalseOrderByCreatedAtDesc`, `findByUserIdOrderByCreatedAtDesc(Pageable)`, `countByUserIdAndIsReadFalse`, `findByIdAndUserId`, `markAllReadForUser(JPQL UPDATE)`, `deleteByUserId` |

### 11.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `InsightDto` | Insight with `id`, `content`, `category`, `isRead`, `isDismissed`, `generatedAt`, `readAt`. Static factory `from(Insight)`. |
| `NotificationDto` | Notification with `id`, `title`, `body`, `type`, `isRead`, `createdAt`, `readAt`, `metadata`. Static factory `from(Notification)`. |
| `PatternSummary` | Activity summary: `recentConversationCount`, `recentConversationTitles`, `highImportanceMemories`, `sensorAverages`, `lowStockItems`, `activeTasks`, `analysisWindowDays`. Method `hasData()` checks for meaningful content. |

---

## 12. Phase 8 -- Privacy & Fortress (`privacy`) + System (`system`)

### 12.1 Privacy Package

**Package:** `com.myoffgridai.privacy` -- 17 files

#### 12.1.1 Controller

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `PrivacyController` | `@RestController` | `/api/privacy` | **Fortress:** `GET /fortress/status`, `POST /fortress/enable` (OWNER/ADMIN), `POST /fortress/disable` (OWNER/ADMIN). **Sovereignty:** `GET /sovereignty-report`. **Audit:** `GET /audit-logs` (paginated, outcome filter; OWNER/ADMIN see all, others see own). **Export:** `POST /export` (AES-256-GCM encrypted ZIP). **Wipe:** `DELETE /wipe` (OWNER/ADMIN, target user param), `DELETE /wipe/self` (any authenticated user). | Comprehensive privacy management. Fortress controls OS-level network lockdown. Export encrypts all user data. Wipe cascade-deletes all user-owned records. |

#### 12.1.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `FortressService` | `@Service` | Controls Privacy Fortress -- OS-level network lockdown via iptables. In mock mode (`app.fortress.mock=true`), no iptables commands are executed. Persists state via `SystemConfigService`. | `enable(UUID)`, `disable(UUID)`, `getFortressStatus()` returns `FortressStatus`, `isFortressActive()` |
| `AuditService` | `@Service` | Audit log persistence and queries. All audit logging flows through this service. | `logAction(AuditLog)`, `getAuditLogs(Pageable)`, `getAuditLogsForUser(UUID, Pageable)`, `getAuditLogsByOutcome(AuditOutcome, Pageable)`, `getAuditLogsBetween(Instant, Instant, Pageable)`, `countByOutcomeBetween(AuditOutcome, Instant, Instant)`, `deleteByUserId(UUID)` |
| `DataExportService` | `@Service` | Exports all user data as AES-256-GCM encrypted ZIP. Includes conversations, messages (grouped by conversation), memories, and export metadata. PBKDF2 key derivation (65536 iterations). Output: `[salt 16B][IV 12B][ciphertext]`. | `exportUserData(UUID, String)` returns `byte[]` |
| `DataWipeService` | `@Service` | Complete data wipe in FK-respecting order (10 steps): messages, conversations, memories+vectors, knowledge docs/chunks/vectors/files, sensors+readings, insights, notifications, inventory items, planned tasks, audit logs. Atomic transaction. | `wipeUser(UUID)` returns `WipeResult` |
| `SovereigntyReportService` | `@Service` | Assembles Sovereignty Report: fortress status, outbound traffic verification, data inventory (record counts), audit summary (24h window), encryption status, telemetry status (always DISABLED). | `generateReport(UUID)` returns `SovereigntyReport` |

#### 12.1.3 AOP Aspect

| File | Type | Description |
|---|---|---|
| `AuditAspect` | `@Aspect @Component` | Package: `com.myoffgridai.privacy.aspect`. Intercepts all public methods in `*.controller.*` classes via `@Around` pointcut: `execution(public * com.myoffgridai.*.controller..*(..))`. Captures HTTP method, path, user, response status, duration, outcome. Never logs request body, passwords, tokens, or sensitive headers. |

#### 12.1.4 Models

| File | Type | Table | Description |
|---|---|---|---|
| `AuditLog` | `@Entity` | `audit_logs` | Fields: `id` (UUID), `userId`, `username`, `action`, `resourceType`, `resourceId`, `httpMethod`, `requestPath`, `ipAddress`, `userAgent`, `responseStatus`, `outcome` (enum), `durationMs`, `timestamp`. Indexes on `(user_id, timestamp DESC)` and `(timestamp DESC)`. |
| `AuditOutcome` | `enum` | -- | `SUCCESS`, `FAILURE`, `DENIED` |

#### 12.1.5 Repository

| File | Type | Key Query Methods |
|---|---|---|
| `AuditLogRepository` | `JpaRepository<AuditLog, UUID>` | `findAllByOrderByTimestampDesc(Pageable)`, `findByUserIdOrderByTimestampDesc(Pageable)`, `findByOutcomeOrderByTimestampDesc(Pageable)`, `findByTimestampBetweenOrderByTimestampDesc(Pageable)`, `countByOutcomeAndTimestampBetween`, `deleteByTimestampBefore`, `deleteByUserId` |

#### 12.1.6 DTOs (all Java records)

| File | Purpose |
|---|---|
| `AuditLogDto` | Audit log entry with all fields. Static factory `from(AuditLog)`. |
| `AuditSummary` | `successCount`, `failureCount`, `deniedCount`, `windowStart`, `windowEnd` |
| `DataInventory` | Record counts: `conversationCount`, `messageCount`, `memoryCount`, `knowledgeDocumentCount`, `sensorCount`, `insightCount` |
| `ExportRequest` | `passphrase` (validated: `@NotBlank`, `@Size(min=8)`) |
| `FortressStatus` | `enabled`, `enabledAt`, `enabledByUsername`, `verified` |
| `SovereigntyReport` | `generatedAt`, `fortressStatus`, `outboundTrafficVerification`, `dataInventory`, `auditSummary`, `encryptionStatus`, `telemetryStatus`, `lastVerifiedAt` |
| `WipeResult` | `targetUserId`, `stepsCompleted`, `completedAt`, `success` |

### 12.2 System Package

**Package:** `com.myoffgridai.system` -- 16 files

#### 12.2.1 Controllers

| File | Type | Base Path | Endpoints | Description |
|---|---|---|---|---|
| `SystemController` | `@RestController` | `/api/system` | `GET /status` (public), `POST /initialize` (public, one-time), `POST /finalize-setup` (public), `POST /factory-reset` (OWNER only) | First-boot setup, network finalization, and factory reset. Status endpoint returns initialization state, fortress mode, server version. Initialize creates the OWNER account and marks system as initialized. Returns 409 on subsequent calls. Finalize-setup transitions from AP mode to normal networking. Factory-reset wipes all data and restores first-boot state. |
| `CaptivePortalController` | `@Controller` | `/setup`, `/api/setup` | `GET /setup` (HTML forward/redirect), `GET /api/setup/wifi/scan`, `POST /api/setup/wifi/connect`, `GET /api/setup/wifi/status` | Captive portal setup wizard. Forwards to setup HTML pages when uninitialized, redirects to `/` when initialized. WiFi JSON API for scanning, connecting, and status checks. |

#### 12.2.2 Services

| File | Type | Description | Key Public Methods |
|---|---|---|---|
| `SystemConfigService` | `@Service` | Manages single-row system configuration. Creates default config if none exists. | `getConfig()`, `save(SystemConfig)`, `isInitialized()`, `setInitialized(String)`, `setFortressEnabled(boolean, UUID)`, `isWifiConfigured()` |
| `ApModeService` | `@Service` | Controls hostapd/dnsmasq AP mode and WiFi via nmcli. In mock mode (`app.ap.mock=true`), no system commands are executed. ProcessBuilder pattern matches FortressService. | `startApMode()`, `stopApMode()`, `isApModeActive()`, `scanWifiNetworks()`, `connectToWifi(String, String)`, `getConnectionStatus()` |
| `ApModeStartupService` | `@Component` | Checks if system is initialized on boot. If not, starts AP mode for captive portal setup. Listens for `ApplicationReadyEvent`. | `onApplicationReady(ApplicationReadyEvent)` |
| `NetworkTransitionService` | `@Service` | Handles transition from AP mode to normal WiFi networking. Stops AP mode, starts avahi-daemon for mDNS, marks wifi as configured. | `finalizeSetup()` -- `@Async` |
| `FactoryResetService` | `@Service` | Performs full factory reset: wipes all users and system config, starts AP mode. Supports USB-triggered resets (deletes trigger file after reset). | `performReset()` -- `@Async`, `performUsbReset(Path)` -- `@Async` |
| `UsbResetWatcherService` | `@Component` | Polls USB mount path every 30 seconds for factory reset trigger files (`myoffgridai-reset.txt`) and update ZIPs. | `checkForTriggerFiles()` -- `@Scheduled(fixedDelay=30000)` |

#### 12.2.3 Model

| File | Type | Table | Description |
|---|---|---|---|
| `SystemConfig` | `@Entity` | `system_config` | Single-row table. Fields: `id` (UUID), `initialized` (boolean), `instanceName`, `fortressEnabled` (boolean), `fortressEnabledAt`, `fortressEnabledByUserId`, `wifiConfigured`, `apModeEnabled` (boolean), `createdAt`, `updatedAt`. |

#### 12.2.4 Repository

| File | Type | Key Query Methods |
|---|---|---|
| `SystemConfigRepository` | `JpaRepository<SystemConfig, UUID>` | `findFirst()` -- JPQL `SELECT s FROM SystemConfig s` returning `Optional<SystemConfig>` |

#### 12.2.5 DTOs (all Java records)

| File | Purpose |
|---|---|
| `SystemStatusDto` | `initialized`, `instanceName`, `fortressEnabled`, `wifiConfigured`, `serverVersion`, `timestamp` |
| `InitializeRequest` | `instanceName` (required, 1-100 chars), `username` (required, 3-50 chars), `displayName` (required), `email` (optional), `password` (required) |
| `FactoryResetRequest` | `confirmPhrase` (required, must match "RESET MY DEVICE") |
| `WifiNetwork` | `ssid`, `signalStrength` (int), `security` (String) |
| `WifiConnectRequest` | `ssid` (required), `password` (optional) |
| `WifiConnectionStatus` | `connected` (boolean), `hasInternet` (boolean) |

---

## 13. Phase 11 -- Captive Portal & Setup Wizard (`system`)

Phase 11 extends the `system` package with captive portal, setup wizard, AP mode management, factory reset, and USB reset watcher functionality. Added 10 main source files and 6 test files to the system package, plus 1 config file and 1 exception class.

### 13.1 New Files Added in Phase 11

| File | Type | Package | Description |
|---|---|---|---|
| `CaptivePortalController` | `@Controller` | system.controller | Setup wizard HTML routes + WiFi JSON API |
| `ApModeService` | `@Service` | system.service | hostapd/dnsmasq/nmcli wrapper with mock mode |
| `ApModeStartupService` | `@Component` | system.service | AP mode on boot if system uninitialized |
| `NetworkTransitionService` | `@Service` | system.service | AP mode → normal WiFi transition |
| `FactoryResetService` | `@Service` | system.service | Full factory reset with data wipe |
| `UsbResetWatcherService` | `@Component` | system.service | Polls USB for reset trigger files |
| `FactoryResetRequest` | record | system.dto | Confirmation phrase DTO |
| `WifiNetwork` | record | system.dto | WiFi scan result DTO |
| `WifiConnectRequest` | record | system.dto | WiFi connection request DTO |
| `WifiConnectionStatus` | record | system.dto | WiFi status response DTO |
| `CaptivePortalRedirectFilter` | `OncePerRequestFilter` | config | Redirects to /setup in AP mode |
| `ApModeException` | `RuntimeException` | common.exception | AP mode operation failures |

### 13.2 Static Web Assets (Setup Wizard)

| File | Path | Description |
|---|---|---|
| `index.html` | `static/setup/` | Step 1: Welcome page |
| `wifi.html` | `static/setup/` | Step 2: WiFi network scanner/connector |
| `account.html` | `static/setup/` | Step 3: Owner account creation form |
| `confirm.html` | `static/setup/` | Step 4: Confirmation + finalize-setup call |
| `setup.css` | `static/setup/` | Mobile-first CSS, MyOffGridAI branding |
| `setup.js` | `static/setup/` | Shared JS utilities for API calls |

---

## 14. Test Suite

**Total Test Files:** 89
**Total @Test Methods:** 649

### 14.1 Test Files by Domain

| Domain | Test Files | @Test Count |
|---|---|---|
| `auth` (controller + service) | 5 | 67 |
| `ai` (controller + service) | 8 | 59 |
| `common` (exception + response + util) | 3 | 22 |
| `config` (TestSecurityConfig) | 1 | 1 |
| `knowledge` (controller + service) | 7 | 53 |
| `memory` (controller + service) | 6 | 49 |
| `sensors` (controller + service) | 5 | 49 |
| `skills` (builtin + controller + service) | 9 | 81 |
| `proactive` (controller + service) | 8 | 62 |
| `privacy` (aspect + controller + service) | 7 | 43 |
| `system` (controller + service) | 8 | 49 |
| `integration` | 22 | 114 |
| **TOTAL** | **89** | **649** |

### 14.2 Unit Test Files

| File | Domain | Description |
|---|---|---|
| `AuthControllerTest` | auth | Controller @WebMvcTest |
| `UserControllerTest` | auth | Controller @WebMvcTest |
| `AuthServiceTest` | auth | Service unit test |
| `JwtServiceTest` | auth | Service unit test |
| `UserServiceTest` | auth | Service unit test |
| `ChatControllerTest` | ai | Controller @WebMvcTest |
| `ModelControllerTest` | ai | Controller @WebMvcTest |
| `AgentServiceTest` | ai | Service unit test |
| `ChatServiceTest` | ai | Service unit test |
| `ContextWindowServiceTest` | ai | Service unit test |
| `ModelHealthCheckServiceTest` | ai | Service unit test |
| `OllamaServiceTest` | ai | Service unit test |
| `SystemPromptBuilderTest` | ai | Service unit test |
| `GlobalExceptionHandlerTest` | common | Exception handler tests |
| `ApiResponseTest` | common | Response wrapper tests |
| `TokenCounterTest` | common | Utility tests |
| `TestSecurityConfig` | config | Test security configuration |
| `KnowledgeControllerTest` | knowledge | Controller @WebMvcTest |
| `ChunkingServiceTest` | knowledge | Service unit test |
| `FileStorageServiceTest` | knowledge | Service unit test |
| `IngestionServiceTest` | knowledge | Service unit test |
| `KnowledgeServiceTest` | knowledge | Service unit test |
| `OcrServiceTest` | knowledge | Service unit test |
| `SemanticSearchServiceTest` | knowledge | Service unit test |
| `MemoryControllerTest` | memory | Controller @WebMvcTest |
| `EmbeddingServiceTest` | memory | Service unit test |
| `MemoryExtractionServiceTest` | memory | Service unit test |
| `MemoryServiceTest` | memory | Service unit test |
| `RagServiceTest` | memory | Service unit test |
| `SummarizationServiceTest` | memory | Service unit test |
| `SensorControllerTest` | sensors | Controller @WebMvcTest |
| `SensorPollingServiceTest` | sensors | Service unit test |
| `SensorServiceTest` | sensors | Service unit test |
| `SensorStartupServiceTest` | sensors | Service unit test |
| `SseEmitterRegistryTest` | sensors | SSE registry tests |
| `SkillControllerTest` | skills | Controller @WebMvcTest |
| `SkillExecutorServiceTest` | skills | Service unit test |
| `SkillSeederServiceTest` | skills | Service unit test |
| `WeatherQuerySkillTest` | skills | Built-in skill test |
| `InventoryTrackerSkillTest` | skills | Built-in skill test |
| `RecipeGeneratorSkillTest` | skills | Built-in skill test |
| `ResourceCalculatorSkillTest` | skills | Built-in skill test |
| `TaskPlannerSkillTest` | skills | Built-in skill test |
| `DocumentSummarizerSkillTest` | skills | Built-in skill test |
| `ProactiveControllerTest` | proactive | Controller @WebMvcTest |
| `InsightServiceTest` | proactive | Service unit test |
| `InsightGeneratorServiceTest` | proactive | Service unit test |
| `NightlyInsightJobTest` | proactive | Scheduled job test |
| `NotificationServiceTest` | proactive | Service unit test |
| `NotificationSseRegistryTest` | proactive | SSE registry tests |
| `PatternAnalysisServiceTest` | proactive | Service unit test |
| `SystemHealthMonitorTest` | proactive | Health monitor tests |
| `PrivacyControllerTest` | privacy | Controller @WebMvcTest |
| `AuditAspectTest` | privacy | AOP aspect test |
| `AuditServiceTest` | privacy | Service unit test |
| `DataExportServiceTest` | privacy | Service unit test |
| `DataWipeServiceTest` | privacy | Service unit test |
| `FortressServiceTest` | privacy | Service unit test |
| `SovereigntyReportServiceTest` | privacy | Service unit test |
| `SystemControllerTest` | system | Controller @WebMvcTest |
| `CaptivePortalControllerTest` | system | Controller @WebMvcTest |
| `SystemConfigServiceTest` | system | Service unit test |
| `ApModeServiceTest` | system | Service unit test (7 tests) |
| `ApModeStartupServiceTest` | system | Service unit test (3 tests) |
| `NetworkTransitionServiceTest` | system | Service unit test (3 tests) |
| `FactoryResetServiceTest` | system | Service unit test (5 tests) |
| `UsbResetWatcherServiceTest` | system | Service unit test (3 tests) |

### 14.3 Integration Test Files

| File | Domain Coverage | Description |
|---|---|---|
| `BaseIntegrationTest` | All | Abstract base class with Testcontainers PostgreSQL setup |
| `AuthIntegrationTest` | Phase 1 | Registration, login, refresh, logout flows |
| `UserIntegrationTest` | Phase 1 | User CRUD, role-based access |
| `SecurityIntegrationTest` | Phase 1 | JWT validation, public vs protected endpoints |
| `ChatIntegrationTest` | Phase 2 | Conversation creation, messaging |
| `ModelControllerIntegrationTest` | Phase 2 | Model listing, health check |
| `MemoryIntegrationTest` | Phase 3 | Memory CRUD, search |
| `MemoryRagIntegrationTest` | Phase 3 | RAG pipeline with memories |
| `RagPipelineIntegrationTest` | Phase 3+4 | End-to-end RAG with knowledge |
| `RagWithKnowledgeIntegrationTest` | Phase 3+4 | RAG context with knowledge documents |
| `KnowledgeIntegrationTest` | Phase 4 | Document upload, processing |
| `SkillsIntegrationTest` | Phase 5 | Skill execution, toggle |
| `InventoryIntegrationTest` | Phase 5 | Inventory CRUD |
| `SensorIntegrationTest` | Phase 6 | Sensor registration, readings |
| `InsightIntegrationTest` | Phase 7 | Insight generation, read/dismiss |
| `NotificationIntegrationTest` | Phase 7 | Notification CRUD, mark-read |
| `AuditIntegrationTest` | Phase 8 | Audit log capture and query |
| `FortressIntegrationTest` | Phase 8 | Fortress enable/disable/status |
| `DataWipeIntegrationTest` | Phase 8 | Complete data wipe verification |
| `SystemInitializationIntegrationTest` | Phase 8 | First-boot initialization flow |
| `CaptivePortalIntegrationTest` | Phase 11 | Captive portal setup wizard flow |
| `FactoryResetIntegrationTest` | Phase 11 | Factory reset full flow |

---

## 15. Database Entities & Relationships

### 15.1 Entity Summary (17 tables)

| Entity | Table | Owner FK | Other FKs |
|---|---|---|---|
| `User` | `users` | -- | -- |
| `Conversation` | `conversations` | `@ManyToOne User` | -- |
| `Message` | `messages` | -- | `@ManyToOne Conversation` |
| `Memory` | `memories` | `userId` (UUID) | `sourceConversationId` (UUID, nullable) |
| `VectorDocument` | `vector_document` | `userId` (UUID) | `sourceId` (UUID), `sourceType` (enum) |
| `KnowledgeDocument` | `knowledge_documents` | `userId` (UUID) | -- |
| `KnowledgeChunk` | `knowledge_chunks` | `userId` (UUID) | `@ManyToOne KnowledgeDocument` |
| `Skill` | `skills` | -- | -- |
| `SkillExecution` | `skill_executions` | `userId` (UUID) | `@ManyToOne Skill` |
| `InventoryItem` | `inventory_items` | `userId` (UUID) | -- |
| `PlannedTask` | `planned_tasks` | `userId` (UUID) | -- |
| `Sensor` | `sensors` | `userId` (UUID) | -- |
| `SensorReading` | `sensor_readings` | -- | `@ManyToOne Sensor` |
| `Insight` | `insights` | `userId` (UUID) | -- |
| `Notification` | `notifications` | `userId` (UUID) | -- |
| `AuditLog` | `audit_logs` | `userId` (UUID, nullable) | -- |
| `SystemConfig` | `system_config` | -- | `fortressEnabledByUserId` (UUID, nullable) |

### 15.2 Entity Relationship Diagram (Text)

```
User (users)
 |--< Conversation (conversations)          @ManyToOne
 |     |--< Message (messages)              @ManyToOne
 |--< Memory (memories)                     userId FK
 |--< VectorDocument (vector_document)      userId FK
 |--< KnowledgeDocument (knowledge_documents) userId FK
 |     |--< KnowledgeChunk (knowledge_chunks) @ManyToOne
 |--< InventoryItem (inventory_items)       userId FK
 |--< PlannedTask (planned_tasks)           userId FK
 |--< Sensor (sensors)                      userId FK
 |     |--< SensorReading (sensor_readings) @ManyToOne
 |--< Insight (insights)                    userId FK
 |--< Notification (notifications)          userId FK
 |--< AuditLog (audit_logs)                 userId FK (nullable)
 |--< SkillExecution (skill_executions)     userId FK
       |-- Skill (skills)                   @ManyToOne

SystemConfig (system_config)                 Singleton row
```

### 15.3 Vector Search

- **Table:** `vector_document`
- **Column:** `embedding` -- `vector(768)` via pgvector extension
- **Operator:** `<=>` (cosine distance) in native SQL queries
- **Sources:** `MEMORY`, `CONVERSATION`, `KNOWLEDGE_CHUNK` (VectorSourceType enum)
- **Threshold:** 0.7 minimum cosine similarity

---

## 16. API Endpoint Summary

### 16.1 Endpoint Count by Domain

| Domain | Controller | Base Path | Endpoint Count |
|---|---|---|---|
| Auth | `AuthController` | `/api/auth` | 4 |
| Users | `UserController` | `/api/users` | 5 |
| Chat | `ChatController` | `/api/chat` | 7 |
| Models | `ModelController` | `/api/models` | 3 |
| Memory | `MemoryController` | `/api/memory` | 7 |
| Knowledge | `KnowledgeController` | `/api/knowledge` | 8 |
| Skills | `SkillController` | `/api/skills` | 9 |
| Sensors | `SensorController` | `/api/sensors` | 12 |
| Insights | `ProactiveController` | `/api/insights` | 5 |
| Notifications | `ProactiveController` | `/api/notifications` | 6 |
| Privacy | `PrivacyController` | `/api/privacy` | 7 |
| System | `SystemController` | `/api/system` | 4 |
| Setup | `CaptivePortalController` | `/api/setup`, `/setup` | 4 |
| **TOTAL** | **12 controllers** | | **81 endpoints** |

### 16.2 Public Endpoints (No Authentication Required)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/login` | User login |
| `POST` | `/api/auth/register` | User registration |
| `POST` | `/api/auth/refresh` | Token refresh |
| `GET` | `/api/system/status` | System status |
| `POST` | `/api/system/initialize` | First-boot setup |
| `POST` | `/api/system/finalize-setup` | Finalize setup (AP → WiFi transition) |
| `GET` | `/api/setup/wifi/scan` | Scan available WiFi networks |
| `POST` | `/api/setup/wifi/connect` | Connect to WiFi network |
| `GET` | `/api/setup/wifi/status` | WiFi connection status |
| `GET` | `/setup` | Setup wizard HTML page |
| `GET` | `/api/models` | List available models |
| `GET` | `/api/models/health` | Ollama health check |
| `GET` | `/actuator/health` | Spring Actuator health |
| `GET` | `/swagger-ui/**` | Swagger UI |
| `GET` | `/v3/api-docs/**` | OpenAPI spec |

### 16.3 Role-Restricted Endpoints

| Method | Path | Required Role |
|---|---|---|
| `GET` | `/api/users` | OWNER, ADMIN |
| `PUT` | `/api/users/{id}` | OWNER, ADMIN |
| `PUT` | `/api/users/{id}/deactivate` | OWNER |
| `DELETE` | `/api/users/{id}` | OWNER |
| `PATCH` | `/api/skills/{id}/toggle` | OWNER, ADMIN |
| `POST` | `/api/privacy/fortress/enable` | OWNER, ADMIN |
| `POST` | `/api/privacy/fortress/disable` | OWNER, ADMIN |
| `DELETE` | `/api/privacy/wipe` | OWNER, ADMIN |
| `POST` | `/api/system/factory-reset` | OWNER |

---

## 17. Summary Statistics

| Metric | Count |
|---|---|
| **Main Source Files** | 202 |
| **Test Files** | 89 |
| **Total Java Files** | 291 |
| **@Test Methods** | 649 |
| **Controllers** | 12 |
| **Services** | 35 |
| **Entities (JPA)** | 17 |
| **Database Tables** | 17 |
| **Repositories** | 14 |
| **DTOs (records)** | 56+ |
| **Enums** | 13 |
| **Custom Exceptions** | 14 |
| **API Endpoints** | 81 |
| **Built-in Skills** | 6 |
| **Scheduled Jobs** | 4 (summarization 2am, insights 3am, health checks every 5min, USB watcher every 30s) |
| **SSE Streams** | 3 (chat tokens, sensor readings, notifications) |
| **Integration Tests** | 22 (Testcontainers PostgreSQL) |

### File Counts by Domain

| Domain | Files |
|---|---|
| config | 8 |
| common | 17 |
| auth (Phase 1) | 14 |
| ai (Phase 2) | 21 |
| memory (Phase 3) | 17 |
| knowledge (Phase 4) | 16 |
| skills (Phase 5) | 27 |
| sensors (Phase 6) | 18 |
| proactive (Phase 7) | 16 |
| privacy (Phase 8a) | 17 |
| system (Phase 8b + Phase 11) | 16 |
| Entry point | 1 |
| application.yml | 1 |
| **Main total** | **202** (+ 1 yml + 6 static HTML/CSS/JS) |

### Async Operations

| Operation | Triggered By | Service |
|---|---|---|
| Memory extraction | Chat message exchange | `MemoryExtractionService.extractMemories()` |
| Title generation | First user message | `ChatService` (inline @Async via OllamaService) |
| Document ingestion | Knowledge upload | `KnowledgeService.processDocumentAsync()` |
| Factory reset | Owner-initiated or USB trigger | `FactoryResetService.performReset()` / `performUsbReset()` |
| Network finalization | Setup wizard completion | `NetworkTransitionService.finalizeSetup()` |

### Scheduled Tasks

| Schedule | Service | Description |
|---|---|---|
| `0 0 2 * * *` (2am daily) | `SummarizationService` | Summarize stale conversations to CRITICAL memories |
| `0 0 3 * * *` (3am daily) | `NightlyInsightJob` | Generate proactive insights for all active users |
| Configurable interval (default 5min) | `SystemHealthMonitor` | Check disk, Ollama, heap; alert admins |
| `fixedDelay=30000` (every 30s) | `UsbResetWatcherService` | Poll USB mount for factory reset trigger files |

---

*This audit was generated by reading every source file on disk. It is the definitive source of truth for the MyOffGridAI-Server codebase as of 2026-03-14.*
