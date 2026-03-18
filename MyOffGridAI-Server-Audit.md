---

## 1. Project Identity

```
Project Name:        MyOffGridAI Server
Repository URL:      (local — com.myoffgridai:myoffgridai-server)
Primary Language:    Java 21 / Spring Boot 3.4.13
Build Tool:          Apache Maven (maven-compiler-plugin, spring-boot-maven-plugin)
Current Branch:      main
Latest Commit:       9f2d12661435204da790acb1d402d4ac06211709 Add centralized file-based logging for server — replace console appenders with RollingFileAppender
Audit Timestamp:     2026-03-18T18:19:17Z
```

---

## 2. Directory Structure

```
./Dockerfile
./pom.xml
./src/main/java/com/myoffgridai/
├── MyOffGridAiApplication.java                    ← Entry point
├── ai/
│   ├── controller/  (ChatController, ModelController)
│   ├── dto/         (17 DTOs: InferenceChunk, OllamaChatRequest, SendMessageRequest, etc.)
│   ├── judge/       (JudgeController, JudgeInferenceService, JudgeModelProcessService, JudgeProperties, JudgeResult)
│   ├── model/       (Conversation, Message, MessageRole)
│   ├── repository/  (ConversationRepository, MessageRepository)
│   ├── service/     (AgentService, ChatService, ContextWindowService, InferenceService, LlamaServerInferenceService,
│   │                 LlamaServerProcessService, ModelHealthCheckService, OllamaInferenceService, OllamaService,
│   │                 ProcessBuilderFactory, SystemPromptBuilder)
│   └── SourceTag.java
├── auth/
│   ├── controller/  (AuthController, UserController)
│   ├── dto/         (AuthResponse, ChangePasswordRequest, LoginRequest, RefreshRequest, RegisterRequest, UpdateUserRequest, UserDetailDto, UserSummaryDto)
│   ├── model/       (Role, User)
│   ├── repository/  (UserRepository)
│   └── service/     (AuthService, JwtService, UserService)
├── common/
│   ├── exception/   (GlobalExceptionHandler + 11 custom exceptions)
│   ├── response/    (ApiResponse)
│   └── util/        (AesAttributeConverter, AesEncryptionUtil, TokenCounter)
├── config/          (AppConstants, CaptivePortalRedirectFilter, InferenceProperties, JpaConfig, JwtAuthFilter,
│                     LlamaServerConfig, LlamaServerProperties, MdcFilter, OllamaConfig, ProcessConfig,
│                     RateLimitingFilter, RequestResponseLoggingFilter, SecurityConfig, VectorStoreConfig, VectorType)
├── enrichment/
│   ├── controller/  (EnrichmentController)
│   ├── dto/         (EnrichmentStatusDto, FetchResult, FetchUrlRequest, SearchEnrichmentResultDto, SearchRequest, SearchResultDto)
│   └── service/     (ClaudeApiService, WebFetchService, WebSearchService)
├── events/
│   ├── controller/  (ScheduledEventController)
│   ├── dto/         (CreateEventRequest, ScheduledEventDto, UpdateEventRequest)
│   ├── model/       (ActionType, EventType, ScheduledEvent, ThresholdOperator)
│   ├── repository/  (ScheduledEventRepository)
│   └── service/     (ScheduledEventService)
├── frontier/        (ClaudeFrontierClient, FrontierApiClient, FrontierApiRouter, FrontierProvider, GrokFrontierClient, OpenAiFrontierClient)
├── knowledge/
│   ├── controller/  (KnowledgeController)
│   ├── dto/         (CreateDocumentRequest, DocumentContentDto, ExtractionResult, KnowledgeDocumentDto, KnowledgeSearchRequest, KnowledgeSearchResultDto, PageContent, SemanticSearchResult, UpdateContentRequest, UpdateDisplayNameRequest)
│   ├── model/       (DocumentStatus, KnowledgeChunk, KnowledgeDocument)
│   ├── repository/  (KnowledgeChunkRepository, KnowledgeDocumentRepository)
│   ├── service/     (ChunkingService, FileStorageService, IngestionService, KnowledgeService, OcrService, SemanticSearchService, StorageHealthService)
│   └── util/        (DeltaJsonUtils)
├── library/
│   ├── config/      (LibraryProperties)
│   ├── controller/  (LibraryController)
│   ├── dto/         (EbookDto, GutenbergBookDto, GutenbergSearchResultDto, KiwixStatusDto, ZimFileDto)
│   ├── model/       (Ebook, EbookFormat, ZimFile)
│   ├── repository/  (EbookRepository, ZimFileRepository)
│   └── service/     (CalibreConversionService, EbookService, GutenbergService, ZimFileService)
├── mcp/
│   ├── config/      (McpAuthentication, McpAuthFilter, McpServerConfig)
│   ├── controller/  (McpDiscoveryController, McpTokenController)
│   ├── dto/         (CreateMcpTokenRequest, McpTokenCreateResult, McpTokenSummaryDto)
│   ├── model/       (McpApiToken)
│   ├── repository/  (McpApiTokenRepository)
│   └── service/     (McpTokenService, McpToolsService)
├── memory/
│   ├── controller/  (MemoryController)
│   ├── dto/         (MemoryDto, MemorySearchRequest, MemorySearchResultDto, RagContext, UpdateImportanceRequest, UpdateTagsRequest)
│   ├── model/       (Memory, MemoryImportance, VectorDocument, VectorSourceType)
│   ├── repository/  (MemoryRepository, VectorDocumentRepository)
│   └── service/     (EmbeddingService, MemoryExtractionService, MemoryService, RagService, SummarizationService)
├── models/
│   ├── controller/  (ModelDownloadController)
│   ├── dto/         (DownloadProgress, DownloadStartedDto, DownloadStatus, HfModelDto, HfModelFileDto, HfSearchResultDto, LocalModelFileDto, QuantizationType, StartDownloadRequest)
│   └── service/     (ModelCatalogService, ModelDownloadProgressRegistry, ModelDownloadService, QuantizationRecommendationService)
├── notification/
│   ├── config/      (MqttConfig)
│   ├── controller/  (DeviceRegistrationController)
│   ├── dto/         (DeviceRegistrationDto, NotificationPayload, RegisterDeviceRequest)
│   ├── model/       (DeviceRegistration)
│   ├── repository/  (DeviceRegistrationRepository)
│   └── service/     (DeviceRegistrationService, MqttPublisherService)
├── privacy/
│   ├── aspect/      (AuditAspect)
│   ├── controller/  (PrivacyController)
│   ├── dto/         (AuditLogDto, AuditSummary, DataInventory, ExportRequest, FortressStatus, SovereigntyReport, WipeResult)
│   ├── model/       (AuditLog, AuditOutcome)
│   ├── repository/  (AuditLogRepository)
│   └── service/     (AuditService, DataExportService, DataWipeService, FortressService, SovereigntyReportService)
├── proactive/
│   ├── controller/  (ProactiveController)
│   ├── dto/         (InsightDto, NotificationDto, PatternSummary)
│   ├── model/       (Insight, InsightCategory, Notification, NotificationSeverity, NotificationType)
│   ├── repository/  (InsightRepository, NotificationRepository)
│   └── service/     (InsightGeneratorService, InsightService, NightlyInsightJob, NotificationService, NotificationSseRegistry, PatternAnalysisService, SystemHealthMonitor)
├── sensors/
│   ├── controller/  (SensorController)
│   ├── dto/         (CreateSensorRequest, SensorDto, SensorReadingDto, SensorTestResult, TestSensorRequest, UpdateThresholdsRequest)
│   ├── model/       (DataFormat, Sensor, SensorReading, SensorType)
│   ├── repository/  (SensorReadingRepository, SensorRepository)
│   └── service/     (SensorPollingService, SensorService, SensorStartupService, SerialPortService, SseEmitterRegistry)
├── settings/
│   ├── controller/  (ExternalApiSettingsController)
│   ├── dto/         (ExternalApiSettingsDto, UpdateExternalApiSettingsRequest)
│   ├── model/       (ExternalApiSettings)
│   ├── repository/  (ExternalApiSettingsRepository)
│   └── service/     (ExternalApiSettingsService)
├── skills/
│   ├── builtin/     (DocumentSummarizerSkill, InventoryTrackerSkill, RecipeGeneratorSkill, ResourceCalculatorSkill, TaskPlannerSkill, WeatherQuerySkill)
│   ├── controller/  (SkillController)
│   ├── dto/         (CreateInventoryItemRequest, InventoryItemDto, SkillDto, SkillExecuteRequest, SkillExecutionDto, UpdateInventoryItemRequest)
│   ├── model/       (ExecutionStatus, InventoryCategory, InventoryItem, PlannedTask, Skill, SkillCategory, SkillExecution, TaskStatus)
│   ├── repository/  (InventoryItemRepository, PlannedTaskRepository, SkillExecutionRepository, SkillRepository)
│   └── service/     (BuiltInSkill, SkillExecutorService, SkillSeederService)
└── system/
    ├── controller/  (CaptivePortalController, SystemController)
    ├── dto/         (AiSettingsDto, FactoryResetRequest, InitializeRequest, StorageSettingsDto, SystemStatusDto, WifiConnectionStatus, WifiConnectRequest, WifiNetwork)
    ├── model/       (SystemConfig)
    ├── repository/  (SystemConfigRepository)
    └── service/     (ApModeService, ApModeStartupService, FactoryResetService, NetworkTransitionService, SystemConfigService, UsbResetWatcherService)
```

Single-module Maven project. Source under `src/main/java/com/myoffgridai/` organized into 16 feature modules (ai, auth, common, config, enrichment, events, frontier, knowledge, library, mcp, memory, models, notification, privacy, proactive, sensors, settings, skills, system). Each module follows controller/dto/model/repository/service layering.

---

## 3. Build & Dependency Manifest

### Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.13 (parent) | REST API, embedded Tomcat |
| spring-boot-starter-data-jpa | 3.4.13 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.4.13 | Authentication & authorization |
| spring-boot-starter-validation | 3.4.13 | Bean validation (@Valid) |
| spring-boot-starter-actuator | 3.4.13 | Health checks, metrics |
| spring-boot-starter-webflux | 3.4.13 | WebClient for reactive HTTP calls |
| spring-boot-starter-aop | 3.4.13 | AOP (audit aspect) |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 | MCP (Model Context Protocol) SSE server |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token creation & validation |
| postgresql | 42.7.7 | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector extension for vector search |
| pdfbox | 3.0.4 | PDF text extraction |
| poi / poi-ooxml / poi-scratchpad | 5.4.0 | Word/Excel document processing |
| tess4j | 5.13.0 | OCR (Tesseract wrapper) |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| bucket4j-core | 8.10.1 | Rate limiting |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | MQTT client for notifications |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger/OpenAPI UI |
| lombok | 1.18.42 | Boilerplate reduction |
| jsoup | 1.18.3 | HTML parsing for web content extraction |
| jackson-core | 2.18.6 (managed) | JSON processing |
| netty-codec-http/http2 | 4.1.129.Final (managed) | Netty HTTP codec |
| commons-lang3 | 3.18.0 (managed) | String/object utilities |

### Test Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-test | 3.4.13 | JUnit 5, Mockito, AssertJ |
| reactor-test | (managed) | WebFlux testing |
| spring-security-test | (managed) | Security test utilities |
| testcontainers-postgresql | 1.20.6 | Real PostgreSQL in integration tests |
| testcontainers-junit-jupiter | 1.20.6 | Testcontainers JUnit 5 support |

### Build Plugins

| Plugin | Version | Configuration |
|---|---|---|
| spring-boot-maven-plugin | 3.4.13 | Excludes Lombok from package |
| maven-compiler-plugin | (managed) | Java 21 source/target, Lombok annotation processor |
| jacoco-maven-plugin | 0.8.12 | 100% LINE + BRANCH coverage enforced; excludes dto/**, model/**, *Application.class |
| maven-surefire-plugin | (managed) | --add-opens for Java 21 module access; Docker env vars for Testcontainers |
| native-maven-plugin | 0.10.4 | GraalVM native image (profile: native) |

### Build Commands

```
Build:   mvn clean compile -DskipTests
Test:    mvn test
Run:     mvn spring-boot:run
Package: mvn clean package -DskipTests
Native:  mvn clean package -Pnative -DskipTests
```

---

## 4. Configuration & Infrastructure Summary

**`src/main/resources/application.yml`** — Default profile: dev. Server port 8080. Flyway disabled. Multipart max 2048MB. MCP server on `/mcp/sse`. Async timeout disabled (-1).

**Dev profile** (in application.yml) — PostgreSQL at `localhost:5432/myoffgridai`, user/pass `myoffgridai/myoffgridai`. HikariCP pool 20 max, 5 idle. Hibernate `ddl-auto: update`. Inference provider: ollama at `localhost:11434`. Fortress/AP mode mocked. Rate limiting enabled. MQTT disabled. Judge disabled. Library dirs: `./library/zim`, `./library/ebooks`.

**`src/main/resources/application-prod.yml`** — DB connection from env vars (`$DB_URL`, `$DB_USERNAME`, `$DB_PASSWORD`). Hibernate `ddl-auto: validate`. Flyway enabled with `classpath:db/migration`. JWT secret from `$JWT_SECRET`. Encryption key from `$ENCRYPTION_KEY`. Inference process managed. Fortress/AP mode real (not mocked). MQTT enabled.

**`src/main/resources/logback-spring.xml`** — Dev: RollingFileAppender (`logs/myoffgridai.log`), human-readable pattern, 50MB max file, 30-day retention, 1GB cap. Prod: same rolling policy, LogstashEncoder (JSON), MDC keys: requestId, username, userId. Test: separate file (`logs/myoffgridai-test.log`), JSON format.

**`Dockerfile`** — Multi-stage build. Stage 1: eclipse-temurin:21-jdk-alpine, Maven build. Stage 2: eclipse-temurin:21-jre-alpine, non-root user `myoffgridai`. Exposes 8080. Healthcheck via `wget http://localhost:8080/api/system/status`.

**Connection Map:**
```
Database:       PostgreSQL, localhost:5432, database: myoffgridai
Cache:          None
Message Broker: MQTT (Eclipse Paho), localhost:1883 (disabled in dev)
External APIs:  Ollama (localhost:11434), llama-server (localhost:1234), Kiwix (localhost:8888), Calibre (localhost:8081), Gutenberg API (gutendex.com), Claude API, OpenAI API, Grok API
Cloud Services: None (offline-first design)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `com.myoffgridai.MyOffGridAiApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`.

**Startup initialization (`@EventListener(ApplicationReadyEvent.class)`):**
- `VectorStoreConfig` — ensures pgvector extension and vector_documents table exist
- `SensorStartupService` — resumes polling for all enabled sensors
- `ApModeStartupService` — checks if system is initialized; if not, activates AP mode for onboarding
- `StorageHealthService` — validates knowledge storage directories exist
- `ModelHealthCheckService` — starts periodic health checks for the active inference model
- `SkillSeederService` — seeds built-in skills into the database if not present

**ApplicationRunner:**
- `LlamaServerProcessService` — starts llama-server process if `app.inference.manage-process=true`

**Scheduled tasks:**
- `LlamaServerProcessService` — health check at configurable interval (default 30s)
- `SummarizationService` — nightly memory summarization (cron: `0 0 2 * * *`)
- `NightlyInsightJob` — nightly proactive insight generation (cron: `0 0 3 * * *`)
- `SystemHealthMonitor` — periodic system health monitoring (default 300s)
- `UsbResetWatcherService` — USB factory reset file watcher (30s fixed delay)
- `SensorPollingService` — sensor data polling (per-sensor configurable interval)

**Health check endpoint:** `GET /api/system/status` (used by Docker HEALTHCHECK)

---

## 6. Entity / Data Model Layer

### Conversation (ai.model)
```
Table: conversations
PK: id UUID (GenerationType.UUID)
Fields:
  - user: User [@ManyToOne EAGER, @JoinColumn user_id, nullable=false]
  - title: String [nullable]
  - isArchived: boolean [default false, nullable=false]
  - messageCount: int [default 0, nullable=false]
Audit: createdAt (Instant, @CreatedDate), updatedAt (Instant, @LastModifiedDate)
Relationships: @ManyToOne → User (EAGER)
Custom: @PrePersist/@PreUpdate lifecycle callbacks
```

### Message (ai.model)
```
Table: messages
PK: id UUID (GenerationType.UUID)
Fields:
  - conversation: Conversation [@ManyToOne LAZY, @JoinColumn conversation_id, nullable=false]
  - role: MessageRole [EnumType.STRING, nullable=false]
  - content: String [TEXT, nullable=false]
  - tokenCount: Integer [nullable]
  - hasRagContext: boolean [default false, nullable=false]
  - thinkingContent: String [TEXT, nullable]
  - tokensPerSecond: Double [nullable]
  - inferenceTimeSeconds: Double [nullable]
  - stopReason: String [nullable]
  - thinkingTokenCount: Integer [nullable]
  - sourceTag: SourceTag [EnumType.STRING, length=20, default LOCAL, nullable=false]
  - judgeScore: Double [nullable]
  - judgeReason: String [TEXT, nullable]
Audit: createdAt (Instant, @CreatedDate)
Relationships: @ManyToOne → Conversation (LAZY)
```

### User (auth.model)
```
Table: users
PK: id UUID (GenerationType.UUID)
Fields:
  - username: String [nullable=false, unique]
  - email: String [unique, nullable]
  - displayName: String [nullable=false]
  - passwordHash: String [nullable=false]
  - role: Role [EnumType.STRING, nullable=false]
  - isActive: boolean [default true, nullable=false]
  - lastLoginAt: Instant [nullable]
Audit: createdAt (Instant, @CreatedDate), updatedAt (Instant, @LastModifiedDate)
Implements: UserDetails (Spring Security)
Custom: getAuthorities() returns role as SimpleGrantedAuthority; isAccountNonLocked() checks isActive
```

### ScheduledEvent (events.model)
```
Table: scheduled_events
PK: id UUID
Indexes: idx_event_user_id (user_id), idx_event_enabled_type (is_enabled, event_type)
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - description: String [TEXT, nullable]
  - eventType: EventType [EnumType.STRING, nullable=false]
  - isEnabled: boolean [default true, nullable=false]
  - cronExpression: String [nullable]
  - recurringIntervalMinutes: Integer [nullable]
  - sensorId: UUID [nullable]
  - thresholdOperator: ThresholdOperator [EnumType.STRING, nullable]
  - thresholdValue: Double [nullable]
  - actionType: ActionType [EnumType.STRING, nullable=false]
  - actionPayload: String [TEXT, nullable=false]
  - lastTriggeredAt: Instant [nullable]
  - nextFireAt: Instant [nullable]
Audit: createdAt, updatedAt
```

### KnowledgeDocument (knowledge.model)
```
Table: knowledge_documents
PK: id UUID
Indexes: idx_knowledge_doc_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - filename: String [nullable=false]
  - displayName: String [nullable]
  - mimeType: String [nullable=false]
  - storagePath: String [nullable=false]
  - fileSizeBytes: long
  - status: DocumentStatus [EnumType.STRING, default PENDING, nullable=false]
  - errorMessage: String [TEXT, nullable]
  - chunkCount: int
  - content: String [TEXT, nullable]
  - processedAt: Instant [nullable]
Audit: uploadedAt (Instant, @CreatedDate)
```

### KnowledgeChunk (knowledge.model)
```
Table: knowledge_chunks
PK: id UUID
Indexes: idx_knowledge_chunk_doc_id (document_id), idx_knowledge_chunk_user_id (user_id)
Fields:
  - document: KnowledgeDocument [@ManyToOne LAZY, @JoinColumn document_id, nullable=false]
  - userId: UUID [nullable=false]
  - chunkIndex: int [nullable=false]
  - content: String [TEXT, nullable=false]
  - pageNumber: Integer [nullable]
Audit: createdAt (Instant, @CreatedDate)
```

### Ebook (library.model)
```
Table: ebooks
PK: id UUID
Indexes: idx_ebooks_gutenberg_id (gutenberg_id)
Fields:
  - title: String [nullable=false]
  - author: String [nullable]
  - description: String [TEXT, nullable]
  - isbn: String [nullable]
  - publisher: String [nullable]
  - publishedYear: Integer [nullable]
  - language: String [nullable]
  - format: EbookFormat [EnumType.STRING, nullable=false]
  - fileSizeBytes: long
  - filePath: String [nullable=false]
  - coverImagePath: String [nullable]
  - gutenbergId: String [nullable]
  - downloadCount: int
  - uploadedBy: UUID [nullable]
Audit: uploadedAt (Instant, @CreatedDate)
```

### ZimFile (library.model)
```
Table: zim_files
PK: id UUID
Fields:
  - filename: String [nullable=false, unique]
  - displayName: String [nullable=false]
  - description: String [length=1000, nullable]
  - language: String [nullable]
  - category: String [nullable]
  - fileSizeBytes: long
  - articleCount: int
  - mediaCount: int
  - createdDate: String [nullable]
  - filePath: String [nullable=false]
  - kiwixBookId: String [nullable]
  - uploadedBy: UUID [nullable]
Audit: uploadedAt (Instant, @CreatedDate)
```

### McpApiToken (mcp.model)
```
Table: mcp_api_tokens
PK: id UUID
Indexes: idx_mcp_token_created_by (created_by)
Fields:
  - tokenHash: String [length=500, nullable=false]
  - name: String [nullable=false]
  - createdBy: UUID [nullable=false]
  - lastUsedAt: Instant [nullable]
  - isActive: boolean [default true, nullable=false]
Audit: createdAt (Instant, @CreatedDate)
Note: Token stored as BCrypt hash; plaintext shown once at creation
```

### Memory (memory.model)
```
Table: memories
PK: id UUID
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - importance: MemoryImportance [EnumType.STRING, nullable=false]
  - tags: String [nullable]
  - sourceConversationId: UUID [nullable]
  - lastAccessedAt: Instant [nullable]
  - accessCount: int [default 0, nullable=false]
Audit: createdAt, updatedAt
```

### VectorDocument (memory.model)
```
Table: vector_document
PK: id UUID
Indexes: idx_vector_doc_user_source_type (user_id, source_type)
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - embedding: float[] [vector(768), custom VectorType Hibernate type]
  - sourceType: VectorSourceType [EnumType.STRING, nullable=false]
  - sourceId: UUID [nullable]
  - metadata: String [TEXT, nullable]
Audit: createdAt (Instant, @CreatedDate)
```

### DeviceRegistration (notification.model)
```
Table: device_registrations
PK: id UUID
UniqueConstraint: uk_device_registration_user_device (user_id, device_id)
Indexes: idx_device_registration_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - deviceId: String [nullable=false]
  - deviceName: String [nullable]
  - platform: String [nullable=false]
  - mqttClientId: String [nullable]
  - lastSeenAt: Instant [nullable]
Audit: createdAt, updatedAt
```

### AuditLog (privacy.model)
```
Table: audit_logs
PK: id UUID
Indexes: idx_audit_user_timestamp (user_id, timestamp DESC), idx_audit_timestamp (timestamp DESC)
Fields:
  - userId: UUID [nullable]
  - username: String [nullable]
  - action: String [nullable=false]
  - resourceType: String [nullable]
  - resourceId: String [nullable]
  - httpMethod: String [nullable=false]
  - requestPath: String [nullable=false]
  - ipAddress: String [nullable]
  - userAgent: String [nullable]
  - responseStatus: int
  - outcome: AuditOutcome [EnumType.STRING, nullable=false]
  - durationMs: long
  - timestamp: Instant [nullable=false]
Audit: None (uses timestamp field directly)
```

### Insight (proactive.model)
```
Table: insights
PK: id UUID
Indexes: idx_insight_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - category: InsightCategory [EnumType.STRING, nullable=false]
  - isRead: boolean [default false, nullable=false]
  - isDismissed: boolean [default false, nullable=false]
  - readAt: Instant [nullable]
Audit: generatedAt (Instant, @PrePersist)
```

### Notification (proactive.model)
```
Table: notifications
PK: id UUID
Indexes: idx_notification_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - title: String [nullable=false]
  - body: String [TEXT, nullable=false]
  - type: NotificationType [EnumType.STRING, nullable=false]
  - isRead: boolean [default false, nullable=false]
  - readAt: Instant [nullable]
  - severity: NotificationSeverity [EnumType.STRING, length=20, nullable]
  - mqttDelivered: boolean [default false, nullable=false]
  - metadata: String [TEXT, nullable]
Audit: createdAt (Instant, @PrePersist)
```

### Sensor (sensors.model)
```
Table: sensors
PK: id UUID
Indexes: idx_sensor_user_id (user_id), idx_sensor_port_path (port_path, unique)
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - type: SensorType [EnumType.STRING, nullable=false]
  - portPath: String [nullable=false]
  - baudRate: int [default 9600, nullable=false]
  - dataFormat: DataFormat [EnumType.STRING, default CSV_LINE, nullable=false]
  - valueField: String [nullable]
  - unit: String [nullable]
  - isActive: boolean [default false, nullable=false]
  - pollIntervalSeconds: int [default 30, nullable=false]
  - lowThreshold: Double [nullable]
  - highThreshold: Double [nullable]
Audit: createdAt, updatedAt
```

### SensorReading (sensors.model)
```
Table: sensor_readings
PK: id UUID
Indexes: idx_sensor_reading_sensor_recorded (sensor_id, recorded_at DESC)
Fields:
  - sensor: Sensor [@ManyToOne LAZY, @JoinColumn sensor_id, nullable=false]
  - value: double [nullable=false]
  - rawData: String [nullable]
  - recordedAt: Instant [nullable=false]
Audit: None
```

### ExternalApiSettings (settings.model)
```
Table: external_api_settings
PK: id UUID
Fields:
  - singletonGuard: String [unique, default "SINGLETON"]
  - anthropicApiKey: String [AES encrypted]
  - anthropicModel: String [default "claude-sonnet-4-20250514"]
  - anthropicEnabled: boolean [default false]
  - braveApiKey: String [AES encrypted]
  - braveEnabled: boolean [default false]
  - maxWebFetchSizeKb: int [default 512]
  - searchResultLimit: int [default 5]
  - huggingFaceToken: String [AES encrypted]
  - huggingFaceEnabled: boolean [default false]
  - grokApiKey: String [AES encrypted, length=1000]
  - grokEnabled: boolean [default false]
  - openAiApiKey: String [AES encrypted, length=1000]
  - openAiEnabled: boolean [default false]
  - preferredFrontierProvider: FrontierProvider [EnumType.STRING, default CLAUDE]
  - judgeEnabled: boolean [default false]
  - judgeModelFilename: String [length=500, nullable]
  - judgeScoreThreshold: double [default 7.5]
Audit: createdAt, updatedAt
Note: Singleton table pattern; API keys encrypted at rest via AesAttributeConverter (AES-256-GCM)
```

### SystemConfig (system.model)
```
Table: system_config
PK: id UUID
Fields:
  - initialized: boolean [default false, nullable=false]
  - instanceName: String [nullable]
  - fortressEnabled: boolean [default false, nullable=false]
  - fortressEnabledAt: Instant [nullable]
  - fortressEnabledByUserId: UUID [nullable]
  - apModeEnabled: boolean [default false, nullable=false]
  - wifiConfigured: boolean [default false, nullable=false]
  - aiModel: String [default "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M"]
  - aiTemperature: Double [default 0.7]
  - aiSimilarityThreshold: Double [default 0.45]
  - aiMemoryTopK: Integer [default 5]
  - aiRagMaxContextTokens: Integer [default 2048]
  - aiContextSize: Integer [default 4096]
  - aiContextMessageLimit: Integer [default 20]
  - activeModelFilename: String [nullable]
  - knowledgeStoragePath: String [default "/var/myoffgridai/knowledge"]
  - maxUploadSizeMb: Integer [default 25]
Audit: createdAt, updatedAt
```

### InventoryItem (skills.model)
```
Table: inventory_items
PK: id UUID
Indexes: idx_inventory_user_id (user_id), idx_inventory_category (user_id, category)
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - category: InventoryCategory [EnumType.STRING, nullable=false]
  - quantity: double [nullable=false]
  - unit: String [nullable]
  - notes: String [TEXT, nullable]
  - lowStockThreshold: Double [nullable]
Audit: createdAt, updatedAt
```

### Skill (skills.model)
```
Table: skills
PK: id UUID
Indexes: idx_skill_name (name, unique), idx_skill_category (category)
Fields:
  - name: String [nullable=false, unique]
  - displayName: String [nullable=false]
  - description: String [TEXT, nullable=false]
  - version: String [nullable=false]
  - author: String [nullable=false]
  - category: SkillCategory [EnumType.STRING, nullable=false]
  - isEnabled: boolean [default true, nullable=false]
  - isBuiltIn: boolean [default false, nullable=false]
  - parametersSchema: String [TEXT, nullable]
Audit: createdAt, updatedAt
```

### SkillExecution (skills.model)
```
Table: skill_executions
PK: id UUID
Indexes: idx_skill_exec_user_id (user_id), idx_skill_exec_skill_id (skill_id)
Fields:
  - skill: Skill [@ManyToOne LAZY, @JoinColumn skill_id, nullable=false]
  - userId: UUID [nullable=false]
  - status: ExecutionStatus [EnumType.STRING, nullable=false]
  - inputParams: String [TEXT, nullable]
  - outputResult: String [TEXT, nullable]
  - errorMessage: String [TEXT, nullable]
  - completedAt: Instant [nullable]
  - durationMs: Long [nullable]
Audit: startedAt (Instant, @CreatedDate)
```

### PlannedTask (skills.model)
```
Table: planned_tasks
PK: id UUID
Indexes: idx_planned_task_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - goalDescription: String [TEXT, nullable=false]
  - title: String [nullable=false]
  - steps: String [TEXT, nullable=false]
  - estimatedResources: String [TEXT, nullable]
  - status: TaskStatus [EnumType.STRING, default ACTIVE, nullable=false]
Audit: createdAt, updatedAt
```

---

## 7. Enum Inventory

**Total enums: 24** | 1 with display labels (`QuantizationType`), 23 without.

| # | Enum | Package | Values | Used In |
|---|------|---------|--------|---------|
| 1 | `MessageRole` | `ai.model` | `USER`, `ASSISTANT`, `SYSTEM` | Message, MessageDto, ChatService |
| 2 | `SourceTag` | `ai` | `LOCAL`, `ENHANCED` | Message, MessageDto, ChatService, ChatController |
| 3 | `ChunkType` | `ai.dto` | `THINKING`, `CONTENT`, `DONE`, `JUDGE_EVALUATING`, `JUDGE_RESULT`, `ENHANCED_CONTENT`, `ENHANCED_DONE` | InferenceChunk, InferenceMetadata, InferenceService, OllamaInferenceService, LlamaServerInferenceService, ChatService |
| 4 | `Role` | `auth.model` | `ROLE_OWNER`, `ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER`, `ROLE_CHILD` | User, UserDetailDto, UserSummaryDto, RegisterRequest, UpdateUserRequest, UserRepository, UserService, AuthService, SystemController, SystemHealthMonitor |
| 5 | `ActionType` | `events.model` | `PUSH_NOTIFICATION`, `AI_PROMPT`, `AI_SUMMARY` | ScheduledEvent, ScheduledEventDto, CreateEventRequest, UpdateEventRequest, ScheduledEventService |
| 6 | `EventType` | `events.model` | `SCHEDULED`, `SENSOR_THRESHOLD`, `RECURRING` | ScheduledEvent, ScheduledEventDto, CreateEventRequest, UpdateEventRequest, ScheduledEventRepository, ScheduledEventService |
| 7 | `ThresholdOperator` | `events.model` | `ABOVE`, `BELOW`, `EQUALS` | ScheduledEvent, ScheduledEventDto, CreateEventRequest, UpdateEventRequest, ScheduledEventService |
| 8 | `DocumentStatus` | `knowledge.model` | `PENDING`, `PROCESSING`, `READY`, `FAILED` | KnowledgeDocument, KnowledgeDocumentDto, KnowledgeDocumentRepository, KnowledgeService, DocumentSummarizerSkill |
| 9 | `EbookFormat` | `library.model` | `EPUB`, `PDF`, `MOBI`, `AZW`, `TXT`, `HTML` | Ebook, EbookDto, EbookRepository, EbookService, GutenbergService, CalibreConversionService, LibraryController |
| 10 | `MemoryImportance` | `memory.model` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | Memory, MemoryDto, UpdateImportanceRequest, MemoryRepository, MemoryService, MemoryExtractionService, SummarizationService, MemoryController, SensorPollingService, PatternAnalysisService |
| 11 | `VectorSourceType` | `memory.model` | `MEMORY`, `CONVERSATION`, `KNOWLEDGE_CHUNK` | VectorDocument, VectorDocumentRepository, KnowledgeService, MemoryService, SemanticSearchService |
| 12 | `DownloadStatus` | `models.dto` | `QUEUED`, `DOWNLOADING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED` | DownloadProgress, ModelDownloadService |
| 13 | `QuantizationType` | `models.dto` | `IQ1_S`, `IQ2_XS`, `Q2_K`, `Q3_K_XS`, `Q3_K_S`, `Q3_K_M`, `Q4_K_S`, `Q4_K_M`, `Q5_K_S`, `Q5_K_M`, `Q6_K`, `Q8_0`, `F16`, `BF16`, `F32` | HfModelFileDto, QuantizationRecommendationService |
| 14 | `FrontierProvider` | `frontier` | `CLAUDE`, `GROK`, `OPENAI` | ExternalApiSettings, ExternalApiSettingsDto, UpdateExternalApiSettingsRequest, ExternalApiSettingsService, FrontierApiRouter, FrontierApiClient, ClaudeFrontierClient, GrokFrontierClient, OpenAiFrontierClient |
| 15 | `AuditOutcome` | `privacy.model` | `SUCCESS`, `FAILURE`, `DENIED` | AuditLog, AuditLogDto, AuditLogRepository, AuditService, SovereigntyReportService, AuditAspect, PrivacyController |
| 16 | `InsightCategory` | `proactive.model` | `HOMESTEAD`, `HEALTH`, `RESOURCE`, `GENERAL` | Insight, InsightDto, InsightRepository, InsightService, InsightGeneratorService, ProactiveController |
| 17 | `NotificationSeverity` | `proactive.model` | `INFO`, `WARNING`, `CRITICAL` | Notification, NotificationDto, InsightGeneratorService, SystemHealthMonitor, NotificationService |
| 18 | `NotificationType` | `proactive.model` | `SENSOR_ALERT`, `SYSTEM_HEALTH`, `INSIGHT_READY`, `MODEL_UPDATE`, `GENERAL` | Notification, NotificationDto, NotificationPayload, InsightGeneratorService, SystemHealthMonitor, NotificationService, SensorPollingService |
| 19 | `DataFormat` | `sensors.model` | `CSV_LINE`, `JSON_LINE` | Sensor, SensorDto, CreateSensorRequest, SensorService, SensorPollingService, IngestionService |
| 20 | `SensorType` | `sensors.model` | `TEMPERATURE`, `HUMIDITY`, `SOIL_MOISTURE`, `POWER`, `VOLTAGE`, `CUSTOM` | Sensor, SensorDto, CreateSensorRequest |
| 21 | `ExecutionStatus` | `skills.model` | `RUNNING`, `COMPLETED`, `FAILED` | SkillExecution, SkillExecutionDto, SkillExecutionRepository, SkillExecutorService |
| 22 | `InventoryCategory` | `skills.model` | `FOOD`, `TOOLS`, `MEDICAL`, `SUPPLIES`, `SEEDS`, `EQUIPMENT`, `OTHER` | InventoryItem, InventoryItemDto, CreateInventoryItemRequest, InventoryItemRepository, InventoryTrackerSkill, RecipeGeneratorSkill, ResourceCalculatorSkill, SkillController, McpToolsService |
| 23 | `SkillCategory` | `skills.model` | `HOMESTEAD`, `RESOURCE`, `PLANNING`, `KNOWLEDGE`, `WEATHER`, `CUSTOM` | Skill, SkillDto, SkillRepository, SkillSeederService |
| 24 | `TaskStatus` | `skills.model` | `ACTIVE`, `COMPLETED`, `CANCELLED` | PlannedTask, PlannedTaskRepository, TaskPlannerSkill, PatternAnalysisService |

**Note:** `QuantizationType` is the only enum with display labels (`label` + `rank` fields). `config/VectorType.java` is NOT an enum — it is a Hibernate `UserType<float[]>` for pgvector column mapping.

---

## 8. Repository Layer

**Total repositories: 23** | All extend `JpaRepository<Entity, UUID>`.

### 8.1 AI Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 1 | `ConversationRepository` | `Conversation` | `findByUserIdOrderByUpdatedAtDesc()`, `findByUserIdAndIsArchivedOrderByUpdatedAtDesc()`, `findByIdAndUserId()`, `countByUserId()`, `findByUserId()`, `findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc()`, `@Modifying @Query deleteByUserId()` |
| 2 | `MessageRepository` | `Message` | `findByConversationIdOrderByCreatedAtAsc()` (List + Page), `findTopNByConversationIdOrderByCreatedAtDesc()`, `countByConversationId()`, `deleteByConversationId()`, `@Query countByUserId()`, `@Modifying @Query deleteByUserId()`, `@Modifying @Query deleteMessagesAfter()` |

### 8.2 Auth Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 3 | `UserRepository` | `User` | `findByUsername()`, `findByEmail()`, `existsByUsername()`, `existsByEmail()`, `findAllByRole()`, `countByIsActiveTrue()`, `findByIsActiveTrue()` |

### 8.3 Events Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 4 | `ScheduledEventRepository` | `ScheduledEvent` | `findAllByUserId()`, `findByIdAndUserId()`, `findByIsEnabledTrueAndEventType()`, `findAllByUserIdOrderByCreatedAtDesc()`, `deleteByUserId()`, `countByUserId()` |

### 8.4 Knowledge Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 5 | `KnowledgeChunkRepository` | `KnowledgeChunk` | `findByDocumentIdOrderByChunkIndexAsc()`, `@Modifying deleteByDocumentId()`, `@Modifying deleteByUserId()`, `countByDocumentId()` |
| 6 | `KnowledgeDocumentRepository` | `KnowledgeDocument` | `findByUserIdOrderByUploadedAtDesc()`, `findByIdAndUserId()`, `findByUserIdAndStatus()`, `@Modifying deleteByUserId()`, `countByUserId()` |

### 8.5 Library Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 7 | `EbookRepository` | `Ebook` | `@Query searchByTitleOrAuthor()`, `findByGutenbergId()`, `existsByGutenbergId()` |
| 8 | `ZimFileRepository` | `ZimFile` | `findByFilename()`, `findAllByOrderByDisplayNameAsc()`, `existsByFilename()` |

### 8.6 MCP Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 9 | `McpApiTokenRepository` | `McpApiToken` | `findByIsActiveTrue()`, `findByCreatedByOrderByCreatedAtDesc()` |

### 8.7 Memory Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 10 | `MemoryRepository` | `Memory` | `findByUserIdOrderByCreatedAtDesc()`, `findByUserIdAndImportance()`, `findByUserIdAndTagsContaining()`, `findByUserId()`, `@Modifying deleteByUserId()`, `countByUserId()` |
| 11 | `VectorDocumentRepository` | `VectorDocument` | `findByUserIdAndSourceType()`, `@Modifying deleteBySourceIdAndSourceType()`, `@Modifying deleteByUserId()`, `@Query(nativeQuery) findMostSimilar()`, `@Query(nativeQuery) findMostSimilarAcrossTypes()` |

**Note:** `VectorDocumentRepository` uses pgvector cosine distance operator (`<=>`) in native queries.

### 8.8 Notification Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 12 | `DeviceRegistrationRepository` | `DeviceRegistration` | `findByUserIdAndDeviceId()`, `findByUserId()`, `deleteByUserIdAndDeviceId()` |

### 8.9 Privacy Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 13 | `AuditLogRepository` | `AuditLog` | `findAllByOrderByTimestampDesc()`, `findByUserIdOrderByTimestampDesc()`, `findByOutcomeOrderByTimestampDesc()`, `findByTimestampBetweenOrderByTimestampDesc()`, `findByUserIdAndTimestampBetween()`, `countByOutcomeAndTimestampBetween()`, `@Modifying deleteByTimestampBefore()`, `@Modifying deleteByUserId()` |

### 8.10 Proactive Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 14 | `InsightRepository` | `Insight` | `findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc()`, `findByUserIdAndCategoryAndIsDismissedFalse()`, `findByUserIdAndIsReadFalseAndIsDismissedFalse()`, `countByUserIdAndIsReadFalseAndIsDismissedFalse()`, `findByIdAndUserId()`, `countByUserId()`, `@Modifying deleteByUserId()` |
| 15 | `NotificationRepository` | `Notification` | `findByUserIdAndIsReadFalseOrderByCreatedAtDesc()`, `findByUserIdOrderByCreatedAtDesc()`, `countByUserIdAndIsReadFalse()`, `findByIdAndUserId()`, `@Modifying @Query markAllReadForUser()`, `@Modifying deleteByUserId()` |

### 8.11 Sensors Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 16 | `SensorReadingRepository` | `SensorReading` | `findBySensorIdOrderByRecordedAtDesc()`, `findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc()`, `findTopBySensorIdOrderByRecordedAtDesc()`, `@Modifying deleteBySensorId()`, `@Modifying @Query deleteByUserId()`, `@Query(nativeQuery) findAverageValueSince()` |
| 17 | `SensorRepository` | `Sensor` | `findByUserIdOrderByNameAsc()`, `findByIdAndUserId()`, `findByUserIdAndIsActiveTrue()`, `findByPortPath()`, `findByIsActiveTrue()`, `countByUserId()`, `deleteByUserId()` |

### 8.12 Settings Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 18 | `ExternalApiSettingsRepository` | `ExternalApiSettings` | `findBySingletonGuard()` |

### 8.13 Skills Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 19 | `InventoryItemRepository` | `InventoryItem` | `findByUserIdOrderByNameAsc()`, `findByUserIdAndCategory()`, `findByUserIdAndQuantityLessThanEqual()`, `findByIdAndUserId()`, `@Modifying deleteByUserId()` |
| 20 | `PlannedTaskRepository` | `PlannedTask` | `findByUserIdAndStatusOrderByCreatedAtDesc()`, `findByIdAndUserId()`, `@Modifying deleteByUserId()` |
| 21 | `SkillExecutionRepository` | `SkillExecution` | `findByUserIdOrderByStartedAtDesc()`, `findBySkillIdAndUserIdOrderByStartedAtDesc()`, `findByUserIdAndStatus()` |
| 22 | `SkillRepository` | `Skill` | `findByIsEnabledTrue()`, `findByIsBuiltInTrue()`, `findByCategory()`, `findByName()`, `findByIsEnabledTrueOrderByDisplayNameAsc()` |

### 8.14 System Module

| # | Repository | Entity | Custom Methods |
|---|-----------|--------|----------------|
| 23 | `SystemConfigRepository` | `SystemConfig` | `@Query findFirst()` |

### 8.15 Repository Layer Summary

| Metric | Value |
|--------|-------|
| Total Repositories | 23 |
| All extend | `JpaRepository<Entity, UUID>` |
| With `@EntityGraph` | 0 |
| With Projections | 0 |
| With `@Query` (JPQL) | 5 |
| With `@Query(nativeQuery=true)` | 2 (VectorDocumentRepository, SensorReadingRepository) |
| With `@Modifying` | 13 |
| Total custom methods | 103 |

### 8.16 Cross-Cutting Patterns

- **User-scoped ownership**: 14 repositories implement `findByIdAndUserId()` for tenant isolation
- **Bulk user-data deletion**: 15 repositories provide `deleteByUserId()` for GDPR-compliant erasure
- **Pagination**: 13 repositories expose `Page<T>` return types
- **No `@EntityGraph`**: All lazy-loaded associations rely on default fetch strategies
- **No projection interfaces**: All queries return full entity objects

---

## 9. Service Layer

**Total service classes: 82** (including interfaces, aspects, and components)

### 9.1 AI Module (14 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 1 | `InferenceService` | Interface | `chat()`, `streamChat()`, `streamChatWithThinking()`, `embed()`, `isAvailable()`, `listModels()`, `getActiveModel()` | -- |
| 2 | `OllamaInferenceService` | `@Service` `@ConditionalOnProperty(provider=ollama)` | Implements `InferenceService` with native thinking field + think-tag state machine fallback | `OllamaService`, `SystemConfigService` |
| 3 | `LlamaServerInferenceService` | `@Service` `@Primary` `@ConditionalOnProperty(provider=llama-server)` | Implements `InferenceService` with OpenAI-compatible /v1/chat/completions | `LlamaServerProperties`, `RestClient`, `WebClient` |
| 4 | `OllamaService` | `@Service` | `isAvailable()`, `listModels()`, `chat()`, `chatStream()`, `embed()`, `embedBatch()` | `RestClient`, `WebClient`, `ObjectMapper` |
| 5 | `ChatService` | `@Service` | `createConversation()`, `sendMessage()`, `streamMessage()`, `editMessage()`, `deleteMessage()`, `branchConversation()`, `regenerateMessage()`, `searchConversations()`, `renameConversation()` | `ConversationRepository`, `MessageRepository`, `InferenceService`, `RagService`, `MemoryExtractionService`, `JudgeInferenceService`, `FrontierApiRouter` |
| 6 | `ContextWindowService` | `@Service` | `prepareMessages()` — builds context-windowed message list from history | `MessageRepository`, `SystemConfigService` |
| 7 | `SystemPromptBuilder` | `@Service` | `build(User, message)`, `build(User, message, ragContext)` | `RagService` |
| 8 | `AgentService` | `@Service` | `executeTask()` — tool-calling agent loop | `OllamaService`, `SkillExecutorService` |
| 9 | `LlamaServerProcessService` | `@Service` `ApplicationRunner` `DisposableBean` | `start()`, `stop()`, `restart()`, `switchModel()`, `getStatus()`, `monitorHealth()` | `InferenceProperties`, `LlamaServerProperties`, `SystemConfigService` |
| 10 | `ModelHealthCheckService` | `@Component` | `checkInferenceProviderOnStartup()` — `@EventListener` | `InferenceService`, `SystemConfigService` |
| 11 | `ProcessBuilderFactory` | `@FunctionalInterface` | `create(List<String>)` | -- |
| 12 | `JudgeInferenceService` | `@Service` | `isAvailable()`, `evaluate()` — scores response quality | `JudgeModelProcessService`, `WebClient` |
| 13 | `JudgeModelProcessService` | `@Service` `DisposableBean` | `start()`, `stop()`, `isRunning()`, `getPort()` | `JudgeProperties`, `ProcessBuilderFactory` |

### 9.2 Auth Module (3 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 14 | `AuthService` | `@Service` | `register()`, `login()`, `refresh()`, `logout()`, `changePassword()` | `UserRepository`, `JwtService`, `PasswordEncoder` |
| 15 | `JwtService` | `@Service` | `generateAccessToken()`, `generateRefreshToken()`, `extractUsername()`, `isTokenValid()`, `isTokenExpired()` | `@Value` config |
| 16 | `UserService` | `@Service` | `listUsers()`, `getUserById()`, `updateUser()`, `deactivateUser()`, `deleteUser()` | `UserRepository` |

### 9.3 Enrichment Module (3 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 17 | `ClaudeApiService` | `@Service` | `isAvailable()`, `complete()`, `summarizeForKnowledgeBase()` | `ExternalApiSettingsService`, `WebClient` |
| 18 | `WebFetchService` | `@Service` | `fetchUrl()`, `fetchAndStore()` | `WebClient`, `ClaudeApiService`, `KnowledgeService` |
| 19 | `WebSearchService` | `@Service` | `isAvailable()`, `search()`, `searchAndStore()` — via Brave Search API | `WebClient`, `ExternalApiSettingsService`, `WebFetchService` |

### 9.4 Events Module (1 service)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 20 | `ScheduledEventService` | `@Service` | `listEvents()`, `createEvent()`, `updateEvent()`, `deleteEvent()`, `toggleEvent()`, `deleteAllForUser()` | `ScheduledEventRepository` |

### 9.5 Frontier Module (5 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 21 | `FrontierApiClient` | Interface | `getProvider()`, `isAvailable()`, `complete()` | -- |
| 22 | `ClaudeFrontierClient` | `@Component` | Implements `FrontierApiClient` for Anthropic | `ExternalApiSettingsService`, `WebClient` |
| 23 | `GrokFrontierClient` | `@Component` | Implements `FrontierApiClient` for xAI Grok | `ExternalApiSettingsService`, `WebClient` |
| 24 | `OpenAiFrontierClient` | `@Component` | Implements `FrontierApiClient` for OpenAI | `ExternalApiSettingsService`, `WebClient` |
| 25 | `FrontierApiRouter` | `@Service` | `complete()` with fallback routing, `isAnyAvailable()`, `getAvailableProviders()` | `List<FrontierApiClient>`, `ExternalApiSettingsService` |

### 9.6 Knowledge Module (6 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 26 | `KnowledgeService` | `@Service` | `upload()`, `processDocumentAsync()`, `listDocuments()`, `deleteDocument()`, `retryProcessing()`, `createFromEditor()`, `updateContent()`, `deleteAllForUser()` | `KnowledgeDocumentRepository`, `KnowledgeChunkRepository`, `FileStorageService`, `IngestionService`, `ChunkingService`, `EmbeddingService` |
| 27 | `FileStorageService` | `@Service` | `store()`, `storeBytes()`, `delete()`, `deleteAllForUser()`, `getInputStream()` | `SystemConfigService` |
| 28 | `IngestionService` | `@Service` | `extractPdf()`, `extractText()`, `extractDocx()`, `extractDoc()`, `extractRtf()`, `extractXlsx()`, `extractXls()`, `extractPptx()`, `extractPpt()` | PDFBox, Apache POI |
| 29 | `OcrService` | `@Service` | `extractFromImage()` — Tesseract OCR | `Tesseract` |
| 30 | `ChunkingService` | `@Service` | `chunkText()` — overlapping semantic chunks | -- (stateless) |
| 31 | `SemanticSearchService` | `@Service` | `search()`, `searchForRagContext()` | `VectorDocumentRepository`, `EmbeddingService`, `KnowledgeChunkRepository` |
| 32 | `StorageHealthService` | `@Component` | `checkStorageDirectory()` — `@EventListener` startup check | `SystemConfigService` |

### 9.7 Library Module (4 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 33 | `EbookService` | `@Service` | `upload()`, `list()`, `get()`, `delete()`, `getForDownload()` | `EbookRepository`, `CalibreConversionService` |
| 34 | `GutenbergService` | `@Service` | `search()`, `getBookMetadata()`, `importBook()` — Project Gutenberg API | `WebClient`, `EbookRepository` |
| 35 | `ZimFileService` | `@Service` | `upload()`, `listAll()`, `delete()`, `getKiwixServeUrl()`, `getKiwixStatus()` | `ZimFileRepository`, `WebClient` |
| 36 | `CalibreConversionService` | `@Service` | `convertToEpub()`, `isAvailable()` — via Docker/ebook-convert | -- |

### 9.8 MCP Module (2 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 37 | `McpTokenService` | `@Service` | `createToken()`, `validateToken()`, `updateLastUsed()`, `listTokens()`, `revokeToken()` | `McpApiTokenRepository`, `PasswordEncoder` |
| 38 | `McpToolsService` | `@Service` | 12 `@Tool` methods for MCP: `searchKnowledge()`, `searchMemories()`, `listInventory()`, `addInventoryItem()`, `listSensors()`, `getLatestSensorReading()`, `listConversations()`, `getSystemStatus()` | `SemanticSearchService`, `KnowledgeService`, `MemoryService`, `InventoryItemRepository`, `SensorService`, `ChatService` |

### 9.9 Memory Module (5 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 39 | `EmbeddingService` | `@Service` | `embed()`, `embedAndFormat()`, `embedBatch()`, `cosineSimilarity()`, `formatEmbedding()` | `OllamaService` |
| 40 | `MemoryService` | `@Service` | `createMemory()`, `findRelevantMemories()`, `searchMemoriesWithScores()`, `deleteMemory()`, `deleteAllMemoriesForUser()`, `exportMemories()` | `MemoryRepository`, `VectorDocumentRepository`, `EmbeddingService` |
| 41 | `MemoryExtractionService` | `@Service` | `extractAndStore()` — `@Async` LLM-based memory extraction from conversation | `OllamaService`, `MemoryService` |
| 42 | `RagService` | `@Service` | `buildRagContext()`, `formatContextBlock()` | `MemoryService`, `SemanticSearchService`, `SystemConfigService` |
| 43 | `SummarizationService` | `@Service` | `summarizeConversation()`, `scheduledNightlySummarization()` — `@Scheduled(cron="0 0 2 * * *")` | `ConversationRepository`, `MessageRepository`, `OllamaService`, `MemoryService` |

### 9.10 Models Module (4 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 44 | `ModelCatalogService` | `@Service` | `searchModels()`, `getModelDetails()`, `getModelFiles()` — HuggingFace API | `WebClient`, `QuantizationRecommendationService` |
| 45 | `ModelDownloadService` | `@Service` | `startDownload()`, `getProgress()`, `cancelDownload()`, `listLocalModels()`, `deleteLocalModel()`, `executeDownload()` — `@Async` | `WebClient`, `ModelDownloadProgressRegistry` |
| 46 | `ModelDownloadProgressRegistry` | `@Component` | `subscribe()`, `emit()`, `complete()` — SSE emitter registry | -- (in-memory) |
| 47 | `QuantizationRecommendationService` | `@Service` | `enrichFiles()` — adds quant metadata + picks recommended GGUF | -- (detects system RAM) |

### 9.11 Notification Module (2 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 48 | `DeviceRegistrationService` | `@Service` | `registerDevice()`, `getDevicesForUser()`, `unregisterDevice()`, `getTopicsForUser()` | `DeviceRegistrationRepository` |
| 49 | `MqttPublisherService` | `@Service` | `publishToTopic()`, `publishToUser()`, `publishBroadcast()` | `MqttClient`, `ObjectMapper` |

### 9.12 Privacy Module (5 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 50 | `AuditService` | `@Service` | `logAction()`, `getAuditLogs()`, `getAuditLogsForUser()`, `getAuditLogsByOutcome()`, `countByOutcomeBetween()`, `deleteByUserId()` | `AuditLogRepository` |
| 51 | `FortressService` | `@Service` | `enable()`, `disable()`, `getFortressStatus()`, `isFortressActive()` — iptables network lockdown | `SystemConfigService`, `UserRepository` |
| 52 | `DataExportService` | `@Service` | `exportUserData()` — AES-256-GCM encrypted zip | `ConversationRepository`, `MessageRepository`, `MemoryRepository` |
| 53 | `DataWipeService` | `@Service` | `wipeUser()` — `@Transactional` cross-domain cascade delete (15 repositories) | All data repositories |
| 54 | `SovereigntyReportService` | `@Service` | `generateReport()` — data sovereignty audit | `FortressService`, `AuditService`, all data repositories |
| 55 | `AuditAspect` | `@Aspect` `@Component` | `auditControllerMethod()` — `@Around` all controller methods | `AuditService` |

### 9.13 Proactive Module (6 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 56 | `InsightService` | `@Service` | `getInsights()`, `getInsightsByCategory()`, `getUnreadInsights()`, `markRead()`, `dismiss()`, `getUnreadCount()`, `deleteAllForUser()` | `InsightRepository` |
| 57 | `InsightGeneratorService` | `@Service` | `generateInsightForUser()` — LLM-powered pattern analysis | `PatternAnalysisService`, `OllamaService`, `InsightRepository`, `NotificationService` |
| 58 | `NightlyInsightJob` | `@Component` | `generateNightlyInsights()` — `@Scheduled(cron="0 0 3 * * *")` | `UserRepository`, `InsightGeneratorService` |
| 59 | `NotificationService` | `@Service` | `createNotification()`, `getNotifications()`, `markRead()`, `markAllRead()`, `getUnreadCount()`, `deleteNotification()`, `deleteAllForUser()` | `NotificationRepository`, `NotificationSseRegistry`, `MqttPublisherService` |
| 60 | `NotificationSseRegistry` | `@Component` | `register()`, `broadcast()`, `broadcastUnreadCount()` — SSE emitter registry | -- (in-memory) |
| 61 | `PatternAnalysisService` | `@Service` | `buildPatternSummary()` — aggregates user activity data | All data repositories |
| 62 | `SystemHealthMonitor` | `@Component` | `checkSystemHealth()` — `@Scheduled` disk/Ollama/heap checks | `OllamaService`, `NotificationService` |

### 9.14 Sensors Module (5 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 63 | `SensorService` | `@Service` | `registerSensor()`, `startSensor()`, `stopSensor()`, `testSensor()`, `getSensor()`, `listSensors()`, `getLatestReading()`, `getReadingHistory()`, `deleteSensor()`, `updateThresholds()`, `listAvailablePorts()`, `deleteAllForUser()` | `SensorRepository`, `SensorReadingRepository`, `SensorPollingService`, `SerialPortService` |
| 64 | `SensorPollingService` | `@Service` | `startPolling()`, `stopPolling()`, `stopAllPolling()` — serial port I/O with threshold alerting | `SerialPortService`, `SensorReadingRepository`, `SseEmitterRegistry`, `MemoryService`, `NotificationService` |
| 65 | `SensorStartupService` | `@Component` | `resumeActiveSensors()` — `@EventListener(ApplicationReadyEvent)` | `SensorRepository`, `SensorPollingService` |
| 66 | `SerialPortService` | `@Service` | `listAvailablePorts()`, `openPort()`, `closePort()`, `readLine()`, `testConnection()` | jSerialComm |
| 67 | `SseEmitterRegistry` | `@Component` | `register()`, `broadcast()`, `remove()` — SSE emitter registry for sensors | -- (in-memory) |

### 9.15 Settings Module (1 service)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 68 | `ExternalApiSettingsService` | `@Service` | `getSettings()`, `updateSettings()`, `getAnthropicKey()`, `getBraveKey()`, `getGrokKey()`, `getOpenAiKey()`, `getHuggingFaceToken()`, `getPreferredFrontierProvider()` | `ExternalApiSettingsRepository` |

### 9.16 Skills Module (8 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 69 | `BuiltInSkill` | Interface | `getSkillName()`, `execute()` | -- |
| 70 | `SkillExecutorService` | `@Service` | `execute()`, `executeByName()` — dispatches to BuiltInSkill implementations | `SkillRepository`, `SkillExecutionRepository`, `List<BuiltInSkill>` |
| 71 | `SkillSeederService` | `@Service` | `seedBuiltInSkills()` — `@EventListener(ApplicationReadyEvent)` | `SkillRepository` |
| 72 | `DocumentSummarizerSkill` | `@Component` | Document summarization via LLM | `KnowledgeService`, `OllamaService` |
| 73 | `InventoryTrackerSkill` | `@Component` | CRUD + low-stock alerts for inventory | `InventoryItemRepository` |
| 74 | `RecipeGeneratorSkill` | `@Component` | Recipe generation from food inventory | `InventoryItemRepository`, `OllamaService` |
| 75 | `ResourceCalculatorSkill` | `@Component` | Power/water/food runway calculations | `InventoryItemRepository` |
| 76 | `TaskPlannerSkill` | `@Component` | AI task planning + CRUD | `PlannedTaskRepository`, `OllamaService` |
| 77 | `WeatherQuerySkill` | `@Component` | Sensor-based weather conditions (stub) | `SensorRepository`, `SensorReadingRepository` |

### 9.17 System Module (6 services)

| # | Service | Type | Key Methods | Dependencies |
|---|---------|------|-------------|-------------|
| 78 | `SystemConfigService` | `@Service` | `getConfig()`, `save()`, `isInitialized()`, `setInitialized()`, `setFortressEnabled()`, `getAiSettings()`, `updateAiSettings()`, `getStorageSettings()`, `updateStorageSettings()`, `getActiveModelFilename()`, `setActiveModelFilename()` | `SystemConfigRepository` |
| 79 | `ApModeService` | `@Service` | `startApMode()`, `stopApMode()`, `isApModeActive()`, `scanWifiNetworks()`, `connectToWifi()`, `getConnectionStatus()` — hostapd/dnsmasq/nmcli | `SystemConfigService` |
| 80 | `ApModeStartupService` | `@Component` | `onApplicationReady()` — `@EventListener` | `SystemConfigService`, `ApModeService` |
| 81 | `FactoryResetService` | `@Service` | `performReset()` (`@Async`), `performUsbReset()` | `SystemConfigService`, `ApModeService` |
| 82 | `NetworkTransitionService` | `@Service` | `finalizeSetup()` (`@Async`) — AP→WiFi transition + avahi mDNS | `ApModeService`, `SystemConfigService` |
| -- | `UsbResetWatcherService` | `@Component` | `checkForTriggerFiles()` — `@Scheduled(fixedDelay=30000)` | `FactoryResetService` |

### 9.18 Service Layer Summary

| Metric | Value |
|--------|-------|
| Total service classes | 82 |
| `@Service` annotated | 45 |
| `@Component` annotated | 20 |
| Interfaces | 3 (`InferenceService`, `FrontierApiClient`, `BuiltInSkill`) |
| `@Aspect` | 1 (`AuditAspect`) |
| `@Scheduled` methods | 5 (nightly summarization, nightly insights, health monitor, USB watcher, llama-server health) |
| `@Async` methods | 5 (document processing, memory extraction, factory reset, network transition, model download) |
| `@EventListener` startup hooks | 4 (model health, storage health, sensor resume, AP mode, skill seeder) |
| `@ConditionalOnProperty` services | 3 (Ollama vs llama-server provider selection) |

---

## 10. Controller / API Layer

**Total controllers: 21** | **Total endpoint methods: 131**

### 10.1 `ChatController` — `/api/chat`
Injects: `ChatService`, `MessageRepository`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/conversations` | `createConversation()` |
| GET | `/conversations` | `listConversations()` |
| GET | `/conversations/search` | `searchConversations()` |
| GET | `/conversations/{conversationId}` | `getConversation()` |
| DELETE | `/conversations/{conversationId}` | `deleteConversation()` |
| PUT | `/conversations/{conversationId}/archive` | `archiveConversation()` |
| PUT | `/conversations/{conversationId}/title` | `renameConversation()` |
| POST | `/conversations/{conversationId}/messages` | `sendMessage()` — delegates to `streamMessage()` or `sendMessage()` |
| GET | `/conversations/{conversationId}/messages` | `listMessages()` |
| PUT | `/conversations/{conversationId}/messages/{messageId}` | `editMessage()` |
| DELETE | `/conversations/{conversationId}/messages/{messageId}` | `deleteMessage()` |
| POST | `/conversations/{conversationId}/branch/{messageId}` | `branchConversation()` |
| POST | `/conversations/{conversationId}/messages/{messageId}/regenerate` | `regenerateMessage()` |

### 10.2 `ModelController` — `/api/models`
Injects: `InferenceService`, `SystemConfigService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listModels()` |
| GET | `/active` | `getActiveModel()` |
| GET | `/health` | `getHealth()` |

### 10.3 `JudgeController` — `/api/ai/judge`
**Auth:** `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")`
Injects: `JudgeModelProcessService`, `JudgeInferenceService`, `ExternalApiSettingsService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/status` | `getStatus()` |
| POST | `/start` | `start()` |
| POST | `/stop` | `stop()` |
| POST | `/test` | `test()` |

### 10.4 `AuthController` — `/api/auth`
Injects: `AuthService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/register` | `register()` |
| POST | `/login` | `login()` |
| POST | `/refresh` | `refresh()` |
| POST | `/logout` | `logout()` |

### 10.5 `UserController` — `/api/users`
Injects: `UserService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listUsers()` |
| GET | `/{id}` | `getUser()` |
| PUT | `/{id}` | `updateUser()` |
| PUT | `/{id}/deactivate` | `deactivateUser()` |
| DELETE | `/{id}` | `deleteUser()` |

### 10.6 `EnrichmentController` — `/api/enrichment`
Injects: `WebFetchService`, `WebSearchService`, `ClaudeApiService`, `ExternalApiSettingsService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/fetch-url` | `fetchUrl()` |
| POST | `/search` | `search()` |
| GET | `/status` | `getStatus()` |

### 10.7 `ScheduledEventController` — `/api/events`
Injects: `ScheduledEventService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listEvents()` |
| GET | `/{eventId}` | `getEvent()` |
| POST | `/` | `createEvent()` |
| PUT | `/{eventId}` | `updateEvent()` |
| DELETE | `/{eventId}` | `deleteEvent()` |
| PUT | `/{eventId}/toggle` | `toggleEvent()` |

### 10.8 `KnowledgeController` — `/api/knowledge`
Injects: `KnowledgeService`, `SemanticSearchService`, `SystemConfigService`, `FileStorageService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/` | `uploadDocument()` |
| GET | `/` | `listDocuments()` |
| GET | `/{documentId}` | `getDocument()` |
| PUT | `/{documentId}/display-name` | `updateDisplayName()` |
| DELETE | `/{documentId}` | `deleteDocument()` |
| POST | `/{documentId}/retry` | `retryProcessing()` |
| GET | `/{documentId}/download` | `downloadDocument()` |
| GET | `/{documentId}/content` | `getDocumentContent()` |
| POST | `/create` | `createDocument()` |
| PUT | `/{documentId}/content` | `updateDocumentContent()` |
| POST | `/search` | `searchKnowledge()` |

### 10.9 `LibraryController` — `/api/library`
Injects: `ZimFileService`, `EbookService`, `GutenbergService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/zim` | `uploadZim()` |
| GET | `/zim` | `listZimFiles()` |
| DELETE | `/zim/{id}` | `deleteZim()` |
| GET | `/kiwix/status` | `kiwixStatus()` |
| GET | `/kiwix/url` | `kiwixUrl()` |
| POST | `/ebooks` | `uploadEbook()` |
| GET | `/ebooks` | `listEbooks()` |
| GET | `/ebooks/{id}` | `getEbook()` |
| DELETE | `/ebooks/{id}` | `deleteEbook()` |
| GET | `/ebooks/{id}/content` | `downloadEbook()` |
| GET | `/gutenberg/search` | `searchGutenberg()` |
| GET | `/gutenberg/{id}` | `getGutenbergBook()` |
| POST | `/gutenberg/{id}/import` | `importGutenberg()` |

### 10.10 `McpDiscoveryController` — `/api/mcp`
**Auth:** `@PreAuthorize("hasRole('OWNER')")`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/claude-desktop-config` | `getClaudeDesktopConfig()` |

### 10.11 `McpTokenController` — `/api/mcp/tokens`
**Auth:** `@PreAuthorize("hasRole('OWNER')")`
Injects: `McpTokenService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/` | `createToken()` |
| GET | `/` | `listTokens()` |
| DELETE | `/{tokenId}` | `revokeToken()` |

### 10.12 `MemoryController` — `/api/memory`
Injects: `MemoryService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listMemories()` |
| GET | `/{id}` | `getMemory()` |
| DELETE | `/{id}` | `deleteMemory()` |
| PUT | `/{id}/importance` | `updateImportance()` |
| PUT | `/{id}/tags` | `updateTags()` |
| POST | `/search` | `searchMemories()` |
| GET | `/export` | `exportMemories()` |

### 10.13 `ModelDownloadController` — `/api/models`
Injects: `ModelCatalogService`, `ModelDownloadService`, `ModelDownloadProgressRegistry`, `SystemConfigService`, `LlamaServerProcessService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/catalog/search` | `searchCatalog()` |
| GET | `/catalog/{author}/{modelId}` | `getModelDetails()` |
| GET | `/catalog/{author}/{modelId}/files` | `getModelFiles()` |
| POST | `/download` | `startDownload()` |
| GET | `/download` | `getAllDownloads()` |
| GET | `/download/{downloadId}/progress` | `getDownloadProgress()` — SSE stream |
| DELETE | `/download/{downloadId}` | `cancelDownload()` |
| GET | `/local` | `listLocalModels()` |
| DELETE | `/local/{filename}` | `deleteLocalModel()` |
| POST | `/active` | `setActiveModel()` |
| GET | `/server-status` | `getServerStatus()` |
| POST | `/restart` | `reloadModel()` |

### 10.14 `DeviceRegistrationController` — `/api/notifications/devices`
Injects: `DeviceRegistrationService`

| HTTP | Route | Method |
|------|-------|--------|
| POST | `/` | `registerDevice()` |
| GET | `/` | `getDevices()` |
| DELETE | `/{deviceId}` | `unregisterDevice()` |

### 10.15 `PrivacyController` — `/api/privacy`
Injects: `FortressService`, `AuditService`, `SovereigntyReportService`, `DataExportService`, `DataWipeService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/fortress/status` | `getFortressStatus()` |
| POST | `/fortress/enable` | `enableFortress()` |
| POST | `/fortress/disable` | `disableFortress()` |
| GET | `/sovereignty-report` | `getSovereigntyReport()` |
| GET | `/audit-logs` | `getAuditLogs()` |
| POST | `/export` | `exportData()` |
| DELETE | `/wipe` | `wipeData()` |
| DELETE | `/wipe/self` | `wipeSelfData()` |

### 10.16 `ProactiveController` — `/api/insights` + `/api/notifications`
Injects: `InsightService`, `InsightGeneratorService`, `NotificationService`, `NotificationSseRegistry`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/api/insights` | `getInsights()` |
| POST | `/api/insights/generate` | `generateInsights()` |
| PUT | `/api/insights/{insightId}/read` | `markInsightRead()` |
| PUT | `/api/insights/{insightId}/dismiss` | `dismissInsight()` |
| GET | `/api/insights/unread-count` | `getInsightUnreadCount()` |
| GET | `/api/notifications` | `getNotifications()` |
| PUT | `/api/notifications/{notificationId}/read` | `markNotificationRead()` |
| PUT | `/api/notifications/read-all` | `markAllNotificationsRead()` |
| GET | `/api/notifications/unread-count` | `getNotificationUnreadCount()` |
| DELETE | `/api/notifications/{notificationId}` | `deleteNotification()` |
| GET | `/api/notifications/stream` | `streamNotifications()` — SSE stream |

### 10.17 `SensorController` — `/api/sensors`
Injects: `SensorService`, `SseEmitterRegistry`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listSensors()` |
| GET | `/{sensorId}` | `getSensor()` |
| POST | `/` | `registerSensor()` |
| DELETE | `/{sensorId}` | `deleteSensor()` |
| POST | `/{sensorId}/start` | `startSensor()` |
| POST | `/{sensorId}/stop` | `stopSensor()` |
| GET | `/{sensorId}/latest` | `getLatestReading()` |
| GET | `/{sensorId}/history` | `getReadingHistory()` |
| PUT | `/{sensorId}/thresholds` | `updateThresholds()` |
| POST | `/test` | `testConnection()` |
| GET | `/ports` | `listAvailablePorts()` |
| GET | `/{sensorId}/stream` | `streamSensor()` — SSE stream |

### 10.18 `ExternalApiSettingsController` — `/api/settings/external-apis`
Injects: `ExternalApiSettingsService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `getSettings()` |
| PUT | `/` | `updateSettings()` |

### 10.19 `SkillController` — `/api/skills`
Injects: `SkillRepository`, `SkillExecutionRepository`, `InventoryItemRepository`, `SkillExecutorService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/` | `listSkills()` |
| GET | `/{skillId}` | `getSkill()` |
| PATCH | `/{skillId}/toggle` | `toggleSkill()` |
| POST | `/execute` | `executeSkill()` |
| GET | `/executions` | `listExecutions()` |
| GET | `/inventory` | `listInventory()` |
| POST | `/inventory` | `createInventoryItem()` |
| PUT | `/inventory/{itemId}` | `updateInventoryItem()` |
| DELETE | `/inventory/{itemId}` | `deleteInventoryItem()` |

### 10.20 `CaptivePortalController` — `/setup`
Injects: `SystemConfigService`, `ApModeService`
**Note:** Uses `@Controller` (not `@RestController`) — returns view forwards/redirects.

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/setup` | `setupWelcome()` |
| GET | `/setup/wifi` | `setupWifi()` |
| GET | `/setup/account` | `setupAccount()` |
| GET | `/setup/confirm` | `setupConfirm()` |
| GET | `/api/setup/wifi/scan` | `scanWifi()` |
| POST | `/api/setup/wifi/connect` | `connectWifi()` |
| GET | `/api/setup/wifi/status` | `wifiStatus()` |

### 10.21 `SystemController` — `/api/system`
Injects: `SystemConfigService`, `AuthService`, `NetworkTransitionService`, `FactoryResetService`, `LlamaServerProcessService`

| HTTP | Route | Method |
|------|-------|--------|
| GET | `/status` | `getStatus()` |
| POST | `/initialize` | `initialize()` |
| POST | `/finalize-setup` | `finalizeSetup()` |
| GET | `/ai-settings` | `getAiSettings()` |
| PUT | `/ai-settings` | `updateAiSettings()` |
| GET | `/storage-settings` | `getStorageSettings()` |
| PUT | `/storage-settings` | `updateStorageSettings()` |
| POST | `/factory-reset` | `factoryReset()` |
| GET | `/models/server/status` | `getLlamaServerStatus()` |
| POST | `/models/server/switch` | `switchLlamaServerModel()` |

---

## 11. Security Configuration

**File:** `config/SecurityConfig.java`

### 11.1 Authentication

- **Strategy:** Stateless JWT (HMAC-SHA256) via JJWT 0.12.6
- **Dual-token model:** Access token (15 min) + Refresh token (7 days)
- **Password encoding:** BCrypt (default strength)
- **Session management:** `STATELESS` — no HTTP sessions
- **CSRF:** Disabled (appropriate for stateless JWT API)

### 11.2 Authorization Rules

| Pattern | Access |
|---------|--------|
| `/api/auth/**` | `permitAll()` |
| `/api/system/status` | `permitAll()` |
| `/api/system/initialize` | `permitAll()` |
| `/setup/**` | `permitAll()` |
| `/api/setup/**` | `permitAll()` |
| `/api/mcp/tokens/**` | `hasRole('OWNER')` — via `@PreAuthorize` on controller |
| `/api/mcp/claude-desktop-config` | `hasRole('OWNER')` — via `@PreAuthorize` on controller |
| `/api/ai/judge/**` | `hasRole('ADMIN') or hasRole('OWNER')` — via `@PreAuthorize` on controller |
| All other `/api/**` | `authenticated()` |

### 11.3 CORS Configuration

- **Allowed origins:** `*` (wildcard — appropriate for offline appliance, but would need restriction for internet-facing deployment)
- **Allowed methods:** GET, POST, PUT, DELETE, PATCH, OPTIONS
- **Allowed headers:** `*`
- **Exposed headers:** `Content-Type`, `Authorization`

### 11.4 Filter Chain

Order of execution (pre-authentication):

1. **`RateLimitingFilter`** — Bucket4j per-IP rate limiting
   - Auth endpoints (`/api/auth/**`): 10 requests/minute
   - API endpoints (`/api/**`): 200 requests/minute
   - Bypasses: `/setup/**`, `/api/setup/**`, static resources
   - Returns `429 Too Many Requests` with JSON body when exceeded

2. **`JwtAuthFilter`** — Extracts `Authorization: Bearer <token>`, validates JWT, sets `SecurityContextHolder`
   - Skips: `/api/auth/**`, `/api/system/status`, `/api/system/initialize`, `/setup/**`, `/api/setup/**`

3. **`CaptivePortalRedirectFilter`** — Redirects to `/setup` when system is uninitialized AND in AP mode
   - Active only when `apModeEnabled=true` AND `initialized=false`
   - Bypasses: `/setup`, `/api/setup`, static resources

4. **`MdcFilter`** — Adds `requestId` (UUID), `username`, `userId` to SLF4J MDC for request tracing

5. **`RequestResponseLoggingFilter`** — Logs method, URI, status, and duration for every request

### 11.5 MCP Authentication

- **`McpAuthFilter`** — Separate filter chain for MCP SSE endpoints (`/mcp/sse`, `/mcp/message`)
- **Token format:** `Bearer <raw-token>` → validated against BCrypt-hashed tokens in `mcp_api_tokens` table
- **Creates:** `McpAuthentication` (custom `AbstractAuthenticationToken`) with `ROLE_MCP_CLIENT` authority
- **No JWT involved** — standalone token-based auth for machine clients

## 12. Auth / JWT / Session Handling

### 12.1 JwtService

**File:** `auth/service/JwtService.java`

| Method | Purpose |
|--------|---------|
| `generateAccessToken(User)` | Creates 15-minute access token with `sub=username`, `userId`, `role` claims |
| `generateRefreshToken(User)` | Creates 7-day refresh token with `sub=username` claim only |
| `extractUsername(token)` | Parses `sub` claim |
| `extractClaim(token, resolver)` | Generic claim extractor |
| `isTokenValid(token, userDetails)` | Validates signature + expiration + username match |
| `isTokenExpired(token)` | Checks expiration claim |

### 12.2 Token Configuration

| Parameter | Value | Source |
|-----------|-------|--------|
| Algorithm | HMAC-SHA256 | Hardcoded (JJWT `Jwts.SIG.HS256`) |
| Secret key | `${app.jwt.secret}` | 256-bit minimum (env var in prod) |
| Access TTL | 15 minutes | `${app.jwt.access-token-expiration}` |
| Refresh TTL | 7 days | `${app.jwt.refresh-token-expiration}` |

### 12.3 Session Strategy

- **No server-side session store** — fully stateless
- **No token blacklist** — logout is client-side only (token remains valid until expiry)
- **Refresh flow:** Client sends refresh token → server validates → issues new access + refresh pair
- **No refresh token rotation tracking** — old refresh tokens are not invalidated server-side

### 12.4 Security Observations

- **No token revocation mechanism** — compromised tokens cannot be invalidated before expiry
- **CORS `*` origin** — acceptable for offline appliance; would need restriction if internet-facing
- **No HTTPS enforcement in code** — expected to be handled by deployment/reverse proxy
- **Rate limiting is per-IP** — effective for direct client access; proxy deployments would need `X-Forwarded-For` handling

---

## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler.java ===
@RestControllerAdvice: YES

Exception Mappings:
  - MethodArgumentNotValidException → 400 (field error details)
  - UsernameNotFoundException → 404
  - BadCredentialsException → 401 ("Invalid username or password")
  - AccessDeniedException → 403 ("Access denied")
  - EntityNotFoundException → 404
  - DuplicateResourceException → 409
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
  - AsyncRequestNotUsableException → void (debug log, client disconnect)
  - AsyncRequestTimeoutException → void (SSE timeout, no body)
  - Exception (catch-all) → 500 ("An unexpected error occurred")

Standard error response format (ApiResponse<T>):
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2026-03-18T18:00:00Z",
  "requestId": "uuid"
}

Custom exception classes (all in common.exception):
  - ApModeException
  - DuplicateResourceException
  - EmbeddingException
  - EntityNotFoundException
  - FortressActiveException
  - FortressOperationException
  - InitializationException
  - OcrException
  - OllamaInferenceException
  - OllamaUnavailableException
  - SensorConnectionException
  - SkillDisabledException
  - StorageException
  - UnsupportedFileTypeException
```

---

## 14. Mappers / DTOs

No MapStruct, ModelMapper, or dedicated mapper classes detected. All DTO mapping is done manually in service methods (inline construction). DTO structures are documented in the OpenAPI spec.

---

## 15. Utility Classes & Shared Components

```
=== AppConstants.java (config) ===
Centralized constants for the entire application: server ports, JWT settings, API paths,
pagination defaults, file upload limits, sensor settings, RAG parameters, memory/knowledge
vault constants, MQTT topics, rate limiting, inference provider settings, HuggingFace API,
judge model config, frontier API URLs, hybrid search weights, retry/backoff settings.
Used by: Nearly every service and controller.
Path: src/main/java/com/myoffgridai/config/AppConstants.java

=== AesEncryptionUtil.java (common.util) ===
Methods:
  - encrypt(String plaintext): String — AES-256-GCM encryption with 12-byte random IV
  - decrypt(String ciphertext): String — Decrypts Base64-encoded ciphertext
Key source: app.encryption.key (64-char hex string = 32 bytes)
Used by: AesAttributeConverter, DataExportService

=== AesAttributeConverter.java (common.util) ===
JPA @Converter — transparently encrypts/decrypts entity string fields at persistence time.
Delegates to AesEncryptionUtil.
Used by: ExternalApiSettings (anthropicApiKey, braveApiKey, huggingFaceToken, grokApiKey, openAiApiKey)

=== TokenCounter.java (common.util) ===
Methods:
  - estimateTokens(String text): int — Character-based approximation (1 token ~= 4 chars)
  - truncateToTokenLimit(List<OllamaMessage>, int maxTokens): List<OllamaMessage> — Removes oldest non-system messages
Used by: ContextWindowService, SystemPromptBuilder

=== ApiResponse.java (common.response) ===
Generic response wrapper: { success, message, data, timestamp, requestId, totalElements, page, size }
Factory methods: success(T), success(T, String), error(String), paginated(T, long, int, int)
Used by: All controllers

=== DeltaJsonUtils.java (knowledge.util) ===
Methods:
  - textToDeltaJson(String plainText): String — Converts plain text to Quill Delta JSON
  - deltaJsonToText(String deltaJson): String — Extracts plain text from Quill Delta JSON
Used by: KnowledgeService (rich text editor support)
```

---

---

## 16. Database Schema

**Strategy:** Hibernate auto-DDL (`ddl-auto: update` in dev, `validate` in prod with Flyway migrations)
**Database:** PostgreSQL with pgvector extension
**Primary Keys:** All entities use `UUID` (auto-generated `GenerationType.UUID`)

### 16.1 Tables (22 total)

| Table | Entity | Key Relationships |
|-------|--------|-------------------|
| `conversations` | `Conversation` | `@ManyToOne → User (EAGER)` |
| `messages` | `Message` | `@ManyToOne → Conversation (LAZY)` |
| `users` | `User` | Implements `UserDetails` |
| `scheduled_events` | `ScheduledEvent` | `@ManyToOne → User (LAZY)` |
| `knowledge_documents` | `KnowledgeDocument` | `@ManyToOne → User (LAZY)` |
| `knowledge_chunks` | `KnowledgeChunk` | `@ManyToOne → KnowledgeDocument (LAZY)` |
| `ebooks` | `Ebook` | Standalone |
| `zim_files` | `ZimFile` | Standalone |
| `mcp_api_tokens` | `McpApiToken` | `createdBy: UUID` (no FK) |
| `memories` | `Memory` | `userId: UUID` (no FK) |
| `vector_document` | `VectorDocument` | `userId: UUID`, `sourceId: UUID` — `vector(768)` column |
| `device_registrations` | `DeviceRegistration` | `userId: UUID` (no FK) |
| `audit_logs` | `AuditLog` | `userId: UUID` (nullable, no FK) |
| `insights` | `Insight` | `userId: UUID` (no FK) |
| `notifications` | `Notification` | `userId: UUID` (no FK) |
| `sensors` | `Sensor` | `userId: UUID` (no FK) |
| `sensor_readings` | `SensorReading` | `@ManyToOne → Sensor (LAZY)` |
| `external_api_settings` | `ExternalApiSettings` | Singleton (unique `singletonGuard`) |
| `system_config` | `SystemConfig` | Singleton |
| `inventory_items` | `InventoryItem` | `userId: UUID` (no FK) |
| `skills` | `Skill` | Standalone |
| `skill_executions` | `SkillExecution` | `userId: UUID`, `skillId: UUID` (no FK) |
| `planned_tasks` | `PlannedTask` | `userId: UUID` (no FK) |

### 16.2 pgvector Configuration

- **Extension:** `vector` (pgvector)
- **Column:** `vector_document.embedding` — `vector(768)` using custom Hibernate `VectorType`
- **Distance function:** Cosine distance (`<=>`) in native queries
- **Model:** nomic-embed-text (768-dimensional embeddings)

### 16.3 Observations

- Most entities use `userId: UUID` as a plain column rather than `@ManyToOne` FK relationships — this provides loose coupling but no referential integrity enforcement at DB level
- `Conversation → User` is the only `EAGER` fetch — all other relationships are `LAZY`
- No composite indexes defined in entity annotations (indexes may exist via Hibernate auto-DDL or manual migration)

---

## 17. Message Broker Configuration

### 17.1 MQTT (Eclipse Paho)

- **Broker:** Configured via `app.mqtt.broker-url` (default `tcp://localhost:1883`)
- **Client:** Eclipse Paho `MqttClient` managed by `MqttService`
- **Topics:** Defined in `AppConstants` (e.g., `offgridai/notifications/{userId}`, `offgridai/sensors/{sensorId}`)
- **QoS:** 1 (at least once)
- **Use cases:**
  - Push notifications to mobile/IoT devices
  - Sensor data publishing
  - System health alerts
- **Dev mode:** MQTT is disabled by default (`app.mqtt.enabled: false` in application.yml)
- **Prod mode:** Expected to connect to local Mosquitto broker on the appliance

### 17.2 No Other Message Brokers

- No Kafka, RabbitMQ, or other message broker dependencies
- SSE (Server-Sent Events) is used for real-time streaming to web clients (notifications, sensor data, download progress, chat)

---

## 18. Cache Layer

**No caching layer is implemented.**

- No `@EnableCaching`, `@Cacheable`, `@CacheEvict`, or `@CachePut` annotations found
- No Redis, Caffeine, or Ehcache dependencies in pom.xml
- No `CacheManager` beans configured

**Implication:** All data access goes directly to PostgreSQL. For an offline appliance with a single user, this is acceptable. Consider adding caching if performance becomes a concern under multi-user scenarios.

---

## 19. Environment Variable Inventory

### 19.1 Production Environment Variables (`application-prod.yml`)

| Variable | Property | Default | Required |
|----------|----------|---------|----------|
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/myoffgridai` | No |
| `DB_USERNAME` | `spring.datasource.username` | `myoffgridai` | No |
| `DB_PASSWORD` | `spring.datasource.password` | `myoffgridai` | No |
| `JWT_SECRET` | `app.jwt.secret` | **None** | **YES** |
| `JWT_EXPIRATION_MS` | `app.jwt.expiration-ms` | `86400000` (24h) | No |
| `JWT_REFRESH_EXPIRATION_MS` | `app.jwt.refresh-expiration-ms` | `604800000` (7d) | No |
| `ENCRYPTION_KEY` | `app.encryption.key` | **None** | **YES** |
| `INFERENCE_PROVIDER` | `app.inference.provider` | `llama-server` | No |
| `INFERENCE_BASE_URL` | `app.inference.base-url` | `http://localhost:1234` | No |
| `INFERENCE_MODEL` | `app.inference.model` | `Jackrong/Qwen3.5-27B-Claude-4.6-Opus-Reasoning-Distilled-GGUF` | No |
| `INFERENCE_EMBED_MODEL` | `app.inference.embed-model` | `nomic-embed-text` | No |
| `LLAMA_SERVER_BINARY` | `app.inference.llama-server-binary` | `/usr/local/bin/llama-server` | No |
| `MODELS_DIR` | `app.inference.models-dir` | `./models` | No |
| `ACTIVE_MODEL` | `app.inference.active-model` | (empty) | No |
| `INFERENCE_PORT` | `app.inference.port` | `1234` | No |
| `INFERENCE_CONTEXT_SIZE` | `app.inference.context-size` | `32768` | No |
| `INFERENCE_GPU_LAYERS` | `app.inference.gpu-layers` | `99` | No |
| `INFERENCE_THREADS` | `app.inference.threads` | `8` | No |
| `INFERENCE_TIMEOUT` | `app.inference.timeout-seconds` | `120` | No |
| `INFERENCE_MAX_TOKENS` | `app.inference.max-tokens` | `4096` | No |
| `INFERENCE_TEMPERATURE` | `app.inference.temperature` | `0.7` | No |
| `HEALTH_CHECK_INTERVAL` | `app.inference.health-check-interval-seconds` | `30` | No |
| `RESTART_DELAY` | `app.inference.restart-delay-seconds` | `5` | No |
| `STARTUP_TIMEOUT` | `app.inference.startup-timeout-seconds` | `120` | No |

### 19.2 Mandatory Secrets (No Defaults)

| Variable | Purpose | Risk if Missing |
|----------|---------|-----------------|
| `JWT_SECRET` | HMAC-SHA256 signing key for JWT tokens | **App will fail to start** |
| `ENCRYPTION_KEY` | AES-256-GCM key for encrypting API keys at rest | **App will fail to start** |

---

## 20. Service Dependency Map

```
┌─────────────────────────────────────────────────────────────────────┐
│                        EXTERNAL SERVICES                            │
├─────────────────────────────────────────────────────────────────────┤
│ PostgreSQL (5432)  │ Ollama (11434)  │ llama-server (1234)         │
│ MQTT Broker (1883) │ Kiwix (8080)    │ Frontier APIs (Claude/      │
│                    │                 │   OpenAI/Grok)              │
└─────────┬──────────┴────────┬────────┴─────────────┬───────────────┘
          │                   │                      │
┌─────────▼───────────────────▼──────────────────────▼───────────────┐
│                    MyOffGridAI-Server (:8080)                       │
├────────────────────────────────────────────────────────────────────┤
│ AI Module ─────────── Inference (Ollama OR llama-server)           │
│ Enrichment Module ─── Claude API (frontier, web search)           │
│ Frontier Module ───── Claude / OpenAI / Grok APIs                 │
│ Knowledge Module ──── Embedding via Ollama (nomic-embed-text)     │
│ Library Module ────── Kiwix Serve (ZIM files), Gutenberg API      │
│ MCP Module ────────── Spring AI MCP SSE Server                    │
│ Memory Module ─────── pgvector similarity search                  │
│ Notification Module ─ MQTT (Eclipse Paho)                         │
│ Sensors Module ────── Serial ports (jSerialComm)                  │
│ System Module ─────── hostapd, dnsmasq, avahi-daemon (AP mode)    │
└────────────────────────────────────────────────────────────────────┘
```

### 20.1 Internal Service Dependencies

| Service | Key Dependencies |
|---------|-----------------|
| `ChatService` | `InferenceService`, `MemoryService`, `SemanticSearchService`, `ContextBuilderService`, `MemoryExtractionService`, `ConversationRepository`, `MessageRepository` |
| `SemanticSearchService` | `EmbeddingService`, `VectorDocumentRepository` |
| `EmbeddingService` | Ollama embedding API (nomic-embed-text) |
| `KnowledgeService` | `FileStorageService`, `ChunkingService`, `EmbeddingService`, `VectorDocumentRepository` |
| `NotificationService` | `MqttService`, `NotificationRepository`, `NotificationSseRegistry` |
| `ScheduledEventService` | `NotificationService`, `ChatService` |
| `FortressService` | `SystemConfigService`, `AuditService` |
| `DataWipeService` | All repositories with `deleteByUserId()` (15 repositories) |

---

## 21. Known Technical Debt & Observations

### 21.1 TODO/FIXME/STUB Scan

**Scan command:** `grep -rn "TODO\|FIXME\|HACK\|XXX\|STUB\|PLACEHOLDER\|HARDCODED\|WORKAROUND" src/`

| Severity | Location | Finding |
|----------|----------|---------|
| INFO | `UsbResetWatcherService.java:20,45` | Firmware update via USB **stubbed for MI-002** — logs detection but takes no action |
| INFO | `SkillSeederService.java:51` | WeatherQuerySkill described as "Stub implementation until sensor integration in Phase 6" |
| INFO | `JwtService.java:24` | Comment states "no hardcoded values" — confirmed, all durations from config |
| INFO | `ApModeService.java:124` | Mock mode returns hardcoded WiFi network list — expected for dev |

### 21.2 Architectural Observations

| Area | Observation |
|------|-------------|
| **Token revocation** | No server-side token blacklist — logout is client-side only. Compromised tokens valid until expiry. |
| **CORS wildcard** | `allowedOrigins("*")` — acceptable for offline appliance, would need restriction for internet deployment |
| **No caching** | All reads hit PostgreSQL directly — acceptable for single-user appliance |
| **No @EntityGraph** | Zero repositories use EntityGraph — lazy associations may cause N+1 queries in some paths |
| **No projections** | All queries return full entities — no DTO projections at repository level |
| **userId FK pattern** | Most entities use bare `UUID userId` columns without `@ManyToOne` foreign key constraints |
| **Conversation EAGER fetch** | `Conversation.user` is the only EAGER relationship — may load unnecessary User data in list queries |
| **SkillController injects repositories directly** | `SkillController` bypasses service layer, calling repositories directly for some operations |
| **ProcessBuilder shell commands** | `ApModeService`, `NetworkTransitionService` execute `sudo` system commands via `ProcessBuilder` — expected for appliance but requires careful permission management |

### 21.3 Stale/Deferred Features

| Feature | Status | Notes |
|---------|--------|-------|
| Firmware update via USB | Stubbed | `UsbResetWatcherService` detects file but defers to MI-002 |
| Weather skill | Stub | Returns canned response; awaiting sensor integration |
| GraalVM native image | Configured | `native` profile in pom.xml; not validated in CI |

---

## 22. Security Vulnerability Scan (Snyk)

**Scan date:** 2026-03-18
**Dependencies tested:** 178
**Issues found:** 2 (2 vulnerable paths)

| # | Severity | Vulnerability | Package | Version | Fix |
|---|----------|--------------|---------|---------|-----|
| 1 | **HIGH** | Incorrect Authorization (CVE pending) | `org.apache.tomcat.embed:tomcat-embed-core` | `10.1.50` | Upgrade `spring-boot-starter-web` to `3.5.11+` |
| 2 | LOW | External Initialization of Trusted Variables | `ch.qos.logback:logback-core` | `1.5.22` | Upgrade `spring-boot-starter-web` to `3.5.11+` |

**Path:** Both vulnerabilities are transitive via `spring-boot-starter-web@3.4.13`.
**Remediation:** Upgrade Spring Boot parent from `3.4.13` to `3.5.11` or later.

**⚠ CRITICAL: The Tomcat Incorrect Authorization vulnerability (HIGH severity) should be addressed before any production deployment.**

---

## Verification

### Completeness Check

| Section | Status |
|---------|--------|
| 1. Project Identity | ✅ Complete |
| 2. Directory Structure | ✅ Complete |
| 3. Build & Dependency Manifest | ✅ Complete |
| 4. Configuration & Infrastructure | ✅ Complete |
| 5. Startup & Runtime Behavior | ✅ Complete |
| 6. Entity / Data Model Layer | ✅ Complete (22 entities) |
| 7. Enum Inventory | ✅ Complete (24 enums) |
| 8. Repository Layer | ✅ Complete (23 repositories, 103 methods) |
| 9. Service Layer | ✅ Complete (82 services) |
| 10. Controller / API Layer | ✅ Complete (21 controllers, 131 endpoints) |
| 11. Security Configuration | ✅ Complete |
| 12. Auth / JWT / Session Handling | ✅ Complete |
| 13. Exception Handling & Error Responses | ✅ Complete |
| 14. Mappers / DTOs | ✅ Complete (manual mapping) |
| 15. Utility Classes & Shared Components | ✅ Complete |
| 16. Database Schema | ✅ Complete |
| 17. Message Broker Configuration | ✅ Complete (MQTT) |
| 18. Cache Layer | ✅ Complete (None) |
| 19. Environment Variable Inventory | ✅ Complete (24 variables) |
| 20. Service Dependency Map | ✅ Complete |
| 21. Known Technical Debt | ✅ Complete |
| 22. Security Vulnerability Scan | ✅ Complete (Snyk) |

### Audit Metadata

- **Audit generated by:** Engineer (Claude Code)
- **Template used:** `~/Documents/Github/codebase-audit-template.md`
- **All source files read from disk:** YES — every file was `cat`/`Read` before documenting
- **Previous audit referenced:** NO — clean generation from source
