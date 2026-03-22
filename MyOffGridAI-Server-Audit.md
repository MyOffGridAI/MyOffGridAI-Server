# MyOffGridAI-Server — Codebase Audit

**Audit Date:** 2026-03-21T23:43:34Z
**Branch:** main
**Commit:** 7366b30264c3dda20f68660a0ae7217e1925c70c Add shared knowledge vault — allow users to share documents with all household members
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** MyOffGridAI-Server-Audit.md
**Scorecard:** MyOffGridAI-Server-Scorecard.md
**OpenAPI Spec:** MyOffGridAI-Server-OpenAPI.yaml (generated separately)

> This audit is the source of truth for the MyOffGridAI-Server codebase structure, entities, services, and configuration.
> The OpenAPI spec (MyOffGridAI-Server-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name:       MyOffGridAI Server
Repository URL:     https://github.com/aallard/MyOffGridAI-Server.git
Primary Language:   Java 21 / Spring Boot 3.4.13
Build Tool:         Apache Maven 3.9.12
Current Branch:     main
Latest Commit:      7366b30264c3dda20f68660a0ae7217e1925c70c
Latest Message:     Add shared knowledge vault — allow users to share documents with all household members
Audit Timestamp:    2026-03-21T23:43:40Z
```

---

## 2. Directory Structure

```
MyOffGridAI-Server/
├── pom.xml
├── Dockerfile
├── CONVENTIONS.md
├── README.md
├── MyOffGridAI-Server-Architecture.md
├── src/
│   ├── main/
│   │   ├── java/com/myoffgridai/
│   │   │   ├── MyOffGridAiApplication.java          ← Entry point
│   │   │   ├── ai/                                   ← AI chat, inference, judge
│   │   │   │   ├── controller/ (ChatController, ModelController)
│   │   │   │   ├── dto/ (18 DTOs)
│   │   │   │   ├── judge/ (JudgeController, services, DTOs)
│   │   │   │   ├── model/ (Conversation, Message, MessageRole)
│   │   │   │   ├── repository/ (ConversationRepository, MessageRepository)
│   │   │   │   └── service/ (12 services: Chat, Inference, Ollama, LlamaServer, Agent, etc.)
│   │   │   ├── auth/                                 ← Authentication & users
│   │   │   │   ├── controller/ (AuthController, UserController)
│   │   │   │   ├── dto/ (7 DTOs)
│   │   │   │   ├── model/ (User, Role)
│   │   │   │   ├── repository/ (UserRepository)
│   │   │   │   └── service/ (AuthService, JwtService, UserService)
│   │   │   ├── common/                               ← Shared exceptions, response, utilities
│   │   │   │   ├── exception/ (GlobalExceptionHandler + 11 custom exceptions)
│   │   │   │   ├── response/ (ApiResponse)
│   │   │   │   └── util/ (AesEncryptionUtil, AesAttributeConverter, TokenCounter)
│   │   │   ├── config/                               ← Spring config, security, filters
│   │   │   │   ├── SecurityConfig, JwtAuthFilter, RateLimitingFilter
│   │   │   │   ├── OllamaConfig, LlamaServerConfig, VectorStoreConfig
│   │   │   │   ├── InferenceProperties, LlamaServerProperties
│   │   │   │   ├── CaptivePortalRedirectFilter, MdcFilter, RequestResponseLoggingFilter
│   │   │   │   ├── JpaConfig, ProcessConfig, AppConstants, VectorType
│   │   │   ├── enrichment/                           ← Web search & fetch, Claude API
│   │   │   │   ├── controller/ (EnrichmentController)
│   │   │   │   ├── dto/ (6 DTOs)
│   │   │   │   └── service/ (ClaudeApiService, WebFetchService, WebSearchService)
│   │   │   ├── events/                               ← Scheduled events & automation
│   │   │   │   ├── controller/ (ScheduledEventController)
│   │   │   │   ├── dto/ (3 DTOs)
│   │   │   │   ├── model/ (ScheduledEvent, ActionType, EventType, ThresholdOperator)
│   │   │   │   ├── repository/ (ScheduledEventRepository)
│   │   │   │   └── service/ (ScheduledEventService)
│   │   │   ├── frontier/                             ← External frontier AI providers
│   │   │   │   ├── FrontierApiClient, FrontierApiRouter, FrontierProvider
│   │   │   │   ├── ClaudeFrontierClient, OpenAiFrontierClient, GrokFrontierClient
│   │   │   ├── knowledge/                            ← Knowledge vault (documents, RAG)
│   │   │   │   ├── controller/ (KnowledgeController)
│   │   │   │   ├── dto/ (9 DTOs)
│   │   │   │   ├── model/ (KnowledgeDocument, KnowledgeChunk, DocumentStatus)
│   │   │   │   ├── repository/ (KnowledgeDocumentRepository, KnowledgeChunkRepository)
│   │   │   │   ├── service/ (7 services: Knowledge, Ingestion, Chunking, FileStorage, OCR, SemanticSearch, StorageHealth)
│   │   │   │   └── util/ (DeltaJsonUtils)
│   │   │   ├── library/                              ← Ebooks, Gutenberg, Kiwix/ZIM
│   │   │   │   ├── config/ (KiwixProperties, LibraryProperties)
│   │   │   │   ├── controller/ (LibraryController)
│   │   │   │   ├── dto/ (10 DTOs)
│   │   │   │   ├── model/ (Ebook, EbookFormat, ZimFile)
│   │   │   │   ├── repository/ (EbookRepository, ZimFileRepository)
│   │   │   │   └── service/ (7 services: Ebook, Gutenberg, KiwixCatalog, KiwixDownload, KiwixProcess, Calibre, ZimFile)
│   │   │   ├── mcp/                                  ← MCP (Model Context Protocol) server
│   │   │   │   ├── config/ (McpServerConfig, McpAuthFilter, McpAuthentication)
│   │   │   │   ├── controller/ (McpDiscoveryController, McpTokenController)
│   │   │   │   ├── dto/ (3 DTOs)
│   │   │   │   ├── model/ (McpApiToken)
│   │   │   │   ├── repository/ (McpApiTokenRepository)
│   │   │   │   └── service/ (McpTokenService, McpToolsService)
│   │   │   ├── memory/                               ← Memory, embeddings, RAG
│   │   │   │   ├── controller/ (MemoryController)
│   │   │   │   ├── dto/ (5 DTOs)
│   │   │   │   ├── model/ (Memory, VectorDocument, MemoryImportance, VectorSourceType)
│   │   │   │   ├── repository/ (MemoryRepository, VectorDocumentRepository)
│   │   │   │   └── service/ (5 services: Memory, Embedding, MemoryExtraction, Rag, Summarization)
│   │   │   ├── models/                               ← Model download & catalog
│   │   │   │   ├── controller/ (ModelDownloadController)
│   │   │   │   ├── dto/ (7 DTOs)
│   │   │   │   └── service/ (4 services: ModelCatalog, ModelDownload, ModelDownloadProgressRegistry, QuantizationRecommendation)
│   │   │   ├── notification/                         ← MQTT notifications, device registration
│   │   │   │   ├── config/ (MqttConfig)
│   │   │   │   ├── controller/ (DeviceRegistrationController)
│   │   │   │   ├── dto/ (3 DTOs)
│   │   │   │   ├── model/ (DeviceRegistration)
│   │   │   │   ├── repository/ (DeviceRegistrationRepository)
│   │   │   │   └── service/ (DeviceRegistrationService, MqttPublisherService)
│   │   │   ├── privacy/                              ← Privacy: audit, export, wipe, fortress
│   │   │   │   ├── aspect/ (AuditAspect)
│   │   │   │   ├── controller/ (PrivacyController)
│   │   │   │   ├── dto/ (6 DTOs)
│   │   │   │   ├── model/ (AuditLog, AuditOutcome)
│   │   │   │   ├── repository/ (AuditLogRepository)
│   │   │   │   └── service/ (5 services: Audit, DataExport, DataWipe, Fortress, SovereigntyReport)
│   │   │   ├── proactive/                            ← Proactive insights & notifications
│   │   │   │   ├── controller/ (ProactiveController)
│   │   │   │   ├── dto/ (3 DTOs)
│   │   │   │   ├── model/ (Insight, Notification, InsightCategory, NotificationSeverity, NotificationType)
│   │   │   │   ├── repository/ (InsightRepository, NotificationRepository)
│   │   │   │   └── service/ (7 services: Insight, InsightGenerator, NightlyInsightJob, Notification, NotificationSseRegistry, PatternAnalysis, SystemHealthMonitor)
│   │   │   ├── sensors/                              ← Hardware sensors (serial, polling, SSE)
│   │   │   │   ├── controller/ (SensorController)
│   │   │   │   ├── dto/ (5 DTOs)
│   │   │   │   ├── model/ (Sensor, SensorReading, SensorType, DataFormat)
│   │   │   │   ├── repository/ (SensorRepository, SensorReadingRepository)
│   │   │   │   └── service/ (5 services: Sensor, SensorPolling, SensorStartup, SerialPort, SseEmitterRegistry)
│   │   │   ├── settings/                             ← User settings & external API keys
│   │   │   │   ├── controller/ (UserSettingsController, ExternalApiSettingsController)
│   │   │   │   ├── dto/ (4 DTOs)
│   │   │   │   ├── model/ (UserSettings, ExternalApiSettings)
│   │   │   │   ├── repository/ (UserSettingsRepository, ExternalApiSettingsRepository)
│   │   │   │   └── service/ (UserSettingsService, ExternalApiSettingsService)
│   │   │   ├── skills/                               ← Skills engine (built-in + custom)
│   │   │   │   ├── builtin/ (6 skills: DocumentSummarizer, InventoryTracker, RecipeGenerator, ResourceCalculator, TaskPlanner, WeatherQuery)
│   │   │   │   ├── controller/ (SkillController)
│   │   │   │   ├── dto/ (6 DTOs)
│   │   │   │   ├── model/ (Skill, SkillExecution, SkillCategory, ExecutionStatus, InventoryItem, InventoryCategory, PlannedTask, TaskStatus)
│   │   │   │   ├── repository/ (SkillRepository, SkillExecutionRepository, InventoryItemRepository, PlannedTaskRepository)
│   │   │   │   └── service/ (SkillExecutorService, SkillSeederService, BuiltInSkill)
│   │   │   └── system/                               ← System config, AP mode, factory reset
│   │   │       ├── controller/ (SystemController, CaptivePortalController)
│   │   │       ├── dto/ (7 DTOs)
│   │   │       ├── model/ (SystemConfig)
│   │   │       ├── repository/ (SystemConfigRepository)
│   │   │       └── service/ (5 services: SystemConfig, ApMode, ApModeStartup, FactoryReset, NetworkTransition, UsbResetWatcher)
│   │   └── resources/
│   │       ├── application.yml, application-prod.yml
│   │       ├── logback-spring.xml
│   │       └── META-INF/native-image/ (proxy-config, reflect-config, resource-config)
│   └── test/
│       ├── java/com/myoffgridai/ (mirrors main — 100+ test files)
│       │   └── integration/ (25 integration tests)
│       └── resources/ (application.yml, application-test.yml)
```

Single-module Spring Boot Maven project. Source lives under `src/main/java/com/myoffgridai/` organized into 15 domain packages (ai, auth, common, config, enrichment, events, frontier, knowledge, library, mcp, memory, models, notification, privacy, proactive, sensors, settings, skills, system).

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.13 (parent) | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | 3.4.13 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.4.13 | Spring Security |
| spring-boot-starter-validation | 3.4.13 | Bean validation |
| spring-boot-starter-actuator | 3.4.13 | Health/metrics endpoints |
| spring-boot-starter-webflux | 3.4.13 | Reactive WebClient for streaming |
| spring-boot-starter-aop | 3.4.13 | Aspect-oriented programming (audit) |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 (BOM) | MCP (Model Context Protocol) SSE server |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation & validation |
| postgresql | 42.7.7 | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector extension for embeddings |
| pdfbox | 3.0.4 | PDF text extraction |
| poi / poi-ooxml / poi-scratchpad | 5.4.0 | Office document processing |
| tess4j | 5.13.0 | Tesseract OCR |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| bucket4j-core | 8.10.1 | Rate limiting |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | MQTT pub/sub (notifications) |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger/OpenAPI UI |
| jsoup | 1.18.3 | HTML parsing for web content extraction |
| lombok | 1.18.42 | Boilerplate reduction |
| jackson-core | 2.18.6 (managed) | JSON processing |
| netty-codec-http / netty-codec-http2 | 4.1.129.Final (managed) | Netty transport |
| commons-lang3 | 3.18.0 (managed) | String/object utilities |

**Test Dependencies:**
| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-test | 3.4.13 | JUnit 5, Mockito, AssertJ |
| reactor-test | (managed) | Reactive stream testing |
| spring-security-test | (managed) | Security test utilities |
| testcontainers-postgresql | 1.20.6 | PostgreSQL in Docker for integration tests |
| testcontainers-junit-jupiter | 1.20.6 | Testcontainers JUnit 5 support |

**Build Plugins:**
- `spring-boot-maven-plugin` — Excludes Lombok from packaged JAR
- `maven-compiler-plugin` — Java 21, Lombok annotation processor
- `jacoco-maven-plugin` 0.8.12 — 100% line + branch coverage enforcement; excludes DTOs, models, Application class
- `maven-surefire-plugin` — Adds `--add-opens` for Java 21, Testcontainers Docker env vars
- `native-maven-plugin` 0.10.4 (profile: `native`) — GraalVM native image build

**Build Commands:**
```
Build:   mvn clean compile -DskipTests
Test:    mvn test
Run:     mvn spring-boot:run
Package: mvn clean package -DskipTests
Native:  mvn -Pnative clean package -DskipTests
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Default profile: dev. Server port: 8080. Flyway disabled. MCP SSE server on `/mcp/sse`. Multipart max: 2048MB. Async request timeout: -1 (unlimited).
- **`application.yml` (dev profile)** — PostgreSQL at `localhost:5432/myoffgridai`, Hikari pool 20/5. Hibernate ddl-auto: update. Dev JWT secret hardcoded. Inference provider: ollama. Ollama at `localhost:11434`, LlamaServer at port 1234. Fortress/AP mode: mock=true. Rate limiting enabled. MQTT disabled. Judge disabled. Kiwix enabled at port 8888.
- **`application-prod.yml`** — All secrets from env vars (`${DB_URL}`, `${JWT_SECRET}`, `${ENCRYPTION_KEY}`). Hibernate ddl-auto: validate. Flyway enabled. Inference manage-process: true. Fortress/AP mock: false.
- **`application-test.yml`** — Hibernate ddl-auto: create-drop. Test JWT secret. Rate limiting disabled. MQTT disabled. Fortress/AP mock: true. Library directories in temp dir.
- **`src/test/resources/application.yml`** — MCP server disabled for tests. LlamaServer defaults.
- **`logback-spring.xml`** — Dev: console + rolling file (plain text). Prod: rolling file (LogstashEncoder JSON, includes requestId/username/userId MDC). Test: rolling file (JSON). Max file: 50MB, 30 days, 1GB cap.
- **`Dockerfile`** — Multi-stage: `eclipse-temurin:21-jdk-alpine` → `eclipse-temurin:21-jre-alpine`. Non-root user `myoffgridai`. Exposed port 8080. Healthcheck on `/api/system/status`.

**Connection Map:**
```
Database:        PostgreSQL, localhost:5432, database: myoffgridai
Cache:           None
Message Broker:  MQTT (Paho), tcp://localhost:1883 (disabled in dev/test)
External APIs:   Ollama (localhost:11434), LlamaServer (localhost:1234),
                 Kiwix (localhost:8888), Calibre (localhost:8081),
                 Gutenberg (gutendex.com), Kiwix Catalog (library.kiwix.org),
                 Frontier AI: Claude, OpenAI, Grok (external)
Cloud Services:  None
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `com.myoffgridai.MyOffGridAiApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`

**Startup Initialization (ApplicationReadyEvent listeners):**
- `VectorStoreConfig.initializeVectorStore()` — Creates pgvector extension & vector_documents table
- `ApModeStartupService.checkApMode()` — Checks system config for AP mode / initialization state
- `SensorStartupService.startPolling()` — Starts serial port polling for registered sensors
- `StorageHealthService.checkStoragePaths()` — Validates knowledge storage directories exist
- `ModelPreloadService.preloadActiveModel()` — Preloads active LLM model on startup
- `ModelHealthCheckService.startHealthCheck()` — Begins periodic model health monitoring
- `KiwixProcessService.startKiwix()` — Starts Kiwix ZIM server process
- `SkillSeederService.seedBuiltInSkills()` — Seeds built-in skills into database

**ApplicationRunner:**
- `LlamaServerProcessService` — Implements `ApplicationRunner`; starts llama-server process if `manage-process=true`

**Scheduled Tasks:**
- `LlamaServerProcessService.healthCheck()` — Periodic health check (configurable interval, default 30s)
- `UsbResetWatcherService.checkForResetTrigger()` — Checks for USB factory-reset trigger every 30s
- `SummarizationService.runNightlySummarization()` — Cron: `0 0 2 * * *` (2 AM daily)
- `NightlyInsightJob.generateInsights()` — Cron: `0 0 3 * * *` (3 AM daily)
- `SystemHealthMonitor.runHealthCheck()` — Periodic health check (default 300s)

**Health Check:** `GET /api/system/status` — Returns system initialization state and status

---

## 6. Entity / Data Model Layer

### === Conversation.java ===
Table: `conversations`
Primary Key: `id` UUID (GenerationType.UUID)
Auditing: `@EntityListeners(AuditingEntityListener.class)`

Fields:
  - id: UUID [@Id, @GeneratedValue(UUID)]
  - user: User [@ManyToOne(EAGER), @JoinColumn("user_id", nullable=false)]
  - title: String [@Column]
  - isArchived: boolean [@Column("is_archived", nullable=false)] default false
  - createdAt: Instant [@CreatedDate, @Column(updatable=false)]
  - updatedAt: Instant [@LastModifiedDate]
  - messageCount: int [@Column("message_count", nullable=false)] default 0

Relationships:
  - @ManyToOne → User (fetch=EAGER, JoinColumn="user_id")

Custom Methods: `@PrePersist onCreate()`, `@PreUpdate onUpdate()`

---

### === Message.java ===
Table: `messages`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - id: UUID [@Id, @GeneratedValue(UUID)]
  - conversation: Conversation [@ManyToOne(LAZY), @JoinColumn("conversation_id", nullable=false)]
  - role: MessageRole [@Enumerated(STRING), nullable=false]
  - content: String [@Column(nullable=false, TEXT)]
  - tokenCount: Integer [@Column("token_count")]
  - hasRagContext: boolean [@Column("has_rag_context", nullable=false)] default false
  - thinkingContent: String [@Column("thinking_content", TEXT)]
  - tokensPerSecond: Double [@Column("tokens_per_second")]
  - inferenceTimeSeconds: Double [@Column("inference_time_seconds")]
  - stopReason: String [@Column("stop_reason")]
  - thinkingTokenCount: Integer [@Column("thinking_token_count")]
  - sourceTag: SourceTag [@Enumerated(STRING), @Column("source_tag", length=20, nullable=false)]
  - judgeScore: Double [@Column("judge_score")]
  - judgeReason: String [@Column("judge_reason", TEXT)]
  - createdAt: Instant [@CreatedDate, @Column(updatable=false)]

Relationships:
  - @ManyToOne → Conversation (fetch=LAZY, JoinColumn="conversation_id")

Custom Methods: `@PrePersist onCreate()`

---

### === User.java ===
Table: `users`
Primary Key: `id` UUID (GenerationType.UUID)
Implements: `UserDetails` (Spring Security)

Fields:
  - id: UUID [@Id, @GeneratedValue(UUID)]
  - username: String [@Column(nullable=false, unique=true)]
  - email: String [@Column(unique=true)]
  - displayName: String [@Column("display_name", nullable=false)]
  - passwordHash: String [@Column("password_hash", nullable=false)]
  - role: Role [@Enumerated(STRING), nullable=false]
  - isActive: boolean [@Column("is_active", nullable=false)] default true
  - createdAt: Instant [@CreatedDate, @Column(updatable=false)]
  - updatedAt: Instant [@LastModifiedDate]
  - lastLoginAt: Instant [@Column("last_login_at")]

Custom Methods: `@PrePersist`, `@PreUpdate`, UserDetails implementation (`getAuthorities()`, `getPassword()`, `isEnabled()`, etc.)

---

### === ScheduledEvent.java ===
Table: `scheduled_events`
Indexes: `idx_event_user_id(user_id)`, `idx_event_enabled_type(is_enabled, event_type)`
Primary Key: `id` UUID

Fields:
  - id: UUID, userId: UUID [@Column(nullable=false)]
  - name: String [@Column(nullable=false)]
  - description: String [TEXT]
  - eventType: EventType [@Enumerated(STRING), nullable=false]
  - isEnabled: boolean [nullable=false] default true
  - cronExpression: String, recurringIntervalMinutes: Integer
  - sensorId: UUID, thresholdOperator: ThresholdOperator, thresholdValue: Double
  - actionType: ActionType [@Enumerated(STRING), nullable=false]
  - actionPayload: String [TEXT, nullable=false]
  - lastTriggeredAt: Instant, nextFireAt: Instant
  - createdAt: Instant [@CreatedDate], updatedAt: Instant [@LastModifiedDate]

---

### === KnowledgeDocument.java ===
Table: `knowledge_documents`
Indexes: `idx_knowledge_doc_user_id(user_id)`, `idx_knowledge_doc_shared(is_shared)`

Fields:
  - id: UUID, userId: UUID [@Column(nullable=false)]
  - filename: String [nullable=false], displayName: String
  - mimeType: String [nullable=false], storagePath: String [nullable=false]
  - fileSizeBytes: long, status: DocumentStatus [@Enumerated(STRING)]
  - errorMessage: String [TEXT], chunkCount: int
  - uploadedAt: Instant [@CreatedDate], processedAt: Instant
  - content: String [TEXT]
  - isShared: boolean [@Column("is_shared", nullable=false)] default false

---

### === KnowledgeChunk.java ===
Table: `knowledge_chunks`
Indexes: `idx_knowledge_chunk_doc_id(document_id)`, `idx_knowledge_chunk_user_id(user_id)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - document: KnowledgeDocument [@ManyToOne(LAZY), @JoinColumn("document_id", nullable=false)]
  - chunkIndex: int [nullable=false], content: String [TEXT, nullable=false]
  - pageNumber: Integer, createdAt: Instant [@CreatedDate]

Relationships:
  - @ManyToOne → KnowledgeDocument (fetch=LAZY)

---

### === Ebook.java ===
Table: `ebooks`
Indexes: `idx_ebooks_gutenberg_id(gutenberg_id)`

Fields:
  - id: UUID, title: String [nullable=false], author: String
  - description: String [TEXT, length=2000], isbn: String, publisher: String
  - publishedYear: Integer, language: String
  - format: EbookFormat [@Enumerated(STRING), nullable=false]
  - fileSizeBytes: long, filePath: String [nullable=false]
  - coverImagePath: String, gutenbergId: String
  - downloadCount: int, uploadedAt: Instant [@CreatedDate], uploadedBy: UUID

---

### === ZimFile.java ===
Table: `zim_files`

Fields:
  - id: UUID, filename: String [nullable=false, unique=true]
  - displayName: String [nullable=false], description: String [length=1000]
  - language: String, category: String
  - fileSizeBytes: long, articleCount: int, mediaCount: int
  - createdDate: String, filePath: String [nullable=false]
  - kiwixBookId: String, uploadedAt: Instant [@CreatedDate], uploadedBy: UUID

---

### === McpApiToken.java ===
Table: `mcp_api_tokens`
Indexes: `idx_mcp_token_created_by(created_by)`

Fields:
  - id: UUID, tokenHash: String [nullable=false, length=500] — BCrypt hashed
  - name: String [nullable=false], createdBy: UUID [nullable=false]
  - lastUsedAt: Instant, isActive: boolean [nullable=false] default true
  - createdAt: Instant [@CreatedDate]

---

### === Memory.java ===
Table: `memories`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - importance: MemoryImportance [@Enumerated(STRING), nullable=false]
  - tags: String, sourceConversationId: UUID
  - createdAt: Instant [@CreatedDate], updatedAt: Instant [@LastModifiedDate]
  - lastAccessedAt: Instant, accessCount: int [nullable=false] default 0

---

### === VectorDocument.java ===
Table: `vector_document`
Indexes: `idx_vector_doc_user_source_type(user_id, source_type)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - embedding: float[] [@Type(VectorType.class), @Column("vector(768)")] — 768-dim pgvector
  - sourceType: VectorSourceType [@Enumerated(STRING), nullable=false]
  - sourceId: UUID, metadata: String [TEXT]
  - createdAt: Instant [@CreatedDate]

---

### === DeviceRegistration.java ===
Table: `device_registrations`
Unique Constraint: `uk_device_registration_user_device(user_id, device_id)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - deviceId: String [nullable=false], deviceName: String
  - platform: String [nullable=false], mqttClientId: String
  - lastSeenAt: Instant, createdAt: Instant, updatedAt: Instant

---

### === AuditLog.java ===
Table: `audit_logs`
Indexes: `idx_audit_user_timestamp(user_id, timestamp DESC)`, `idx_audit_timestamp(timestamp DESC)`

Fields:
  - id: UUID, userId: UUID, username: String
  - action: String [nullable=false], resourceType: String, resourceId: String
  - httpMethod: String [nullable=false], requestPath: String [nullable=false]
  - ipAddress: String, userAgent: String
  - responseStatus: int, outcome: AuditOutcome [@Enumerated(STRING), nullable=false]
  - durationMs: long, timestamp: Instant [nullable=false]

---

### === Insight.java ===
Table: `insights`
Indexes: `idx_insight_user_id(user_id)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - category: InsightCategory [@Enumerated(STRING), nullable=false]
  - isRead: boolean [nullable=false] default false
  - isDismissed: boolean [nullable=false] default false
  - generatedAt: Instant [updatable=false], readAt: Instant

---

### === Notification.java ===
Table: `notifications`
Indexes: `idx_notification_user_id(user_id)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - title: String [nullable=false], body: String [TEXT, nullable=false]
  - type: NotificationType [@Enumerated(STRING), nullable=false]
  - isRead: boolean [nullable=false] default false
  - createdAt: Instant [updatable=false], readAt: Instant
  - severity: NotificationSeverity [@Enumerated(STRING), length=20]
  - mqttDelivered: boolean [nullable=false] default false
  - metadata: String [TEXT]

---

### === Sensor.java ===
Table: `sensors`
Indexes: `idx_sensor_user_id(user_id)`, `idx_sensor_port_path(port_path, unique=true)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - name: String [nullable=false], type: SensorType [@Enumerated(STRING)]
  - portPath: String [nullable=false, unique]
  - baudRate: int [nullable=false] default 9600
  - dataFormat: DataFormat [@Enumerated(STRING), nullable=false]
  - valueField: String, unit: String
  - isActive: boolean [nullable=false] default false
  - pollIntervalSeconds: int [nullable=false] default 30
  - lowThreshold: Double, highThreshold: Double
  - createdAt: Instant [@CreatedDate], updatedAt: Instant [@LastModifiedDate]

---

### === SensorReading.java ===
Table: `sensor_readings`
Indexes: `idx_sensor_reading_sensor_recorded(sensor_id, recorded_at DESC)`

Fields:
  - id: UUID
  - sensor: Sensor [@ManyToOne(LAZY), @JoinColumn("sensor_id", nullable=false)]
  - value: double [nullable=false], rawData: String
  - recordedAt: Instant [nullable=false]

Relationships:
  - @ManyToOne → Sensor (fetch=LAZY)

---

### === UserSettings.java ===
Table: `user_settings`

Fields:
  - id: UUID
  - user: User [@OneToOne(LAZY), @JoinColumn("user_id", nullable=false, unique=true)]
  - themePreference: String [nullable=false] default "system"
  - createdAt: Instant, updatedAt: Instant

Relationships:
  - @OneToOne → User

---

### === ExternalApiSettings.java ===
Table: `external_api_settings`

Fields:
  - id: UUID, singletonGuard: String [unique, nullable=false] — enforces singleton row
  - anthropicApiKey: String [@Convert(AesAttributeConverter)] — AES-256-GCM encrypted
  - anthropicModel: String [nullable=false] default "claude-sonnet-4-20250514"
  - anthropicEnabled: boolean, braveApiKey: String [encrypted], braveEnabled: boolean
  - maxWebFetchSizeKb: int default 512, searchResultLimit: int default 5
  - huggingFaceToken: String [encrypted], huggingFaceEnabled: boolean
  - grokApiKey: String [encrypted, length=1000], grokEnabled: boolean
  - openAiApiKey: String [encrypted, length=1000], openAiEnabled: boolean
  - preferredFrontierProvider: FrontierProvider [@Enumerated(STRING)]
  - judgeEnabled: boolean, judgeModelFilename: String [length=500]
  - judgeScoreThreshold: double, createdAt: Instant, updatedAt: Instant

---

### === Skill.java ===
Table: `skills`
Indexes: `idx_skill_name(name, unique=true)`, `idx_skill_category(category)`

Fields:
  - id: UUID, name: String [unique, nullable=false]
  - displayName: String [nullable=false], description: String [TEXT, nullable=false]
  - version: String [nullable=false], author: String [nullable=false]
  - category: SkillCategory [@Enumerated(STRING), nullable=false]
  - isEnabled: boolean default true, isBuiltIn: boolean default false
  - parametersSchema: String [TEXT]
  - createdAt: Instant, updatedAt: Instant

---

### === SkillExecution.java ===
Table: `skill_executions`
Indexes: `idx_skill_exec_user_id(user_id)`, `idx_skill_exec_skill_id(skill_id)`

Fields:
  - id: UUID
  - skill: Skill [@ManyToOne(LAZY), @JoinColumn("skill_id", nullable=false)]
  - userId: UUID [nullable=false]
  - status: ExecutionStatus [@Enumerated(STRING), nullable=false]
  - inputParams: String [TEXT], outputResult: String [TEXT], errorMessage: String [TEXT]
  - startedAt: Instant [@CreatedDate], completedAt: Instant, durationMs: Long

Relationships:
  - @ManyToOne → Skill (fetch=LAZY)

---

### === InventoryItem.java ===
Table: `inventory_items`
Indexes: `idx_inventory_user_id(user_id)`, `idx_inventory_category(user_id, category)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - name: String [nullable=false], category: InventoryCategory [@Enumerated(STRING)]
  - quantity: double [nullable=false], unit: String, notes: String [TEXT]
  - lowStockThreshold: Double
  - createdAt: Instant, updatedAt: Instant

---

### === PlannedTask.java ===
Table: `planned_tasks`
Indexes: `idx_planned_task_user_id(user_id)`

Fields:
  - id: UUID, userId: UUID [nullable=false]
  - goalDescription: String [TEXT, nullable=false], title: String [nullable=false]
  - steps: String [TEXT, nullable=false], estimatedResources: String [TEXT]
  - status: TaskStatus [@Enumerated(STRING), nullable=false]
  - createdAt: Instant, updatedAt: Instant

---

### === SystemConfig.java ===
Table: `system_config`

Fields:
  - id: UUID, initialized: boolean default false
  - instanceName: String
  - fortressEnabled: boolean default false, fortressEnabledAt: Instant, fortressEnabledByUserId: UUID
  - apModeEnabled: boolean default false, wifiConfigured: boolean default false
  - aiModel: String default "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M"
  - aiTemperature: Double default 0.7, aiSimilarityThreshold: Double default 0.45
  - aiMemoryTopK: Integer default 5, aiRagMaxContextTokens: Integer default 2048
  - aiContextSize: Integer default 4096, aiContextMessageLimit: Integer default 20
  - activeModelFilename: String
  - knowledgeStoragePath: String default "/var/myoffgridai/knowledge"
  - maxUploadSizeMb: Integer default 25
  - createdAt: Instant, updatedAt: Instant

---

## 7. Enum Inventory

### === MessageRole.java ===
Values: USER, ASSISTANT, SYSTEM
Used in: Message.role

### === SourceTag.java ===
Values: LOCAL, ENHANCED
Used in: Message.sourceTag — indicates if response was refined by cloud frontier model

### === Role.java ===
Values: ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD
Used in: User.role

### === ActionType.java ===
Values: PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY
Used in: ScheduledEvent.actionType

### === EventType.java ===
Values: SCHEDULED, SENSOR_THRESHOLD, RECURRING
Used in: ScheduledEvent.eventType

### === ThresholdOperator.java ===
Values: ABOVE, BELOW, EQUALS
Used in: ScheduledEvent.thresholdOperator

### === DocumentStatus.java ===
Values: PENDING, PROCESSING, READY, FAILED
Used in: KnowledgeDocument.status

### === EbookFormat.java ===
Values: EPUB, PDF, MOBI, AZW, TXT, HTML
Used in: Ebook.format

### === MemoryImportance.java ===
Values: LOW, MEDIUM, HIGH, CRITICAL
Used in: Memory.importance

### === VectorSourceType.java ===
Values: MEMORY, CONVERSATION, KNOWLEDGE_CHUNK
Used in: VectorDocument.sourceType

### === AuditOutcome.java ===
Values: SUCCESS, FAILURE, DENIED
Used in: AuditLog.outcome

### === InsightCategory.java ===
Values: HOMESTEAD, HEALTH, RESOURCE, GENERAL
Used in: Insight.category

### === NotificationType.java ===
Values: SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL
Used in: Notification.type

### === NotificationSeverity.java ===
Values: INFO, WARNING, CRITICAL
Used in: Notification.severity

### === SensorType.java ===
Values: TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM
Used in: Sensor.type

### === DataFormat.java ===
Values: CSV_LINE, JSON_LINE
Used in: Sensor.dataFormat

### === SkillCategory.java ===
Values: HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM
Used in: Skill.category

### === ExecutionStatus.java ===
Values: RUNNING, COMPLETED, FAILED
Used in: SkillExecution.status

### === InventoryCategory.java ===
Values: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER
Used in: InventoryItem.category

### === TaskStatus.java ===
Values: ACTIVE, COMPLETED, CANCELLED
Used in: PlannedTask.status

### === FrontierProvider.java ===
Values: CLAUDE, GROK, OPENAI
Used in: ExternalApiSettings.preferredFrontierProvider, FrontierApiClient/Router

### === QuantizationType.java ===
Values: Q2_K, Q3_K_S, Q3_K_M, Q3_K_L, Q4_0, Q4_K_S, Q4_K_M, Q5_0, Q5_K_S, Q5_K_M, Q6_K, Q8_0, F16, F32, IQ1_S, IQ2_XXS, IQ2_XS, IQ2_S, IQ3_XXS, IQ3_XS, IQ3_S, IQ4_XS, IQ4_NL
Used in: Model download DTOs — has display label via `getLabel()`

### === KiwixDownloadState.java ===
Values: QUEUED, DOWNLOADING, VALIDATING, REGISTERING, COMPLETED, FAILED, CANCELLED
Used in: KiwixDownloadStatusDto

### === KiwixInstallationStatus.java ===
Values: NOT_INSTALLED, INSTALLED, RUNNING
Used in: KiwixStatusDto

---

## 8. Repository Layer

### === ConversationRepository ===
Entity: Conversation | Extends: JpaRepository<Conversation, UUID>
Custom Methods:
  - findByUserIdOrderByUpdatedAtDesc(UUID, Pageable): Page<Conversation>
  - findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID, boolean, Pageable): Page<Conversation>
  - findByIdAndUserId(UUID, UUID): Optional<Conversation>
  - countByUserId(UUID): long
  - findByUserId(UUID): List<Conversation>
  - findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID, String, Pageable): Page<Conversation>
  - @Modifying @Query("DELETE FROM Conversation c WHERE c.user.id = :userId") deleteByUserId(UUID): void

### === MessageRepository ===
Entity: Message | Extends: JpaRepository<Message, UUID>
Custom Methods:
  - findByConversationIdOrderByCreatedAtAsc(UUID): List<Message>
  - findByConversationIdOrderByCreatedAtAsc(UUID, Pageable): Page<Message>
  - findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable): List<Message>
  - countByConversationId(UUID): long
  - deleteByConversationId(UUID): void
  - @Query("SELECT COUNT(m)…") countByUserId(UUID): long
  - @Modifying @Query("DELETE FROM Message m WHERE m.conversation.user.id = :userId") deleteByUserId(UUID): void
  - @Modifying @Query("DELETE messages after messageId") deleteMessagesAfter(UUID, UUID): void

### === UserRepository ===
Entity: User | Extends: JpaRepository<User, UUID>
Custom Methods:
  - findByUsername(String): Optional<User>
  - findByEmail(String): Optional<User>
  - existsByUsername(String): boolean
  - existsByEmail(String): boolean
  - findAllByRole(Role): List<User>
  - countByIsActiveTrue(): long
  - findByIsActiveTrue(): List<User>

### === ScheduledEventRepository ===
Entity: ScheduledEvent | Extends: JpaRepository<ScheduledEvent, UUID>
Custom Methods:
  - findAllByUserId(UUID, Pageable): Page<ScheduledEvent>
  - findByIdAndUserId(UUID, UUID): Optional<ScheduledEvent>
  - findByIsEnabledTrueAndEventType(EventType): List<ScheduledEvent>
  - findAllByUserIdOrderByCreatedAtDesc(UUID): List<ScheduledEvent>
  - deleteByUserId(UUID): void
  - countByUserId(UUID): long

### === KnowledgeDocumentRepository ===
Entity: KnowledgeDocument | Extends: JpaRepository<KnowledgeDocument, UUID>
Custom Methods:
  - findByUserIdOrderByUploadedAtDesc(UUID, Pageable): Page<KnowledgeDocument>
  - findByIdAndUserId(UUID, UUID): Optional<KnowledgeDocument>
  - findByUserIdAndStatus(UUID, DocumentStatus): List<KnowledgeDocument>
  - countByUserId(UUID): long
  - findByIsSharedTrueAndUserIdNotOrderByUploadedAtDesc(UUID, Pageable): Page<KnowledgeDocument>
  - findByIdAndIsSharedTrue(UUID): Optional<KnowledgeDocument>
  - @Modifying deleteByUserId(UUID): void

### === KnowledgeChunkRepository ===
Entity: KnowledgeChunk | Extends: JpaRepository<KnowledgeChunk, UUID>
Custom Methods:
  - findByDocumentIdOrderByChunkIndexAsc(UUID): List<KnowledgeChunk>
  - countByDocumentId(UUID): long
  - @Modifying deleteByDocumentId(UUID): void
  - @Modifying deleteByUserId(UUID): void

### === EbookRepository ===
Entity: Ebook | Extends: JpaRepository<Ebook, UUID>
Custom Methods:
  - @Query("SELECT e FROM Ebook e WHERE search/format filter…") searchByTitleOrAuthor(String, EbookFormat, Pageable): Page<Ebook>
  - findByGutenbergId(String): Optional<Ebook>
  - existsByGutenbergId(String): boolean

### === ZimFileRepository ===
Entity: ZimFile | Extends: JpaRepository<ZimFile, UUID>
Custom Methods:
  - findByFilename(String): Optional<ZimFile>
  - findAllByOrderByDisplayNameAsc(): List<ZimFile>
  - existsByFilename(String): boolean

### === McpApiTokenRepository ===
Entity: McpApiToken | Extends: JpaRepository<McpApiToken, UUID>
Custom Methods:
  - findByIsActiveTrue(): List<McpApiToken>
  - findByCreatedByOrderByCreatedAtDesc(UUID): List<McpApiToken>

### === MemoryRepository ===
Entity: Memory | Extends: JpaRepository<Memory, UUID>
Custom Methods:
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Memory>
  - findByUserIdAndImportance(UUID, MemoryImportance, Pageable): Page<Memory>
  - findByUserIdAndTagsContaining(UUID, String, Pageable): Page<Memory>
  - findByUserId(UUID): List<Memory>
  - countByUserId(UUID): long
  - @Modifying deleteByUserId(UUID): void

### === VectorDocumentRepository ===
Entity: VectorDocument | Extends: JpaRepository<VectorDocument, UUID>
Custom Methods:
  - findByUserIdAndSourceType(UUID, VectorSourceType): List<VectorDocument>
  - @Query(nativeQuery) findMostSimilar(UUID, String, String, int): List<VectorDocument> — pgvector cosine distance
  - @Query(nativeQuery) findMostSimilarAcrossTypes(UUID, String, int): List<VectorDocument>
  - @Query(nativeQuery) findMostSimilarIncludingShared(UUID, String, String, int): List<VectorDocument> — includes shared knowledge
  - @Modifying deleteBySourceIdAndSourceType(UUID, VectorSourceType): void
  - @Modifying deleteByUserId(UUID): void

### === DeviceRegistrationRepository ===
Entity: DeviceRegistration | Extends: JpaRepository<DeviceRegistration, UUID>
Custom Methods:
  - findByUserIdAndDeviceId(UUID, String): Optional<DeviceRegistration>
  - findByUserId(UUID): List<DeviceRegistration>
  - deleteByUserIdAndDeviceId(UUID, String): void

### === AuditLogRepository ===
Entity: AuditLog | Extends: JpaRepository<AuditLog, UUID>
Custom Methods:
  - findAllByOrderByTimestampDesc(Pageable): Page<AuditLog>
  - findByUserIdOrderByTimestampDesc(UUID, Pageable): Page<AuditLog>
  - findByOutcomeOrderByTimestampDesc(AuditOutcome, Pageable): Page<AuditLog>
  - findByTimestampBetweenOrderByTimestampDesc(Instant, Instant, Pageable): Page<AuditLog>
  - findByUserIdAndTimestampBetween(UUID, Instant, Instant, Pageable): Page<AuditLog>
  - countByOutcomeAndTimestampBetween(AuditOutcome, Instant, Instant): long
  - @Modifying deleteByTimestampBefore(Instant): void
  - @Modifying deleteByUserId(UUID): void

### === InsightRepository ===
Entity: Insight | Extends: JpaRepository<Insight, UUID>
Custom Methods:
  - findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID, Pageable): Page<Insight>
  - findByUserIdAndCategoryAndIsDismissedFalse(UUID, InsightCategory, Pageable): Page<Insight>
  - findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): List<Insight>
  - countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Insight>
  - countByUserId(UUID): long
  - @Modifying deleteByUserId(UUID): void

### === NotificationRepository ===
Entity: Notification | Extends: JpaRepository<Notification, UUID>
Custom Methods:
  - findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID): List<Notification>
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Notification>
  - countByUserIdAndIsReadFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Notification>
  - @Modifying @Query("UPDATE…SET isRead=true…") markAllReadForUser(UUID, Instant): void
  - @Modifying deleteByUserId(UUID): void

### === SensorRepository ===
Entity: Sensor | Extends: JpaRepository<Sensor, UUID>
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<Sensor>
  - findByIdAndUserId(UUID, UUID): Optional<Sensor>
  - findByUserIdAndIsActiveTrue(UUID): List<Sensor>
  - findByPortPath(String): Optional<Sensor>
  - findByIsActiveTrue(): List<Sensor>
  - countByUserId(UUID): long
  - deleteByUserId(UUID): void

### === SensorReadingRepository ===
Entity: SensorReading | Extends: JpaRepository<SensorReading, UUID>
Custom Methods:
  - findBySensorIdOrderByRecordedAtDesc(UUID, Pageable): Page<SensorReading>
  - findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID, Instant): List<SensorReading>
  - findTopBySensorIdOrderByRecordedAtDesc(UUID): Optional<SensorReading>
  - @Query(nativeQuery) findAverageValueSince(UUID, Instant): Double
  - @Modifying deleteBySensorId(UUID): void
  - @Modifying @Query("DELETE … WHERE sensor.userId = :userId") deleteByUserId(UUID): void

### === UserSettingsRepository ===
Entity: UserSettings | Extends: JpaRepository<UserSettings, UUID>
Custom Methods:
  - findByUserId(UUID): Optional<UserSettings>

### === ExternalApiSettingsRepository ===
Entity: ExternalApiSettings | Extends: JpaRepository<ExternalApiSettings, UUID>
Custom Methods:
  - findBySingletonGuard(String): Optional<ExternalApiSettings>

### === SkillRepository ===
Entity: Skill | Extends: JpaRepository<Skill, UUID>
Custom Methods:
  - findByIsEnabledTrue(): List<Skill>
  - findByIsBuiltInTrue(): List<Skill>
  - findByCategory(SkillCategory): List<Skill>
  - findByName(String): Optional<Skill>
  - findByIsEnabledTrueOrderByDisplayNameAsc(): List<Skill>

### === SkillExecutionRepository ===
Entity: SkillExecution | Extends: JpaRepository<SkillExecution, UUID>
Custom Methods:
  - findByUserIdOrderByStartedAtDesc(UUID, Pageable): Page<SkillExecution>
  - findBySkillIdAndUserIdOrderByStartedAtDesc(UUID, UUID, Pageable): Page<SkillExecution>
  - findByUserIdAndStatus(UUID, ExecutionStatus): List<SkillExecution>

### === InventoryItemRepository ===
Entity: InventoryItem | Extends: JpaRepository<InventoryItem, UUID>
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<InventoryItem>
  - findByUserIdAndCategory(UUID, InventoryCategory): List<InventoryItem>
  - findByUserIdAndQuantityLessThanEqual(UUID, double): List<InventoryItem>
  - findByIdAndUserId(UUID, UUID): Optional<InventoryItem>
  - @Modifying deleteByUserId(UUID): void

### === PlannedTaskRepository ===
Entity: PlannedTask | Extends: JpaRepository<PlannedTask, UUID>
Custom Methods:
  - findByUserIdAndStatusOrderByCreatedAtDesc(UUID, TaskStatus, Pageable): Page<PlannedTask>
  - findByIdAndUserId(UUID, UUID): Optional<PlannedTask>
  - @Modifying deleteByUserId(UUID): void

### === SystemConfigRepository ===
Entity: SystemConfig | Extends: JpaRepository<SystemConfig, UUID>
Custom Methods:
  - @Query("SELECT s FROM SystemConfig s") findFirst(): Optional<SystemConfig>

**Summary:** 24 repositories, 0 use @EntityGraph, 4 use native SQL (pgvector similarity + aggregation), 21 @Modifying queries.

---

## 9. Service Layer — Full Method Signatures

### === ChatService ===
Injects: ConversationRepository, MessageRepository, UserRepository, OllamaService, InferenceService, SystemPromptBuilder, ContextWindowService, RagService, MemoryExtractionService, SystemConfigService, JudgeInferenceService, FrontierApiRouter, ExternalApiSettingsService

Public Methods:
  - createConversation(UUID userId, String initialTitle): Conversation — @Transactional
  - getConversations(UUID userId, boolean includeArchived, Pageable pageable): Page<Conversation>
  - getConversation(UUID conversationId, UUID userId): Conversation
  - archiveConversation(UUID conversationId, UUID userId): void
  - deleteConversation(UUID conversationId, UUID userId): void — @Transactional
  - sendMessage(UUID conversationId, UUID userId, String userContent): Message — @Transactional, builds RAG, calls inference
  - streamMessage(UUID conversationId, UUID userId, String userContent): Flux<String> — streaming SSE with thinking tokens
  - editMessage(UUID conversationId, UUID messageId, UUID userId, String newContent): Message — @Transactional
  - deleteMessage(UUID conversationId, UUID messageId, UUID userId): void — @Transactional
  - branchConversation(UUID conversationId, UUID messageId, UUID userId, String title): Conversation — @Transactional
  - regenerateMessage(UUID conversationId, UUID messageId, UUID userId): Flux<String>
  - getMessages(UUID userId, UUID conversationId, Pageable pageable): Page<Message>
  - searchConversations(UUID userId, String query, Pageable pageable): Page<Conversation>
  - renameConversation(UUID conversationId, UUID userId, String newTitle): Conversation
  - generateTitle(UUID conversationId, String firstUserMessage): void — @Async

### === InferenceService === (Interface)
Methods:
  - chat(List<OllamaMessage>, UUID): String
  - streamChat(List<OllamaMessage>, UUID): Flux<String>
  - streamChatWithThinking(List<OllamaMessage>, UUID): Flux<InferenceChunk>
  - embed(String): float[]
  - isAvailable(): boolean
  - listModels(): List<InferenceModelInfo>
  - getActiveModel(): InferenceModelInfo

### === OllamaInferenceService ===
Implements InferenceService. @ConditionalOnProperty app.inference.provider=ollama (default)
Injects: OllamaService, SystemConfigService
Additional: warmThinkingCache(String model): void

### === LlamaServerInferenceService ===
Implements InferenceService. @ConditionalOnProperty app.inference.provider=llama-server. @Primary
Injects: LlamaServerProperties, RestClient, WebClient

### === OllamaService ===
Injects: RestClient ("ollamaRestClient"), WebClient ("ollamaWebClient"), ObjectMapper
Public Methods:
  - isAvailable(): boolean
  - listModels(): List<OllamaModelInfo>
  - chat(OllamaChatRequest): OllamaChatResponse
  - chatStream(OllamaChatRequest): Flux<OllamaChatChunk>
  - embed(String): float[]
  - embedBatch(List<String>): List<float[]>
  - getModelCapabilities(String): List<String>
  - logLoadedModels(): void

### === LlamaServerProcessService ===
Implements ApplicationRunner, DisposableBean
Injects: InferenceProperties, LlamaServerProperties, SystemConfigService, ProcessBuilderFactory
Public Methods:
  - run(ApplicationArguments): void — starts llama-server on boot
  - start(): void — starts process
  - stop(): void — graceful shutdown
  - restart(): void
  - switchModel(String filename): LlamaServerStatusDto
  - getStatus(): LlamaServerStatusDto
  - getRecentLogLines(int): List<String>
  - monitorHealth(): void — @Scheduled health check
  - destroy(): void — DisposableBean cleanup

### === AgentService ===
Injects: OllamaService, SkillExecutorService, ObjectMapper, SystemConfigService
Public Methods:
  - executeTask(UUID userId, UUID conversationId, String taskDescription): AgentTaskResult

### === ChatExportService ===
Injects: ChatService, MessageRepository, FileStorageService, KnowledgeDocumentRepository, KnowledgeService
Public Methods:
  - generateConversationPdf(UUID conversationId, UUID userId): byte[]
  - saveConversationToLibrary(UUID conversationId, UUID userId): KnowledgeDocumentDto — @Transactional

### === ContextWindowService ===
Injects: MessageRepository, SystemConfigService
Public Methods:
  - prepareMessages(UUID conversationId, String systemPrompt, String newUserMessage): List<OllamaMessage>

### === SystemPromptBuilder ===
Injects: RagService
Public Methods:
  - build(User user, String instanceName): String
  - build(User user, String instanceName, RagContext ragContext): String

### === AuthService ===
Injects: UserRepository, JwtService, PasswordEncoder, @Value profile
Public Methods:
  - register(RegisterRequest): AuthResponse — @Transactional
  - login(LoginRequest): AuthResponse — @Transactional
  - refresh(String refreshToken): AuthResponse
  - logout(String token): void
  - isTokenBlacklisted(String): boolean
  - changePassword(UUID, ChangePasswordRequest): void — @Transactional

### === JwtService ===
Injects: @Value jwt.secret, expiration-ms, refresh-expiration-ms
Public Methods:
  - generateAccessToken(UserDetails): String
  - generateRefreshToken(UserDetails): String
  - extractUsername(String): String
  - extractExpiration(String): Date
  - isTokenValid(String, UserDetails): boolean
  - isTokenExpired(String): boolean
  - getAccessExpirationMs(): long

### === UserService ===
Injects: UserRepository
Public Methods:
  - listUsers(int, int): Page<UserSummaryDto>
  - getUserById(UUID): UserDetailDto
  - updateUser(UUID, String, String, Role): UserDetailDto — @Transactional
  - deactivateUser(UUID): void — @Transactional
  - deleteUser(UUID): void — @Transactional

### === KnowledgeService ===
Injects: KnowledgeDocumentRepository, KnowledgeChunkRepository, VectorDocumentRepository, FileStorageService, IngestionService, OcrService, ChunkingService, EmbeddingService, ApplicationContext, UserRepository
Public Methods:
  - upload(UUID, MultipartFile): KnowledgeDocumentDto — @Transactional
  - processDocumentAsync(UUID): void — @Async
  - listDocuments(UUID, Pageable): Page<KnowledgeDocumentDto>
  - listDocuments(UUID, String scope, Pageable): Page<KnowledgeDocumentDto> — MINE/SHARED filter
  - getDocument(UUID, UUID): KnowledgeDocumentDto
  - updateDisplayName(UUID, UUID, String): KnowledgeDocumentDto
  - updateSharing(UUID, UUID, boolean): KnowledgeDocumentDto — @Transactional
  - deleteDocument(UUID, UUID): void — @Transactional
  - retryProcessing(UUID, UUID): KnowledgeDocumentDto
  - getDocumentContent(UUID, UUID): DocumentContentDto
  - getDocumentForDownload(UUID, UUID): KnowledgeDocument
  - createFromEditor(UUID, String, String deltaJson): KnowledgeDocumentDto — @Transactional
  - updateContent(UUID, UUID, String deltaJson): KnowledgeDocumentDto — @Transactional
  - deleteAllForUser(UUID): void — @Transactional

### === MemoryService ===
Injects: MemoryRepository, VectorDocumentRepository, EmbeddingService, SystemConfigService
Public Methods:
  - createMemory(UUID, String, MemoryImportance, String, UUID): Memory — @Transactional
  - findRelevantMemories(UUID, String, int): List<Memory> — @Transactional
  - findRelevantMemories(UUID, String, int, float[]): List<Memory> — with pre-computed embedding
  - searchMemoriesWithScores(UUID, String, int): List<MemorySearchResultDto>
  - getMemory(UUID, UUID): Memory
  - updateImportance(UUID, UUID, MemoryImportance): Memory — @Transactional
  - updateTags(UUID, UUID, String): Memory — @Transactional
  - deleteMemory(UUID, UUID): void — @Transactional
  - deleteAllMemoriesForUser(UUID): void — @Transactional
  - exportMemories(UUID): List<Memory>
  - getMemories(UUID, MemoryImportance, String, Pageable): Page<Memory>
  - toDto(Memory): MemoryDto

### === EmbeddingService ===
Injects: OllamaService
Public Methods:
  - embed(String): float[]
  - embedAndFormat(String): String — pgvector string format
  - embedBatch(List<String>): List<float[]>
  - cosineSimilarity(float[], float[]): float
  - static formatEmbedding(float[]): String

### === RagService ===
Injects: MemoryService, SemanticSearchService, SystemConfigService, EmbeddingService
Public Methods:
  - buildRagContext(UUID, String): RagContext — builds from memories + knowledge
  - formatContextBlock(RagContext): String

### === MemoryExtractionService ===
Injects: OllamaService, MemoryService, ObjectMapper, SystemConfigService
Public Methods:
  - storeFrontierKnowledge(UUID, UUID, String, String): void — @Async
  - extractAndStore(UUID, UUID, String, String): void — @Async

### === SummarizationService ===
Injects: ConversationRepository, MessageRepository, OllamaService, MemoryService, MemoryRepository, SystemConfigService
Public Methods:
  - summarizeConversation(UUID, UUID): Memory
  - scheduledNightlySummarization(): void — @Scheduled(cron="0 0 2 * * *")

### === FrontierApiRouter ===
Injects: List<FrontierApiClient>, ExternalApiSettingsService
Public Methods:
  - complete(String systemPrompt, String userMessage): Optional<String>
  - isAnyAvailable(): boolean
  - getAvailableProviders(): List<FrontierProvider>

### === ClaudeFrontierClient / OpenAiFrontierClient / GrokFrontierClient ===
Each implements FrontierApiClient.
Public Methods: getProvider(), isAvailable(), complete(String, String): Optional<String>

### === ScheduledEventService ===
Injects: ScheduledEventRepository
Public Methods: listEvents, getEvent, createEvent, updateEvent, deleteEvent, toggleEvent, deleteAllForUser, calculateNextFireAt

### === EbookService ===
Injects: EbookRepository, LibraryProperties, CalibreConversionService
Public Methods: listEbooks(search, format, pageable), getEbook(id), uploadEbook(file, userId), deleteEbook(id), getEbookForDownload(id)

### === GutenbergService ===
Injects: WebClient, EbookRepository, LibraryProperties
Public Methods: browse(page), search(query, page), getBook(id), importBook(id, userId)

### === KiwixCatalogService ===
Injects: KiwixProperties, WebClient
Public Methods: browse(category, page, size), search(query, page, size)

### === KiwixDownloadService ===
Injects: KiwixProperties, WebClient, ZimFileService, KiwixProcessService, TaskExecutor
Public Methods: startDownload(bookId, fileName), getDownloadStatus(id), getAllDownloads(), cancelDownload(id)

### === KiwixProcessService ===
Injects: KiwixProperties, ProcessBuilderFactory, ZimFileRepository
Public Methods: startKiwix(), stopKiwix(), getStatus(), getKiwixUrl(), isRunning(), isHealthy(), installKiwix()
@EventListener(ApplicationReadyEvent.class): startKiwix()

### === ZimFileService ===
Injects: ZimFileRepository, KiwixProperties
Public Methods: registerZimFile(filename, displayName, description, fileSizeBytes, kiwixBookId), listAll(), delete(id), uploadZim(file, userId)

### === McpTokenService ===
Injects: McpApiTokenRepository, PasswordEncoder
Public Methods: createToken(userId, name), listTokens(userId), revokeToken(tokenId, userId), validateToken(rawToken): Optional<McpApiToken>

### === McpToolsService ===
Injects: ChatService, MemoryService, KnowledgeService, SensorService, EmbeddingService
@Tool annotated methods exposed via MCP: searchMemory, addMemory, listSensors, latestSensorReading, searchKnowledge

### === ModelCatalogService ===
Injects: WebClient (huggingface.co)
Public Methods: searchModels(query, format, limit), getModelDetails(author, modelId), getModelFiles(author, modelId)

### === ModelDownloadService ===
Injects: InferenceProperties, WebClient, ModelDownloadProgressRegistry, ExternalApiSettingsService
Public Methods: startDownload(author, modelId, filename), cancelDownload(downloadId), listLocalModels(), deleteLocalModel(filename)

### === ModelDownloadProgressRegistry ===
Public Methods: registerDownload(id, progress), getProgress(id), getAllDownloads(), removeDownload(id)

### === QuantizationRecommendationService ===
Public Methods: recommend(ramGb): QuantizationType, describeAll(): List<QuantDescription>

### === DeviceRegistrationService ===
Injects: DeviceRegistrationRepository
Public Methods: register(userId, request), getDevices(userId), unregister(userId, deviceId)

### === MqttPublisherService ===
Injects: MqttConfig properties
Public Methods: publish(userId, payload): void
Conditional: only active when app.mqtt.enabled=true

### === AuditService ===
Injects: AuditLogRepository
Public Methods: log(AuditLog), getLogs(outcome, page, size), getUserLogs(userId, page, size), getLogsBetween(from, to, page, size), deleteOldLogs(before), deleteForUser(userId)

### === DataExportService ===
Injects: Multiple repositories across all domains
Public Methods: exportUserData(userId, passphrase): byte[] — encrypted ZIP

### === DataWipeService ===
Injects: Multiple repositories across all domains, FileStorageService
Public Methods: wipeAllDataForUser(userId): WipeResult — @Transactional, wipeSelfData(userId): WipeResult

### === FortressService ===
Injects: SystemConfigService, ApModeService
Public Methods: enableFortress(userId), disableFortress(userId), getStatus(): FortressStatus, isFortressActive(): boolean

### === SovereigntyReportService ===
Injects: SystemConfigService, AuditLogRepository, multiple count repositories
Public Methods: generateReport(userId): SovereigntyReport

### === InsightService ===
Injects: InsightRepository
Public Methods: getInsights(userId, category, pageable), markAsRead(insightId, userId), dismiss(insightId, userId), getUnreadCount(userId), save(insight), deleteAllForUser(userId)

### === InsightGeneratorService ===
Injects: InferenceService, SensorReadingRepository, MemoryRepository, ConversationRepository, InsightService, UserRepository
Public Methods: generateInsightsForUser(userId): List<Insight>

### === NightlyInsightJob ===
Injects: UserRepository, InsightGeneratorService
Public Methods: run(): void — @Scheduled(cron="0 0 3 * * *")

### === NotificationService ===
Injects: NotificationRepository, MqttPublisherService, NotificationSseRegistry
Public Methods: send(userId, title, body, type, severity), getNotifications(userId, unreadOnly, pageable), markRead(id, userId), markAllRead(userId), getUnreadCount(userId), delete(id, userId), deleteAllForUser(userId)

### === PatternAnalysisService ===
Injects: SensorReadingRepository, MemoryRepository
Public Methods: analyzePatterns(userId): PatternSummary

### === SystemHealthMonitor ===
Injects: InferenceService, SensorRepository, SensorReadingRepository, NotificationService, SystemConfigService, UserRepository
Public Methods: runHealthCheck(): void — @Scheduled

### === SensorService ===
Injects: SensorRepository, SensorReadingRepository, SensorPollingService
Public Methods: listSensors(userId), getSensor(sensorId, userId), registerSensor(userId, request), deleteSensor(sensorId, userId), startSensor(sensorId, userId), stopSensor(sensorId, userId), getLatestReading(sensorId, userId), getReadingHistory(sensorId, userId, hours, pageable), updateThresholds(sensorId, userId, request), testConnection(request), listAvailablePorts(), getAverageValue(sensorId, since)

### === SensorPollingService ===
Injects: SerialPortService, SensorReadingRepository, SensorRepository, SseEmitterRegistry, NotificationService
Public Methods: startPolling(sensor), stopPolling(sensorId), isPolling(sensorId)

### === SensorStartupService ===
Injects: SensorRepository, SensorPollingService
Public Methods: startActiveSensors(): void — @EventListener(ApplicationReadyEvent.class)

### === SerialPortService ===
Public Methods: readLine(portPath, baudRate, timeoutMs): String, listPorts(): List<String>

### === SseEmitterRegistry ===
Public Methods: register(sensorId): SseEmitter, send(sensorId, data), remove(sensorId, emitter)

### === UserSettingsService ===
Injects: UserSettingsRepository, UserRepository
Public Methods: getSettings(userId), updateSettings(userId, request)

### === ExternalApiSettingsService ===
Injects: ExternalApiSettingsRepository
Public Methods: getSettings(), updateSettings(request), getOrCreate(): ExternalApiSettings

### === SkillExecutorService ===
Injects: SkillRepository, SkillExecutionRepository, List<BuiltInSkill>, InferenceService, ObjectMapper
Public Methods: execute(userId, request): SkillExecution

### === SkillSeederService ===
Injects: SkillRepository
Public Methods: seedBuiltInSkills(): void — @EventListener(ApplicationReadyEvent.class)

### === BuiltInSkill === (Interface)
Methods: getName(), execute(userId, params, objectMapper): String

### === Built-In Skills (6) ===
DocumentSummarizerSkill, InventoryTrackerSkill, RecipeGeneratorSkill, ResourceCalculatorSkill, TaskPlannerSkill, WeatherQuerySkill
Each implements BuiltInSkill with respective domain logic.

### === SystemConfigService ===
Injects: SystemConfigRepository
Public Methods: getConfig(), initialize(InitializeRequest, UUID), isInitialized(), getAiModel(), updateAiSettings(request), updateStorageSettings(request), getActiveModelFilename(), setActiveModelFilename(String)

### === ApModeService ===
Injects: SystemConfigService
Public Methods: isApModeEnabled(), enableApMode(), disableApMode(), isWifiConfigured(), scanWifiNetworks(), connectToWifi(request), getWifiStatus()

### === FactoryResetService ===
Injects: SystemConfigRepository + all domain repositories, FileStorageService
Public Methods: factoryReset(confirmPhrase): void — @Transactional, deletes ALL data

### === NetworkTransitionService ===
Injects: ApModeService, SystemConfigService
Public Methods: finalizeSetup(): void

### === UsbResetWatcherService ===
Injects: FactoryResetService
Public Methods: checkForResetTrigger(): void — @Scheduled(fixedDelay=30000)

---

## 10. Controller / API Layer — Method Signatures Only

### === ChatController ===
Base Path: `/api/chat`
Injects: ChatService, MessageRepository, ChatExportService
Endpoints:
  - createConversation() → chatService.createConversation()
  - listConversations() → chatService.getConversations()
  - searchConversations() → chatService.searchConversations()
  - getConversation() → chatService.getConversation()
  - deleteConversation() → chatService.deleteConversation()
  - archiveConversation() → chatService.archiveConversation()
  - renameConversation() → chatService.renameConversation()
  - sendMessage() → chatService.sendMessage() or chatService.streamMessage() [SSE]
  - listMessages() → chatService.getMessages()
  - editMessage() → chatService.editMessage()
  - deleteMessage() → chatService.deleteMessage()
  - branchConversation() → chatService.branchConversation()
  - exportPdf() → chatExportService.generateConversationPdf()
  - saveToLibrary() → chatExportService.saveConversationToLibrary()
  - regenerateMessage() → chatService.regenerateMessage() [SSE]

### === ModelController ===
Base Path: `/api/models`
Injects: InferenceService, SystemConfigService
Endpoints:
  - listModels() → inferenceService.listModels() [public]
  - getActiveModel() → inferenceService.getActiveModel()
  - getHealth() → inferenceService.isAvailable() [public]

### === JudgeController ===
Base Path: `/api/ai/judge` | Auth: @PreAuthorize OWNER/ADMIN
Injects: JudgeModelProcessService, JudgeInferenceService, ExternalApiSettingsService, OllamaService, SystemConfigService
Endpoints:
  - getStatus() → judgeModelProcessService.getStatus()
  - start() → judgeModelProcessService.start()
  - stop() → judgeModelProcessService.stop()
  - test() → judgeInferenceService.evaluate() (auto-generates response if not provided)

### === AuthController ===
Base Path: `/api/auth`
Injects: AuthService
Endpoints:
  - register() → authService.register() [public]
  - login() → authService.login() [public]
  - refresh() → authService.refresh() [public]
  - logout() → authService.logout()

### === UserController ===
Base Path: `/api/users`
Injects: UserService
Endpoints:
  - listUsers() → userService.listUsers() [@PreAuthorize OWNER/ADMIN]
  - getUser() → userService.getUserById()
  - updateUser() → userService.updateUser() [@PreAuthorize OWNER/ADMIN]
  - deactivateUser() → userService.deactivateUser() [@PreAuthorize OWNER]
  - deleteUser() → userService.deleteUser() [@PreAuthorize OWNER]

### === EnrichmentController ===
Base Path: `/api/enrichment`
Injects: WebFetchService, WebSearchService, ClaudeApiService, ExternalApiSettingsService
Endpoints:
  - fetchUrl() → webFetchService.fetchUrl()
  - search() → webSearchService.search() / searchAndStore()
  - getStatus() → checks Claude/Brave availability

### === ScheduledEventController ===
Base Path: `/api/events`
Injects: ScheduledEventService
Endpoints: CRUD (list, get, create, update, delete, toggle)

### === KnowledgeController ===
Base Path: `/api/knowledge`
Injects: KnowledgeService, SemanticSearchService, SystemConfigService, FileStorageService
Endpoints:
  - uploadDocument() → knowledgeService.upload()
  - listDocuments() → knowledgeService.listDocuments(scope=MINE|SHARED)
  - getDocument() → knowledgeService.getDocument()
  - updateDisplayName() → knowledgeService.updateDisplayName()
  - updateSharing() → knowledgeService.updateSharing()
  - deleteDocument() → knowledgeService.deleteDocument()
  - retryProcessing() → knowledgeService.retryProcessing()
  - downloadDocument() → knowledgeService.getDocumentForDownload()
  - getDocumentContent() → knowledgeService.getDocumentContent()
  - createDocument() → knowledgeService.createFromEditor()
  - updateDocumentContent() → knowledgeService.updateContent()
  - searchKnowledge() → semanticSearchService.search()

### === LibraryController ===
Base Path: `/api/library`
Injects: ZimFileService, EbookService, GutenbergService, KiwixProcessService, KiwixCatalogService, KiwixDownloadService
Endpoints: ZIM upload/list/delete, Kiwix lifecycle (status/install/start/stop), Kiwix catalog browse/search/download, eBook CRUD, Gutenberg browse/search/import

### === McpDiscoveryController ===
Base Path: `/api/mcp` | Auth: @PreAuthorize OWNER
Endpoints: getClaudeDesktopConfig()

### === McpTokenController ===
Base Path: `/api/mcp/tokens` | Auth: @PreAuthorize OWNER
Injects: McpTokenService
Endpoints: createToken(), listTokens(), revokeToken()

### === MemoryController ===
Base Path: `/api/memory`
Injects: MemoryService
Endpoints: list, get, delete, updateImportance, updateTags, search, export

### === ModelDownloadController ===
Base Path: `/api/models`
Injects: ModelCatalogService, ModelDownloadService, ModelDownloadProgressRegistry, SystemConfigService
Endpoints: catalog search/details/files, download start/progress/cancel, local model list/delete, active model set, server status/restart

### === DeviceRegistrationController ===
Base Path: `/api/notifications/devices`
Injects: DeviceRegistrationService
Endpoints: register, list, unregister

### === PrivacyController ===
Base Path: `/api/privacy`
Injects: FortressService, AuditService, SovereigntyReportService, DataExportService, DataWipeService
Endpoints: fortress status/enable/disable, sovereignty-report, audit-logs, export, wipe/wipe-self

### === ProactiveController ===
Base Paths: `/api/insights`, `/api/notifications`
Injects: InsightService, InsightGeneratorService, NotificationService, NotificationSseRegistry
Endpoints: insights (list, generate, read, dismiss, unread-count), notifications (list, read, read-all, unread-count, delete, stream SSE)

### === SensorController ===
Base Path: `/api/sensors`
Injects: SensorService, SseEmitterRegistry
Endpoints: CRUD, start/stop, latest/history, thresholds, test, ports, stream SSE

### === UserSettingsController ===
Base Path: `/api/users/me/settings`
Injects: UserSettingsService
Endpoints: get, update

### === ExternalApiSettingsController ===
Base Path: `/api/settings/external-apis` | Auth: @PreAuthorize OWNER
Injects: ExternalApiSettingsService
Endpoints: get, update

### === SkillController ===
Base Path: `/api/skills`
Injects: SkillRepository, SkillExecutionRepository, InventoryItemRepository, SkillExecutorService
Endpoints: skills (list, get, toggle, create), execute, executions, inventory CRUD

### === SystemController ===
Base Path: `/api/system`
Injects: SystemConfigService, AuthService, NetworkTransitionService, FactoryResetService, LlamaServerProcessService
Endpoints: status [public], initialize [public], finalize-setup [public], ai-settings, storage-settings, factory-reset, llama-server status/switch

### === CaptivePortalController ===
Base Paths: `/setup`, `/api/setup`
Endpoints: setup pages (welcome, wifi, account, confirm), wifi scan/connect/status [all public]

---

## 11. Security Configuration

```
Authentication: JWT (JJWT 0.12.6, HMAC-SHA)
Token issuer/validator: Internal (JwtService)
Password encoder: BCrypt (Spring Security default)

Public endpoints (no auth required):
  - /api/auth/** (register, login, refresh)
  - /api/system/** (status, initialize, finalize-setup)
  - /api/models (list, health)
  - /setup/** (captive portal pages)
  - /api/setup/** (wifi scan/connect/status)
  - /mcp/** (MCP SSE — uses separate McpAuthFilter with Bearer token)
  - /v3/api-docs/**, /swagger-ui/**
  - /actuator/health

Protected endpoints (patterns):
  - /api/users/** → authenticated (OWNER/ADMIN for list/update/delete)
  - /api/chat/** → authenticated
  - /api/memory/** → authenticated
  - /api/knowledge/** → authenticated
  - /api/library/** → authenticated (OWNER/ADMIN for uploads/deletes)
  - /api/skills/** → authenticated (OWNER/ADMIN for create/toggle)
  - /api/sensors/** → authenticated
  - /api/events/** → authenticated
  - /api/insights/** → authenticated
  - /api/notifications/** → authenticated
  - /api/privacy/** → authenticated (OWNER/ADMIN for fortress/wipe)
  - /api/settings/external-apis/** → ROLE_OWNER
  - /api/mcp/tokens/** → ROLE_OWNER
  - /api/ai/judge/** → ROLE_OWNER or ROLE_ADMIN
  - /api/models/download/** → ROLE_OWNER

CORS: All origins allowed (development), all methods, all headers
CSRF: Disabled (stateless JWT API)
Session: STATELESS
Rate limiting: Bucket4j, 10 req/min auth endpoints, 200 req/min API endpoints
```

---

## 12. Custom Security Components

### === JwtAuthFilter ===
Extends: OncePerRequestFilter
Purpose: Extracts and validates JWT from Authorization header
Extracts token from: Authorization header (Bearer prefix)
Validates via: JwtService.isTokenValid() + AuthService.isTokenBlacklisted()
Sets SecurityContext: YES — UsernamePasswordAuthenticationToken

### === RateLimitingFilter ===
Extends: OncePerRequestFilter | @Order(3)
Purpose: Per-IP rate limiting using Bucket4j ConcurrentHashMap
Two tiers: 10/min for /api/auth/**, 200/min for other /api/**
Returns: HTTP 429 with ApiResponse error JSON

### === CaptivePortalRedirectFilter ===
Extends: OncePerRequestFilter | @Order(1)
Purpose: Redirects to /setup when AP mode is active
Excludes: /api/, /setup, /actuator, /v3/api-docs, /swagger-ui

### === MdcFilter ===
Extends: OncePerRequestFilter | @Order(2)
Purpose: Populates SLF4J MDC with requestId (UUID), username, userId
Clears: MDC in finally block

### === RequestResponseLoggingFilter ===
Extends: OncePerRequestFilter | @Order(4)
Purpose: Logs HTTP method, URI, status, duration at DEBUG level
Skips: /actuator, /setup paths

### === McpAuthFilter ===
Extends: OncePerRequestFilter
Purpose: Authenticates MCP requests with Bearer token
Applies to: /mcp/** paths only
Validates via: McpTokenService.validateToken()
Sets SecurityContext: McpAuthentication with ROLE_MCP_CLIENT

### === McpAuthentication ===
Extends: AbstractAuthenticationToken
Authority: ROLE_MCP_CLIENT
Principal: "mcpClient"
Fields: ownerUserId (UUID), tokenId (UUID)

### === AuditAspect ===
@Aspect @Component
Pointcut: execution(public * com.myoffgridai.*.controller..*(..))
Purpose: AOP around-advice that logs all controller method invocations
Captures: userId, username, HTTP method/path, IP, User-Agent, status, outcome, duration

**Filter chain order:** CaptivePortal(1) → MDC(2) → RateLimit(3) → RequestLogging(4) → McpAuth → JwtAuth → Spring Security

---

## 13. Exception Handling & Error Responses

### === GlobalExceptionHandler ===
@RestControllerAdvice: YES

Exception Mappings:
  - MethodArgumentNotValidException → 400 (field validation errors)
  - UsernameNotFoundException → 404
  - BadCredentialsException → 401 ("Invalid credentials")
  - AccessDeniedException → 403 ("Access denied")
  - EntityNotFoundException → 404 (custom)
  - DuplicateResourceException → 409 (custom)
  - IllegalArgumentException → 400
  - FortressActiveException → 403
  - OllamaUnavailableException → 503
  - OllamaInferenceException → 502
  - EmbeddingException → 503
  - StorageException → 500
  - UnsupportedFileTypeException → 400
  - OcrException → 500
  - SkillDisabledException → 400
  - ApModeException → 500
  - SensorConnectionException → 502
  - MaxUploadSizeExceededException → 413
  - AsyncRequestNotUsableException → void (client disconnect)
  - AsyncRequestTimeoutException → void (SSE timeout)
  - Exception (catch-all) → 500

Standard error response format (ApiResponse):
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2026-03-21T...",
  "requestId": "uuid"
}
```

Custom exception classes (11):
  - ApModeException, DuplicateResourceException, EmbeddingException
  - EntityNotFoundException, FortressActiveException, FortressOperationException
  - InitializationException, OcrException, OllamaInferenceException
  - OllamaUnavailableException, SensorConnectionException
  - SkillDisabledException, StorageException, UnsupportedFileTypeException

---

## 14. Mappers / DTOs

No formal MapStruct or ModelMapper usage. All mapping is **manual** — inline `toDto()` and `toEntity()` methods within service classes.

Key mapping patterns:
  - `KnowledgeService.toDto(KnowledgeDocument)` → KnowledgeDocumentDto
  - `MemoryService.toDto(Memory)` → MemoryDto
  - `UserService.toSummary(User)` → UserSummaryDto
  - `UserService.toDetail(User)` → UserDetailDto
  - `AuthService.toUserSummary(User)` → UserSummaryDto
  - `AuthService.buildAuthResponse(User, tokens)` → AuthResponse

DTO structures are documented in the OpenAPI spec.

---

## 15. Utility Classes & Shared Components

### === AesEncryptionUtil ===
Path: `common/util/AesEncryptionUtil.java`
Algorithm: AES-256-GCM with 12-byte random IV, 128-bit tag
Key source: `app.encryption.key` (64-char hex → 32 bytes)
Methods:
  - encrypt(String plaintext): String (Base64 with IV prepended)
  - decrypt(String ciphertext): String
Used by: AesAttributeConverter (transparent entity field encryption)

### === AesAttributeConverter ===
Path: `common/util/AesAttributeConverter.java`
Implements: AttributeConverter<String, String>
Purpose: JPA converter for transparent AES-256-GCM encryption of entity fields
Used by: ExternalApiSettings (anthropicApiKey, braveApiKey, huggingFaceToken, grokApiKey, openAiApiKey)

### === TokenCounter ===
Path: `common/util/TokenCounter.java`
Methods:
  - estimateTokens(String text): int (approximation: 1 token ≈ 4 chars)
  - truncateToTokenLimit(List<OllamaMessage>, int maxTokens): List<OllamaMessage>
Used by: ContextWindowService, SystemPromptBuilder

### === DeltaJsonUtils ===
Path: `knowledge/util/DeltaJsonUtils.java`
Purpose: Quill Delta JSON ↔ plain text conversion
Methods:
  - textToDeltaJson(String): String
  - deltaJsonToText(String): String
Used by: KnowledgeService (editor document create/update)

### === ApiResponse<T> ===
Path: `common/response/ApiResponse.java`
Generic API response envelope used by ALL controllers.
Fields: success, message, data, timestamp, requestId, totalElements, page, size
Static methods: success(T), success(T, String), error(String), paginated(T, long, int, int)

### === AppConstants ===
Path: `config/AppConstants.java`
Centralized constants: ports, JWT durations, API paths, role names, AP mode config, password requirements, Ollama defaults, RAG parameters, rate limits, knowledge limits, skill names, judge defaults, library config.

### === InferenceProperties ===
Path: `config/InferenceProperties.java`
@ConfigurationProperties prefix: app.inference
Fields: manageProcess, llamaServerBinary, modelsDir, activeModel, port, contextSize, gpuLayers, threads, healthCheckIntervalSeconds, etc.

### === LlamaServerProperties ===
Path: `config/LlamaServerProperties.java`
@ConfigurationProperties prefix: app.llama-server
Fields: binary, modelsDir, activeModel, port, contextSize, gpuLayers, threads, startupTimeoutSeconds, healthCheckIntervalSeconds

### === KiwixProperties / LibraryProperties ===
Path: `library/config/`
@ConfigurationProperties for Kiwix (binary-path, port, manage-process, catalog-base-url) and Library (zim-directory, ebook-directory, kiwix-url, calibre-content-server-url, gutenberg-api-url, max-upload-size-mb)

### === JudgeProperties ===
Path: `ai/judge/JudgeProperties.java`
@ConfigurationProperties prefix: app.judge
Fields: enabled, modelFilename, port, scoreThreshold, timeoutSeconds, contextSize

---

## 16. Database Schema (Live)

Database not available via Docker at audit time. Schema is managed by Hibernate (`ddl-auto: update` in dev, `validate` in prod).

**Tables (derived from @Entity annotations):**
conversations, messages, users, scheduled_events, knowledge_documents, knowledge_chunks, ebooks, zim_files, mcp_api_tokens, memories, vector_document, device_registrations, audit_logs, insights, notifications, sensors, sensor_readings, user_settings, external_api_settings, skills, skill_executions, inventory_items, planned_tasks, system_config

**pgvector extension:** Required — `vector_document.embedding` column uses `vector(768)` type.

**Key indexes (from @Table annotations):**
- idx_event_user_id, idx_event_enabled_type
- idx_knowledge_doc_user_id, idx_knowledge_doc_shared
- idx_knowledge_chunk_doc_id, idx_knowledge_chunk_user_id
- idx_ebooks_gutenberg_id
- idx_mcp_token_created_by
- idx_vector_doc_user_source_type
- idx_audit_user_timestamp, idx_audit_timestamp
- idx_insight_user_id
- idx_notification_user_id
- idx_sensor_user_id, idx_sensor_port_path (unique)
- idx_sensor_reading_sensor_recorded
- idx_skill_name (unique), idx_skill_category
- idx_skill_exec_user_id, idx_skill_exec_skill_id
- idx_inventory_user_id, idx_inventory_category
- idx_planned_task_user_id
- idx_device_registration_user_id, uk_device_registration_user_device (unique constraint)

---

## 17. Message Broker Configuration

<!-- MESSAGE BROKER DETECTION -->
No RabbitMQ or Kafka detected.

**MQTT (Eclipse Paho):**
```
Broker: MQTT (Eclipse Paho v3 client)
Connection: tcp://localhost:1883, client-id: myoffgridai-server
Enabled: false (dev/test), true (prod) — controlled by app.mqtt.enabled

Publisher:
  - MqttPublisherService.publish(UUID userId, NotificationPayload payload)
    Publishes to: myoffgridai/notifications/{userId}
    Message type: NotificationPayload (JSON)

Consumers: None on server side — MQTT is publish-only to mobile clients
```

---

## 18. Cache Layer

No Redis or caching layer detected. No @Cacheable, @CacheEvict, or CacheManager usage.

---

## 19. Environment Variable Inventory

<!-- ENVIRONMENT VARIABLE INVENTORY -->
```
Variable              | Used In                    | Default           | Required in Prod
----------------------|----------------------------|-------------------|------------------
DB_URL                | application-prod.yml       | jdbc:...localhost  | YES
DB_USERNAME           | application-prod.yml       | myoffgridai       | YES
DB_PASSWORD           | application-prod.yml       | myoffgridai       | YES
JWT_SECRET            | application-prod.yml       | (none)            | YES
ENCRYPTION_KEY        | application-prod.yml       | (none)            | YES
INFERENCE_PROVIDER    | application-prod.yml       | llama-server      | NO
INFERENCE_BASE_URL    | application-prod.yml       | http://localhost:1234 | NO
INFERENCE_MODEL       | application-prod.yml       | (Qwen default)    | NO
INFERENCE_EMBED_MODEL | application-prod.yml       | nomic-embed-text  | NO
LLAMA_SERVER_BINARY   | application-prod.yml       | /usr/local/bin/llama-server | NO
MODELS_DIR            | application-prod.yml       | ./models          | NO
ACTIVE_MODEL          | application-prod.yml       | (empty)           | NO
INFERENCE_PORT        | application-prod.yml       | 1234              | NO
INFERENCE_CONTEXT_SIZE| application-prod.yml       | 4096              | NO
INFERENCE_GPU_LAYERS  | application-prod.yml       | 99                | NO
INFERENCE_THREADS     | application-prod.yml       | 8                 | NO
INFERENCE_TIMEOUT     | application-prod.yml       | 120               | NO
INFERENCE_MAX_TOKENS  | application-prod.yml       | 4096              | NO
INFERENCE_TEMPERATURE | application-prod.yml       | 0.7               | NO
HEALTH_CHECK_INTERVAL | application-prod.yml       | 30                | NO
RESTART_DELAY         | application-prod.yml       | 5                 | NO
STARTUP_TIMEOUT       | application-prod.yml       | 120               | NO
```

---

## 20. Service Dependency Map

<!-- SERVICE DEPENDENCY MAP -->
```
This Service → Depends On
--------------------------
PostgreSQL + pgvector: localhost:5432 (required)
Ollama: localhost:11434 (when provider=ollama)
llama-server: localhost:1234 (when provider=llama-server, managed or external)
Kiwix: localhost:8888 (managed process, optional)
Calibre Content Server: localhost:8081 (optional, for ebook conversion)
MQTT Broker: localhost:1883 (optional, prod-only)
Gutendex API: https://gutendex.com (optional, Project Gutenberg)
Kiwix Catalog: https://library.kiwix.org (optional, ZIM downloads)
Anthropic Claude API: https://api.anthropic.com (optional, enrichment/frontier)
OpenAI API: https://api.openai.com (optional, frontier)
xAI Grok API: https://api.x.ai (optional, frontier)
Brave Search API: https://api.search.brave.com (optional, web search)
HuggingFace Hub: https://huggingface.co (optional, model downloads)

Downstream Consumers:
- Flutter mobile app (REST + SSE + MQTT)
- Claude Desktop (via MCP SSE transport)
```

Standalone appliance server — no inter-service dependencies. All external APIs are optional and gracefully degrade when unavailable.

---

## 21. Known Technical Debt & Issues

### TODO/PLACEHOLDER/STUB Scan
**PASS — 0 TODO/FIXME/placeholder/stub patterns found in src/main/.**

### Issues Discovered During Audit

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| CRITICAL: spring-security-web CVE (cache containing sensitive info) | spring-security-web@6.4.13 | Critical | Fix: upgrade to 6.5.9 (requires Spring Boot 3.5.x) |
| HIGH: tomcat-embed-core incorrect authorization | tomcat-embed-core@10.1.50 | High | Fix: upgrade to 9.0.114+ |
| HIGH: Spring WebMVC directory traversal | spring-webmvc@6.2.15 | High | Fix: upgrade to 6.2.17 |
| HIGH: Spring WebFlux directory traversal | spring-webflux@6.2.15 | High | Fix: upgrade to 6.2.17 |
| HIGH: Spring Boot Actuator auth bypass (x2) | spring-boot-actuator@3.4.13 | High | Fix: upgrade to 3.5.12 |
| HIGH: Spring Boot Actuator Autoconfigure auth bypass (x2) | spring-boot-actuator-autoconfigure@3.4.13 | High | Fix: upgrade to 3.5.12 |
| LOW: logback-core external initialization | logback-core@1.5.22 | Low | Fix: upgrade to 1.5.25 |
| LOW: Spring Web injection (x2) | spring-web/spring-webmvc@6.2.15 | Low | Fix: upgrade to 6.2.17 |
| Dev secrets hardcoded | application.yml (dev profile) | Low | Expected for dev; prod uses env vars |
| No @Version optimistic locking | All entities | Medium | Consider adding for concurrent update safety |
| No CI/CD pipeline | Project root | Medium | No .github/workflows or equivalent |

---

## 22. Security Vulnerability Scan (Snyk)

Scan Date: 2026-03-21T23:44:00Z
Snyk CLI Version: 1.1303.0

### Dependency Vulnerabilities (Open Source)
Critical: 1
High: 7
Medium: 0
Low: 3

| Severity | Package | Version | Vulnerability | Fix Available |
|----------|---------|---------|---------------|---------------|
| CRITICAL | spring-security-web | 6.4.13 | Use of Cache Containing Sensitive Information | 6.5.9 |
| HIGH | tomcat-embed-core | 10.1.50 | Incorrect Authorization | 9.0.114 |
| HIGH | spring-webmvc | 6.2.15 | Directory Traversal | 6.2.17 |
| HIGH | spring-webflux | 6.2.15 | Directory Traversal | 6.2.17 |
| HIGH | spring-boot-actuator-autoconfigure | 3.4.13 | Authentication Bypass (x2) | 3.5.12 |
| HIGH | spring-boot-actuator | 3.4.13 | Authentication Bypass (x2) | 3.5.12 |
| LOW | logback-core | 1.5.22 | External Initialization | 1.5.25 |
| LOW | spring-web | 6.2.15 | Injection | 6.2.17 |
| LOW | spring-webmvc | 6.2.15 | Injection | 6.2.17 |

**Primary remediation:** Upgrade Spring Boot from 3.4.13 to 3.5.12+ (will pull in fixed transitive dependencies).

### Code Vulnerabilities (SAST)
Errors: 0
Warnings: 0
Notes: 0

**PASS — No code vulnerabilities detected by Snyk Code.**

### IaC Findings
Not scanned (Dockerfile present but IaC scan deferred — no Kubernetes/Terraform).

---
