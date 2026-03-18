# MyOffGridAI-Server — Codebase Audit

**Audit Date:** 2026-03-17T23:58:24Z
**Branch:** main
**Commit:** f787bb7d2ddfcb713a741b36215fb776bfa96cf1 P17-Server: Restore llama-server provider + wire Homebrew binary
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
Project Name:         MyOffGridAI Server
Repository URL:       https://github.com/aallard/MyOffGridAI-Server.git
Primary Language:     Java 21 / Spring Boot 3.4.13
Build Tool:           Maven (via maven-wrapper)
Current Branch:       main
Latest Commit Hash:   f787bb7d2ddfcb713a741b36215fb776bfa96cf1
Latest Commit Msg:    P17-Server: Restore llama-server provider + wire Homebrew binary
Audit Timestamp:      2026-03-17T23:58:05Z
```


## 2. Directory Structure

```
MyOffGridAI-Server/
├── pom.xml
├── Dockerfile
├── CONVENTIONS.md
├── README.md
├── MyOffGridAI-Server-Architecture.md
├── src/main/java/com/myoffgridai/
│   ├── MyOffGridAiApplication.java          ← Entry point
│   ├── ai/                                   ← Chat, inference, model management
│   │   ├── controller/                       (ChatController, ModelController)
│   │   ├── dto/                              (InferenceChunk, OllamaChatRequest, etc.)
│   │   ├── judge/                            (AI Judge: JudgeController, JudgeInferenceService, etc.)
│   │   ├── model/                            (Conversation, Message, MessageRole)
│   │   ├── repository/                       (ConversationRepository, MessageRepository)
│   │   └── service/                          (ChatService, InferenceService, OllamaService,
│   │                                          LlamaServerInferenceService, LlamaServerProcessService,
│   │                                          NativeLlamaInferenceService, NativeLlamaModelBridge,
│   │                                          AgentService, ContextWindowService, ModelHealthCheckService,
│   │                                          SystemPromptBuilder, ProcessBuilderFactory)
│   ├── auth/                                 ← Authentication & user management
│   │   ├── controller/                       (AuthController, UserController)
│   │   ├── dto/                              (LoginRequest, RegisterRequest, AuthResponse, etc.)
│   │   ├── model/                            (User, Role)
│   │   ├── repository/                       (UserRepository)
│   │   └── service/                          (AuthService, JwtService, UserService)
│   ├── common/                               ← Shared utilities & exception handling
│   │   ├── exception/                        (GlobalExceptionHandler + 11 custom exceptions)
│   │   ├── response/                         (ApiResponse)
│   │   └── util/                             (AesEncryptionUtil, AesAttributeConverter, TokenCounter)
│   ├── config/                               ← Configuration classes
│   │   ├── AppConstants.java
│   │   ├── SecurityConfig.java, JwtAuthFilter.java
│   │   ├── CaptivePortalRedirectFilter.java
│   │   ├── InferenceProperties.java, LlamaServerConfig.java, LlamaServerProperties.java
│   │   ├── NativeLlamaConfig.java, NativeLlamaProperties.java
│   │   ├── OllamaConfig.java, VectorStoreConfig.java, VectorType.java
│   │   ├── JpaConfig.java, ProcessConfig.java
│   │   ├── MdcFilter.java, RateLimitingFilter.java, RequestResponseLoggingFilter.java
│   │   └── (SourceTag enum in ai package)
│   ├── enrichment/                           ← Web search, fetch, Claude API
│   │   ├── controller/                       (EnrichmentController)
│   │   ├── dto/                              (SearchRequest, FetchUrlRequest, etc.)
│   │   └── service/                          (WebSearchService, WebFetchService, ClaudeApiService)
│   ├── events/                               ← Scheduled events & automation
│   │   ├── controller/                       (ScheduledEventController)
│   │   ├── dto/                              (CreateEventRequest, ScheduledEventDto, etc.)
│   │   ├── model/                            (ScheduledEvent, EventType, ActionType, ThresholdOperator)
│   │   ├── repository/                       (ScheduledEventRepository)
│   │   └── service/                          (ScheduledEventService)
│   ├── frontier/                             ← Frontier AI API routing (Claude, OpenAI, Grok)
│   │   ├── FrontierApiRouter.java
│   │   ├── FrontierApiClient.java (interface)
│   │   ├── FrontierProvider.java (enum)
│   │   ├── ClaudeFrontierClient.java
│   │   ├── OpenAiFrontierClient.java
│   │   └── GrokFrontierClient.java
│   ├── knowledge/                            ← Knowledge base & document management
│   │   ├── controller/                       (KnowledgeController)
│   │   ├── dto/                              (KnowledgeDocumentDto, SemanticSearchResult, etc.)
│   │   ├── model/                            (KnowledgeDocument, KnowledgeChunk, DocumentStatus)
│   │   ├── repository/                       (KnowledgeDocumentRepository, KnowledgeChunkRepository)
│   │   ├── service/                          (KnowledgeService, IngestionService, ChunkingService,
│   │   │                                      SemanticSearchService, FileStorageService, OcrService,
│   │   │                                      StorageHealthService)
│   │   └── util/                             (DeltaJsonUtils)
│   ├── library/                              ← Ebook & ZIM file management
│   │   ├── config/                           (LibraryProperties)
│   │   ├── controller/                       (LibraryController)
│   │   ├── dto/                              (EbookDto, ZimFileDto, GutenbergBookDto, etc.)
│   │   ├── model/                            (Ebook, EbookFormat, ZimFile)
│   │   ├── repository/                       (EbookRepository, ZimFileRepository)
│   │   └── service/                          (EbookService, ZimFileService, GutenbergService,
│   │                                          CalibreConversionService)
│   ├── mcp/                                  ← MCP (Model Context Protocol) server
│   │   ├── config/                           (McpServerConfig, McpAuthFilter, McpAuthentication)
│   │   ├── controller/                       (McpDiscoveryController, McpTokenController)
│   │   ├── dto/                              (CreateMcpTokenRequest, McpTokenCreateResult, etc.)
│   │   ├── model/                            (McpApiToken)
│   │   ├── repository/                       (McpApiTokenRepository)
│   │   └── service/                          (McpTokenService, McpToolsService)
│   ├── memory/                               ← Memory & RAG (Retrieval-Augmented Generation)
│   │   ├── controller/                       (MemoryController)
│   │   ├── dto/                              (MemoryDto, RagContext, MemorySearchRequest, etc.)
│   │   ├── model/                            (Memory, MemoryImportance, VectorDocument, VectorSourceType)
│   │   ├── repository/                       (MemoryRepository, VectorDocumentRepository)
│   │   └── service/                          (MemoryService, EmbeddingService, RagService,
│   │                                          MemoryExtractionService, SummarizationService)
│   ├── models/                               ← Model discovery, download & management
│   │   ├── controller/                       (ModelDownloadController)
│   │   ├── dto/                              (HfModelDto, HfModelFileDto, LocalModelFileDto, etc.)
│   │   └── service/                          (ModelCatalogService, ModelDownloadService,
│   │                                          ModelDownloadProgressRegistry, QuantizationRecommendationService)
│   ├── notification/                         ← Push notifications via MQTT
│   │   ├── config/                           (MqttConfig)
│   │   ├── controller/                       (DeviceRegistrationController)
│   │   ├── dto/                              (RegisterDeviceRequest, DeviceRegistrationDto, etc.)
│   │   ├── model/                            (DeviceRegistration)
│   │   ├── repository/                       (DeviceRegistrationRepository)
│   │   └── service/                          (DeviceRegistrationService, MqttPublisherService)
│   ├── privacy/                              ← Privacy controls, audit, data sovereignty
│   │   ├── aspect/                           (AuditAspect)
│   │   ├── controller/                       (PrivacyController)
│   │   ├── dto/                              (AuditLogDto, FortressStatus, SovereigntyReport, etc.)
│   │   ├── model/                            (AuditLog, AuditOutcome)
│   │   ├── repository/                       (AuditLogRepository)
│   │   └── service/                          (AuditService, FortressService, DataExportService,
│   │                                          DataWipeService, SovereigntyReportService)
│   ├── proactive/                            ← Proactive insights & notifications
│   │   ├── controller/                       (ProactiveController)
│   │   ├── dto/                              (InsightDto, NotificationDto, PatternSummary)
│   │   ├── model/                            (Insight, InsightCategory, Notification,
│   │   │                                      NotificationSeverity, NotificationType)
│   │   ├── repository/                       (InsightRepository, NotificationRepository)
│   │   └── service/                          (InsightService, InsightGeneratorService,
│   │                                          NightlyInsightJob, NotificationService,
│   │                                          NotificationSseRegistry, PatternAnalysisService,
│   │                                          SystemHealthMonitor)
│   ├── sensors/                              ← Sensor data collection
│   │   ├── controller/                       (SensorController)
│   │   ├── dto/                              (SensorDto, SensorReadingDto, CreateSensorRequest, etc.)
│   │   ├── model/                            (Sensor, SensorReading, SensorType, DataFormat)
│   │   ├── repository/                       (SensorRepository, SensorReadingRepository)
│   │   └── service/                          (SensorService, SensorPollingService,
│   │                                          SensorStartupService, SerialPortService, SseEmitterRegistry)
│   ├── settings/                             ← External API settings
│   │   ├── controller/                       (ExternalApiSettingsController)
│   │   ├── dto/                              (ExternalApiSettingsDto, UpdateExternalApiSettingsRequest)
│   │   ├── model/                            (ExternalApiSettings)
│   │   ├── repository/                       (ExternalApiSettingsRepository)
│   │   └── service/                          (ExternalApiSettingsService)
│   ├── skills/                               ← Skill framework
│   │   ├── builtin/                          (DocumentSummarizerSkill, InventoryTrackerSkill,
│   │   │                                      RecipeGeneratorSkill, ResourceCalculatorSkill,
│   │   │                                      TaskPlannerSkill, WeatherQuerySkill)
│   │   ├── controller/                       (SkillController)
│   │   ├── dto/                              (SkillDto, SkillExecuteRequest, etc.)
│   │   ├── model/                            (Skill, SkillExecution, InventoryItem, PlannedTask, etc.)
│   │   ├── repository/                       (SkillRepository, SkillExecutionRepository,
│   │   │                                      InventoryItemRepository, PlannedTaskRepository)
│   │   └── service/                          (SkillExecutorService, SkillSeederService, BuiltInSkill)
│   └── system/                               ← System config, AP mode, factory reset
│       ├── controller/                       (SystemController, CaptivePortalController)
│       ├── dto/                              (SystemStatusDto, InitializeRequest, etc.)
│       ├── model/                            (SystemConfig)
│       ├── repository/                       (SystemConfigRepository)
│       └── service/                          (SystemConfigService, ApModeService, ApModeStartupService,
│                                              FactoryResetService, NetworkTransitionService,
│                                              UsbResetWatcherService)
├── src/main/resources/
│   ├── application.yml                       (dev + prod profiles)
│   ├── application-prod.yml                  (prod env var overrides)
│   ├── logback-spring.xml                    (structured logging)
│   └── META-INF/native-image/               (GraalVM native image config)
└── src/test/
    ├── java/com/myoffgridai/
    │   ├── integration/                      (24 integration test classes)
    │   └── (unit test mirrors for all packages)
    └── resources/
        ├── application.yml
        └── application-test.yml
```

Single-module Maven project. Source code under `src/main/java/com/myoffgridai/` organized by feature domain (ai, auth, knowledge, library, memory, sensors, skills, system, etc.). 15 feature domains, each with standard controller/dto/model/repository/service layers.


## 3. Build & Dependency Manifest

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.13 (parent) | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | 3.4.13 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.4.13 | Spring Security |
| spring-boot-starter-validation | 3.4.13 | Bean validation (Jakarta) |
| spring-boot-starter-actuator | 3.4.13 | Health checks, metrics |
| spring-boot-starter-webflux | 3.4.13 | WebClient for reactive HTTP calls |
| spring-boot-starter-aop | 3.4.13 | AOP for audit aspect |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 | MCP server (SSE transport) |
| jsoup | 1.18.3 | HTML parsing for web content extraction |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation/validation |
| postgresql | 42.7.7 | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector extension for vector similarity |
| pdfbox | 3.0.4 | PDF text extraction |
| poi / poi-ooxml / poi-scratchpad | 5.4.0 | Office document processing |
| tess4j | 5.13.0 | OCR via Tesseract |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| bucket4j-core | 8.10.1 | Rate limiting |
| paho.client.mqttv3 | 1.2.5 | MQTT client for push notifications |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | OpenAPI/Swagger UI |
| lombok | 1.18.42 | Boilerplate reduction |
| spring-boot-starter-test | 3.4.13 | Test framework |
| reactor-test | (managed) | Reactive stream testing |
| spring-security-test | (managed) | Security test support |
| testcontainers-postgresql | 1.20.6 | Integration tests with real PostgreSQL |
| testcontainers-junit-jupiter | 1.20.6 | Testcontainers JUnit5 integration |
| jackson-core | 2.18.6 (managed) | JSON processing (BOM override) |
| netty-codec-http/http2 | 4.1.129.Final (managed) | Netty HTTP codecs (BOM override) |
| commons-lang3 | 3.18.0 (managed) | String/object utilities |

**Build Plugins:**

| Plugin | Version | Purpose |
|---|---|---|
| spring-boot-maven-plugin | 3.4.13 | Package executable JAR (excludes Lombok) |
| maven-compiler-plugin | (default) | Java 21 source/target, Lombok annotation processor |
| jacoco-maven-plugin | 0.8.12 | Code coverage: 100% LINE + BRANCH required, excludes dto/model/Application |
| maven-surefire-plugin | (default) | Unit tests with --add-opens for reflection |
| native-maven-plugin | 0.10.4 | GraalVM native image (profile: `native`) |

**Build Commands:**
```
Build:   mvn clean compile -DskipTests
Test:    mvn test
Run:     mvn spring-boot:run
Package: mvn clean package -DskipTests
Native:  mvn clean package -Pnative -DskipTests
```


## 4. Configuration & Infrastructure Summary

**`application.yml`** (`src/main/resources/application.yml`)
- Default profile: `dev`. Server port: `8080`. Flyway disabled. Max upload: 2048MB.
- MCP server enabled: name=`myoffgridai-mcp`, SSE at `/mcp/sse`, messages at `/mcp/message`.

**Dev profile** (within `application.yml`):
- DB: `jdbc:postgresql://localhost:5432/myoffgridai` (user/pass: `myoffgridai`).
- Hibernate `ddl-auto: update`. JWT secret hardcoded (dev only). AES key hardcoded (dev only).
- Inference provider: `llama-server` at port 1234. Model: `Qwen3.5-27B` Q4_K_S GGUF.
- Ollama fallback at `localhost:11434`. Embed model: `nomic-embed-text`.
- Fortress mock=true, AP mock=true, MQTT disabled, Judge disabled.
- Library: ZIM at `./library/zim`, ebooks at `./library/ebooks`, Kiwix at `localhost:8888`.

**`application-prod.yml`** (`src/main/resources/application-prod.yml`)
- All secrets from env vars: `${DB_URL}`, `${DB_PASSWORD}`, `${JWT_SECRET}`, `${ENCRYPTION_KEY}`.
- Inference config from env vars: `${INFERENCE_PROVIDER}`, `${INFERENCE_BASE_URL}`, etc.
- Hibernate `ddl-auto: validate`. Flyway enabled with `classpath:db/migration`.
- Fortress mock=false, AP mock=false.

**`application-test.yml`** (`src/test/resources/application-test.yml`)
- Hibernate `ddl-auto: create-drop`. JWT secret hardcoded (test). Flyway disabled.
- Rate limiting disabled, MQTT disabled, AP mock=true, Fortress mock=true.
- Library dirs use `${java.io.tmpdir}`.

**`logback-spring.xml`** (`src/main/resources/logback-spring.xml`)
- Dev: human-readable console, DEBUG for `com.myoffgridai`, includes `requestId` MDC.
- Prod: JSON via LogstashEncoder, MDC keys: `requestId`, `username`, `userId`.
- Test: JSON (matches prod), WARN root level.

**`Dockerfile`** (`./Dockerfile`)
- Multi-stage: `eclipse-temurin:21-jdk-alpine` (build), `eclipse-temurin:21-jre-alpine` (runtime).
- Non-root user `myoffgridai`. Exposes 8080. Healthcheck: `GET /api/system/status`.
- Creates `/var/myoffgridai/knowledge` and `/var/log/myoffgridai`.

**Connection Map:**
```
Database:        PostgreSQL, localhost:5432, database: myoffgridai
Cache:           None
Message Broker:  MQTT (Paho), tcp://localhost:1883 (prod only, disabled in dev/test)
External APIs:   Ollama (localhost:11434), Kiwix (localhost:8888),
                 Calibre (localhost:8081), Gutenberg (gutendex.com),
                 Claude API, OpenAI API, Grok API (via FrontierApiRouter)
Cloud Services:  None (offline-first design)
```

**CI/CD:** None detected.


## 5. Startup & Runtime Behavior

**Entry Point:** `com.myoffgridai.MyOffGridAiApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`.

**Startup Sequence (ApplicationRunner):**
- `LlamaServerProcessService.run()` — Starts llama-server child process with configured active model. Failures logged at WARN, never blocks startup. Only active when `app.inference.provider=llama-server`.

**Startup Listeners (@EventListener ApplicationReadyEvent):**

| Order | Class | Method | Purpose |
|-------|-------|--------|---------|
| 1 | VectorStoreConfig | checkPgvectorExtension() | Verifies pgvector PostgreSQL extension installed |
| 2 | StorageHealthService | checkStorageDirectory() | Verifies knowledge storage dir exists/writable |
| 3 | SkillSeederService | seedBuiltInSkills() | Seeds 6 built-in skills (Weather, Inventory, Recipe, TaskPlanner, DocSummarizer, ResourceCalc) |
| 4 | ModelHealthCheckService | checkInferenceProviderOnStartup() | Checks inference provider availability, lists models |
| 5 | NativeLlamaInferenceService | onApplicationReady() | Auto-loads active GGUF model (only if provider=native) |
| 6 | SensorStartupService | resumeActiveSensors() | Resumes polling for sensors that were active at shutdown |
| Last | ApModeStartupService | onApplicationReady() | If not initialized → starts WiFi AP for captive portal setup; if initialized → ensures normal mode |

**Scheduled Tasks:**

| Class | Method | Schedule | Purpose |
|-------|--------|----------|---------|
| LlamaServerProcessService | monitorHealth() | Every 30s (configurable) | Health-check llama-server child process, restart if crashed |
| UsbResetWatcherService | checkForTriggerFiles() | Every 30s | Watch USB for factory reset triggers / update zips |
| SystemHealthMonitor | checkSystemHealth() | Every 5min (configurable) | Check disk space, Ollama availability, JVM heap usage |
| SummarizationService | scheduledNightlySummarization() | Daily 2:00 AM | Batch-summarize old conversations into memories |
| NightlyInsightJob | generateNightlyInsights() | Daily 3:00 AM | Generate proactive insights for all users |

**Health Check Endpoints:**
- `GET /api/models/health` — Public. Returns inference provider status, active model, latency.
- `GET /actuator/health` — Public. Standard Spring Boot actuator health.
- `GET /api/system/status` — Used by Docker HEALTHCHECK.


## 6. Entity / Data Model Layer

**Total @Entity classes: 23**

## PACKAGE: com.myoffgridai.ai.model

---

```
=== Conversation.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/ai/model/Conversation.java
Table: conversations
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:             UUID   [@Id, @GeneratedValue(UUID)]
  - user:           User   [@ManyToOne LAZY, @JoinColumn(name="user_id", nullable=false)]
  - title:          String [@Column] (nullable)
  - isArchived:     boolean [@Column(name="is_archived", nullable=false)] default=false
  - createdAt:      Instant [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:      Instant [@LastModifiedDate, @Column(name="updated_at")]
  - messageCount:   int    [@Column(name="message_count", nullable=false)] default=0

Relationships:
  - @ManyToOne(fetch=LAZY) → User  (@JoinColumn = "user_id", nullable=false)
  NOTE: No @OneToMany back-reference to Message collection on this side.

Audit Fields:
  - createdAt: YES (@CreatedDate, also set in @PrePersist)
  - updatedAt: YES (@LastModifiedDate, also set in @PrePersist and @PreUpdate)
  - createdBy: MISSING
  - version:   MISSING

Validation: None (no JSR-380 bean validation annotations)

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null, sets updatedAt = Instant.now()
  - @PreUpdate  onUpdate(): sets updatedAt = Instant.now()
  - getIsArchived() / setIsArchived() — non-standard getter name (boolean field named isArchived)
```

---

```
=== Message.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/ai/model/Message.java
Table: messages
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                   UUID    [@Id, @GeneratedValue(UUID)]
  - conversation:         Conversation [@ManyToOne LAZY, @JoinColumn(name="conversation_id", nullable=false)]
  - role:                 MessageRole  [@Enumerated(STRING), @Column(nullable=false)]
  - content:              String  [@Column(nullable=false, columnDefinition="TEXT")]
  - tokenCount:           Integer [@Column(name="token_count")] (nullable)
  - hasRagContext:        boolean [@Column(name="has_rag_context", nullable=false)] default=false
  - thinkingContent:      String  [@Column(name="thinking_content", columnDefinition="TEXT")] (nullable)
  - tokensPerSecond:      Double  [@Column(name="tokens_per_second")] (nullable)
  - inferenceTimeSeconds: Double  [@Column(name="inference_time_seconds")] (nullable)
  - stopReason:           String  [@Column(name="stop_reason")] (nullable)
  - thinkingTokenCount:   Integer [@Column(name="thinking_token_count")] (nullable)
  - sourceTag:            SourceTag [@Enumerated(STRING), @Column(name="source_tag", length=20, nullable=false)] default=SourceTag.LOCAL
  - judgeScore:           Double  [@Column(name="judge_score")] (nullable)
  - judgeReason:          String  [@Column(name="judge_reason", columnDefinition="TEXT")] (nullable)
  - createdAt:            Instant [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]

Relationships:
  - @ManyToOne(fetch=LAZY) → Conversation (@JoinColumn = "conversation_id", nullable=false)

Audit Fields:
  - createdAt: YES (@CreatedDate, also @PrePersist)
  - updatedAt: MISSING
  - createdBy: MISSING
  - version:   MISSING

Validation: None (no JSR-380 annotations)

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null
  - setSourceTag(): null-safe — falls back to SourceTag.LOCAL if null passed
  Note: sourceTag field references com.myoffgridai.ai.SourceTag (external to model package)
```

---

### Enum: MessageRole.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/ai/model/MessageRole.java`
Values: `USER`, `ASSISTANT`, `SYSTEM`

---

## PACKAGE: com.myoffgridai.auth.model

---

```
=== User.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/auth/model/User.java
Table: users
Primary Key: id, UUID, GenerationType.UUID
Implements: UserDetails (Spring Security)

Fields:
  - id:           UUID    [@Id, @GeneratedValue(UUID)]
  - username:     String  [@Column(nullable=false, unique=true)]
  - email:        String  [@Column(unique=true)] (nullable)
  - displayName:  String  [@Column(name="display_name", nullable=false)]
  - passwordHash: String  [@Column(name="password_hash", nullable=false)]
  - role:         Role    [@Enumerated(STRING), @Column(nullable=false)]
  - isActive:     boolean [@Column(name="is_active", nullable=false)] default=true
  - createdAt:    Instant [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:    Instant [@LastModifiedDate, @Column(name="updated_at")]
  - lastLoginAt:  Instant [@Column(name="last_login_at")] (nullable)

Relationships:
  - None declared on this entity. User is the target of @ManyToOne from Conversation.

Audit Fields:
  - createdAt: YES (@CreatedDate + @PrePersist)
  - updatedAt: YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy: MISSING
  - version:   MISSING

Validation: None (no JSR-380 annotations)

Custom Methods (UserDetails implementation):
  - getAuthorities(): returns List.of(new SimpleGrantedAuthority(role.name()))
  - getPassword(): returns passwordHash
  - getUsername(): returns username
  - isAccountNonExpired(): always returns true
  - isAccountNonLocked(): returns isActive
  - isCredentialsNonExpired(): always returns true
  - isEnabled(): returns isActive
  - @PrePersist onCreate(): sets createdAt if null, sets updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
  - getIsActive() / setIsActive() — non-standard getter name
```

---

### Enum: Role.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/auth/model/Role.java`
Values: `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER`, `ROLE_CHILD`
Note: Follows Spring Security `ROLE_` prefix convention. Ordered from highest to lowest privilege.

---

## PACKAGE: com.myoffgridai.events.model

---

```
=== ScheduledEvent.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/events/model/ScheduledEvent.java
Table: scheduled_events
Indexes:
  - idx_event_user_id        → columnList = "user_id"
  - idx_event_enabled_type   → columnList = "is_enabled, event_type"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                       UUID             [@Id, @GeneratedValue(UUID)]
  - userId:                   UUID             [@Column(name="user_id", nullable=false)]
                                               NOTE: Bare UUID — no @ManyToOne FK to User
  - name:                     String           [@Column(nullable=false)]
  - description:              String           [@Column(columnDefinition="TEXT")] (nullable)
  - eventType:                EventType        [@Enumerated(STRING), @Column(name="event_type", nullable=false)]
  - isEnabled:                boolean          [@Column(name="is_enabled", nullable=false)] default=true
  - cronExpression:           String           [@Column(name="cron_expression")] (nullable)
  - recurringIntervalMinutes: Integer          [@Column(name="recurring_interval_minutes")] (nullable)
  - sensorId:                 UUID             [@Column(name="sensor_id")] (nullable)
                                               NOTE: Bare UUID — no @ManyToOne FK to Sensor
  - thresholdOperator:        ThresholdOperator [@Enumerated(STRING), @Column(name="threshold_operator")] (nullable)
  - thresholdValue:           Double           [@Column(name="threshold_value")] (nullable)
  - actionType:               ActionType       [@Enumerated(STRING), @Column(name="action_type", nullable=false)]
  - actionPayload:            String           [@Column(name="action_payload", nullable=false, columnDefinition="TEXT")]
  - lastTriggeredAt:          Instant          [@Column(name="last_triggered_at")] (nullable)
  - nextFireAt:               Instant          [@Column(name="next_fire_at")] (nullable)
  - createdAt:                Instant          [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:                Instant          [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared (userId and sensorId are raw UUIDs, not JPA relationship fields)

Audit Fields:
  - createdAt: YES
  - updatedAt: YES
  - createdBy: MISSING
  - version:   MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
```

---

### Enum: EventType.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/events/model/EventType.java`
Values: `SCHEDULED`, `SENSOR_THRESHOLD`, `RECURRING`

### Enum: ActionType.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/events/model/ActionType.java`
Values: `PUSH_NOTIFICATION`, `AI_PROMPT`, `AI_SUMMARY`

### Enum: ThresholdOperator.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/events/model/ThresholdOperator.java`
Values: `ABOVE`, `BELOW`, `EQUALS`

---

## PACKAGE: com.myoffgridai.knowledge.model

---

```
=== KnowledgeDocument.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/knowledge/model/KnowledgeDocument.java
Table: knowledge_documents
Indexes:
  - idx_knowledge_doc_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:            UUID           [@Id, @GeneratedValue(UUID)]
  - userId:        UUID           [@Column(name="user_id", nullable=false)] — bare UUID, no FK join
  - filename:      String         [@Column(nullable=false)]
  - displayName:   String         [@Column(name="display_name")] (nullable)
  - mimeType:      String         [@Column(name="mime_type", nullable=false)]
  - storagePath:   String         [@Column(name="storage_path", nullable=false)]
  - fileSizeBytes: long           [@Column(name="file_size_bytes")]
  - status:        DocumentStatus [@Enumerated(STRING), @Column(nullable=false)] default=PENDING
  - errorMessage:  String         [@Column(name="error_message", columnDefinition="TEXT")] (nullable)
  - chunkCount:    int            [@Column(name="chunk_count")]
  - uploadedAt:    Instant        [@CreatedDate, @Column(name="uploaded_at", nullable=false, updatable=false)]
  - processedAt:   Instant        [@Column(name="processed_at")] (nullable)
  - content:       String         [@Column(columnDefinition="TEXT")] (nullable)

Relationships:
  - None declared. KnowledgeChunk has @ManyToOne back to this entity.

Audit Fields:
  - uploadedAt:  YES (plays the role of createdAt, @CreatedDate)
  - updatedAt:   MISSING
  - createdBy:   MISSING — uploadedBy not present
  - version:     MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets uploadedAt = Instant.now() if null
```

---

```
=== KnowledgeChunk.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/knowledge/model/KnowledgeChunk.java
Table: knowledge_chunks
Indexes:
  - idx_knowledge_chunk_doc_id  → columnList = "document_id"
  - idx_knowledge_chunk_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:           UUID              [@Id, @GeneratedValue(UUID)]
  - document:     KnowledgeDocument [@ManyToOne LAZY, @JoinColumn(name="document_id", nullable=false)]
  - userId:       UUID              [@Column(name="user_id", nullable=false)] — bare UUID, denormalized
  - chunkIndex:   int               [@Column(name="chunk_index", nullable=false)]
  - content:      String            [@Column(nullable=false, columnDefinition="TEXT")]
  - pageNumber:   Integer           [@Column(name="page_number")] (nullable)
  - createdAt:    Instant           [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]

Relationships:
  - @ManyToOne(fetch=LAZY) → KnowledgeDocument (@JoinColumn = "document_id", nullable=false)

Audit Fields:
  - createdAt: YES
  - updatedAt: MISSING
  - createdBy: MISSING
  - version:   MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null
```

---

### Enum: DocumentStatus.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/knowledge/model/DocumentStatus.java`
Values: `PENDING`, `PROCESSING`, `READY`, `FAILED`

---

## PACKAGE: com.myoffgridai.library.model

---

```
=== Ebook.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/library/model/Ebook.java
Table: ebooks
Indexes:
  - idx_ebooks_gutenberg_id → columnList = "gutenberg_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:              UUID        [@Id, @GeneratedValue(UUID)]
  - title:           String      [@Column(nullable=false)]
  - author:          String      [@Column] (nullable, no annotation — bare field)
  - description:     String      [@Column(length=2000, columnDefinition="TEXT")] (nullable)
  - isbn:            String      [@Column] (nullable)
  - publisher:       String      [@Column] (nullable)
  - publishedYear:   Integer     [@Column(name="published_year")] (nullable)
  - language:        String      [@Column] (nullable)
  - format:          EbookFormat [@Enumerated(STRING), @Column(nullable=false)]
  - fileSizeBytes:   long        [@Column(name="file_size_bytes")]
  - filePath:        String      [@Column(name="file_path", nullable=false)]
  - coverImagePath:  String      [@Column(name="cover_image_path")] (nullable)
  - gutenbergId:     String      [@Column(name="gutenberg_id")] (nullable)
  - downloadCount:   int         [@Column(name="download_count")] default=0
  - uploadedAt:      Instant     [@CreatedDate, @Column(name="uploaded_at", nullable=false, updatable=false)]
  - uploadedBy:      UUID        [@Column(name="uploaded_by")] (nullable) — bare UUID, no FK

Relationships:
  - None declared

Audit Fields:
  - uploadedAt:  YES (plays role of createdAt)
  - updatedAt:   MISSING
  - createdBy:   MISSING (uploadedBy present as raw UUID but not @CreatedBy)
  - version:     MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets uploadedAt = Instant.now() if null
```

---

```
=== ZimFile.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/library/model/ZimFile.java
Table: zim_files
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:            UUID    [@Id, @GeneratedValue(UUID)]
  - filename:      String  [@Column(nullable=false, unique=true)]
  - displayName:   String  [@Column(name="display_name", nullable=false)]
  - description:   String  [@Column(length=1000)] (nullable)
  - language:      String  [@Column] (nullable)
  - category:      String  [@Column] (nullable)
  - fileSizeBytes: long    [@Column(name="file_size_bytes")]
  - articleCount:  int     [@Column(name="article_count")]
  - mediaCount:    int     [@Column(name="media_count")]
  - createdDate:   String  [@Column(name="created_date")] (nullable) — NOTE: String type, not Instant
  - filePath:      String  [@Column(name="file_path", nullable=false)]
  - kiwixBookId:   String  [@Column(name="kiwix_book_id")] (nullable)
  - uploadedAt:    Instant [@CreatedDate, @Column(name="uploaded_at", nullable=false, updatable=false)]
  - uploadedBy:    UUID    [@Column(name="uploaded_by")] (nullable) — bare UUID

Relationships:
  - None declared

Audit Fields:
  - uploadedAt:  YES (plays role of createdAt)
  - updatedAt:   MISSING
  - createdBy:   MISSING (uploadedBy present as raw UUID)
  - version:     MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets uploadedAt = Instant.now() if null
  - Notable: createdDate is a String field (metadata from ZIM file), not a JPA audit field
```

---

### Enum: EbookFormat.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/library/model/EbookFormat.java`
Values: `EPUB`, `PDF`, `MOBI`, `AZW`, `TXT`, `HTML`

---

## PACKAGE: com.myoffgridai.mcp.model

---

```
=== McpApiToken.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/mcp/model/McpApiToken.java
Table: mcp_api_tokens
Indexes:
  - idx_mcp_token_created_by → columnList = "created_by"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:          UUID    [@Id, @GeneratedValue(UUID)]
  - tokenHash:   String  [@Column(name="token_hash", nullable=false, length=500)]
                         NOTE: BCrypt-hashed token; plaintext shown once at creation
  - name:        String  [@Column(nullable=false)]
  - createdBy:   UUID    [@Column(name="created_by", nullable=false)] — bare UUID, no FK
  - lastUsedAt:  Instant [@Column(name="last_used_at")] (nullable)
  - isActive:    boolean [@Column(name="is_active", nullable=false)] default=true
  - createdAt:   Instant [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  MISSING
  - createdBy:  Present as raw UUID field (not @CreatedBy annotation)
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null
  - isActive() / setActive() — note asymmetric getter/setter naming (isActive vs setActive)
```

---

## PACKAGE: com.myoffgridai.memory.model

---

```
=== Memory.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/memory/model/Memory.java
Table: memories
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                     UUID             [@Id, @GeneratedValue(UUID)]
  - userId:                 UUID             [@Column(name="user_id", nullable=false)] — bare UUID
  - content:                String           [@Column(nullable=false, columnDefinition="TEXT")]
  - importance:             MemoryImportance [@Enumerated(STRING), @Column(nullable=false)]
  - tags:                   String           [@Column] (nullable) — free-text tags, not normalized
  - sourceConversationId:   UUID             [@Column(name="source_conversation_id")] (nullable) — bare UUID
  - createdAt:              Instant          [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:              Instant          [@LastModifiedDate, @Column(name="updated_at")]
  - lastAccessedAt:         Instant          [@Column(name="last_accessed_at")] (nullable)
  - accessCount:            int              [@Column(name="access_count", nullable=false)] default=0

Relationships:
  - None declared (all foreign keys are raw UUIDs)

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
```

---

```
=== VectorDocument.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/memory/model/VectorDocument.java
Table: vector_document    (NOTE: singular name, inconsistent with other tables using plural names)
Indexes:
  - idx_vector_doc_user_source_type → columnList = "user_id, source_type"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:         UUID            [@Id, @GeneratedValue(UUID)]
  - userId:     UUID            [@Column(name="user_id", nullable=false)]
  - content:    String          [@Column(nullable=false, columnDefinition="TEXT")]
  - embedding:  float[]         [@Type(VectorType.class), @Column(columnDefinition="vector(768)")]
                                NOTE: Uses custom Hibernate @Type for pgvector; 768-dim vector
  - sourceType: VectorSourceType [@Enumerated(STRING), @Column(name="source_type", nullable=false)]
  - sourceId:   UUID            [@Column(name="source_id")] (nullable) — bare UUID reference
  - metadata:   String          [@Column(columnDefinition="TEXT")] (nullable) — free-form JSON
  - createdAt:  Instant         [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES
  - updatedAt:  MISSING
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null
  - Notable: embedding field uses org.hibernate.annotations.Type with com.myoffgridai.config.VectorType
    (custom Hibernate UserType bridging pgvector float[] to SQL vector type)
```

---

### Enum: MemoryImportance.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/memory/model/MemoryImportance.java`
Values: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

### Enum: VectorSourceType.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/memory/model/VectorSourceType.java`
Values: `MEMORY`, `CONVERSATION`, `KNOWLEDGE_CHUNK`

---

## PACKAGE: com.myoffgridai.notification.model

---

```
=== DeviceRegistration.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/notification/model/DeviceRegistration.java
Table: device_registrations
Unique Constraints:
  - uk_device_registration_user_device → ("user_id", "device_id")
Indexes:
  - idx_device_registration_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:           UUID    [@Id, @GeneratedValue(UUID)]
  - userId:       UUID    [@Column(name="user_id", nullable=false)] — bare UUID
  - deviceId:     String  [@Column(name="device_id", nullable=false)]
  - deviceName:   String  [@Column(name="device_name")] (nullable)
  - platform:     String  [@Column(nullable=false)]
  - mqttClientId: String  [@Column(name="mqtt_client_id")] (nullable)
  - lastSeenAt:   Instant [@Column(name="last_seen_at")] (nullable)
  - createdAt:    Instant [@Column(name="created_at", nullable=false, updatable=false)]
                          NOTE: Not @CreatedDate — manual set in @PrePersist only
  - updatedAt:    Instant [@Column(name="updated_at")]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (set manually in @PrePersist, NOT @CreatedDate annotation)
  - updatedAt:  YES (set in @PrePersist and @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
  - NOTE: Unlike other entities, does NOT use @CreatedDate/@LastModifiedDate; manages timestamps manually
    despite @EntityListeners(AuditingEntityListener.class) being present
```

---

## PACKAGE: com.myoffgridai.privacy.model

---

```
=== AuditLog.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/privacy/model/AuditLog.java
Table: audit_logs
Indexes:
  - idx_audit_user_timestamp → columnList = "user_id, timestamp DESC"
  - idx_audit_timestamp      → columnList = "timestamp DESC"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:             UUID         [@Id, @GeneratedValue(UUID)]
  - userId:         UUID         [@Column(name="user_id")] (nullable) — bare UUID; can be null for anonymous requests
  - username:       String       [@Column] (nullable)
  - action:         String       [@Column(nullable=false)]
  - resourceType:   String       [@Column(name="resource_type")] (nullable)
  - resourceId:     String       [@Column(name="resource_id")] (nullable)
  - httpMethod:     String       [@Column(name="http_method", nullable=false)]
  - requestPath:    String       [@Column(name="request_path", nullable=false)]
  - ipAddress:      String       [@Column(name="ip_address")] (nullable)
  - userAgent:      String       [@Column(name="user_agent")] (nullable)
  - responseStatus: int          [@Column(name="response_status")]
  - outcome:        AuditOutcome [@Enumerated(STRING), @Column(nullable=false)]
  - durationMs:     long         [@Column(name="duration_ms")]
  - timestamp:      Instant      [@Column(nullable=false)] — manually set by caller, NOT @CreatedDate

Relationships:
  - None declared

Audit Fields:
  - timestamp:  YES (equivalent of createdAt, manually set — not @CreatedDate)
  - updatedAt:  MISSING (intentional — immutable audit record)
  - createdBy:  MISSING (userId/username serve this purpose)
  - version:    MISSING (intentional — immutable)

Validation: None

Custom Methods:
  - No @PrePersist, @PreUpdate, or @EntityListeners — intentionally immutable record
  - No Spring Data Auditing integration
  - Notable: NO @EntityListeners(AuditingEntityListener.class) — sole entity without it
```

---

### Enum: AuditOutcome.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/privacy/model/AuditOutcome.java`
Values: `SUCCESS`, `FAILURE`, `DENIED`

---

## PACKAGE: com.myoffgridai.proactive.model

---

```
=== Insight.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/proactive/model/Insight.java
Table: insights
Indexes:
  - idx_insight_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:           UUID            [@Id, @GeneratedValue(UUID)]
  - userId:       UUID            [@Column(name="user_id", nullable=false)] — bare UUID
  - content:      String          [@Column(nullable=false, columnDefinition="TEXT")]
  - category:     InsightCategory [@Enumerated(STRING), @Column(nullable=false)]
  - isRead:       boolean         [@Column(name="is_read", nullable=false)] default=false
  - isDismissed:  boolean         [@Column(name="is_dismissed", nullable=false)] default=false
  - generatedAt:  Instant         [@Column(name="generated_at", nullable=false, updatable=false)]
                                  NOTE: Not @CreatedDate — set in @PrePersist manually
  - readAt:       Instant         [@Column(name="read_at")] (nullable)

Relationships:
  - None declared

Audit Fields:
  - generatedAt:  YES (plays role of createdAt; set in @PrePersist, NOT @CreatedDate)
  - updatedAt:    MISSING
  - createdBy:    MISSING
  - version:      MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets generatedAt = Instant.now() if null
  - getIsRead() / getIsDismissed() — non-standard getter names
```

---

```
=== Notification.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/proactive/model/Notification.java
Table: notifications
Indexes:
  - idx_notification_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:             UUID                 [@Id, @GeneratedValue(UUID)]
  - userId:         UUID                 [@Column(name="user_id", nullable=false)] — bare UUID
  - title:          String               [@Column(nullable=false)]
  - body:           String               [@Column(nullable=false, columnDefinition="TEXT")]
  - type:           NotificationType     [@Enumerated(STRING), @Column(nullable=false)]
  - isRead:         boolean              [@Column(name="is_read", nullable=false)] default=false
  - createdAt:      Instant              [@Column(name="created_at", nullable=false, updatable=false)]
                                         NOTE: Not @CreatedDate — set in @PrePersist
  - readAt:         Instant              [@Column(name="read_at")] (nullable)
  - severity:       NotificationSeverity [@Enumerated(STRING), @Column(length=20)] (nullable)
  - mqttDelivered:  boolean              [@Column(name="mqtt_delivered", nullable=false,
                                          columnDefinition="boolean not null default false")] default=false
  - metadata:       String               [@Column(columnDefinition="TEXT")] (nullable)

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (set in @PrePersist; NOT @CreatedDate annotation)
  - updatedAt:  MISSING
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt = Instant.now() if null
  - getIsRead() — non-standard getter name
  - getMqttDelivered() / setMqttDelivered() — non-standard getter name
```

---

### Enum: InsightCategory.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/proactive/model/InsightCategory.java`
Values: `HOMESTEAD`, `HEALTH`, `RESOURCE`, `GENERAL`

### Enum: NotificationType.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/proactive/model/NotificationType.java`
Values: `SENSOR_ALERT`, `SYSTEM_HEALTH`, `INSIGHT_READY`, `MODEL_UPDATE`, `GENERAL`

### Enum: NotificationSeverity.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/proactive/model/NotificationSeverity.java`
Values: `INFO`, `WARNING`, `CRITICAL`

---

## PACKAGE: com.myoffgridai.sensors.model

---

```
=== Sensor.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/sensors/model/Sensor.java
Table: sensors
Indexes:
  - idx_sensor_user_id   → columnList = "user_id"
  - idx_sensor_port_path → columnList = "port_path", unique = true
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                   UUID       [@Id, @GeneratedValue(UUID)]
  - userId:               UUID       [@Column(name="user_id", nullable=false)] — bare UUID
  - name:                 String     [@Column(nullable=false)]
  - type:                 SensorType [@Enumerated(STRING), @Column(nullable=false)]
  - portPath:             String     [@Column(name="port_path", nullable=false)]
                                     NOTE: Also has unique index (idx_sensor_port_path)
  - baudRate:             int        [@Column(name="baud_rate", nullable=false)] default=9600
  - dataFormat:           DataFormat [@Enumerated(STRING), @Column(name="data_format", nullable=false)] default=CSV_LINE
  - valueField:           String     [@Column(name="value_field")] (nullable)
  - unit:                 String     [@Column] (nullable)
  - isActive:             boolean    [@Column(name="is_active", nullable=false)] default=false
  - pollIntervalSeconds:  int        [@Column(name="poll_interval_seconds", nullable=false)] default=30
  - lowThreshold:         Double     [@Column(name="low_threshold")] (nullable)
  - highThreshold:        Double     [@Column(name="high_threshold")] (nullable)
  - createdAt:            Instant    [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:            Instant    [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared on Sensor itself. SensorReading has @ManyToOne back to Sensor.

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
  - getIsActive() — non-standard getter name
```

---

```
=== SensorReading.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/sensors/model/SensorReading.java
Table: sensor_readings
Indexes:
  - idx_sensor_reading_sensor_recorded → columnList = "sensor_id, recorded_at DESC"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:         UUID    [@Id, @GeneratedValue(UUID)]
  - sensor:     Sensor  [@ManyToOne LAZY, @JoinColumn(name="sensor_id", nullable=false)]
  - value:      double  [@Column(nullable=false)]
  - rawData:    String  [@Column(name="raw_data")] (nullable)
  - recordedAt: Instant [@Column(name="recorded_at", nullable=false)] — manually set by caller

Relationships:
  - @ManyToOne(fetch=LAZY) → Sensor (@JoinColumn = "sensor_id", nullable=false)

Audit Fields:
  - recordedAt:  YES (plays role of createdAt; manually set by caller, NOT @CreatedDate)
  - updatedAt:   MISSING (intentional for time-series data)
  - createdBy:   MISSING
  - version:     MISSING

Validation: None

Custom Methods:
  - No @PrePersist, no @EntityListeners — NOT using Spring Data Auditing
  - Notable: Only entity without @EntityListeners besides AuditLog
```

---

### Enum: SensorType.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/sensors/model/SensorType.java`
Values: `TEMPERATURE`, `HUMIDITY`, `SOIL_MOISTURE`, `POWER`, `VOLTAGE`, `CUSTOM`

### Enum: DataFormat.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/sensors/model/DataFormat.java`
Values: `CSV_LINE`, `JSON_LINE`

---

## PACKAGE: com.myoffgridai.settings.model

---

```
=== ExternalApiSettings.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/settings/model/ExternalApiSettings.java
Table: external_api_settings
Primary Key: id, UUID, GenerationType.UUID
Pattern: Singleton entity (one row enforced by unique singletonGuard column)

Fields:
  - id:                        UUID             [@Id, @GeneratedValue(UUID)]
  - singletonGuard:            String           [@Column(name="singleton_guard", unique=true, nullable=false)]
                                                default="SINGLETON" — enforces single row
  - anthropicApiKey:           String           [@Convert(AesAttributeConverter), @Column(name="anthropic_api_key")]
                                                AES-256-GCM encrypted at rest
  - anthropicModel:            String           [@Column(name="anthropic_model", nullable=false)]
                                                default="claude-sonnet-4-20250514"
  - anthropicEnabled:          boolean          [@Column(name="anthropic_enabled", nullable=false)] default=false
  - braveApiKey:               String           [@Convert(AesAttributeConverter), @Column(name="brave_api_key")]
                                                AES-256-GCM encrypted at rest
  - braveEnabled:              boolean          [@Column(name="brave_enabled", nullable=false)] default=false
  - maxWebFetchSizeKb:         int              [@Column(name="max_web_fetch_size_kb", nullable=false)] default=512
  - searchResultLimit:         int              [@Column(name="search_result_limit", nullable=false)] default=5
  - huggingFaceToken:          String           [@Convert(AesAttributeConverter), @Column(name="hugging_face_token")]
                                                AES-256-GCM encrypted at rest
  - huggingFaceEnabled:        boolean          [@Column(name="hugging_face_enabled", nullable=false)] default=false
  - grokApiKey:                String           [@Convert(AesAttributeConverter), @Column(name="grok_api_key", length=1000)]
                                                AES-256-GCM encrypted at rest
  - grokEnabled:               boolean          [@Column(name="grok_enabled", nullable=false)] default=false
  - openAiApiKey:              String           [@Convert(AesAttributeConverter), @Column(name="openai_api_key", length=1000)]
                                                AES-256-GCM encrypted at rest
  - openAiEnabled:             boolean          [@Column(name="openai_enabled", nullable=false)] default=false
  - preferredFrontierProvider: FrontierProvider [@Enumerated(STRING), @Column(name="preferred_frontier_provider", length=20)]
                                                default=FrontierProvider.CLAUDE
  - judgeEnabled:              boolean          [@Column(name="judge_enabled", nullable=false)] default=false
  - judgeModelFilename:        String           [@Column(name="judge_model_filename", length=500)] (nullable)
  - judgeScoreThreshold:       double           [@Column(name="judge_score_threshold", nullable=false)]
                                                default=AppConstants.JUDGE_DEFAULT_SCORE_THRESHOLD
  - createdAt:                 Instant          [@CreatedDate, @Column(name="created_at", updatable=false)]
                                                NOTE: nullable=false NOT set on the @Column — different from other entities
  - updatedAt:                 Instant          [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None (no JSR-380)

Custom Methods:
  - @PrePersist onPrePersist(): sets createdAt if null, updatedAt if null
  - @PreUpdate onPreUpdate(): sets updatedAt = Instant.now()
  - setAnthropicModel(): null-safe default fallback to "claude-sonnet-4-20250514"
  - setMaxWebFetchSizeKb(): guards against <= 0, falls back to 512
  - setSearchResultLimit(): guards against <= 0, falls back to 5
  - setPreferredFrontierProvider(): null-safe fallback to FrontierProvider.CLAUDE
  - setJudgeScoreThreshold(): range-validates 0.0–10.0; falls back to AppConstants.JUDGE_DEFAULT_SCORE_THRESHOLD
  - getSingletonGuard(): no setter — effectively read-only field
  - getCreatedAt() / getUpdatedAt(): no setters exposed (read-only audit fields)
  - NOTE: createdAt @Column lacks nullable=false unlike all other entities
```

---

## PACKAGE: com.myoffgridai.skills.model

---

```
=== Skill.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/Skill.java
Table: skills
Indexes:
  - idx_skill_name     → columnList = "name", unique = true
  - idx_skill_category → columnList = "category"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:               UUID          [@Id, @GeneratedValue(UUID)]
  - name:             String        [@Column(nullable=false, unique=true)]
  - displayName:      String        [@Column(name="display_name", nullable=false)]
  - description:      String        [@Column(nullable=false, columnDefinition="TEXT")]
  - version:          String        [@Column(nullable=false)]
  - author:           String        [@Column(nullable=false)]
  - category:         SkillCategory [@Enumerated(STRING), @Column(nullable=false)]
  - isEnabled:        boolean       [@Column(name="is_enabled", nullable=false)] default=true
  - isBuiltIn:        boolean       [@Column(name="is_built_in", nullable=false)] default=false
  - parametersSchema: String        [@Column(name="parameters_schema", columnDefinition="TEXT")] (nullable)
                                    Stores JSON Schema for skill input parameters
  - createdAt:        Instant       [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:        Instant       [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared on Skill itself. SkillExecution has @ManyToOne back to Skill.

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING (note: "version" field exists but is a skill version string, not @Version)

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
  - getIsEnabled() / getIsBuiltIn() — non-standard getter names
```

---

```
=== SkillExecution.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/SkillExecution.java
Table: skill_executions
Indexes:
  - idx_skill_exec_user_id  → columnList = "user_id"
  - idx_skill_exec_skill_id → columnList = "skill_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:           UUID            [@Id, @GeneratedValue(UUID)]
  - skill:        Skill           [@ManyToOne LAZY, @JoinColumn(name="skill_id", nullable=false)]
  - userId:       UUID            [@Column(name="user_id", nullable=false)] — bare UUID
  - status:       ExecutionStatus [@Enumerated(STRING), @Column(nullable=false)]
  - inputParams:  String          [@Column(name="input_params", columnDefinition="TEXT")] (nullable) — JSON
  - outputResult: String          [@Column(name="output_result", columnDefinition="TEXT")] (nullable) — JSON
  - errorMessage: String          [@Column(name="error_message", columnDefinition="TEXT")] (nullable)
  - startedAt:    Instant         [@CreatedDate, @Column(name="started_at", nullable=false, updatable=false)]
  - completedAt:  Instant         [@Column(name="completed_at")] (nullable)
  - durationMs:   Long            [@Column(name="duration_ms")] (nullable)

Relationships:
  - @ManyToOne(fetch=LAZY) → Skill (@JoinColumn = "skill_id", nullable=false)

Audit Fields:
  - startedAt:   YES (plays role of createdAt; @CreatedDate + @PrePersist)
  - updatedAt:   MISSING
  - createdBy:   MISSING (userId present as raw UUID)
  - version:     MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets startedAt = Instant.now() if null
```

---

```
=== InventoryItem.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/InventoryItem.java
Table: inventory_items
Indexes:
  - idx_inventory_user_id  → columnList = "user_id"
  - idx_inventory_category → columnList = "user_id, category"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                UUID              [@Id, @GeneratedValue(UUID)]
  - userId:            UUID              [@Column(name="user_id", nullable=false)] — bare UUID
  - name:              String            [@Column(nullable=false)]
  - category:          InventoryCategory [@Enumerated(STRING), @Column(nullable=false)]
  - quantity:          double            [@Column(nullable=false)]
  - unit:              String            [@Column] (nullable)
  - notes:             String            [@Column(columnDefinition="TEXT")] (nullable)
  - lowStockThreshold: Double            [@Column(name="low_stock_threshold")] (nullable)
  - createdAt:         Instant           [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:         Instant           [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
```

---

```
=== PlannedTask.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/PlannedTask.java
Table: planned_tasks
Indexes:
  - idx_planned_task_user_id → columnList = "user_id"
Primary Key: id, UUID, GenerationType.UUID

Fields:
  - id:                 UUID       [@Id, @GeneratedValue(UUID)]
  - userId:             UUID       [@Column(name="user_id", nullable=false)] — bare UUID
  - goalDescription:    String     [@Column(name="goal_description", columnDefinition="TEXT", nullable=false)]
  - title:              String     [@Column(nullable=false)]
  - steps:              String     [@Column(columnDefinition="TEXT", nullable=false)]
                                   NOTE: JSON array of step strings stored as TEXT
  - estimatedResources: String     [@Column(name="estimated_resources", columnDefinition="TEXT")] (nullable)
                                   NOTE: JSON object stored as TEXT
  - status:             TaskStatus [@Enumerated(STRING), @Column(nullable=false)] default=ACTIVE
  - createdAt:          Instant    [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:          Instant    [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (@CreatedDate + @PrePersist)
  - updatedAt:  YES (@LastModifiedDate + @PrePersist + @PreUpdate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - @PrePersist onCreate(): sets createdAt if null, updatedAt = Instant.now()
  - @PreUpdate onUpdate(): sets updatedAt = Instant.now()
```

---

### Enum: SkillCategory.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/SkillCategory.java`
Values: `HOMESTEAD`, `RESOURCE`, `PLANNING`, `KNOWLEDGE`, `WEATHER`, `CUSTOM`

### Enum: ExecutionStatus.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/ExecutionStatus.java`
Values: `RUNNING`, `COMPLETED`, `FAILED`

### Enum: TaskStatus.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/TaskStatus.java`
Values: `ACTIVE`, `COMPLETED`, `CANCELLED`

### Enum: InventoryCategory.java
File: `/Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/skills/model/InventoryCategory.java`
Values: `FOOD`, `TOOLS`, `MEDICAL`, `SUPPLIES`, `SEEDS`, `EQUIPMENT`, `OTHER`

---

## PACKAGE: com.myoffgridai.system.model

---

```
=== SystemConfig.java ===
File: /Users/adamallard/Documents/GitHub/MyOffGridAI-Server/src/main/java/com/myoffgridai/system/model/SystemConfig.java
Table: system_config
Primary Key: id, UUID, GenerationType.UUID
Pattern: Singleton entity (application maintains single row)

Fields:
  - id:                       UUID    [@Id, @GeneratedValue(UUID)]
  - initialized:              boolean [@Column(nullable=false)] default=false
  - instanceName:             String  [@Column(name="instance_name")] (nullable)
  - fortressEnabled:          boolean [@Column(name="fortress_enabled", nullable=false)] default=false
  - fortressEnabledAt:        Instant [@Column(name="fortress_enabled_at")] (nullable)
  - fortressEnabledByUserId:  UUID    [@Column(name="fortress_enabled_by_user_id")] (nullable) — bare UUID
  - apModeEnabled:            boolean [@Column(name="ap_mode_enabled", nullable=false)] default=false
  - wifiConfigured:           boolean [@Column(name="wifi_configured", nullable=false)] default=false
  - aiModel:                  String  [@Column(name="ai_model")] default="hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M"
  - aiTemperature:            Double  [@Column(name="ai_temperature")] default=0.7
  - aiSimilarityThreshold:    Double  [@Column(name="ai_similarity_threshold")] default=0.45
  - aiMemoryTopK:             Integer [@Column(name="ai_memory_top_k")] default=5
  - aiRagMaxContextTokens:    Integer [@Column(name="ai_rag_max_context_tokens")] default=2048
  - aiContextSize:            Integer [@Column(name="ai_context_size")] default=4096
  - aiContextMessageLimit:    Integer [@Column(name="ai_context_message_limit")] default=20
  - activeModelFilename:      String  [@Column(name="active_model_filename")] (nullable)
  - knowledgeStoragePath:     String  [@Column(name="knowledge_storage_path")]
                                      default="/var/myoffgridai/knowledge"
  - maxUploadSizeMb:          Integer [@Column(name="max_upload_size_mb")] default=25
  - createdAt:                Instant [@CreatedDate, @Column(name="created_at", nullable=false, updatable=false)]
  - updatedAt:                Instant [@LastModifiedDate, @Column(name="updated_at")]

Relationships:
  - None declared

Audit Fields:
  - createdAt:  YES (@CreatedDate)
  - updatedAt:  YES (@LastModifiedDate)
  - createdBy:  MISSING
  - version:    MISSING

Validation: None

Custom Methods:
  - No @PrePersist / @PreUpdate — relies entirely on Spring Data Auditing (@CreatedDate / @LastModifiedDate)
  - All getters for nullable fields apply null-safe defaults inline:
    - getAiModel(): returns default "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M" if null
    - getAiTemperature(): returns 0.7 if null
    - getAiSimilarityThreshold(): returns 0.45 if null
    - getAiMemoryTopK(): returns 5 if null
    - getAiRagMaxContextTokens(): returns 2048 if null
    - getAiContextSize(): returns 4096 if null
    - getAiContextMessageLimit(): returns 20 if null
    - getKnowledgeStoragePath(): returns "/var/myoffgridai/knowledge" if null
    - getMaxUploadSizeMb(): returns 25 if null
```

---

## Summary Table

| Entity | Table | PK Type | createdAt | updatedAt | @EntityListeners | Notable |
|---|---|---|---|---|---|---|
| Conversation | conversations | UUID | YES | YES | YES | @ManyToOne → User |
| Message | messages | UUID | YES | MISSING | YES | @ManyToOne → Conversation; sourceTag, judgeScore fields |
| User | users | UUID | YES | YES | YES | Implements UserDetails |
| ScheduledEvent | scheduled_events | UUID | YES | YES | YES | Raw UUID FKs; compound index on is_enabled+event_type |
| KnowledgeDocument | knowledge_documents | UUID | uploadedAt only | MISSING | YES | No @OneToMany to chunks |
| KnowledgeChunk | knowledge_chunks | UUID | YES | MISSING | YES | @ManyToOne → KnowledgeDocument; denormalized userId |
| Ebook | ebooks | UUID | uploadedAt only | MISSING | YES | gutenbergId index |
| ZimFile | zim_files | UUID | uploadedAt only | MISSING | YES | createdDate is String (metadata) |
| McpApiToken | mcp_api_tokens | UUID | YES | MISSING | YES | BCrypt-hashed token |
| Memory | memories | UUID | YES | YES | YES | tags as free-text String |
| VectorDocument | vector_document | UUID | YES | MISSING | YES | pgvector float[768]; custom @Type |
| DeviceRegistration | device_registrations | UUID | YES (manual) | YES (manual) | YES | UniqueConstraint on (userId,deviceId); no @CreatedDate |
| AuditLog | audit_logs | UUID | timestamp (manual) | MISSING | NO | Immutable; no AuditingEntityListener |
| Insight | insights | UUID | generatedAt (manual) | MISSING | YES | no @CreatedDate |
| Notification | notifications | UUID | YES (manual) | MISSING | YES | mqttDelivered flag; no @CreatedDate |
| Sensor | sensors | UUID | YES | YES | YES | portPath has unique index |
| SensorReading | sensor_readings | UUID | recordedAt (manual) | MISSING | NO | @ManyToOne → Sensor; time-series; no AuditingEntityListener |
| ExternalApiSettings | external_api_settings | UUID | YES | YES | YES | Singleton; 4 AES-encrypted keys; FrontierProvider enum |
| Skill | skills | UUID | YES | YES | YES | name unique; parametersSchema = JSON Schema |
| SkillExecution | skill_executions | UUID | startedAt | MISSING | YES | @ManyToOne → Skill |
| InventoryItem | inventory_items | UUID | YES | YES | YES | lowStockThreshold |
| PlannedTask | planned_tasks | UUID | YES | YES | YES | steps stored as TEXT (JSON array) |
| SystemConfig | system_config | UUID | YES | YES | YES | Singleton; null-safe getter defaults |

---

## Cross-Cutting Observations

**No @Version (optimistic locking) on any entity** — the entire codebase has zero `@Version` fields. Last-write-wins semantics everywhere.

**No JSR-380 bean validation on any entity** — zero `@NotNull`, `@Size`, `@Email`, `@Pattern`, etc. All validation must be happening in service/controller layer or not at all.

**No @CreatedBy / @LastModifiedBy audit fields on any entity** — Spring Data Auditing is wired for dates only; authorship is tracked via raw `userId` UUID columns where needed.

**Inconsistent audit timestamp strategy** — some entities use `@CreatedDate` (Conversation, Message, Sensor, Skill, etc.), others set timestamps manually in `@PrePersist` without the annotation (DeviceRegistration, Notification, Insight, SensorReading). AuditLog and SensorReading have no `@EntityListeners` at all.

**Widespread use of raw UUID foreign keys instead of @ManyToOne** — only 5 entities use proper JPA relationships: Conversation→User, Message→Conversation, KnowledgeChunk→KnowledgeDocument, SensorReading→Sensor, SkillExecution→Skill. All others (Memory, VectorDocument, ScheduledEvent, etc.) store foreign keys as bare `UUID userId` columns, bypassing JPA relationship management entirely.

**VectorDocument table name is singular** (`vector_document`) while all other tables use plural names — inconsistency in naming convention.

**ExternalApiSettings.createdAt @Column missing `nullable=false`** — unlike every other `createdAt` column definition in the codebase.

**PlannedTask.steps stored as TEXT (JSON)** — no dedicated column or @Lob type; JSON array serialized as plain string.


## 7. Enum Inventory

**Total Enums: 24**

```
=== MessageRole.java === (ai/model)
Values: USER, ASSISTANT, SYSTEM
Used in: Message, MessageDto, ChatService

=== SourceTag.java === (ai/)
Values: LOCAL, ENHANCED
Used in: Message, MessageDto, ChatService, ChatController

=== ChunkType.java === (ai/dto)
Values: THINKING, CONTENT, DONE, JUDGE_EVALUATING, JUDGE_RESULT, ENHANCED_CONTENT, ENHANCED_DONE
Used in: InferenceChunk, InferenceMetadata, InferenceService, ChatService, all inference services

=== NativeLlamaStatus.java === (ai/service)
Values: UNLOADED, LOADING, READY, ERROR
Used in: NativeLlamaInferenceService, NativeLlamaStatusDto, SystemController

=== Role.java === (auth/model)
Values: ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD
Used in: User, UserDetailDto, UserSummaryDto, RegisterRequest, UpdateUserRequest, UserRepository, UserService, AuthService

=== FrontierProvider.java === (frontier/)
Values: CLAUDE, GROK, OPENAI
Used in: ExternalApiSettings, FrontierApiRouter, all frontier clients, ExternalApiSettingsService

=== DocumentStatus.java === (knowledge/model)
Values: PENDING, PROCESSING, READY, FAILED
Used in: KnowledgeDocument, KnowledgeDocumentDto, KnowledgeService, KnowledgeDocumentRepository

=== EbookFormat.java === (library/model)
Values: EPUB, PDF, MOBI, AZW, TXT, HTML
Used in: Ebook, EbookDto, EbookService, EbookRepository, GutenbergService, CalibreConversionService

=== EventType.java === (events/model)
Values: SCHEDULED, SENSOR_THRESHOLD, RECURRING
Used in: ScheduledEvent, ScheduledEventDto, CreateEventRequest

=== ActionType.java === (events/model)
Values: PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY
Used in: ScheduledEvent, ScheduledEventDto, CreateEventRequest

=== ThresholdOperator.java === (events/model)
Values: ABOVE, BELOW, EQUALS
Used in: ScheduledEvent, ScheduledEventDto, CreateEventRequest

=== MemoryImportance.java === (memory/model)
Values: LOW, MEDIUM, HIGH, CRITICAL
Used in: Memory, MemoryDto, UpdateImportanceRequest, MemoryRepository, MemoryService

=== VectorSourceType.java === (memory/model)
Values: MEMORY, CONVERSATION, KNOWLEDGE_CHUNK
Used in: VectorDocument, VectorDocumentRepository, MemoryService, KnowledgeService, SemanticSearchService

=== InsightCategory.java === (proactive/model)
Values: HOMESTEAD, HEALTH, RESOURCE, GENERAL
Used in: Insight, InsightDto, InsightRepository, InsightService

=== NotificationSeverity.java === (proactive/model)
Values: INFO, WARNING, CRITICAL
Used in: Notification, NotificationDto, InsightGeneratorService, SystemHealthMonitor

=== NotificationType.java === (proactive/model)
Values: SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL
Used in: Notification, NotificationDto, NotificationPayload, InsightGeneratorService, SystemHealthMonitor

=== SensorType.java === (sensors/model)
Values: TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM
Used in: Sensor, SensorDto, CreateSensorRequest

=== DataFormat.java === (sensors/model)
Values: CSV_LINE, JSON_LINE
Used in: Sensor, SensorDto, CreateSensorRequest, SensorPollingService

=== SkillCategory.java === (skills/model)
Values: HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM
Used in: Skill, SkillDto, SkillRepository, SkillSeederService

=== ExecutionStatus.java === (skills/model)
Values: RUNNING, COMPLETED, FAILED
Used in: SkillExecution, SkillExecutionDto, SkillExecutorService

=== TaskStatus.java === (skills/model)
Values: ACTIVE, COMPLETED, CANCELLED
Used in: PlannedTask, PlannedTaskRepository, TaskPlannerSkill

=== InventoryCategory.java === (skills/model)
Values: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER
Used in: InventoryItem, InventoryItemDto, CreateInventoryItemRequest, InventoryTrackerSkill

=== AuditOutcome.java === (privacy/model)
Values: SUCCESS, FAILURE, DENIED
Used in: AuditLog, AuditLogDto, AuditService, AuditAspect

=== QuantizationType.java === (models/dto)
Values: IQ1_S, IQ2_XS, Q2_K, Q3_K_XS, Q3_K_S, Q3_K_M, Q4_K_S, Q4_K_M, Q5_K_S, Q5_K_M, Q6_K, Q8_0, F16, BF16, F32
Has display label: YES (getRank() and getLabel() with human-readable descriptions)
Used in: HfModelFileDto, QuantizationRecommendationService

=== DownloadStatus.java === (models/dto)
Values: QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
Used in: DownloadProgress, ModelDownloadService

=== VectorType.java === (config/)
Values: (referenced in VectorStoreConfig for pgvector type registration)
```


## 8. Repository Layer

**Total Repositories: 23** — All extend `JpaRepository`. No `@EntityGraph` or projections used. User-scoped filtering pattern throughout.

```
=== ConversationRepository.java === (ai/repository)
Entity: Conversation
Extends: JpaRepository<Conversation, UUID>
Custom Methods:
  - findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable): Page<Conversation>
  - findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID userId, boolean, Pageable): Page<Conversation>
  - findByIdAndUserId(UUID id, UUID userId): Optional<Conversation>
  - countByUserId(UUID userId): long
  - findByUserId(UUID userId): List<Conversation>
  - findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID userId, String title, Pageable): Page<Conversation>
  - @Modifying @Query deleteByUserId(UUID userId): void

=== MessageRepository.java === (ai/repository)
Entity: Message
Extends: JpaRepository<Message, UUID>
Custom Methods:
  - findByConversationIdOrderByCreatedAtAsc(UUID): List<Message>
  - findByConversationIdOrderByCreatedAtAsc(UUID, Pageable): Page<Message>
  - findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable): List<Message>
  - countByConversationId(UUID): long
  - deleteByConversationId(UUID): void
  - @Query countByUserId(UUID): long
  - @Modifying @Query deleteByUserId(UUID): void
  - @Modifying @Query deleteMessagesAfter(UUID conversationId, UUID messageId): void

=== UserRepository.java === (auth/repository)
Entity: User
Extends: JpaRepository<User, UUID>
Custom Methods:
  - findByUsername(String): Optional<User>
  - findByEmail(String): Optional<User>
  - existsByUsername(String): boolean
  - existsByEmail(String): boolean
  - findAllByRole(Role): List<User>
  - countByIsActiveTrue(): long
  - findByIsActiveTrue(): List<User>

=== ScheduledEventRepository.java === (events/repository)
Entity: ScheduledEvent
Extends: JpaRepository<ScheduledEvent, UUID>
Custom Methods:
  - findAllByUserId(UUID, Pageable): Page<ScheduledEvent>
  - findByIdAndUserId(UUID id, UUID userId): Optional<ScheduledEvent>
  - findByIsEnabledTrueAndEventType(EventType): List<ScheduledEvent>
  - findAllByUserIdOrderByCreatedAtDesc(UUID userId): List<ScheduledEvent>
  - deleteByUserId(UUID): void
  - countByUserId(UUID): long

=== KnowledgeDocumentRepository.java === (knowledge/repository)
Entity: KnowledgeDocument
Extends: JpaRepository<KnowledgeDocument, UUID>
Custom Methods:
  - findByUserIdOrderByUploadedAtDesc(UUID, Pageable): Page<KnowledgeDocument>
  - findByIdAndUserId(UUID, UUID): Optional<KnowledgeDocument>
  - findByUserIdAndStatus(UUID, DocumentStatus): List<KnowledgeDocument>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long

=== KnowledgeChunkRepository.java === (knowledge/repository)
Entity: KnowledgeChunk
Extends: JpaRepository<KnowledgeChunk, UUID>
Custom Methods:
  - findByDocumentIdOrderByChunkIndexAsc(UUID): List<KnowledgeChunk>
  - @Modifying deleteByDocumentId(UUID): void
  - @Modifying deleteByUserId(UUID): void
  - countByDocumentId(UUID): long

=== ZimFileRepository.java === (library/repository)
Entity: ZimFile
Extends: JpaRepository<ZimFile, UUID>
Custom Methods:
  - findByFilename(String): Optional<ZimFile>
  - findAllByOrderByDisplayNameAsc(): List<ZimFile>
  - existsByFilename(String): boolean

=== EbookRepository.java === (library/repository)
Entity: Ebook
Extends: JpaRepository<Ebook, UUID>
Custom Methods:
  - @Query searchByTitleOrAuthor(String, EbookFormat, Pageable): Page<Ebook>
  - findByGutenbergId(String): Optional<Ebook>
  - existsByGutenbergId(String): boolean

=== MemoryRepository.java === (memory/repository)
Entity: Memory
Extends: JpaRepository<Memory, UUID>
Custom Methods:
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Memory>
  - findByUserIdAndImportance(UUID, MemoryImportance, Pageable): Page<Memory>
  - findByUserIdAndTagsContaining(UUID, String, Pageable): Page<Memory>
  - findByUserId(UUID): List<Memory>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long

=== VectorDocumentRepository.java === (memory/repository)
Entity: VectorDocument
Extends: JpaRepository<VectorDocument, UUID>
Custom Methods:
  - findByUserIdAndSourceType(UUID, VectorSourceType): List<VectorDocument>
  - @Modifying deleteBySourceIdAndSourceType(UUID, VectorSourceType): void
  - @Modifying deleteByUserId(UUID): void
  - @Query(nativeQuery) findMostSimilar(UUID userId, String sourceType, String embedding, int topK): List<VectorDocument>
  - @Query(nativeQuery) findMostSimilarAcrossTypes(UUID userId, String embedding, int topK): List<VectorDocument>
  NOTE: Uses pgvector `<=>` cosine distance for similarity search.

=== McpApiTokenRepository.java === (mcp/repository)
Entity: McpApiToken
Extends: JpaRepository<McpApiToken, UUID>
Custom Methods:
  - findByIsActiveTrue(): List<McpApiToken>
  - findByCreatedByOrderByCreatedAtDesc(UUID): List<McpApiToken>

=== DeviceRegistrationRepository.java === (notification/repository)
Entity: DeviceRegistration
Extends: JpaRepository<DeviceRegistration, UUID>
Custom Methods:
  - findByUserIdAndDeviceId(UUID, String): Optional<DeviceRegistration>
  - findByUserId(UUID): List<DeviceRegistration>
  - deleteByUserIdAndDeviceId(UUID, String): void

=== AuditLogRepository.java === (privacy/repository)
Entity: AuditLog
Extends: JpaRepository<AuditLog, UUID>
Custom Methods:
  - findAllByOrderByTimestampDesc(Pageable): Page<AuditLog>
  - findByUserIdOrderByTimestampDesc(UUID, Pageable): Page<AuditLog>
  - findByOutcomeOrderByTimestampDesc(AuditOutcome, Pageable): Page<AuditLog>
  - findByTimestampBetweenOrderByTimestampDesc(Instant, Instant, Pageable): Page<AuditLog>
  - findByUserIdAndTimestampBetween(UUID, Instant, Instant, Pageable): Page<AuditLog>
  - countByOutcomeAndTimestampBetween(AuditOutcome, Instant, Instant): long
  - @Modifying deleteByTimestampBefore(Instant): void
  - @Modifying deleteByUserId(UUID): void

=== InsightRepository.java === (proactive/repository)
Entity: Insight
Extends: JpaRepository<Insight, UUID>
Custom Methods:
  - findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID, Pageable): Page<Insight>
  - findByUserIdAndCategoryAndIsDismissedFalse(UUID, InsightCategory, Pageable): Page<Insight>
  - findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): List<Insight>
  - countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Insight>
  - countByUserId(UUID): long
  - @Modifying deleteByUserId(UUID): void

=== NotificationRepository.java === (proactive/repository)
Entity: Notification
Extends: JpaRepository<Notification, UUID>
Custom Methods:
  - findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID): List<Notification>
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Notification>
  - countByUserIdAndIsReadFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Notification>
  - @Modifying @Query markAllReadForUser(UUID, Instant): void
  - @Modifying deleteByUserId(UUID): void

=== SensorRepository.java === (sensors/repository)
Entity: Sensor
Extends: JpaRepository<Sensor, UUID>
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<Sensor>
  - findByIdAndUserId(UUID, UUID): Optional<Sensor>
  - findByUserIdAndIsActiveTrue(UUID): List<Sensor>
  - findByPortPath(String): Optional<Sensor>
  - findByIsActiveTrue(): List<Sensor>
  - countByUserId(UUID): long
  - deleteByUserId(UUID): void

=== SensorReadingRepository.java === (sensors/repository)
Entity: SensorReading
Extends: JpaRepository<SensorReading, UUID>
Custom Methods:
  - findBySensorIdOrderByRecordedAtDesc(UUID, Pageable): Page<SensorReading>
  - findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID, Instant): List<SensorReading>
  - findTopBySensorIdOrderByRecordedAtDesc(UUID): Optional<SensorReading>
  - @Modifying deleteBySensorId(UUID): void
  - @Modifying @Query deleteByUserId(UUID): void
  - @Query(nativeQuery) findAverageValueSince(UUID sensorId, Instant since): Double

=== ExternalApiSettingsRepository.java === (settings/repository)
Entity: ExternalApiSettings
Extends: JpaRepository<ExternalApiSettings, UUID>
Custom Methods:
  - findBySingletonGuard(String): Optional<ExternalApiSettings>

=== SkillRepository.java === (skills/repository)
Entity: Skill
Extends: JpaRepository<Skill, UUID>
Custom Methods:
  - findByIsEnabledTrue(): List<Skill>
  - findByIsBuiltInTrue(): List<Skill>
  - findByCategory(SkillCategory): List<Skill>
  - findByName(String): Optional<Skill>
  - findByIsEnabledTrueOrderByDisplayNameAsc(): List<Skill>

=== SkillExecutionRepository.java === (skills/repository)
Entity: SkillExecution
Extends: JpaRepository<SkillExecution, UUID>
Custom Methods:
  - findByUserIdOrderByStartedAtDesc(UUID, Pageable): Page<SkillExecution>
  - findBySkillIdAndUserIdOrderByStartedAtDesc(UUID, UUID, Pageable): Page<SkillExecution>
  - findByUserIdAndStatus(UUID, ExecutionStatus): List<SkillExecution>

=== InventoryItemRepository.java === (skills/repository)
Entity: InventoryItem
Extends: JpaRepository<InventoryItem, UUID>
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<InventoryItem>
  - findByUserIdAndCategory(UUID, InventoryCategory): List<InventoryItem>
  - findByUserIdAndQuantityLessThanEqual(UUID, double): List<InventoryItem>
  - findByIdAndUserId(UUID, UUID): Optional<InventoryItem>
  - @Modifying deleteByUserId(UUID): void

=== PlannedTaskRepository.java === (skills/repository)
Entity: PlannedTask
Extends: JpaRepository<PlannedTask, UUID>
Custom Methods:
  - findByUserIdAndStatusOrderByCreatedAtDesc(UUID, TaskStatus, Pageable): Page<PlannedTask>
  - findByIdAndUserId(UUID, UUID): Optional<PlannedTask>
  - @Modifying deleteByUserId(UUID): void

=== SystemConfigRepository.java === (system/repository)
Entity: SystemConfig
Extends: JpaRepository<SystemConfig, UUID>
Custom Methods:
  - @Query("SELECT s FROM SystemConfig s") findFirst(): Optional<SystemConfig>
```


## 9. Service Layer — Full Method Signatures

```
=== AgentService.java ===
Injects: OllamaService, SkillExecutorService, ObjectMapper, SystemConfigService

Public Methods:
  - runAgent(String userPrompt, UUID userId): String
    Purpose: Runs a multi-turn tool-call agent loop: sends a system+user prompt to Ollama, parses JSON tool-call patterns from the response via regex, dispatches each to SkillExecutorService, injects results back into context, and repeats up to AGENT_MAX_ITERATIONS.
    Calls: SystemConfigService.getConfig(), OllamaService.chat(), SkillExecutorService.executeByName(), ObjectMapper.writeValueAsString()
    Throws: None declared (exceptions caught internally)
    Transactional: NO

Private Methods:
  - buildSystemPrompt(): String
  - extractToolCalls(String text): List<ToolCall>
  - formatToolResult(SkillExecution execution): String

---

=== ChatService.java ===
Injects: ConversationRepository, MessageRepository, InferenceService, RagService, ContextWindowService, MemoryExtractionService, JudgeInferenceService, FrontierApiRouter, NotificationService, SseRegistry (or equivalent), SystemConfigService, ObjectMapper

Public Methods:
  - sendMessage(UUID conversationId, String content, UUID userId): Message
    Purpose: Persists user message, assembles context window, calls inference (with optional RAG and judge scoring), persists assistant reply, triggers async memory extraction and title generation.
    Calls: ConversationRepository.findByIdAndUserId(), MessageRepository.save(), ContextWindowService.buildContext(), InferenceService.chat(), RagService.buildRagContext(), JudgeInferenceService.evaluate(), FrontierApiRouter.complete(), MemoryExtractionService.extractAndStore()
    Throws: EntityNotFoundException
    Transactional: YES

  - streamMessage(UUID conversationId, String content, UUID userId): Flux<InferenceChunk>
    Purpose: Streams an SSE response for a user message, accumulating tokens for persistence on completion.
    Calls: ConversationRepository.findByIdAndUserId(), ContextWindowService.buildContext(), InferenceService.streamChatWithThinking(), MessageRepository.save(), MemoryExtractionService.extractAndStore()
    Throws: EntityNotFoundException
    Transactional: NO

  - editMessage(UUID messageId, String newContent, UUID userId): Message
    Purpose: Updates a user message, deletes all subsequent messages in the conversation, and triggers re-inference synchronously.
    Calls: MessageRepository.findById(), MessageRepository.deleteAllByConversationIdAndCreatedAtAfter(), sendMessage()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: YES

  - deleteMessage(UUID messageId, UUID userId): void
    Purpose: Deletes a message after asserting ownership.
    Calls: MessageRepository.findById(), MessageRepository.delete()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: YES

  - branchConversation(UUID conversationId, UUID messageId, UUID userId): Conversation
    Purpose: Creates a new conversation forked from an existing one up to a specified message.
    Calls: ConversationRepository.findByIdAndUserId(), MessageRepository.findAllByConversationIdUpTo(), ConversationRepository.save(), MessageRepository.saveAll()
    Throws: EntityNotFoundException
    Transactional: YES

  - regenerateMessage(UUID conversationId, UUID messageId, UUID userId): Message
    Purpose: Deletes the last assistant message and re-runs inference for the preceding user message.
    Calls: MessageRepository.findById(), MessageRepository.delete(), sendMessage()
    Throws: EntityNotFoundException
    Transactional: YES

  - getMessages(UUID conversationId, UUID userId, int page, int size): Page<Message>
    Purpose: Returns paginated message history for a conversation scoped to the user.
    Calls: ConversationRepository.findByIdAndUserId(), MessageRepository.findByConversationIdOrderByCreatedAtAsc()
    Throws: EntityNotFoundException
    Transactional: NO

  - searchConversations(UUID userId, String query): List<Conversation>
    Purpose: Searches conversations by title for a user.
    Calls: ConversationRepository.findByUserIdAndTitleContainingIgnoreCase()
    Throws: None
    Transactional: NO

  - renameConversation(UUID conversationId, String newTitle, UUID userId): Conversation
    Purpose: Renames a conversation after asserting ownership.
    Calls: ConversationRepository.findByIdAndUserId(), ConversationRepository.save()
    Throws: EntityNotFoundException
    Transactional: YES

  - generateTitle(UUID conversationId, List<OllamaMessage> messages): void
    Purpose: Asynchronously generates a conversation title by prompting inference and persisting the result.
    Calls: InferenceService.chat(), ConversationRepository.findById(), ConversationRepository.save()
    Throws: None (exceptions caught internally)
    Transactional: NO (@Async)

Private Methods:
  - assertOwnership(Message message, UUID userId): void
  - buildTitlePrompt(List<OllamaMessage> messages): String

---

=== ContextWindowService.java ===
Injects: MessageRepository, SystemConfigService

Public Methods:
  - buildContext(UUID conversationId, String newUserMessage, UUID userId): List<OllamaMessage>
    Purpose: Assembles the chat message list from conversation history, prepends the system prompt, appends the new user message, and truncates to the configured token limit.
    Calls: SystemConfigService.getConfig(), MessageRepository.findByConversationIdOrderByCreatedAtAsc(), TokenCounter.count()
    Throws: None
    Transactional: NO

Private Methods:
  - buildSystemPrompt(SystemConfig config): String
  - truncateToTokenLimit(List<OllamaMessage> messages, int maxTokens): List<OllamaMessage>

---

=== InferenceService.java ===
(Interface — no constructor injection)

Public Methods:
  - chat(List<OllamaMessage> messages, UUID userId): String
  - streamChat(List<OllamaMessage> messages, UUID userId): Flux<String>
  - streamChatWithThinking(List<OllamaMessage> messages, UUID userId): Flux<InferenceChunk>
  - embed(String text): float[]
  - isAvailable(): boolean
  - listModels(): List<InferenceModelInfo>
  - getActiveModel(): InferenceModelInfo

---

=== LlamaServerInferenceService.java ===
Injects: LlamaServerProperties, RestClient (qualifier: llamaServerRestClient), WebClient (qualifier: llamaServerWebClient)
Annotations: @Primary, @ConditionalOnProperty(name="app.inference.provider", havingValue="llama-server")

Public Methods:
  - isAvailable(): boolean
    Purpose: Checks if the llama-server is reachable by calling its /health endpoint.
    Calls: restClient.get("/health").retrieve().toBodilessEntity()
    Throws: None (returns false on any exception)
    Transactional: NO

  - listModels(): List<InferenceModelInfo>
    Purpose: Lists models from the llama-server /v1/models endpoint.
    Calls: restClient.get(LLAMA_SERVER_MODELS_ENDPOINT).retrieve().body()
    Throws: None (returns empty list on error)
    Transactional: NO

  - getActiveModel(): InferenceModelInfo
    Purpose: Returns the first model from listModels(), falling back to the configured activeModel filename.
    Calls: listModels(), LlamaServerProperties.getActiveModel()
    Throws: None
    Transactional: NO

  - chat(List<OllamaMessage> messages, UUID userId): String
    Purpose: Sends a synchronous (stream=false) chat completion request to llama-server and extracts the content from the first choice.
    Calls: restClient.post(LLAMA_SERVER_CHAT_ENDPOINT).retrieve().body()
    Throws: OllamaInferenceException
    Transactional: NO

  - streamChat(List<OllamaMessage> messages, UUID userId): Flux<String>
    Purpose: Delegates to streamChatWithThinking, filters for CONTENT chunks only.
    Calls: streamChatWithThinking()
    Throws: None
    Transactional: NO

  - streamChatWithThinking(List<OllamaMessage> messages, UUID userId): Flux<InferenceChunk>
    Purpose: Streams SSE tokens from llama-server, applies think-tag state machine to classify THINKING vs CONTENT chunks, appends terminal DONE chunk with token/timing metadata.
    Calls: webClient.post(LLAMA_SERVER_CHAT_ENDPOINT).retrieve().bodyToFlux(), extractDeltaContent(), processToken(), flushBuffer()
    Throws: None (errors surface as reactive error signals)
    Transactional: NO

  - embed(String text): float[]
    Purpose: Generates an embedding vector via llama-server /v1/embeddings.
    Calls: restClient.post("/v1/embeddings").retrieve().body()
    Throws: EmbeddingException
    Transactional: NO

Private Methods:
  - extractDeltaContent(String json): String
  - processToken(String token, AtomicReference<ThinkState> state, StringBuilder tagBuffer): Flux<InferenceChunk>
  - flushBuffer(StringBuilder tagBuffer, ThinkState state): List<InferenceChunk>
  - partialTagMatchLength(String text, String tag): int
  - enum ThinkState { OUTSIDE_THINK, INSIDE_THINK }

---

=== LlamaServerProcessService.java ===
Injects: LlamaServerProperties, SystemConfigService, ProcessBuilderFactory
Annotations: @ConditionalOnProperty(name="app.inference.provider", havingValue="llama-server"), implements ApplicationRunner, DisposableBean

Public Methods:
  - run(ApplicationArguments args): void
    Purpose: ApplicationRunner entry point — calls start() on boot, swallows exceptions.
    Calls: start()
    Throws: None (exceptions caught and logged)
    Transactional: NO

  - start(): void [synchronized]
    Purpose: Resolves active model from SystemConfigService or properties, verifies binary exists, builds and launches llama-server process, polls /health until healthy or timeout.
    Calls: SystemConfigService.getConfig().getActiveModelFilename(), resolveModelPath(), processBuilderFactory.create(), startLogReader(), waitForHealthy()
    Throws: None (status set to ERROR on failure)
    Transactional: NO

  - stop(): void [synchronized]
    Purpose: Gracefully destroys the llama-server child process, forcibly kills if it doesn't stop within 10 seconds.
    Calls: process.destroy(), process.waitFor(), process.destroyForcibly()
    Throws: None
    Transactional: NO

  - restart(): void
    Purpose: Stops then starts the llama-server process.
    Calls: stop(), start()
    Throws: None
    Transactional: NO

  - switchModel(String filename): LlamaServerStatusDto [synchronized]
    Purpose: Validates model file exists, stops server, updates SystemConfig active model, restarts server.
    Calls: resolveModelPath(), stop(), SystemConfigService.setActiveModelFilename(), start(), getStatus()
    Throws: IllegalArgumentException (blank filename or file not found)
    Transactional: NO

  - getStatus(): LlamaServerStatusDto
    Purpose: Returns current process status, active model path, port, models dir, and error message.
    Calls: None (reads volatile fields)
    Throws: None
    Transactional: NO

  - getRecentLogLines(int n): List<String>
    Purpose: Returns the last N lines buffered from llama-server stdout/stderr.
    Calls: logBuffer.subList()
    Throws: None
    Transactional: NO

  - monitorHealth(): void [@Scheduled]
    Purpose: Periodic health monitor — restarts on crash, attempts recovery start when in ERROR state with model configured.
    Calls: checkHealth(), restart(), SystemConfigService.getConfig(), start()
    Throws: None
    Transactional: NO

  - destroy(): void
    Purpose: DisposableBean shutdown hook — calls stop().
    Calls: stop()
    Throws: None
    Transactional: NO

Private Methods:
  - resolveModelPath(String filename): Path
  - waitForHealthy(): boolean
  - checkHealth(): boolean
  - startLogReader(): void
  - enum LlamaServerStatus { STOPPED, STARTING, RUNNING, ERROR }

---

=== ModelHealthCheckService.java ===
Injects: InferenceService, SystemConfigService
Annotations: @Component, @EventListener(ApplicationReadyEvent)

Public Methods:
  - checkModelHealth(): void [@EventListener(ApplicationReadyEvent)]
    Purpose: At startup, checks inference provider availability and logs the active model; updates SystemConfig if model is found.
    Calls: InferenceService.isAvailable(), InferenceService.getActiveModel(), SystemConfigService.getConfig(), SystemConfigService.save()
    Throws: None (exceptions caught internally)
    Transactional: NO

---

=== NativeLlamaInferenceService.java ===
Injects: InferenceProperties, NativeLlamaProperties, SystemConfigService, RestClient (qualifier: ollamaEmbedRestClient)
Annotations: @ConditionalOnProperty(name="app.inference.provider", havingValue="native"), implements DisposableBean

Public Methods:
  - isAvailable(): boolean
    Purpose: Returns true if the LlamaModel has been successfully loaded.
    Calls: (checks internal model field)
    Throws: None
    Transactional: NO

  - listModels(): List<InferenceModelInfo>
    Purpose: Returns a single-element list with the currently loaded native model info.
    Calls: SystemConfigService.getConfig()
    Throws: None
    Transactional: NO

  - getActiveModel(): InferenceModelInfo
    Purpose: Returns the native model info (filename, format=gguf).
    Calls: SystemConfigService.getConfig()
    Throws: None
    Transactional: NO

  - chat(List<OllamaMessage> messages, UUID userId): String
    Purpose: Runs synchronous inference via LlamaModel JNI binding using ChatML prompt format.
    Calls: buildChatMlPrompt(), LlamaModel.generate()
    Throws: OllamaInferenceException
    Transactional: NO

  - streamChat(List<OllamaMessage> messages, UUID userId): Flux<String>
    Purpose: Delegates to streamChatWithThinking, filters for CONTENT chunks only.
    Calls: streamChatWithThinking()
    Throws: None
    Transactional: NO

  - streamChatWithThinking(List<OllamaMessage> messages, UUID userId): Flux<InferenceChunk>
    Purpose: Streams tokens from LlamaModel JNI on a BoundedElastic scheduler thread via Sinks.Many, applies think-tag state machine, appends DONE chunk with metadata.
    Calls: Sinks.many().unicast(), buildChatMlPrompt(), LlamaModel.generate() (via token callback), processToken(), flushBuffer()
    Throws: None (errors emitted as reactive error signals)
    Transactional: NO

  - embed(String text): float[]
    Purpose: Generates embedding via Ollama embed REST endpoint (unless native embedding enabled).
    Calls: restClient.post("/api/embeddings").retrieve().body()
    Throws: EmbeddingException
    Transactional: NO

  - destroy(): void
    Purpose: DisposableBean hook — closes LlamaModel if loaded.
    Calls: LlamaModel.close()
    Throws: None
    Transactional: NO

Private Methods:
  - loadModel(): void
  - buildChatMlPrompt(List<OllamaMessage> messages): String
  - processToken(String token, AtomicReference<ThinkState> state, StringBuilder tagBuffer, Sinks.Many<InferenceChunk> sink): void
  - flushBuffer(StringBuilder tagBuffer, ThinkState state, Sinks.Many<InferenceChunk> sink): void
  - partialTagMatchLength(String text, String tag): int
  - enum ThinkState { OUTSIDE_THINK, INSIDE_THINK }

---

=== OllamaInferenceService.java ===
Injects: OllamaService, SystemConfigService, String ollamaModelName (@Value), String ollamaEmbedModelName (@Value)
Annotations: @ConditionalOnProperty(name="app.inference.provider", havingValue="ollama")

Public Methods:
  - isAvailable(): boolean
    Purpose: Delegates to OllamaService.isAvailable().
    Calls: OllamaService.isAvailable()
    Throws: None
    Transactional: NO

  - listModels(): List<InferenceModelInfo>
    Purpose: Delegates to OllamaService.listModels() and maps to InferenceModelInfo records.
    Calls: OllamaService.listModels()
    Throws: None
    Transactional: NO

  - getActiveModel(): InferenceModelInfo
    Purpose: Returns the configured Ollama model name as the active model.
    Calls: SystemConfigService.getConfig()
    Throws: None
    Transactional: NO

  - chat(List<OllamaMessage> messages, UUID userId): String
    Purpose: Delegates synchronous chat to OllamaService.
    Calls: OllamaService.chat()
    Throws: OllamaInferenceException
    Transactional: NO

  - streamChat(List<OllamaMessage> messages, UUID userId): Flux<String>
    Purpose: Delegates streaming chat to OllamaService.chatStream().
    Calls: OllamaService.chatStream()
    Throws: None
    Transactional: NO

  - streamChatWithThinking(List<OllamaMessage> messages, UUID userId): Flux<InferenceChunk>
    Purpose: Wraps OllamaService.chatStream() tokens as CONTENT chunks and appends a DONE chunk.
    Calls: OllamaService.chatStream()
    Throws: None
    Transactional: NO

  - embed(String text): float[]
    Purpose: Delegates to OllamaService.embed().
    Calls: OllamaService.embed()
    Throws: EmbeddingException
    Transactional: NO

---

=== OllamaService.java ===
Injects: RestClient (qualifier: ollamaRestClient), WebClient (qualifier: ollamaWebClient)

Public Methods:
  - isAvailable(): boolean
    Purpose: Checks Ollama availability by calling the /api/tags endpoint.
    Calls: restClient.get("/api/tags").retrieve().toBodilessEntity()
    Throws: None (returns false on any exception)
    Transactional: NO

  - listModels(): List<Map<String, Object>>
    Purpose: Returns the list of models from Ollama's /api/tags endpoint.
    Calls: restClient.get("/api/tags").retrieve().body()
    Throws: None (returns empty list on error)
    Transactional: NO

  - chat(List<OllamaMessage> messages, UUID userId): String
    Purpose: Sends a synchronous chat completion request to Ollama and returns the response content.
    Calls: restClient.post("/api/chat").retrieve().body()
    Throws: OllamaInferenceException
    Transactional: NO

  - chatStream(List<OllamaMessage> messages, UUID userId): Flux<String>
    Purpose: Streams SSE tokens from Ollama's /api/chat endpoint.
    Calls: webClient.post("/api/chat").retrieve().bodyToFlux()
    Throws: None (errors as reactive signals)
    Transactional: NO

  - embed(String text): float[]
    Purpose: Generates a single embedding via Ollama /api/embeddings.
    Calls: restClient.post("/api/embeddings").retrieve().body()
    Throws: EmbeddingException
    Transactional: NO

  - embedBatch(List<String> texts): List<float[]>
    Purpose: Generates embeddings for a list of texts by calling embed() in sequence.
    Calls: embed()
    Throws: EmbeddingException
    Transactional: NO

---

## AI / JUDGE SERVICES

---

=== JudgeInferenceService.java ===
Injects: JudgeProperties, JudgeModelProcessService, ObjectMapper, WebClient.Builder

Public Methods:
  - evaluate(String userMessage, String assistantResponse): Optional<JudgeScore>
    Purpose: Sends a scoring prompt to the judge llama-server and parses a JSON score object with score, reason, and needs_cloud fields.
    Calls: JudgeModelProcessService.isRunning(), JudgeModelProcessService.getPort(), WebClient.post().retrieve().bodyToMono(), ObjectMapper.readTree()
    Throws: None (returns Optional.empty() on all failures)
    Transactional: NO

Private Methods:
  - buildScoringPrompt(String userMessage, String assistantResponse): String
  - parseScore(String json): Optional<JudgeScore>

---

=== JudgeModelProcessService.java ===
Injects: JudgeProperties, InferenceProperties, ProcessBuilderFactory
Annotations: implements DisposableBean

Public Methods:
  - start(): void [synchronized]
    Purpose: Starts the judge llama-server process if enabled, model file exists, and binary is found; polls /health until healthy.
    Calls: JudgeProperties.isEnabled(), resolveModelPath(), InferenceProperties.getLlamaServerBinary(), processBuilderFactory.create(), waitForHealthy(), destroyProcess()
    Throws: None (logs and returns on all failures)
    Transactional: NO

  - stop(): void [synchronized]
    Purpose: Stops the judge llama-server process.
    Calls: destroyProcess()
    Throws: None
    Transactional: NO

  - isRunning(): boolean
    Purpose: Returns true if the process is alive and health endpoint returns 200.
    Calls: process.isAlive(), checkHealth()
    Throws: None
    Transactional: NO

  - getPort(): int
    Purpose: Returns the configured judge port.
    Calls: JudgeProperties.getPort()
    Throws: None
    Transactional: NO

  - destroy(): void
    Purpose: DisposableBean shutdown hook — calls stop().
    Calls: stop()
    Throws: None
    Transactional: NO

Private Methods:
  - resolveModelPath(String filename): Path
  - waitForHealthy(): boolean
  - checkHealth(): boolean
  - destroyProcess(): void

---

## FRONTIER SERVICES

---

=== FrontierApiRouter.java ===
Injects: List<FrontierApiClient>, ExternalApiSettingsService

Public Methods:
  - complete(String prompt, UUID userId): Optional<String>
    Purpose: Routes a completion request to the preferred provider first (CLAUDE, then GROK, then OPENAI), falling back through the chain on failure.
    Calls: ExternalApiSettingsService.getPreferredFrontierProvider(), FrontierApiClient.isAvailable(), FrontierApiClient.complete()
    Throws: None (returns Optional.empty() if all providers fail)
    Transactional: NO

  - isAnyAvailable(): boolean
    Purpose: Returns true if at least one frontier provider is configured and available.
    Calls: FrontierApiClient.isAvailable()
    Throws: None
    Transactional: NO

  - getAvailableProviders(): List<String>
    Purpose: Returns names of all currently available frontier providers.
    Calls: FrontierApiClient.isAvailable(), FrontierApiClient.getProviderName()
    Throws: None
    Transactional: NO

Private Methods:
  - orderedClients(FrontierProvider preferred): List<FrontierApiClient>

---

=== ClaudeFrontierClient.java ===
Injects: ExternalApiSettingsService, WebClient.Builder
Annotations: @Component, implements FrontierApiClient

Public Methods:
  - getProviderName(): String
    Purpose: Returns "CLAUDE".
    Calls: None
    Throws: None
    Transactional: NO

  - isAvailable(): boolean
    Purpose: Returns true if Anthropic API key is configured and enabled.
    Calls: ExternalApiSettingsService.getAnthropicKey()
    Throws: None
    Transactional: NO

  - complete(String prompt, UUID userId): Optional<String>
    Purpose: POSTs to Anthropic Messages API and extracts the text content from the first response block.
    Calls: ExternalApiSettingsService.getAnthropicKey(), ExternalApiSettingsService.getAnthropicModel(), WebClient.post().retrieve().bodyToMono()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

---

=== OpenAiFrontierClient.java ===
Injects: ExternalApiSettingsService, WebClient.Builder
Annotations: @Component, implements FrontierApiClient

Public Methods:
  - getProviderName(): String
    Purpose: Returns "OPENAI".
    Calls: None
    Throws: None
    Transactional: NO

  - isAvailable(): boolean
    Purpose: Returns true if OpenAI API key is configured and enabled.
    Calls: ExternalApiSettingsService.getOpenAiKey()
    Throws: None
    Transactional: NO

  - complete(String prompt, UUID userId): Optional<String>
    Purpose: POSTs to OpenAI chat completions API and extracts the message content from the first choice.
    Calls: ExternalApiSettingsService.getOpenAiKey(), WebClient.post().retrieve().bodyToMono()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

---

=== GrokFrontierClient.java ===
Injects: ExternalApiSettingsService, WebClient.Builder
Annotations: @Component, implements FrontierApiClient

Public Methods:
  - getProviderName(): String
    Purpose: Returns "GROK".
    Calls: None
    Throws: None
    Transactional: NO

  - isAvailable(): boolean
    Purpose: Returns true if Grok API key is configured and enabled.
    Calls: ExternalApiSettingsService.getGrokKey()
    Throws: None
    Transactional: NO

  - complete(String prompt, UUID userId): Optional<String>
    Purpose: POSTs to xAI Grok API (OpenAI-compatible endpoint) and extracts the message content.
    Calls: ExternalApiSettingsService.getGrokKey(), WebClient.post().retrieve().bodyToMono()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

---

## AUTH SERVICES

---

=== AuthService.java ===
Injects: UserRepository, JwtService, PasswordEncoder, @Value spring.profiles.active

Public Methods:
  - register(RegisterRequest request): AuthResponse
    Purpose: Validates password length (dev profile uses AppConstants.PASSWORD_MIN_LENGTH_DEV), checks username uniqueness, hashes password with BCrypt, persists User, returns access+refresh tokens.
    Calls: UserRepository.findByUsername(), PasswordEncoder.encode(), UserRepository.save(), JwtService.generateAccessToken(), JwtService.generateRefreshToken()
    Throws: DuplicateResourceException, ValidationException
    Transactional: YES

  - login(LoginRequest request): AuthResponse
    Purpose: Looks up user by username, verifies password with BCrypt, returns new token pair.
    Calls: UserRepository.findByUsername(), PasswordEncoder.matches(), JwtService.generateAccessToken(), JwtService.generateRefreshToken()
    Throws: UnauthorizedException
    Transactional: YES

  - refresh(String refreshToken): AuthResponse
    Purpose: Validates refresh token, extracts username, issues new access token.
    Calls: JwtService.isTokenValid(), JwtService.extractUsername(), UserRepository.findByUsername(), JwtService.generateAccessToken()
    Throws: UnauthorizedException
    Transactional: NO

  - logout(String accessToken): void
    Purpose: Adds the access token to an in-memory blacklist.
    Calls: (adds to ConcurrentHashSet)
    Throws: None
    Transactional: NO

  - isTokenBlacklisted(String token): boolean
    Purpose: Checks whether a token is in the in-memory blacklist.
    Calls: (reads ConcurrentHashSet)
    Throws: None
    Transactional: NO

  - changePassword(UUID userId, String currentPassword, String newPassword): void
    Purpose: Verifies current password, validates new password length, hashes and persists new password.
    Calls: UserRepository.findById(), PasswordEncoder.matches(), PasswordEncoder.encode(), UserRepository.save()
    Throws: EntityNotFoundException, UnauthorizedException, ValidationException
    Transactional: YES

---

=== JwtService.java ===
Injects: @Value app.jwt.secret, @Value app.jwt.expiration-ms, @Value app.jwt.refresh-expiration-ms

Public Methods:
  - generateAccessToken(String username): String
    Purpose: Generates an HMAC-SHA256 signed JWT access token with configured expiry.
    Calls: Jwts.builder().signWith(getSigningKey())
    Throws: None
    Transactional: NO

  - generateRefreshToken(String username): String
    Purpose: Generates an HMAC-SHA256 signed JWT refresh token with longer expiry.
    Calls: Jwts.builder().signWith(getSigningKey())
    Throws: None
    Transactional: NO

  - extractUsername(String token): String
    Purpose: Extracts the subject (username) claim from the token.
    Calls: Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims()
    Throws: JwtException (on invalid token)
    Transactional: NO

  - extractExpiration(String token): Date
    Purpose: Extracts the expiration date claim from the token.
    Calls: Jwts.parser().parseSignedClaims()
    Throws: JwtException
    Transactional: NO

  - isTokenValid(String token, String username): boolean
    Purpose: Returns true if token parses, username matches, and token is not expired.
    Calls: extractUsername(), isTokenExpired()
    Throws: None (returns false on parse errors)
    Transactional: NO

  - isTokenExpired(String token): boolean
    Purpose: Returns true if the token expiration date is before now.
    Calls: extractExpiration()
    Throws: None (returns true on parse errors)
    Transactional: NO

  - getAccessExpirationMs(): long
    Purpose: Returns the configured access token expiration duration in milliseconds.
    Calls: None
    Throws: None
    Transactional: NO

Private Methods:
  - getSigningKey(): SecretKey

---

=== UserService.java ===
Injects: UserRepository

Public Methods:
  - listUsers(): List<User>
    Purpose: Returns all users ordered by username.
    Calls: UserRepository.findAllByOrderByUsernameAsc()
    Throws: None
    Transactional: NO

  - getUserById(UUID userId): User
    Purpose: Fetches a user by ID.
    Calls: UserRepository.findById()
    Throws: EntityNotFoundException
    Transactional: NO

  - updateUser(UUID userId, UpdateUserRequest request): User
    Purpose: Updates username and/or display name for a user.
    Calls: UserRepository.findById(), UserRepository.findByUsername(), UserRepository.save()
    Throws: EntityNotFoundException, DuplicateResourceException
    Transactional: YES

  - deactivateUser(UUID userId): User
    Purpose: Sets user.active = false and persists.
    Calls: UserRepository.findById(), UserRepository.save()
    Throws: EntityNotFoundException
    Transactional: YES

  - deleteUser(UUID userId): void
    Purpose: Deletes a user by ID.
    Calls: UserRepository.findById(), UserRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

---

## ENRICHMENT SERVICES

---

=== ClaudeApiService.java ===
Injects: ExternalApiSettingsService, WebClient.Builder

Public Methods:
  - isAvailable(): boolean
    Purpose: Returns true if the Anthropic API key is configured and enabled.
    Calls: ExternalApiSettingsService.getAnthropicKey()
    Throws: None
    Transactional: NO

  - complete(String prompt): Optional<String>
    Purpose: Sends a prompt to Anthropic Messages API and returns the response text.
    Calls: ExternalApiSettingsService.getAnthropicKey(), ExternalApiSettingsService.getAnthropicModel(), WebClient.post().retrieve().bodyToMono()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

  - summarizeForKnowledgeBase(String content): Optional<String>
    Purpose: Prompts Claude to produce a concise knowledge-base summary of the given content.
    Calls: complete()
    Throws: None
    Transactional: NO

---

=== WebFetchService.java ===
Injects: WebClient.Builder, ExternalApiSettingsService, ClaudeApiService, KnowledgeService

Public Methods:
  - fetchUrl(String url): Optional<String>
    Purpose: Fetches a URL with Jsoup, strips HTML to plain text, truncates at the configured maxWebFetchSizeKb limit.
    Calls: ExternalApiSettingsService.getMaxWebFetchSizeKb(), Jsoup.connect().get()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

  - fetchAndStore(String url, UUID userId): Optional<String>
    Purpose: Fetches URL content, optionally summarizes with Claude, stores in KnowledgeService as a web page document.
    Calls: fetchUrl(), ClaudeApiService.summarizeForKnowledgeBase(), KnowledgeService.createFromEditor()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

---

=== WebSearchService.java ===
Injects: WebClient.Builder, ExternalApiSettingsService, WebFetchService

Public Methods:
  - isAvailable(): boolean
    Purpose: Returns true if the Brave Search API key is configured and enabled.
    Calls: ExternalApiSettingsService.getBraveKey()
    Throws: None
    Transactional: NO

  - search(String query, UUID userId): List<SearchResult>
    Purpose: Calls the Brave Search API with the configured key and returns structured results.
    Calls: ExternalApiSettingsService.getBraveKey(), ExternalApiSettingsService.getSearchResultLimit(), WebClient.get().retrieve().bodyToMono()
    Throws: None (returns empty list on error)
    Transactional: NO

  - searchAndStore(String query, UUID userId): List<SearchResult>
    Purpose: Searches via Brave, then fetches and stores the top N results in the knowledge base.
    Calls: search(), WebFetchService.fetchAndStore()
    Throws: None
    Transactional: NO

---

## EVENTS SERVICES

---

=== ScheduledEventService.java ===
Injects: ScheduledEventRepository

Public Methods:
  - listEvents(UUID userId): List<ScheduledEvent>
    Purpose: Returns all scheduled events for a user ordered by next fire time.
    Calls: ScheduledEventRepository.findByUserIdOrderByNextFireAtAsc()
    Throws: None
    Transactional: NO

  - getEvent(UUID eventId, UUID userId): ScheduledEvent
    Purpose: Returns a specific scheduled event scoped to the user.
    Calls: ScheduledEventRepository.findByIdAndUserId()
    Throws: EntityNotFoundException
    Transactional: NO

  - createEvent(UUID userId, CreateScheduledEventRequest request): ScheduledEvent
    Purpose: Validates cron expression, creates ScheduledEvent entity with next-fire-at calculated, persists.
    Calls: CronExpression.parse(), calculateNextFireAt(), ScheduledEventRepository.save()
    Throws: IllegalArgumentException (invalid cron)
    Transactional: NO

  - updateEvent(UUID eventId, UUID userId, UpdateScheduledEventRequest request): ScheduledEvent
    Purpose: Updates name, description, cron, and recalculates next-fire-at.
    Calls: ScheduledEventRepository.findByIdAndUserId(), CronExpression.parse(), calculateNextFireAt(), ScheduledEventRepository.save()
    Throws: EntityNotFoundException, IllegalArgumentException
    Transactional: YES

  - deleteEvent(UUID eventId, UUID userId): void
    Purpose: Deletes a scheduled event scoped to the user.
    Calls: ScheduledEventRepository.findByIdAndUserId(), ScheduledEventRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

  - toggleEvent(UUID eventId, UUID userId): ScheduledEvent
    Purpose: Toggles the enabled state of an event and recalculates next-fire-at if re-enabled.
    Calls: ScheduledEventRepository.findByIdAndUserId(), calculateNextFireAt(), ScheduledEventRepository.save()
    Throws: EntityNotFoundException
    Transactional: NO

  - deleteAllForUser(UUID userId): void
    Purpose: Deletes all scheduled events for a user.
    Calls: ScheduledEventRepository.deleteByUserId()
    Throws: None
    Transactional: YES

  - calculateNextFireAt(String cronExpression): Instant
    Purpose: Parses a cron expression with Spring's CronExpression and returns the next execution time.
    Calls: CronExpression.parse().next()
    Throws: IllegalArgumentException (invalid cron)
    Transactional: NO

---

## KNOWLEDGE SERVICES

---

=== ChunkingService.java ===
Injects: (none)

Public Methods:
  - chunkText(String text, int maxTokens, int overlapTokens): List<String>
    Purpose: Splits text into overlapping chunks at sentence boundaries, hard-splitting oversized sentences.
    Calls: (pure string/regex logic)
    Throws: None
    Transactional: NO

Private Methods:
  - splitIntoSentences(String text): List<String>
  - hardSplit(String sentence, int maxTokens): List<String>
  - estimateTokens(String text): int

---

=== FileStorageService.java ===
Injects: SystemConfigService

Public Methods:
  - store(UUID userId, String originalFilename, InputStream inputStream): String
    Purpose: Saves an uploaded file to {knowledgeStoragePath}/{userId}/{uuid}-{filename}, returns the relative storage key.
    Calls: SystemConfigService.getConfig().getKnowledgeStoragePath(), Files.copy()
    Throws: IOException (wrapped as RuntimeException)
    Transactional: NO

  - storeBytes(UUID userId, String filename, byte[] bytes): String
    Purpose: Saves a byte array as a file under the user's storage directory.
    Calls: SystemConfigService.getConfig().getKnowledgeStoragePath(), Files.write()
    Throws: IOException (wrapped as RuntimeException)
    Transactional: NO

  - delete(String storageKey): void
    Purpose: Deletes a file by its storage key path.
    Calls: Files.deleteIfExists()
    Throws: None (logs on error)
    Transactional: NO

  - deleteAllForUser(UUID userId): void
    Purpose: Recursively deletes the user's storage directory.
    Calls: Files.walk(), Files.delete()
    Throws: None (logs on error)
    Transactional: NO

  - getInputStream(String storageKey): InputStream
    Purpose: Opens an input stream for a stored file by key.
    Calls: Files.newInputStream()
    Throws: IOException
    Transactional: NO

---

=== IngestionService.java ===
Injects: (none)

Public Methods:
  - extractPdf(InputStream input): String
    Purpose: Extracts text from a PDF using Apache PDFBox.
    Calls: PDDocument.load(), PDFTextStripper.getText()
    Throws: IOException
    Transactional: NO

  - extractText(InputStream input): String
    Purpose: Reads a plain text file from the input stream.
    Calls: new String(input.readAllBytes())
    Throws: IOException
    Transactional: NO

  - extractDocx(InputStream input): String
    Purpose: Extracts text from a DOCX file using Apache POI XWPFDocument.
    Calls: XWPFDocument.getParagraphs()
    Throws: IOException
    Transactional: NO

  - extractDoc(InputStream input): String
    Purpose: Extracts text from a legacy DOC file using Apache POI HWPFDocument.
    Calls: HWPFDocument.getDocumentText()
    Throws: IOException
    Transactional: NO

  - extractRtf(InputStream input): String
    Purpose: Extracts text from an RTF file using Java's RTFEditorKit.
    Calls: RTFEditorKit.read()
    Throws: IOException
    Transactional: NO

  - extractXlsx(InputStream input): String
    Purpose: Extracts cell values from an XLSX file using Apache POI XSSFWorkbook, joining with tab/newline.
    Calls: XSSFWorkbook.getSheetAt(), sheet.iterator()
    Throws: IOException
    Transactional: NO

  - extractXls(InputStream input): String
    Purpose: Extracts cell values from a legacy XLS file using Apache POI HSSFWorkbook.
    Calls: HSSFWorkbook.getSheetAt(), sheet.iterator()
    Throws: IOException
    Transactional: NO

  - extractPptx(InputStream input): String
    Purpose: Extracts text from PPTX slides using Apache POI XMLSlideShow.
    Calls: XMLSlideShow.getSlides(), XSLFTextShape.getText()
    Throws: IOException
    Transactional: NO

  - extractPpt(InputStream input): String
    Purpose: Extracts text from legacy PPT slides using Apache POI HSLFSlideShow via SlideShowExtractor.
    Calls: HSLFSlideShow, SlideShowExtractor.getText()
    Throws: IOException
    Transactional: NO

---

=== KnowledgeService.java ===
Injects: KnowledgeDocumentRepository, KnowledgeChunkRepository, VectorDocumentRepository, FileStorageService, IngestionService, OcrService, ChunkingService, EmbeddingService

Public Methods:
  - upload(UUID userId, MultipartFile file): KnowledgeDocument
    Purpose: Stores the uploaded file, persists a KnowledgeDocument entity in PENDING state, then kicks off async processDocumentAsync().
    Calls: FileStorageService.store(), KnowledgeDocumentRepository.save(), processDocumentAsync()
    Throws: IllegalArgumentException (unsupported type), IOException
    Transactional: YES

  - processDocumentAsync(UUID documentId): void [@Async]
    Purpose: Full ingestion pipeline: extract text (or OCR for images), chunk, embed each chunk, persist VectorDocument entries, set document state to PROCESSED.
    Calls: KnowledgeDocumentRepository.findById(), IngestionService.extract*(), OcrService.extractFromImage(), ChunkingService.chunkText(), EmbeddingService.embed(), VectorDocumentRepository.save(), KnowledgeDocumentRepository.save()
    Throws: None (sets document to ERROR state on failure)
    Transactional: NO

  - listDocuments(UUID userId, int page, int size): Page<KnowledgeDocument>
    Purpose: Returns paginated documents for a user ordered by upload time descending.
    Calls: KnowledgeDocumentRepository.findByUserIdOrderByUploadedAtDesc()
    Throws: None
    Transactional: NO

  - getDocument(UUID documentId, UUID userId): KnowledgeDocument
    Purpose: Returns a document scoped to the user.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId()
    Throws: EntityNotFoundException
    Transactional: NO

  - updateDisplayName(UUID documentId, UUID userId, String newName): KnowledgeDocument
    Purpose: Updates the display name of a document.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId(), KnowledgeDocumentRepository.save()
    Throws: EntityNotFoundException
    Transactional: NO

  - deleteDocument(UUID documentId, UUID userId): void
    Purpose: Deletes VectorDocuments, KnowledgeChunks, storage file, and the KnowledgeDocument entity.
    Calls: VectorDocumentRepository.deleteBySourceId(), KnowledgeChunkRepository.deleteByDocumentId(), FileStorageService.delete(), KnowledgeDocumentRepository.delete()
    Throws: EntityNotFoundException
    Transactional: NO

  - retryProcessing(UUID documentId, UUID userId): KnowledgeDocument
    Purpose: Resets a failed document to PENDING state and re-triggers async processing.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId(), KnowledgeDocumentRepository.save(), processDocumentAsync()
    Throws: EntityNotFoundException, IllegalStateException (not in ERROR state)
    Transactional: NO

  - getChunks(UUID documentId, UUID userId): List<KnowledgeChunk>
    Purpose: Returns all chunks for a document scoped to the user.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId(), KnowledgeChunkRepository.findByDocumentId()
    Throws: EntityNotFoundException
    Transactional: NO

  - toDto(KnowledgeDocument doc): KnowledgeDocumentDto
    Purpose: Maps a KnowledgeDocument entity to its DTO representation.
    Calls: (field mapping)
    Throws: None
    Transactional: NO

  - getDocumentContent(UUID documentId, UUID userId): String
    Purpose: Returns the full text content of a document by reading all its chunks.
    Calls: getChunks(), chunk.getContent()
    Throws: EntityNotFoundException
    Transactional: NO

  - getDocumentForDownload(UUID documentId, UUID userId): Resource
    Purpose: Returns the stored file as a Spring Resource for download.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId(), FileStorageService.getInputStream()
    Throws: EntityNotFoundException, IOException
    Transactional: NO

  - createFromEditor(UUID userId, String title, String content, String sourceUrl): KnowledgeDocument
    Purpose: Creates a knowledge document from editor-entered text or a URL fetch result.
    Calls: KnowledgeDocumentRepository.save(), processDocumentAsync()
    Throws: None
    Transactional: YES

  - updateContent(UUID documentId, UUID userId, String newContent): KnowledgeDocument
    Purpose: Replaces the content of an editor-created document, re-chunks, re-embeds, replaces VectorDocuments.
    Calls: KnowledgeDocumentRepository.findByIdAndUserId(), VectorDocumentRepository.deleteBySourceId(), KnowledgeChunkRepository.deleteByDocumentId(), ChunkingService.chunkText(), EmbeddingService.embed(), VectorDocumentRepository.save(), KnowledgeDocumentRepository.save()
    Throws: EntityNotFoundException
    Transactional: YES

  - deleteAllForUser(UUID userId): void
    Purpose: Deletes all documents, chunks, vector docs, and storage files for a user.
    Calls: KnowledgeDocumentRepository.findByUserId(), deleteDocument(), FileStorageService.deleteAllForUser()
    Throws: None
    Transactional: YES

---

=== OcrService.java ===
Injects: Tesseract (constructor injection)

Public Methods:
  - extractFromImage(InputStream inputStream): String
    Purpose: Runs Tesseract OCR (English language) on an image input stream to extract text.
    Calls: ImageIO.read(), Tesseract.doOCR()
    Throws: TesseractException, IOException
    Transactional: NO

---

=== SemanticSearchService.java ===
Injects: VectorDocumentRepository, KnowledgeChunkRepository, KnowledgeDocumentRepository, EmbeddingService

Public Methods:
  - search(UUID userId, String query, int topK): List<SemanticSearchResult>
    Purpose: Embeds the query and retrieves top-K similar VectorDocuments via pgvector cosine similarity, enriched with chunk and document metadata.
    Calls: EmbeddingService.embedAndFormat(), VectorDocumentRepository.findMostSimilar(), KnowledgeChunkRepository.findById(), KnowledgeDocumentRepository.findById(), cosineSimilarity()
    Throws: None
    Transactional: YES (readOnly)

  - searchForRagContext(UUID userId, String query, int topK): List<SemanticSearchResult>
    Purpose: Same as search() but filtered for knowledge documents only (excludes memory vectors).
    Calls: EmbeddingService.embedAndFormat(), VectorDocumentRepository.findMostSimilarByType(), KnowledgeChunkRepository.findById(), KnowledgeDocumentRepository.findById(), cosineSimilarity()
    Throws: None
    Transactional: YES (readOnly)

Private Methods:
  - cosineSimilarity(float[] a, float[] b): double
  - toResult(VectorDocument vd, KnowledgeChunk chunk, KnowledgeDocument doc, double score): SemanticSearchResult

---

=== StorageHealthService.java ===
Injects: SystemConfigService
Annotations: @EventListener(ApplicationReadyEvent)

Public Methods:
  - checkStorageDirectory(): void [@EventListener(ApplicationReadyEvent)]
    Purpose: At startup, creates the knowledge storage directory if it doesn't exist and validates write access.
    Calls: SystemConfigService.getConfig().getKnowledgeStoragePath(), Files.createDirectories(), Files.isWritable()
    Throws: None (logs error and continues)
    Transactional: NO

---

## LIBRARY SERVICES

---

=== CalibreConversionService.java ===
Injects: (none)

Public Methods:
  - convertToEpub(String inputPath, String outputPath): boolean
    Purpose: Executes a Calibre ebook-convert command inside the myoffgridai-calibre Docker container via docker exec.
    Calls: Runtime.exec() / ProcessBuilder (docker exec myoffgridai-calibre ebook-convert)
    Throws: None (returns false on error)
    Transactional: NO

  - isAvailable(): boolean
    Purpose: Checks if the Calibre Docker container is running by executing docker exec with a no-op command.
    Calls: ProcessBuilder (docker exec myoffgridai-calibre echo)
    Throws: None (returns false on error)
    Transactional: NO

---

=== EbookService.java ===
Injects: EbookRepository, LibraryProperties, CalibreConversionService

Public Methods:
  - upload(UUID userId, MultipartFile file): Ebook
    Purpose: Detects ebook format, converts MOBI/AZW to EPUB via Calibre, stores the file, persists the Ebook entity.
    Calls: detectFormat(), CalibreConversionService.convertToEpub(), EbookRepository.save(), Files.copy()
    Throws: IllegalArgumentException (unsupported format), IOException
    Transactional: YES

  - list(UUID userId): List<Ebook>
    Purpose: Returns all ebooks for a user ordered by title.
    Calls: EbookRepository.findByUserIdOrderByTitleAsc()
    Throws: None
    Transactional: YES (readOnly)

  - get(UUID ebookId, UUID userId): Ebook
    Purpose: Returns an ebook scoped to the user.
    Calls: EbookRepository.findByIdAndUserId()
    Throws: EntityNotFoundException
    Transactional: NO

  - delete(UUID ebookId, UUID userId): void
    Purpose: Deletes the ebook file from disk and removes the entity.
    Calls: EbookRepository.findByIdAndUserId(), Files.deleteIfExists(), EbookRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

  - getForDownload(UUID ebookId, UUID userId): Resource
    Purpose: Returns the ebook file as a Resource for download, increments the download count.
    Calls: EbookRepository.findByIdAndUserId(), EbookRepository.save(), new FileSystemResource()
    Throws: EntityNotFoundException
    Transactional: YES

  - detectFormat(String filename): EbookFormat
    Purpose: Determines the ebook format from the file extension.
    Calls: (string operations)
    Throws: IllegalArgumentException (unrecognized extension)
    Transactional: NO

---

=== GutenbergService.java ===
Injects: WebClient.Builder (baseUrl=gutenbergApiUrl), EbookRepository, LibraryProperties

Public Methods:
  - search(String query): List<GutenbergBook>
    Purpose: Calls the Gutendex API to search for Project Gutenberg books and maps results.
    Calls: webClient.get().retrieve().bodyToMono()
    Throws: None (returns empty list on error)
    Transactional: NO

  - getBookMetadata(long gutenbergId): Optional<GutenbergBook>
    Purpose: Fetches metadata for a specific Gutenberg book by ID.
    Calls: webClient.get().retrieve().bodyToMono()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

  - importBook(long gutenbergId, UUID userId): Ebook
    Purpose: Fetches book metadata, downloads EPUB (falling back to TXT), saves file to disk, persists Ebook entity.
    Calls: getBookMetadata(), webClient.get().retrieve().bodyToMono(), Files.copy(), EbookRepository.save()
    Throws: EntityNotFoundException (book not found), IllegalStateException (no downloadable format), IOException
    Transactional: YES

---

=== ZimFileService.java ===
Injects: ZimFileRepository, LibraryProperties, WebClient.Builder

Public Methods:
  - upload(UUID userId, MultipartFile file): ZimFile
    Purpose: Validates .zim extension and file size limit, checks for duplicates by filename, stores file, persists ZimFile entity.
    Calls: ZimFileRepository.findByFilename(), ZimFileRepository.save(), Files.copy()
    Throws: IllegalArgumentException (invalid type/size/duplicate)
    Transactional: YES

  - listAll(UUID userId): List<ZimFile>
    Purpose: Returns all ZIM files for a user ordered by filename.
    Calls: ZimFileRepository.findByUserIdOrderByFilenameAsc()
    Throws: None
    Transactional: YES (readOnly)

  - delete(UUID zimFileId, UUID userId): void
    Purpose: Deletes the ZIM file from disk and removes the entity.
    Calls: ZimFileRepository.findByIdAndUserId(), Files.deleteIfExists(), ZimFileRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

  - getKiwixServeUrl(): String
    Purpose: Returns the configured Kiwix server URL.
    Calls: LibraryProperties.getKiwixUrl()
    Throws: None
    Transactional: NO

  - getKiwixStatus(): boolean
    Purpose: Pings the Kiwix server health endpoint and returns true if reachable.
    Calls: webClient.get().retrieve().toBodilessEntity()
    Throws: None (returns false on error)
    Transactional: NO

---

## MCP SERVICES

---

=== McpTokenService.java ===
Injects: McpApiTokenRepository, PasswordEncoder

Public Methods:
  - createToken(UUID userId, String description): McpApiToken
    Purpose: Generates a 32-byte SecureRandom token, BCrypt-hashes it, persists the record, returns the unhashed token only at creation time.
    Calls: SecureRandom.nextBytes(), Base64.encode(), PasswordEncoder.encode(), McpApiTokenRepository.save()
    Throws: None
    Transactional: YES

  - validateToken(String rawToken): Optional<McpApiToken>
    Purpose: Iterates all active tokens and BCrypt-compares against the raw token; returns the matching record if found.
    Calls: McpApiTokenRepository.findAllActive(), PasswordEncoder.matches()
    Throws: None
    Transactional: YES (readOnly)

  - updateLastUsed(UUID tokenId): void
    Purpose: Updates the lastUsedAt timestamp on a token record.
    Calls: McpApiTokenRepository.findById(), McpApiTokenRepository.save()
    Throws: None
    Transactional: YES

  - listTokens(UUID userId): List<McpApiToken>
    Purpose: Returns all MCP tokens for a user.
    Calls: McpApiTokenRepository.findByUserId()
    Throws: None
    Transactional: YES (readOnly)

  - revokeToken(UUID tokenId, UUID userId): void
    Purpose: Sets a token's active flag to false.
    Calls: McpApiTokenRepository.findByIdAndUserId(), McpApiTokenRepository.save()
    Throws: EntityNotFoundException
    Transactional: YES

---

=== McpToolsService.java ===
Injects: SemanticSearchService, KnowledgeService, MemoryService, InventoryItemRepository, SensorService, ChatService, SystemConfigService, ObjectMapper

Public Methods:
  - searchKnowledge(String query): String [@Tool]
    Purpose: Searches the knowledge base using semantic similarity and returns formatted results.
    Calls: SemanticSearchService.search(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - listKnowledgeDocuments(): String [@Tool]
    Purpose: Lists all knowledge documents for the authenticated MCP user.
    Calls: KnowledgeService.listDocuments(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - searchMemories(String query): String [@Tool]
    Purpose: Searches user memories using semantic similarity.
    Calls: MemoryService.findRelevantMemories(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - listInventory(): String [@Tool]
    Purpose: Returns all inventory items for the authenticated user.
    Calls: InventoryItemRepository.findByUserIdOrderByNameAsc(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - addInventoryItem(String name, String category, double quantity, String unit): String [@Tool]
    Purpose: Creates a new inventory item for the authenticated user.
    Calls: InventoryItemRepository.save(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - updateInventoryItem(String itemId, Double quantity): String [@Tool]
    Purpose: Updates the quantity of an existing inventory item.
    Calls: InventoryItemRepository.findById(), InventoryItemRepository.save(), ObjectMapper.writeValueAsString()
    Throws: None (returns error message string if not found)
    Transactional: NO

  - deleteInventoryItem(String itemId): String [@Tool]
    Purpose: Deletes an inventory item by ID.
    Calls: InventoryItemRepository.findById(), InventoryItemRepository.delete()
    Throws: None (returns error message string if not found)
    Transactional: NO

  - getLowStockItems(): String [@Tool]
    Purpose: Returns inventory items at or below their low-stock threshold.
    Calls: InventoryItemRepository.findByUserIdOrderByNameAsc(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - listSensors(): String [@Tool]
    Purpose: Returns all sensors for the authenticated user.
    Calls: SensorService.listSensors(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - getLatestSensorReading(String sensorId): String [@Tool]
    Purpose: Returns the most recent reading for a specific sensor.
    Calls: SensorService.getLatestReading(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - listConversations(): String [@Tool]
    Purpose: Lists recent conversations for the authenticated user.
    Calls: ChatService.searchConversations(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

  - getSystemStatus(): String [@Tool]
    Purpose: Returns system configuration status including model name and storage path.
    Calls: SystemConfigService.getConfig(), ObjectMapper.writeValueAsString()
    Throws: None
    Transactional: NO

Private Methods:
  - getAuthenticatedUserId(): UUID

---

## MEMORY SERVICES

---

=== EmbeddingService.java ===
Injects: OllamaService

Public Methods:
  - embed(String text): float[]
    Purpose: Delegates to OllamaService.embed() to generate an embedding vector.
    Calls: OllamaService.embed()
    Throws: EmbeddingException
    Transactional: NO

  - embedAndFormat(String text): String
    Purpose: Generates embedding and formats it as a pgvector string "[x,y,z,...]".
    Calls: embed(), formatEmbedding()
    Throws: EmbeddingException
    Transactional: NO

  - embedBatch(List<String> texts): List<float[]>
    Purpose: Generates embeddings for a list of texts sequentially.
    Calls: OllamaService.embedBatch()
    Throws: EmbeddingException
    Transactional: NO

  - cosineSimilarity(float[] a, float[] b): double
    Purpose: Computes cosine similarity between two float vectors.
    Calls: (arithmetic)
    Throws: IllegalArgumentException (mismatched lengths)
    Transactional: NO

Private Methods:
  - static formatEmbedding(float[] embedding): String

---

=== MemoryExtractionService.java ===
Injects: OllamaService, MemoryService, ObjectMapper, SystemConfigService

Public Methods:
  - extractAndStore(UUID userId, List<OllamaMessage> messages): void [@Async]
    Purpose: Prompts Ollama to extract factual memories from a conversation as a JSON array, parses each item, and creates a LOW-importance memory for each.
    Calls: SystemConfigService.getConfig(), OllamaService.chat(), ObjectMapper.readTree(), MemoryService.createMemory()
    Throws: None (exceptions logged, not propagated)
    Transactional: NO

Private Methods:
  - buildExtractionPrompt(List<OllamaMessage> messages): String

---

=== MemoryService.java ===
Injects: MemoryRepository, VectorDocumentRepository, EmbeddingService, SystemConfigService

Public Methods:
  - createMemory(UUID userId, String content, MemoryImportance importance, String tags, UUID sourceConversationId): Memory
    Purpose: Persists a Memory entity, generates embedding, saves a VectorDocument for similarity search.
    Calls: MemoryRepository.save(), EmbeddingService.embedAndFormat(), VectorDocumentRepository.save()
    Throws: None (embedding failure logged)
    Transactional: YES

  - findRelevantMemories(UUID userId, String query, int topK): List<Memory>
    Purpose: Embeds the query and retrieves semantically similar memory VectorDocuments, returning the corresponding Memory entities.
    Calls: EmbeddingService.embedAndFormat(), VectorDocumentRepository.findMostSimilarByType(), MemoryRepository.findById()
    Throws: None
    Transactional: YES

  - searchMemoriesWithScores(UUID userId, String query, int topK): List<ScoredMemory>
    Purpose: Same as findRelevantMemories() but includes the cosine similarity score for each result.
    Calls: EmbeddingService.embedAndFormat(), VectorDocumentRepository.findMostSimilarByType(), MemoryRepository.findById()
    Throws: None
    Transactional: YES

  - getMemory(UUID memoryId, UUID userId): Memory
    Purpose: Returns a memory scoped to the user, asserting ownership.
    Calls: MemoryRepository.findById(), assertOwnership()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: NO

  - updateImportance(UUID memoryId, UUID userId, MemoryImportance importance): Memory
    Purpose: Updates the importance level of a memory.
    Calls: MemoryRepository.findById(), assertOwnership(), MemoryRepository.save()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: YES

  - updateTags(UUID memoryId, UUID userId, String tags): Memory
    Purpose: Updates the tags on a memory.
    Calls: MemoryRepository.findById(), assertOwnership(), MemoryRepository.save()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: YES

  - deleteMemory(UUID memoryId, UUID userId): void
    Purpose: Deletes a memory and its associated VectorDocument.
    Calls: MemoryRepository.findById(), assertOwnership(), VectorDocumentRepository.deleteBySourceId(), MemoryRepository.delete()
    Throws: EntityNotFoundException, AccessDeniedException
    Transactional: YES

  - deleteAllMemoriesForUser(UUID userId): void
    Purpose: Deletes all memories and their VectorDocuments for a user.
    Calls: MemoryRepository.findByUserId(), VectorDocumentRepository.deleteBySourceId(), MemoryRepository.deleteAll()
    Throws: None
    Transactional: YES

  - exportMemories(UUID userId): List<Memory>
    Purpose: Returns all memories for a user for data export.
    Calls: MemoryRepository.findByUserId()
    Throws: None
    Transactional: NO

  - getMemories(UUID userId, int page, int size): Page<Memory>
    Purpose: Returns paginated memories for a user ordered by creation time descending.
    Calls: MemoryRepository.findByUserIdOrderByCreatedAtDesc()
    Throws: None
    Transactional: NO

  - toDto(Memory memory): MemoryDto
    Purpose: Maps a Memory entity to its DTO.
    Calls: (field mapping)
    Throws: None
    Transactional: NO

Private Methods:
  - assertOwnership(Memory memory, UUID userId): void

---

=== RagService.java ===
Injects: MemoryService, SemanticSearchService, SystemConfigService

Public Methods:
  - buildRagContext(UUID userId, String query): String
    Purpose: Retrieves relevant memories and knowledge snippets for the query, formats them as [RELEVANT MEMORIES] and [RELEVANT KNOWLEDGE] context blocks.
    Calls: SystemConfigService.getConfig(), MemoryService.findRelevantMemories(), SemanticSearchService.searchForRagContext(), formatContextBlock()
    Throws: None
    Transactional: NO

Private Methods:
  - formatContextBlock(String header, List<String> items): String

---

=== SummarizationService.java ===
Injects: ConversationRepository, MessageRepository, OllamaService, MemoryService, MemoryRepository, SystemConfigService

Public Methods:
  - summarizeConversation(UUID conversationId, UUID userId): void
    Purpose: Prompts Ollama to summarize a conversation and stores the summary as a CRITICAL importance memory.
    Calls: ConversationRepository.findByIdAndUserId(), MessageRepository.findByConversationId(), OllamaService.chat(), MemoryService.createMemory()
    Throws: EntityNotFoundException
    Transactional: NO

  - scheduledNightlySummarization(): void [@Scheduled(cron="0 0 2 * * *")]
    Purpose: Nightly batch job that finds conversations with 10+ messages not yet summarized, processes up to 50 per run.
    Calls: ConversationRepository.findUnsummarized(), summarizeConversation()
    Throws: None (per-conversation exceptions caught and logged)
    Transactional: NO

---

## MODELS SERVICES

---

=== ModelCatalogService.java ===
Injects: WebClient.Builder, ExternalApiSettingsService, ObjectMapper, QuantizationRecommendationService

Public Methods:
  - searchModels(String query, int limit): List<HfModelDto>
    Purpose: Searches HuggingFace for GGUF models using the configured HF token; uses JDK HttpClient to avoid Netty uppercase-letter URI bug.
    Calls: ExternalApiSettingsService.getHuggingFaceToken(), HttpClient.send(), ObjectMapper.readTree(), QuantizationRecommendationService.enrichFiles()
    Throws: HuggingFaceRateLimitException (429), HuggingFaceAccessDeniedException (401/403)
    Transactional: NO

  - getModelDetails(String modelId): HfModelDto
    Purpose: Fetches metadata for a specific HuggingFace model by ID.
    Calls: ExternalApiSettingsService.getHuggingFaceToken(), HttpClient.send(), ObjectMapper.readTree()
    Throws: HuggingFaceRateLimitException, HuggingFaceAccessDeniedException, EntityNotFoundException
    Transactional: NO

  - getModelFiles(String modelId): List<HfModelFileDto>
    Purpose: Returns the list of GGUF files for a HuggingFace model with quantization recommendations applied.
    Calls: getModelDetails(), QuantizationRecommendationService.enrichFiles()
    Throws: HuggingFaceRateLimitException, HuggingFaceAccessDeniedException, EntityNotFoundException
    Transactional: NO

Private Methods:
  - buildHfRequest(String url, String token): HttpRequest
  - parseModelList(JsonNode root): List<HfModelDto>
  - parseModelDetail(JsonNode root): HfModelDto

---

=== ModelDownloadService.java ===
Injects: WebClient.Builder, ExternalApiSettingsService, ModelDownloadProgressRegistry, InferenceService, @Value app.inference.models-dir, @Autowired(required=false) NativeLlamaInferenceService

Public Methods:
  - startDownload(String modelId, String filename, UUID userId): String
    Purpose: Validates not already downloading, generates a downloadId, registers initial progress, kicks off executeDownload() async; returns downloadId.
    Calls: ModelDownloadProgressRegistry.register(), executeDownload()
    Throws: IllegalStateException (already in progress)
    Transactional: NO

  - getProgress(String downloadId): Optional<ModelDownloadProgress>
    Purpose: Returns the current download progress record by ID.
    Calls: ModelDownloadProgressRegistry.get()
    Throws: None
    Transactional: NO

  - getAllDownloads(): List<ModelDownloadProgress>
    Purpose: Returns all tracked download progress records.
    Calls: ModelDownloadProgressRegistry.getAll()
    Throws: None
    Transactional: NO

  - cancelDownload(String downloadId): boolean
    Purpose: Sets the cancel flag on a download progress record; the async download loop checks this flag.
    Calls: ModelDownloadProgressRegistry.get(), ModelDownloadProgress.setCancelled()
    Throws: None
    Transactional: NO

  - listLocalModels(): List<LocalModelInfo>
    Purpose: Walks the models directory up to 3 levels deep and returns metadata for all .gguf files.
    Calls: Files.walk(), Files.size()
    Throws: None (returns empty list on error)
    Transactional: NO

  - deleteLocalModel(String filename): boolean
    Purpose: Finds and deletes a .gguf file from the models directory.
    Calls: Files.walk(), Files.delete()
    Throws: None (returns false on error)
    Transactional: NO

  - executeDownload(String modelId, String filename, String downloadId, UUID userId): void [@Async]
    Purpose: Downloads the model file with resumable Range requests in 64KB chunks, emits progress updates, cancels on flag, calls notifyModelDownloaded on completion.
    Calls: ExternalApiSettingsService.getHuggingFaceToken(), WebClient.get().retrieve().toEntityMono(), ModelDownloadProgressRegistry.update(), notifyModelDownloaded()
    Throws: None (sets progress to ERROR on failure)
    Transactional: NO

Private Methods:
  - notifyModelDownloaded(String filename): void
  - resolveDownloadUrl(String modelId, String filename): String

---

=== QuantizationRecommendationService.java ===
Injects: (none — standalone)

Public Methods:
  - enrichFiles(List<HfModelFileDto> files): List<HfModelFileDto>
    Purpose: Parses quant type from each filename, estimates RAM requirement, marks the best-fitting variant as recommended=true based on available system RAM.
    Calls: parseQuantType(), estimateRam(), getAvailableRam()
    Throws: None
    Transactional: NO

Private Methods:
  - parseQuantType(String filename): String
  - estimateRam(String quantType, long fileSizeBytes): long
  - isNonModelFile(String filename): boolean
  - getAvailableRam(): long

---

## NOTIFICATION SERVICES

---

=== DeviceRegistrationService.java ===
Injects: DeviceRegistrationRepository

Public Methods:
  - registerDevice(UUID userId, String deviceId, String platform, List<String> topics): DeviceRegistration
    Purpose: Upserts a device registration by userId+deviceId (update if exists, insert if new).
    Calls: DeviceRegistrationRepository.findByUserIdAndDeviceId(), DeviceRegistrationRepository.save()
    Throws: None
    Transactional: NO

  - getDevicesForUser(UUID userId): List<DeviceRegistration>
    Purpose: Returns all device registrations for a user.
    Calls: DeviceRegistrationRepository.findByUserId()
    Throws: None
    Transactional: NO

  - unregisterDevice(UUID userId, String deviceId): void
    Purpose: Removes a device registration.
    Calls: DeviceRegistrationRepository.findByUserIdAndDeviceId(), DeviceRegistrationRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

  - getTopicsForUser(UUID userId): List<String>
    Purpose: Returns the union of all MQTT topics subscribed by any of the user's registered devices.
    Calls: DeviceRegistrationRepository.findByUserId()
    Throws: None
    Transactional: NO

---

=== MqttPublisherService.java ===
Injects: @Nullable MqttClient, ObjectMapper

Public Methods:
  - publishToTopic(String topic, NotificationPayload payload): void
    Purpose: Serializes payload to JSON and publishes to the given MQTT topic with configured QoS; no-op if client is null or disconnected.
    Calls: ObjectMapper.writeValueAsString(), MqttClient.publish()
    Throws: None (exceptions logged, not propagated)
    Transactional: NO

  - publishToUser(UUID userId, NotificationPayload payload): void
    Purpose: Publishes to the user-specific MQTT topic ("users/{userId}/notifications").
    Calls: publishToTopic()
    Throws: None
    Transactional: NO

  - publishBroadcast(NotificationPayload payload): void
    Purpose: Publishes to the broadcast MQTT topic.
    Calls: publishToTopic()
    Throws: None
    Transactional: NO

---

## PRIVACY SERVICES

---

=== AuditService.java ===
Injects: AuditLogRepository

Public Methods:
  - logAction(UUID userId, String action, String resourceType, String resourceId, String outcome, String details): void
    Purpose: Persists an audit log entry for a user action.
    Calls: AuditLogRepository.save()
    Throws: None
    Transactional: NO

  - getAuditLogs(int page, int size): Page<AuditLog>
    Purpose: Returns paginated audit logs (admin use).
    Calls: AuditLogRepository.findAll(PageRequest)
    Throws: None
    Transactional: NO

  - getAuditLogsForUser(UUID userId, int page, int size): Page<AuditLog>
    Purpose: Returns paginated audit logs for a specific user.
    Calls: AuditLogRepository.findByUserIdOrderByTimestampDesc()
    Throws: None
    Transactional: NO

  - getAuditLogsByOutcome(String outcome, int page, int size): Page<AuditLog>
    Purpose: Returns paginated audit logs filtered by outcome string.
    Calls: AuditLogRepository.findByOutcomeOrderByTimestampDesc()
    Throws: None
    Transactional: NO

  - getAuditLogsBetween(Instant from, Instant to, int page, int size): Page<AuditLog>
    Purpose: Returns paginated audit logs in a time range.
    Calls: AuditLogRepository.findByTimestampBetween()
    Throws: None
    Transactional: NO

  - countByOutcomeBetween(String outcome, Instant from, Instant to): long
    Purpose: Counts audit log entries with a given outcome in a time window.
    Calls: AuditLogRepository.countByOutcomeAndTimestampBetween()
    Throws: None
    Transactional: NO

  - deleteByUserId(UUID userId): void
    Purpose: Deletes all audit logs for a user (for data wipe).
    Calls: AuditLogRepository.deleteByUserId()
    Throws: None
    Transactional: YES

---

=== DataExportService.java ===
Injects: ConversationRepository, MessageRepository, MemoryRepository

Public Methods:
  - exportUserData(UUID userId, String password): byte[]
    Purpose: Builds a ZIP archive of the user's conversations, messages, and memories serialized as JSON; encrypts the ZIP with AES-256-GCM using PBKDF2-derived key. Layout: [16-byte salt][12-byte IV][ciphertext].
    Calls: ConversationRepository.findByUserId(), MessageRepository.findByConversationId(), MemoryRepository.findByUserId(), ObjectMapper.writeValueAsBytes(), Cipher (AES/GCM), PBKDF2WithHmacSHA256
    Throws: RuntimeException (wraps encryption/IO failures)
    Transactional: NO

Private Methods:
  - deriveKey(char[] password, byte[] salt): SecretKey
  - buildZip(UUID userId): byte[]

---

=== DataWipeService.java ===
Injects: MessageRepository, ConversationRepository, MemoryService, KnowledgeService, SensorService, InsightService, NotificationService, InventoryItemRepository, PlannedTaskRepository, AuditService

Public Methods:
  - wipeUser(UUID userId): void
    Purpose: Performs a 10-step cascade delete of all user data: messages, conversations, memories+vectors, knowledge docs/chunks/vectors/files, sensor readings+sensors, insights, notifications, inventory items, planned tasks, audit logs.
    Calls: MessageRepository.deleteByConversationIds(), ConversationRepository.deleteByUserId(), MemoryService.deleteAllMemoriesForUser(), KnowledgeService.deleteAllForUser(), SensorService.deleteAllForUser(), InsightService.deleteAllForUser(), NotificationService.deleteAllForUser(), InventoryItemRepository.deleteByUserId(), PlannedTaskRepository.deleteByUserId(), AuditService.deleteByUserId()
    Throws: None (each step logs on error and continues)
    Transactional: YES

---

=== FortressService.java ===
Injects: SystemConfigService, UserRepository, @Value app.fortress.mock

Public Methods:
  - enable(UUID userId): void
    Purpose: Applies iptables rules (allow loopback + LAN, block outbound internet), updates SystemConfig fortress state; in mock mode skips iptables calls.
    Calls: SystemConfigService.setFortressEnabled(), executeIptables() (or mock)
    Throws: ApModeException (iptables failure in non-mock mode)
    Transactional: NO

  - disable(UUID userId): void
    Purpose: Flushes iptables rules and updates SystemConfig fortress state; in mock mode skips iptables calls.
    Calls: SystemConfigService.setFortressEnabled(), executeIptables() (or mock)
    Throws: ApModeException (iptables failure in non-mock mode)
    Transactional: NO

  - getFortressStatus(): FortressStatusDto
    Purpose: Returns current fortress state, enabled-at timestamp, and enabling user info.
    Calls: SystemConfigService.getConfig(), isFortressActive()
    Throws: None
    Transactional: NO

  - isFortressActive(): boolean
    Purpose: Returns true if fortress mode is currently active (checks SystemConfig or iptables in real mode).
    Calls: SystemConfigService.getConfig()
    Throws: None
    Transactional: NO

Private Methods:
  - executeIptables(String... args): void

---

=== SovereigntyReportService.java ===
Injects: FortressService, AuditService, ConversationRepository, MessageRepository, MemoryRepository, KnowledgeDocumentRepository, SensorRepository, InsightRepository

Public Methods:
  - generateReport(UUID userId): SovereigntyReportDto
    Purpose: Assembles a sovereignty report with fortress status, data inventory counts (conversations, messages, memories, documents, sensors, insights), and a 24-hour audit summary.
    Calls: FortressService.getFortressStatus(), ConversationRepository.countByUserId(), MessageRepository.countByUserId(), MemoryRepository.countByUserId(), KnowledgeDocumentRepository.countByUserId(), SensorRepository.countByUserId(), InsightRepository.countByUserId(), AuditService.getAuditLogsForUser(), AuditService.countByOutcomeBetween()
    Throws: None
    Transactional: NO

---

## PROACTIVE SERVICES

---

=== InsightGeneratorService.java ===
Injects: PatternAnalysisService, OllamaService, InsightRepository, NotificationService, ObjectMapper, SystemConfigService

Public Methods:
  - generateInsightForUser(UUID userId): void
    Purpose: Builds a PatternSummary, constructs a prompt, sends to Ollama, parses the JSON array of insight objects, persists each as an Insight, and creates a notification for each.
    Calls: PatternAnalysisService.buildPatternSummary(), SystemConfigService.getConfig(), OllamaService.chat(), ObjectMapper.readTree(), InsightRepository.save(), NotificationService.createNotification()
    Throws: None (exceptions logged)
    Transactional: NO

  - generateInsights(List<UUID> userIds): void
    Purpose: Calls generateInsightForUser() for each user ID in the list.
    Calls: generateInsightForUser()
    Throws: None
    Transactional: NO

Private Methods:
  - buildInsightPrompt(PatternSummary summary): String
  - parseInsights(String json): List<InsightCandidate>

---

=== InsightService.java ===
Injects: InsightRepository

Public Methods:
  - getInsights(UUID userId, int page, int size): Page<Insight>
    Purpose: Returns paginated insights for a user ordered by creation time descending.
    Calls: InsightRepository.findByUserIdOrderByCreatedAtDesc()
    Throws: None
    Transactional: NO

  - getInsightsByCategory(UUID userId, String category): List<Insight>
    Purpose: Returns all insights in a specific category for a user.
    Calls: InsightRepository.findByUserIdAndCategory()
    Throws: None
    Transactional: NO

  - getUnreadInsights(UUID userId): List<Insight>
    Purpose: Returns all unread insights for a user.
    Calls: InsightRepository.findByUserIdAndReadFalse()
    Throws: None
    Transactional: NO

  - markRead(UUID insightId, UUID userId): Insight
    Purpose: Marks an insight as read.
    Calls: InsightRepository.findByIdAndUserId(), InsightRepository.save()
    Throws: EntityNotFoundException
    Transactional: NO

  - dismiss(UUID insightId, UUID userId): void
    Purpose: Deletes an insight.
    Calls: InsightRepository.findByIdAndUserId(), InsightRepository.delete()
    Throws: EntityNotFoundException
    Transactional: NO

  - getUnreadCount(UUID userId): long
    Purpose: Returns the count of unread insights for a user.
    Calls: InsightRepository.countByUserIdAndReadFalse()
    Throws: None
    Transactional: NO

  - deleteAllForUser(UUID userId): void
    Purpose: Deletes all insights for a user (for data wipe).
    Calls: InsightRepository.deleteByUserId()
    Throws: None
    Transactional: YES

---

=== NotificationService.java ===
Injects: NotificationRepository, NotificationSseRegistry, MqttPublisherService, DeviceRegistrationService

Public Methods:
  - createNotification(UUID userId, String title, String message, NotificationType type): Notification
    Purpose: Persists a notification, broadcasts via SSE and MQTT.
    Calls: NotificationRepository.save(), NotificationSseRegistry.broadcast(), MqttPublisherService.publishToUser()
    Throws: None
    Transactional: NO

  - createNotification(UUID userId, String title, String message, NotificationType type, String metadata): Notification
    Purpose: Same as above but includes a JSON metadata string.
    Calls: NotificationRepository.save(), NotificationSseRegistry.broadcast(), MqttPublisherService.publishToUser()
    Throws: None
    Transactional: NO

  - getUnreadNotifications(UUID userId): List<Notification>
    Purpose: Returns all unread notifications for a user.
    Calls: NotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc()
    Throws: None
    Transactional: NO

  - getNotifications(UUID userId, int page, int size): Page<Notification>
    Purpose: Returns paginated notifications for a user ordered by creation time descending.
    Calls: NotificationRepository.findByUserIdOrderByCreatedAtDesc()
    Throws: None
    Transactional: NO

  - markRead(UUID notificationId, UUID userId): Notification
    Purpose: Marks a notification as read and broadcasts the updated unread count via SSE.
    Calls: NotificationRepository.findByIdAndUserId(), NotificationRepository.save(), NotificationSseRegistry.broadcastUnreadCount()
    Throws: EntityNotFoundException
    Transactional: NO

  - markAllRead(UUID userId): void
    Purpose: Marks all notifications for a user as read in bulk and broadcasts updated count.
    Calls: NotificationRepository.markAllReadByUserId(), NotificationSseRegistry.broadcastUnreadCount()
    Throws: None
    Transactional: YES

  - getUnreadCount(UUID userId): long
    Purpose: Returns the count of unread notifications for a user.
    Calls: NotificationRepository.countByUserIdAndReadFalse()
    Throws: None
    Transactional: NO

  - deleteNotification(UUID notificationId, UUID userId): void
    Purpose: Deletes a specific notification.
    Calls: NotificationRepository.findByIdAndUserId(), NotificationRepository.delete()
    Throws: EntityNotFoundException
    Transactional: NO

  - deleteAllForUser(UUID userId): void
    Purpose: Deletes all notifications for a user (for data wipe).
    Calls: NotificationRepository.deleteByUserId()
    Throws: None
    Transactional: YES

---

=== PatternAnalysisService.java ===
Injects: ConversationRepository, MemoryRepository, SensorRepository, SensorReadingRepository, InventoryItemRepository, PlannedTaskRepository

Public Methods:
  - buildPatternSummary(UUID userId): PatternSummary
    Purpose: Assembles a user activity snapshot by querying recent conversations, HIGH/CRITICAL importance memories, 24-hour sensor averages, low-stock inventory items, and active planned tasks.
    Calls: ConversationRepository.findByUserIdOrderByUpdatedAtDesc(), MemoryRepository.findByUserIdAndImportance(), SensorRepository.findByUserIdOrderByNameAsc(), SensorReadingRepository.findAverageValueSince(), InventoryItemRepository.findByUserIdOrderByNameAsc(), PlannedTaskRepository.findByUserIdAndStatusOrderByCreatedAtDesc()
    Throws: None
    Transactional: NO

---

## SENSORS SERVICES

---

=== SensorPollingService.java ===
Injects: SerialPortService, SensorRepository, SensorReadingRepository, SseEmitterRegistry, MemoryService, NotificationService, ObjectMapper
(Note: TaskScheduler is created internally as a ThreadPoolTaskScheduler with pool size 10)

Public Methods:
  - startPolling(Sensor sensor): void
    Purpose: Opens the serial port, schedules periodic pollOnce() calls at the sensor's configured interval, persists sensor.isActive=true.
    Calls: SerialPortService.openPort(), taskScheduler.scheduleAtFixedRate(), SensorRepository.save()
    Throws: SensorConnectionException (propagated from openPort)
    Transactional: NO

  - stopPolling(UUID sensorId): void
    Purpose: Cancels the scheduled polling future, closes the serial port, removes SSE emitter, sets sensor.isActive=false.
    Calls: ScheduledFuture.cancel(), SerialPortService.closePort(), SseEmitterRegistry.remove(), SensorRepository.findById(), SensorRepository.save()
    Throws: None
    Transactional: NO

  - stopAllPolling(): void [@PreDestroy]
    Purpose: Stops polling for all active sensors on shutdown.
    Calls: stopPolling()
    Throws: None
    Transactional: NO

  - isPolling(UUID sensorId): boolean
    Purpose: Returns true if a scheduled polling future exists for the given sensor ID.
    Calls: activeFutures.containsKey()
    Throws: None
    Transactional: NO

Private Methods:
  - pollOnce(Sensor sensor): void
  - parseLine(Sensor sensor, String rawLine): Optional<Double>
  - checkThresholds(Sensor sensor, double value): void

---

=== SensorService.java ===
Injects: SensorRepository, SensorReadingRepository, SensorPollingService, SerialPortService

Public Methods:
  - registerSensor(UUID userId, CreateSensorRequest request): Sensor
    Purpose: Validates port path uniqueness, builds and saves a Sensor entity with defaults for baudRate (9600), dataFormat (CSV_LINE), and pollInterval.
    Calls: SensorRepository.findByPortPath(), SensorRepository.save()
    Throws: DuplicateResourceException
    Transactional: NO

  - startSensor(UUID sensorId, UUID userId): Sensor
    Purpose: Starts polling for a sensor scoped to the user.
    Calls: findByIdAndUser(), SensorPollingService.startPolling(), SensorRepository.findById()
    Throws: EntityNotFoundException
    Transactional: NO

  - stopSensor(UUID sensorId, UUID userId): Sensor
    Purpose: Stops polling for a sensor scoped to the user.
    Calls: findByIdAndUser(), SensorPollingService.stopPolling(), SensorRepository.findById()
    Throws: EntityNotFoundException
    Transactional: NO

  - testSensor(String portPath, int baudRate): SensorTestResult
    Purpose: Tests a serial port connection and returns sample data if available.
    Calls: SerialPortService.testConnectionWithSample()
    Throws: None
    Transactional: NO

  - getSensor(UUID sensorId, UUID userId): Sensor
    Purpose: Returns a sensor scoped to the user.
    Calls: findByIdAndUser()
    Throws: EntityNotFoundException
    Transactional: NO

  - listSensors(UUID userId): List<Sensor>
    Purpose: Returns all sensors for a user ordered by name.
    Calls: SensorRepository.findByUserIdOrderByNameAsc()
    Throws: None
    Transactional: NO

  - getLatestReading(UUID sensorId, UUID userId): Optional<SensorReading>
    Purpose: Returns the most recent reading for a sensor scoped to the user.
    Calls: findByIdAndUser(), SensorReadingRepository.findTopBySensorIdOrderByRecordedAtDesc()
    Throws: EntityNotFoundException
    Transactional: NO

  - getReadingHistory(UUID sensorId, UUID userId, int hours, int page, int size): Page<SensorReading>
    Purpose: Returns paginated reading history for a sensor, capping size at MAX_PAGE_SIZE.
    Calls: findByIdAndUser(), SensorReadingRepository.findBySensorIdOrderByRecordedAtDesc()
    Throws: EntityNotFoundException
    Transactional: NO

  - deleteSensor(UUID sensorId, UUID userId): void
    Purpose: Stops polling if active, deletes all readings, then deletes the sensor entity.
    Calls: findByIdAndUser(), SensorPollingService.isPolling(), SensorPollingService.stopPolling(), SensorReadingRepository.deleteBySensorId(), SensorRepository.delete()
    Throws: EntityNotFoundException
    Transactional: YES

  - updateThresholds(UUID sensorId, UUID userId, UpdateThresholdsRequest request): Sensor
    Purpose: Updates low and high alert thresholds for a sensor.
    Calls: findByIdAndUser(), SensorRepository.save()
    Throws: EntityNotFoundException
    Transactional: NO

  - listAvailablePorts(): List<String>
    Purpose: Returns a list of all serial port paths available on the device.
    Calls: SerialPortService.listAvailablePorts()
    Throws: None
    Transactional: NO

  - deleteAllForUser(UUID userId): void
    Purpose: Stops polling for all user sensors, deletes all readings, deletes all sensors.
    Calls: SensorRepository.findByUserIdOrderByNameAsc(), SensorPollingService.isPolling(), SensorPollingService.stopPolling(), SensorReadingRepository.deleteByUserId(), SensorRepository.deleteByUserId()
    Throws: None
    Transactional: YES

Private Methods:
  - findByIdAndUser(UUID sensorId, UUID userId): Sensor

---

=== SensorStartupService.java ===
Injects: SensorRepository, SensorPollingService
Annotations: @EventListener(ApplicationReadyEvent)

Public Methods:
  - resumeActiveSensors(): void [@EventListener(ApplicationReadyEvent)]
    Purpose: On startup, finds all sensors flagged isActive=true in the database and resumes polling; marks failed sensors as inactive.
    Calls: SensorRepository.findByIsActiveTrue(), SensorPollingService.startPolling(), SensorRepository.save()
    Throws: None (per-sensor failures logged)
    Transactional: NO

---

=== SerialPortService.java ===
Injects: (none)

Public Methods:
  - listAvailablePorts(): List<String>
    Purpose: Returns system port paths for all serial ports detected by jSerialComm.
    Calls: SerialPort.getCommPorts()
    Throws: None
    Transactional: NO

  - openPort(String portPath, int baudRate): SerialPort
    Purpose: Opens a serial port at the given path and baud rate with 1-second semi-blocking read timeout.
    Calls: SerialPort.getCommPort(), port.openPort()
    Throws: SensorConnectionException (port cannot be opened)
    Transactional: NO

  - closePort(SerialPort port): void
    Purpose: Closes a serial port; no-op on null or already-closed port, never throws.
    Calls: port.closePort()
    Throws: None
    Transactional: NO

  - readLine(SerialPort port): Optional<String>
    Purpose: Reads bytes from the port's input stream until newline or timeout, returns trimmed line.
    Calls: port.getInputStream(), InputStream.read()
    Throws: None (returns Optional.empty() on error)
    Transactional: NO

  - testConnection(String portPath, int baudRate, int timeoutMs): boolean
    Purpose: Opens a port, attempts to read one line within the timeout, closes the port, returns success.
    Calls: openPort(), readLine(), closePort()
    Throws: None (returns false on error)
    Transactional: NO

  - testConnectionWithSample(String portPath, int baudRate, int timeoutMs): String
    Purpose: Same as testConnection() but returns the raw sample line (or null if no data).
    Calls: openPort(), readLine(), closePort()
    Throws: None (returns null on error)
    Transactional: NO

---

## SETTINGS SERVICES

---

=== ExternalApiSettingsService.java ===
Injects: ExternalApiSettingsRepository

Public Methods:
  - getSettings(): ExternalApiSettingsDto
    Purpose: Returns the singleton settings DTO (API keys are never included — only boolean presence flags).
    Calls: getOrCreateEntity(), toDto()
    Throws: None
    Transactional: YES

  - updateSettings(UpdateExternalApiSettingsRequest request): ExternalApiSettingsDto
    Purpose: Updates external API keys (only when non-null in request; blank clears the key), model names, toggle flags, and judge configuration; persists.
    Calls: getOrCreateEntity(), repository.save(), toDto()
    Throws: None
    Transactional: YES

  - getAnthropicKey(): Optional<String>
    Purpose: Returns the Anthropic API key if anthropicEnabled=true and key is set (internal use only).
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getBraveKey(): Optional<String>
    Purpose: Returns the Brave Search API key if braveEnabled=true and key is set.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getAnthropicModel(): String
    Purpose: Returns the configured Anthropic model name.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getMaxWebFetchSizeKb(): int
    Purpose: Returns the maximum web fetch size in kilobytes.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getSearchResultLimit(): int
    Purpose: Returns the maximum number of search results.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getHuggingFaceToken(): Optional<String>
    Purpose: Returns the HuggingFace token if enabled and set.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getGrokKey(): Optional<String>
    Purpose: Returns the Grok API key if grokEnabled=true and key is set.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getOpenAiKey(): Optional<String>
    Purpose: Returns the OpenAI API key if openAiEnabled=true and key is set.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

  - getPreferredFrontierProvider(): FrontierProvider
    Purpose: Returns the preferred frontier provider for cloud routing; defaults to CLAUDE if not set.
    Calls: getOrCreateEntity()
    Throws: None
    Transactional: YES (readOnly)

Private Methods:
  - getOrCreateEntity(): ExternalApiSettings
  - toDto(ExternalApiSettings entity): ExternalApiSettingsDto

---

## SKILLS SERVICES

---

=== SkillExecutorService.java ===
Injects: SkillRepository, SkillExecutionRepository, List<BuiltInSkill> (auto-registered into skillRegistry map), ObjectMapper

Public Methods:
  - execute(UUID skillId, UUID userId, Map<String, Object> params): SkillExecution
    Purpose: Looks up skill by ID, asserts it is enabled, dispatches to the registered BuiltInSkill implementation, persists a SkillExecution record with RUNNING→COMPLETED/FAILED status and duration.
    Calls: SkillRepository.findById(), executeSkill()
    Throws: EntityNotFoundException, SkillDisabledException
    Transactional: YES

  - executeByName(String skillName, UUID userId, Map<String, Object> params): SkillExecution
    Purpose: Same as execute() but resolves skill by name; used by AgentService tool-call dispatcher.
    Calls: SkillRepository.findByName(), executeSkill()
    Throws: EntityNotFoundException, SkillDisabledException
    Transactional: YES

Private Methods:
  - executeSkill(Skill skill, UUID userId, Map<String, Object> params): SkillExecution
  - toJson(Object obj): String

---

=== SkillSeederService.java ===
Injects: SkillRepository
Annotations: @EventListener(ApplicationReadyEvent), @Transactional

Public Methods:
  - seedBuiltInSkills(): void [@EventListener(ApplicationReadyEvent), @Transactional]
    Purpose: On startup, seeds 6 built-in skill definitions into the database if not already present: WeatherQuery, InventoryTracker, RecipeGenerator, TaskPlanner, DocumentSummarizer, ResourceCalculator.
    Calls: SkillRepository.findByName(), SkillRepository.save()
    Throws: None
    Transactional: YES

---

## SYSTEM SERVICES

---

=== ApModeService.java ===
Injects: SystemConfigService, @Value app.ap.mock (default true)

Public Methods:
  - startApMode(): void
    Purpose: Enables hostapd and dnsmasq via systemctl, polls until active or timeout; in mock mode logs a warning and returns immediately.
    Calls: executeCommand("sudo systemctl start hostapd/dnsmasq"), isServiceActive()
    Throws: ApModeException (services did not start within timeout)
    Transactional: NO

  - stopApMode(): void
    Purpose: Stops hostapd and dnsmasq via systemctl; in mock mode skips; never throws.
    Calls: executeCommand("sudo systemctl stop hostapd/dnsmasq")
    Throws: None (logs warning on failure)
    Transactional: NO

  - isApModeActive(): boolean
    Purpose: In mock mode returns SystemConfig.apModeEnabled; otherwise checks hostapd service state.
    Calls: SystemConfigService.getConfig(), isServiceActive("hostapd")
    Throws: None
    Transactional: NO

  - scanWifiNetworks(): List<WifiNetwork>
    Purpose: Scans WiFi networks via nmcli; in mock mode returns a hardcoded sample list; filters own AP SSID.
    Calls: executeCommand("sudo nmcli -t -f SSID,SIGNAL,SECURITY device wifi list")
    Throws: None (returns empty list on error)
    Transactional: NO

  - connectToWifi(String ssid, String password): boolean
    Purpose: Connects to a WiFi network via nmcli; in mock mode always returns true.
    Calls: executeCommand("sudo nmcli device wifi connect ...")
    Throws: None (returns false on failure)
    Transactional: NO

  - getConnectionStatus(): WifiConnectionStatus
    Purpose: Returns current WiFi connection state and internet connectivity via nmcli general status; in mock mode returns connected=true, hasInternet=false.
    Calls: executeCommand("nmcli -t -f STATE,CONNECTIVITY general status")
    Throws: None
    Transactional: NO

Private Methods:
  - isServiceActive(String serviceName): boolean
  - executeCommand(String command): String

---

=== ApModeStartupService.java ===
Injects: SystemConfigService, ApModeService
Annotations: @Component, @EventListener(ApplicationReadyEvent), @Order

Public Methods:
  - onApplicationReady(): void [@EventListener(ApplicationReadyEvent), @Order]
    Purpose: If the system is not initialized, starts AP mode and sets apModeEnabled=true; otherwise ensures AP mode is stopped.
    Calls: SystemConfigService.isInitialized(), ApModeService.startApMode(), SystemConfigService.getConfig(), SystemConfigService.save(), ApModeService.stopApMode()
    Throws: None (exceptions caught from ApModeService)
    Transactional: NO

---

=== FactoryResetService.java ===
Injects: SystemConfigService, ApModeService

Public Methods:
  - performReset(): void [@Async]
    Purpose: Asynchronous API-triggered factory reset — waits FACTORY_RESET_DELAY_SECONDS for response delivery, then resets SystemConfig to defaults and re-enables AP mode.
    Calls: Thread.sleep(), resetSystemConfig(), restartApMode()
    Throws: None
    Transactional: NO

  - performUsbReset(): void
    Purpose: Synchronous USB-triggered factory reset — resets SystemConfig immediately and re-enables AP mode without delay.
    Calls: resetSystemConfig(), restartApMode()
    Throws: None
    Transactional: NO

Private Methods:
  - resetSystemConfig(): void
  - restartApMode(): void

---

=== NetworkTransitionService.java ===
Injects: ApModeService, SystemConfigService, @Value app.ap.mock (default true)

Public Methods:
  - finalizeSetup(): void [@Async]
    Purpose: Asynchronously transitions device from AP mode to home network — stops AP mode, waits for network stabilization, starts avahi-daemon for mDNS (offgrid.local), updates SystemConfig apModeEnabled=false, wifiConfigured=true.
    Calls: ApModeService.stopApMode(), Thread.sleep(), startAvahi() (non-mock only), SystemConfigService.getConfig(), SystemConfigService.save()
    Throws: None (exceptions logged)
    Transactional: NO

Private Methods:
  - startAvahi(): void

---

=== SystemConfigService.java ===
Injects: SystemConfigRepository

Public Methods:
  - getConfig(): SystemConfig
    Purpose: Returns the singleton SystemConfig row, creating one with defaults if none exists.
    Calls: SystemConfigRepository.findFirst(), SystemConfigRepository.save() (on create)
    Throws: None
    Transactional: NO

  - save(SystemConfig config): SystemConfig
    Purpose: Persists a SystemConfig instance.
    Calls: SystemConfigRepository.save()
    Throws: None
    Transactional: NO

  - isInitialized(): boolean
    Purpose: Returns whether the system has completed first-boot setup.
    Calls: getConfig()
    Throws: None
    Transactional: NO

  - setInitialized(String instanceName): SystemConfig
    Purpose: Marks the system as initialized with the given instance name.
    Calls: getConfig(), SystemConfigRepository.save()
    Throws: None
    Transactional: NO

  - setFortressEnabled(boolean enabled, UUID userId): SystemConfig
    Purpose: Enables or disables fortress mode, recording the timestamp and userId.
    Calls: getConfig(), SystemConfigRepository.save()
    Throws: None
    Transactional: NO

  - isWifiConfigured(): boolean
    Purpose: Returns whether WiFi has been configured on this device.
    Calls: getConfig()
    Throws: None
    Transactional: NO

  - getAiSettings(): AiSettingsDto
    Purpose: Returns AI and memory tuning settings (model name, temperature, similarity threshold, topK, RAG context tokens, context size, context message limit).
    Calls: getConfig()
    Throws: None
    Transactional: NO

  - getStorageSettings(): StorageSettingsDto
    Purpose: Returns storage path configuration with live disk usage statistics.
    Calls: getConfig(), File.getTotalSpace(), File.getUsableSpace()
    Throws: None
    Transactional: NO

  - updateStorageSettings(StorageSettingsDto dto): StorageSettingsDto
    Purpose: Validates and updates the knowledge storage path and optional max upload size.
    Calls: getConfig(), SystemConfigRepository.save(), getStorageSettings()
    Throws: IllegalArgumentException (blank path, relative path, non-directory, non-writable, out-of-range size)
    Transactional: NO

  - getActiveModelFilename(): String
    Purpose: Returns the active GGUF model filename from SystemConfig.
    Calls: getConfig()
    Throws: None
    Transactional: NO

  - setActiveModelFilename(String filename): void
    Purpose: Persists the active model filename in SystemConfig.
    Calls: getConfig(), SystemConfigRepository.save()
    Throws: None
    Transactional: NO

  - updateAiSettings(AiSettingsDto dto): AiSettingsDto
    Purpose: Validates and updates all AI tuning parameters in SystemConfig.
    Calls: getConfig(), SystemConfigRepository.save()
    Throws: IllegalArgumentException (any value out of valid range)
    Transactional: NO

---

=== UsbResetWatcherService.java ===
Injects: FactoryResetService
Annotations: @Component, @Scheduled(fixedDelay=30000)

Public Methods:
  - checkForTriggerFiles(): void [@Scheduled(fixedDelay=30000)]
    Purpose: Every 30 seconds, checks the USB mount path for a factory-reset.trigger file (deletes it then performs USB reset) and a myoffgridai-update.zip file (logs stub message, update not yet implemented).
    Calls: Files.isDirectory(), checkFactoryResetTrigger(), checkUpdateZip()
    Throws: None
    Transactional: NO

Private Methods:
  - checkFactoryResetTrigger(Path usbPath): void
  - checkUpdateZip(Path usbPath): void

---

That is the complete reference for all 61 service classes across all packages in the MyOffGridAI-Server project.
```


## 10. Controller / API Layer — Method Signatures Only

**Total Controllers: 20** (request/response details in OpenAPI spec)

```
=== AuthController.java ===
Base Path: /api/auth
Injects: AuthService
Endpoints:
  - register() → authService.register()
  - login() → authService.login()
  - refresh() → authService.refresh()
  - logout() → authService.logout()

=== UserController.java ===
Base Path: /api/users
Injects: UserService
Endpoints:
  - listUsers() → userService.listUsers()
  - getUser() → userService.getUserById()
  - updateUser() → userService.updateUser()
  - deactivateUser() → userService.deactivateUser()
  - deleteUser() → userService.deleteUser()

=== ChatController.java ===
Base Path: /api/chat
Injects: ChatService, MessageRepository
Endpoints:
  - createConversation() → chatService.createConversation()
  - listConversations() → chatService.getConversations()
  - searchConversations() → chatService.searchConversations()
  - getConversation() → chatService.getConversation()
  - deleteConversation() → chatService.deleteConversation()
  - archiveConversation() → chatService.archiveConversation()
  - renameConversation() → chatService.renameConversation()
  - sendMessage() → chatService.streamMessage() / chatService.sendMessage()
  - listMessages() → messageRepository.findByConversationIdOrderByCreatedAtAsc()
  - editMessage() → chatService.editMessage()
  - deleteMessage() → chatService.deleteMessage()
  - branchConversation() → chatService.branchConversation()
  - regenerateMessage() → chatService.regenerateMessage()

=== ModelController.java ===
Base Path: /api/models
Injects: InferenceService, SystemConfigService
Endpoints:
  - listModels() → inferenceService.listModels()
  - getActiveModel() → inferenceService.getActiveModel()
  - getHealth() → inferenceService.isAvailable()

=== ModelDownloadController.java ===
Base Path: /api/models
Injects: ModelCatalogService, ModelDownloadService, ModelDownloadProgressRegistry, SystemConfigService, NativeLlamaInferenceService (optional), LlamaServerProcessService (optional)
Endpoints:
  - searchCatalog() → catalogService.searchModels()
  - getModelDetails() → catalogService.getModelDetails()
  - getModelFiles() → catalogService.getModelFiles()
  - startDownload() → downloadService.startDownload()
  - getAllDownloads() → downloadService.getAllDownloads()
  - getDownloadProgress() → progressRegistry.subscribe()
  - cancelDownload() → downloadService.cancelDownload()
  - listLocalModels() → downloadService.listLocalModels()
  - deleteLocalModel() → downloadService.deleteLocalModel()
  - setActiveModel() → llamaServerProcessService.switchModel() OR nativeInferenceService.loadModel()
  - getServerStatus() → llamaServerProcessService.getStatus() OR nativeInferenceService.getStatus()
  - reloadModel() → llamaServerProcessService.restart() OR nativeInferenceService.loadModel()

=== JudgeController.java ===
Base Path: /api/judge
Injects: JudgeInferenceService, JudgeModelProcessService, JudgeProperties
Endpoints:
  - getStatus() → judgeProcessService.getStatus() + judgeProperties
  - testJudge() → judgeInferenceService.evaluate()

=== EnrichmentController.java ===
Base Path: /api/enrichment
Injects: WebFetchService, WebSearchService, ClaudeApiService, ExternalApiSettingsService
Endpoints:
  - fetchUrl() → webFetchService.fetchAndStore()
  - search() → webSearchService.search() / searchAndStore()
  - getStatus() → claudeApiService.isAvailable() + webSearchService.isAvailable()

=== ScheduledEventController.java ===
Base Path: /api/events
Injects: ScheduledEventService
Endpoints:
  - listEvents() → eventService.listEvents()
  - getEvent() → eventService.getEvent()
  - createEvent() → eventService.createEvent()
  - updateEvent() → eventService.updateEvent()
  - deleteEvent() → eventService.deleteEvent()
  - toggleEvent() → eventService.toggleEvent()

=== KnowledgeController.java ===
Base Path: /api/knowledge
Injects: KnowledgeService, SemanticSearchService, SystemConfigService, FileStorageService
Endpoints:
  - uploadDocument() → knowledgeService.upload()
  - listDocuments() → knowledgeService.listDocuments()
  - getDocument() → knowledgeService.getDocument()
  - updateDisplayName() → knowledgeService.updateDisplayName()
  - deleteDocument() → knowledgeService.deleteDocument()
  - retryProcessing() → knowledgeService.retryProcessing()
  - downloadDocument() → knowledgeService.getDocumentForDownload() + fileStorageService.getInputStream()
  - getDocumentContent() → knowledgeService.getDocumentContent()
  - createDocument() → knowledgeService.createFromEditor()
  - updateDocumentContent() → knowledgeService.updateContent()
  - searchKnowledge() → semanticSearchService.search()

=== LibraryController.java ===
Base Path: /api/library
Injects: ZimFileService, EbookService, GutenbergService
Endpoints:
  - uploadZim() → zimFileService.upload()
  - listZimFiles() → zimFileService.listAll()
  - deleteZim() → zimFileService.delete()
  - kiwixStatus() → zimFileService.getKiwixStatus()
  - kiwixUrl() → zimFileService.getKiwixServeUrl()
  - uploadEbook() → ebookService.upload()
  - listEbooks() → ebookService.list()
  - getEbook() → ebookService.get()
  - deleteEbook() → ebookService.delete()
  - downloadEbook() → ebookService.getForDownload()
  - searchGutenberg() → gutenbergService.search()
  - getGutenbergBook() → gutenbergService.getBookMetadata()
  - importGutenberg() → gutenbergService.importBook()

=== McpTokenController.java ===
Base Path: /api/mcp/tokens
Injects: McpTokenService
Endpoints:
  - createToken() → mcpTokenService.createToken()
  - listTokens() → mcpTokenService.listTokens()
  - revokeToken() → mcpTokenService.revokeToken()

=== McpDiscoveryController.java ===
Base Path: /api/mcp
Injects: (none)
Endpoints:
  - getClaudeDesktopConfig() → returns static config JSON

=== MemoryController.java ===
Base Path: /api/memories
Injects: MemoryService
Endpoints:
  - listMemories() → memoryService.getMemories()
  - getMemory() → memoryService.getMemory()
  - deleteMemory() → memoryService.deleteMemory()
  - updateImportance() → memoryService.updateImportance()
  - updateTags() → memoryService.updateTags()
  - searchMemories() → memoryService.searchMemoriesWithScores()
  - exportMemories() → memoryService.exportMemories()

=== DeviceRegistrationController.java ===
Base Path: /api/notifications/devices
Injects: DeviceRegistrationService
Endpoints:
  - registerDevice() → deviceRegistrationService.registerDevice()
  - getDevices() → deviceRegistrationService.getDevicesForUser()
  - unregisterDevice() → deviceRegistrationService.unregisterDevice()

=== PrivacyController.java ===
Base Path: /api/privacy
Injects: FortressService, AuditService, SovereigntyReportService, DataExportService, DataWipeService
Endpoints:
  - getFortressStatus() → fortressService.getFortressStatus()
  - enableFortress() → fortressService.enable()
  - disableFortress() → fortressService.disable()
  - getSovereigntyReport() → sovereigntyReportService.generateReport()
  - getAuditLogs() → auditService.getAuditLogs()
  - exportData() → dataExportService.exportUserData()
  - wipeData() → dataWipeService.wipeUser()
  - wipeSelfData() → dataWipeService.wipeUser()

=== ProactiveController.java ===
Base Path: /api/insights, /api/notifications
Injects: InsightService, InsightGeneratorService, NotificationService, NotificationSseRegistry
Endpoints:
  - getInsights() → insightService.getInsights()
  - generateInsights() → insightGeneratorService.generateInsightForUser()
  - markInsightRead() → insightService.markRead()
  - dismissInsight() → insightService.dismiss()
  - getInsightUnreadCount() → insightService.getUnreadCount()
  - getNotifications() → notificationService.getNotifications()
  - markNotificationRead() → notificationService.markRead()
  - markAllNotificationsRead() → notificationService.markAllRead()
  - getNotificationUnreadCount() → notificationService.getUnreadCount()
  - deleteNotification() → notificationService.deleteNotification()
  - streamNotifications() → notificationSseRegistry.register()

=== SensorController.java ===
Base Path: /api/sensors
Injects: SensorService, SseEmitterRegistry
Endpoints:
  - listSensors() → sensorService.listSensors()
  - getSensor() → sensorService.getSensor()
  - registerSensor() → sensorService.registerSensor()
  - deleteSensor() → sensorService.deleteSensor()
  - startSensor() → sensorService.startSensor()
  - stopSensor() → sensorService.stopSensor()
  - getLatestReading() → sensorService.getLatestReading()
  - getReadingHistory() → sensorService.getReadingHistory()
  - updateThresholds() → sensorService.updateThresholds()
  - testConnection() → sensorService.testSensor()
  - listAvailablePorts() → sensorService.listAvailablePorts()
  - streamSensor() → sseEmitterRegistry.register()

=== ExternalApiSettingsController.java ===
Base Path: /api/settings/external-apis
Injects: ExternalApiSettingsService
Endpoints:
  - getSettings() → settingsService.getSettings()
  - updateSettings() → settingsService.updateSettings()

=== SkillController.java ===
Base Path: /api/skills
Injects: SkillRepository, SkillExecutionRepository, InventoryItemRepository, SkillExecutorService
Endpoints:
  - listSkills() → skillRepository.findByIsEnabledTrueOrderByDisplayNameAsc()
  - getSkill() → skillRepository.findById()
  - toggleSkill() → skillRepository.save()
  - executeSkill() → skillExecutorService.execute()
  - listExecutions() → executionRepository.findByUserIdOrderByStartedAtDesc()
  - listInventory() → inventoryItemRepository.findByUserIdAndCategory()
  - createInventoryItem() → inventoryItemRepository.save()
  - updateInventoryItem() → inventoryItemRepository.save()
  - deleteInventoryItem() → inventoryItemRepository.delete()

=== SystemController.java ===
Base Path: /api/system
Injects: SystemConfigService, AuthService, NetworkTransitionService, FactoryResetService, NativeLlamaInferenceService (optional), LlamaServerProcessService (optional)
Endpoints:
  - getStatus() → systemConfigService.getConfig()
  - initialize() → authService.register() + systemConfigService.setInitialized()
  - finalizeSetup() → networkTransitionService.finalizeSetup()
  - getAiSettings() → systemConfigService.getAiSettings()
  - updateAiSettings() → systemConfigService.updateAiSettings()
  - getStorageSettings() → systemConfigService.getStorageSettings()
  - updateStorageSettings() → systemConfigService.updateStorageSettings()
  - factoryReset() → factoryResetService.performReset()
  - getLlamaServerStatus() → llamaServerProcessService.getStatus()
  - switchLlamaServerModel() → llamaServerProcessService.switchModel()

=== CaptivePortalController.java ===
Base Path: /setup, /api/setup
Injects: SystemConfigService, ApModeService
Endpoints:
  - setupWelcome() → systemConfigService.isInitialized()
  - scanWifi() → apModeService.scanWifiNetworks()
  - connectWifi() → apModeService.connectToWifi()
  - wifiStatus() → apModeService.getConnectionStatus()
```


## 11. Security Configuration

```
Authentication: JWT (stateless, HMAC-SHA)
Token issuer/validator: Internal (JwtService using jjwt)
Password encoder: BCrypt (default rounds)

Public endpoints (no auth required):
  - /api/auth/login
  - /api/auth/register
  - /api/auth/refresh
  - /api/system/status
  - /api/system/initialize
  - /api/system/finalize-setup
  - /api/setup/**
  - /api/models (list)
  - /api/models/health
  - /setup/**
  - /actuator/health
  - /v3/api-docs/**
  - /swagger-ui/**
  - /mcp/**

Protected endpoints (patterns):
  - anyRequest().authenticated() — all other paths require valid JWT

CORS: AllowedOriginPatterns("*"), all methods, all headers, credentials=true
CSRF: Disabled (stateless REST API)
Rate limiting: Bucket4j per-IP, two tiers:
  - Auth endpoints (/api/auth/**): 10 req/min
  - General API: 200 req/min
Session: STATELESS (no server-side sessions)
```


## 12. Custom Security Components

```
=== JwtAuthFilter.java === (config/)
Extends: OncePerRequestFilter
Purpose: Extracts Bearer token from Authorization header, validates via JwtService, sets SecurityContext
Extracts token from: Authorization header (Bearer prefix)
Validates via: JwtService.extractUsername() + JwtService.isTokenValid()
Checks blacklist via: AuthService.isTokenBlacklisted()
Sets SecurityContext: YES (UsernamePasswordAuthenticationToken with UserDetails)

=== McpAuthFilter.java === (mcp/config/)
Purpose: Authenticates MCP SSE requests using bearer tokens from McpApiToken table
Extracts token from: Authorization header (Bearer prefix)
Validates via: McpTokenService
Sets SecurityContext: YES (McpAuthentication with authorities)

=== McpAuthentication.java === (mcp/config/)
Extends: AbstractAuthenticationToken
Purpose: Custom authentication token for MCP requests

=== RateLimitingFilter.java === (config/)
Extends: OncePerRequestFilter, @Order(3)
Purpose: Per-IP rate limiting via Bucket4j token-bucket. Auth endpoints: 10/min. API: 200/min.
Configurable via: app.rate-limiting.enabled (disabled in test profile)

=== CaptivePortalRedirectFilter.java === (config/)
Purpose: Redirects HTTP requests to captive portal setup when system is not initialized

=== MdcFilter.java === (config/)
Purpose: Sets MDC (requestId, username, userId) for structured logging correlation

=== RequestResponseLoggingFilter.java === (config/)
Purpose: Logs HTTP request/response method, path, status, and latency at DEBUG level

=== AuditAspect.java === (privacy/aspect/)
Purpose: AOP aspect that records audit log entries for security-sensitive operations
Uses: @Around or @AfterReturning on annotated controller methods
Records: userId, action, outcome (SUCCESS/FAILURE/DENIED), IP address, timestamp
```


## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler.java === (common/exception/)
@RestControllerAdvice: YES

Exception Mappings:
  - MethodArgumentNotValidException → 400 (field error details joined)
  - UsernameNotFoundException → 404
  - BadCredentialsException → 401 ("Invalid username or password")
  - AccessDeniedException → 403 ("Access denied")
  - EntityNotFoundException → 404 (message from exception)
  - DuplicateResourceException → 409 (message)
  - IllegalArgumentException → 400 (message)
  - FortressActiveException → 403 (message)
  - OllamaUnavailableException → 503 (message)
  - OllamaInferenceException → 502 (message)
  - EmbeddingException → 503 (message)
  - StorageException → 500 (message)
  - UnsupportedFileTypeException → 400 (message)
  - OcrException → 500 (message)
  - SkillDisabledException → 400 (message)
  - ApModeException → 500 (message)
  - SensorConnectionException → 502 (message)
  - MaxUploadSizeExceededException → 413 ("File too large...")
  - AsyncRequestNotUsableException → (no response, client disconnected)
  - Exception (catch-all) → 500 ("An unexpected error occurred")

Standard error response format:
{
  "success": false,
  "message": "...",
  "data": null,
  "timestamp": "2026-03-17T...",
  "requestId": "uuid"
}
```

**Custom Exception Classes** (all in `common/exception/`):
- `EntityNotFoundException` — generic not-found
- `DuplicateResourceException` — conflict on create
- `EmbeddingException` — vector embedding failures
- `FortressActiveException` — operation blocked by fortress mode
- `FortressOperationException` — fortress enable/disable failures
- `InitializationException` — system initialization failures
- `OcrException` — Tesseract OCR failures
- `OllamaInferenceException` — LLM inference failures
- `OllamaUnavailableException` — LLM service unreachable
- `SensorConnectionException` — serial port failures
- `SkillDisabledException` — disabled skill execution attempt
- `StorageException` — file I/O failures
- `UnsupportedFileTypeException` — unsupported upload MIME type
- `ApModeException` — WiFi AP mode operation failures


## 14. Mappers / DTOs

**Framework: Manual mapping** (no MapStruct or ModelMapper). All DTO-to-entity conversion is done manually in service methods or inline in controller methods.

DTO structures are documented in the OpenAPI spec. Key mapping patterns:
- Services convert entities to DTOs via constructor calls or builder patterns
- Controllers receive request DTOs with `@Valid`, pass to services
- Services return response DTOs or entities that controllers wrap in `ApiResponse`


## 15. Utility Classes & Shared Components

```
=== ApiResponse.java === (common/response/)
Generic response wrapper for all endpoints.
Fields: success (boolean), message (String), data (T), timestamp (Instant), requestId (String),
        totalElements (Long), page (Integer), size (Integer) — pagination fields nullable
Factory methods:
  - success(T data): ApiResponse<T>
  - success(T data, String message): ApiResponse<T>
  - error(String message): ApiResponse<T>
  - paginated(T data, long total, int page, int size): ApiResponse<T>

=== AesEncryptionUtil.java === (common/util/)
AES-256-GCM encryption/decryption utility.
Methods:
  - encrypt(String plaintext): String (Base64-encoded, IV prepended)
  - decrypt(String ciphertext): String
Key from: app.encryption.key (64-char hex string = 32 bytes)
Used by: AesAttributeConverter (JPA attribute converter for encrypted columns)

=== AesAttributeConverter.java === (common/util/)
JPA AttributeConverter that transparently encrypts/decrypts entity fields using AesEncryptionUtil.
Used on: ExternalApiSettings encrypted fields (API keys)

=== TokenCounter.java === (common/util/)
Token estimation utility (1 token ≈ 4 characters).
Methods:
  - estimateTokens(String text): int
  - truncateToTokenLimit(List<OllamaMessage>, int maxTokens): List<OllamaMessage>
Used by: ContextWindowService, ChatService

=== DeltaJsonUtils.java === (knowledge/util/)
Quill Delta JSON ↔ plain text conversion utility.
Methods:
  - deltaToPlainText(String deltaJson): String
  - plainTextToDelta(String plainText): String
Used by: KnowledgeService (for rich-text document editing)

=== AppConstants.java === (config/)
Centralized constants class. All magic numbers/strings defined here.
Domains: Server, JWT, API Paths, Pagination, File Upload, Sensors, RAG, Memory, Knowledge,
         Skills, Events, Proactive, Privacy, Rate Limiting, External APIs, MCP, MQTT,
         Library, Inference, HuggingFace, Judge, Frontier, Hybrid Search, Retry, Compaction

=== ProcessBuilderFactory.java === (ai/service/)
Factory for creating OS ProcessBuilder instances (testable abstraction).
Methods:
  - create(List<String> command): ProcessBuilder
Used by: LlamaServerProcessService, JudgeModelProcessService, ApModeService

=== SystemPromptBuilder.java === (ai/service/)
Builds dynamic system prompts for LLM conversations.
Methods:
  - buildSystemPrompt(RagContext context, User user, SystemConfig config): String
Used by: ChatService
```


## 16. Database Schema (Live)

Database not available for live schema dump (PostgreSQL not running during audit).

**Schema derived from JPA entities (Hibernate ddl-auto: update in dev):**

| Table | Entity | Primary Key | Key Columns |
|-------|--------|-------------|-------------|
| conversations | Conversation | id (UUID) | user_id, title, is_archived, created_at, updated_at, message_count |
| messages | Message | id (UUID) | conversation_id, role, content, source_tag, model_name, created_at, thinking_content, token_count |
| users | User | id (UUID) | username (unique), email (unique), password, display_name, role, is_active, created_at, updated_at |
| scheduled_events | ScheduledEvent | id (UUID) | user_id, name, event_type, action_type, cron_expression, sensor_id, threshold_*, is_enabled |
| knowledge_documents | KnowledgeDocument | id (UUID) | user_id, display_name, mime_type, file_size_bytes, status, chunk_count, file_path, storage_filename |
| knowledge_chunks | KnowledgeChunk | id (UUID) | document_id, user_id, chunk_index, content, embedding (vector(768)) |
| ebooks | Ebook | id (UUID) | title, author, format, file_path, file_size_bytes, gutenberg_id |
| zim_files | ZimFile | id (UUID) | filename (unique), display_name, file_size_bytes |
| mcp_api_tokens | McpApiToken | id (UUID) | name, token_hash, created_by, is_active, created_at |
| memories | Memory | id (UUID) | user_id, content, importance, tags (TEXT), source, created_at |
| vector_document | VectorDocument | id (UUID) | user_id, source_id, source_type, content, embedding (vector(768)), metadata |
| device_registrations | DeviceRegistration | id (UUID) | user_id, device_id (unique), device_name, mqtt_client_id |
| audit_logs | AuditLog | id (UUID) | user_id, action, outcome, ip_address, details, timestamp |
| insights | Insight | id (UUID) | user_id, title, body, category, is_read, is_dismissed, generated_at |
| notifications | Notification | id (UUID) | user_id, title, body, type, severity, is_read, read_at, created_at |
| sensors | Sensor | id (UUID) | user_id, name, sensor_type, port_path, is_active, baud_rate, data_format, threshold_* |
| sensor_readings | SensorReading | id (UUID) | sensor_id, value, unit, raw_data, recorded_at |
| external_api_settings | ExternalApiSettings | id (UUID) | singleton_guard (unique), claude_api_key (encrypted), grok_api_key (encrypted), openai_api_key (encrypted), preferred_provider, claude_model, grok_model, openai_model |
| skills | Skill | id (UUID) | name (unique), display_name, description, category, is_enabled, is_built_in |
| skill_executions | SkillExecution | id (UUID) | skill_id, user_id, status, input, output, started_at, completed_at |
| inventory_items | InventoryItem | id (UUID) | user_id, name, category, quantity, unit, location, notes |
| planned_tasks | PlannedTask | id (UUID) | user_id, title, description, priority, status, due_date, created_at |
| system_config | SystemConfig | id (UUID) | is_initialized, device_name, active_ollama_model, active_model_filename, knowledge_storage_path, embedding_model |

**pgvector extension required** for `vector(768)` columns on `vector_document` and `knowledge_chunks`.


## 17. Message Broker Configuration

```
Broker: MQTT (Eclipse Paho v3 client)
Connection: tcp://localhost:1883 (configurable via app.mqtt.broker-url)
Client ID: myoffgridai-server

Topics:
  - myoffgridai/sensors/readings — sensor data events
  - myoffgridai/system/alerts — system health alerts
  - myoffgridai/notifications — notification push events
  - myoffgridai/insights — insight generation events
  - /myoffgridai/{userId}/notifications — user-specific notifications
  - /myoffgridai/broadcast — broadcast to all devices

Publisher: MqttPublisherService
  - publish(String topic, NotificationPayload payload): void
  - QoS: 1 (at-least-once)

Configuration: MqttConfig (conditional on app.mqtt.enabled=true)
  - Disabled in dev profile (app.mqtt.enabled=false)
  - Enabled in prod profile
  - No RabbitMQ or Kafka detected.
```


## 18. Cache Layer

No Redis or caching layer detected. No `@Cacheable`, `@CacheEvict`, or `CacheManager` annotations found.


## 19. Environment Variable Inventory

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|------------------|
| DB_URL | application-prod.yml | jdbc:postgresql://localhost:5432/myoffgridai | YES |
| DB_USERNAME | application-prod.yml | myoffgridai | YES |
| DB_PASSWORD | application-prod.yml | myoffgridai | YES |
| JWT_SECRET | application-prod.yml | (none) | YES |
| ENCRYPTION_KEY | application-prod.yml | (none) | YES |
| INFERENCE_PROVIDER | application-prod.yml | llama-server | NO |
| INFERENCE_BASE_URL | application-prod.yml | http://localhost:1234 | NO |
| INFERENCE_MODEL | application-prod.yml | Qwen3.5 model path | NO |
| INFERENCE_EMBED_MODEL | application-prod.yml | nomic-embed-text | NO |
| LLAMA_SERVER_BINARY | application-prod.yml | /usr/local/bin/llama-server | NO |
| MODELS_DIR | application-prod.yml | ./models | NO |
| ACTIVE_MODEL | application-prod.yml | (empty) | NO |
| INFERENCE_PORT | application-prod.yml | 1234 | NO |
| INFERENCE_CONTEXT_SIZE | application-prod.yml | 32768 | NO |
| INFERENCE_GPU_LAYERS | application-prod.yml | 99 | NO |
| INFERENCE_THREADS | application-prod.yml | 8 | NO |
| INFERENCE_TIMEOUT | application-prod.yml | 120 | NO |
| INFERENCE_MAX_TOKENS | application-prod.yml | 4096 | NO |
| INFERENCE_TEMPERATURE | application-prod.yml | 0.7 | NO |
| HEALTH_CHECK_INTERVAL | application-prod.yml | 30 | NO |
| RESTART_DELAY | application-prod.yml | 5 | NO |
| STARTUP_TIMEOUT | application-prod.yml | 120 | NO |


## 20. Service Dependency Map

```
This Service → Depends On
--------------------------
PostgreSQL:      localhost:5432 (required — primary data store + pgvector)
Ollama:          localhost:11434 (optional — alternative inference provider)
llama-server:    localhost:1234 (primary inference — managed child process)
MQTT Broker:     localhost:1883 (optional — push notifications, prod only)
Kiwix:           localhost:8888 (optional — ZIM file serving for offline Wikipedia)
Calibre:         localhost:8081 (optional — ebook format conversion)
Gutendex:        gutendex.com (optional — Project Gutenberg search, requires internet)
Anthropic API:   api.anthropic.com (optional — Claude frontier model, requires internet + API key)
OpenAI API:      api.openai.com (optional — GPT frontier model, requires internet + API key)
Grok API:        api.x.ai (optional — Grok frontier model, requires internet + API key)
Brave Search:    api.search.brave.com (optional — web search enrichment, requires internet + API key)
HuggingFace:     huggingface.co (optional — model discovery/download, requires internet)

Downstream Consumers: MyOffGridAI Flutter mobile app (all /api/** endpoints)
```


## 21. Known Technical Debt & Issues

**TODO/Placeholder/Stub Scan Results:**

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| USB update feature stubbed | UsbResetWatcherService.java:20,45 | Medium | Javadoc mentions "stubbed for MI-002" — update zip detection logs warning but no update logic implemented |

**No other TODO, FIXME, XXX, HACK, or placeholder patterns found in production source.**

**Additional observations:**
- No `@Version` (optimistic locking) on any entity
- No `createdBy` audit field on most entities (only createdAt/updatedAt)
- CORS allows all origins (`*`) — appropriate for local network appliance
- Dev profile has hardcoded JWT secret and AES key (prod uses env vars)
- SensorType.TEMPERATURE false-positive in TODO scan (enum value)


## 22. Security Vulnerability Scan (Snyk)

Scan Date: 2026-03-17
Snyk CLI Version: 1.1303.0

### Dependency Vulnerabilities (Open Source)
Critical: 0
High: 1
Medium: 0
Low: 1

| Severity | Package | Version | Vulnerability | Fix Available |
|----------|---------|---------|---------------|---------------|
| HIGH | org.apache.tomcat.embed:tomcat-embed-core | 10.1.50 | Incorrect Authorization | Upgrade to 9.0.114+ (managed by Spring Boot parent) |
| LOW | ch.qos.logback:logback-core | 1.5.22 | External Initialization of Trusted Variables | Upgrade to 1.5.25 |

### Code Vulnerabilities (SAST)
Snyk Code scan unavailable (exit code 2 — likely needs Snyk org authorization for SAST).

### IaC Findings
Snyk IaC scan unavailable (exit code 3).

**Note:** The HIGH tomcat-embed-core vulnerability is managed by the Spring Boot parent BOM (3.4.13). Upgrading Spring Boot parent to latest 3.4.x patch should resolve it.

