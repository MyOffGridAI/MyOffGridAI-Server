# MyOffGridAI-Server — Codebase Audit

**Audit Date:** 2026-03-16T23:24:33Z
**Branch:** main
**Commit:** 072217065a39eb2d63e12baccde26f8eddc3e5df P12-Server: Add HuggingFace model catalog and LM Studio download manager
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** MyOffGridAI-Server-Audit.md
**Scorecard:** MyOffGridAI-Server-Scorecard.md
**OpenAPI Spec:** Generated separately

> This audit is the source of truth for the MyOffGridAI-Server codebase structure, entities, services, and configuration.
> The OpenAPI spec is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: MyOffGridAI Server
Repository URL: https://github.com/aallard/MyOffGridAI-Server.git
Primary Language / Framework: Java 21 / Spring Boot 3.4.6
Build Tool + Version: Maven (wrapper included, apache-maven-3.9.9)
Current Branch: main
Latest Commit Hash: 072217065a39eb2d63e12baccde26f8eddc3e5df
Latest Commit Message: P12-Server: Add HuggingFace model catalog and LM Studio download manager
Audit Timestamp: 2026-03-16T23:24:33Z
```

---

## 2. Directory Structure

Single-module Maven project. Source root: `src/main/java/com/myoffgridai/`. Organized into 15 feature packages: `ai`, `auth`, `common`, `config`, `enrichment`, `events`, `knowledge`, `library`, `mcp`, `memory`, `models`, `notification`, `privacy`, `proactive`, `sensors`, `settings`, `skills`, `system`. Tests mirror the source structure under `src/test/java/com/myoffgridai/` with integration tests in `integration/`.

```
MyOffGridAI-Server/
├── pom.xml
├── Dockerfile
├── CONVENTIONS.md
├── MyOffGridAI-Server-Architecture.md
├── README.md
├── .gitignore
├── src/main/java/com/myoffgridai/
│   ├── MyOffGridAiApplication.java
│   ├── ai/          (controller, dto, model, repository, service)
│   ├── auth/        (controller, dto, model, repository, service)
│   ├── common/      (exception, response, util)
│   ├── config/      (security, JPA, Ollama, LmStudio, rate limiting, filters)
│   ├── enrichment/  (controller, dto, service)
│   ├── events/      (controller, dto, model, repository, service)
│   ├── knowledge/   (controller, dto, model, repository, service, util)
│   ├── library/     (config, controller, dto, model, repository, service)
│   ├── mcp/         (config, controller, dto, model, repository, service)
│   ├── memory/      (controller, dto, model, repository, service)
│   ├── models/      (controller, dto, service)
│   ├── notification/ (config, controller, dto, model, repository, service)
│   ├── privacy/     (aspect, controller, dto, model, repository, service)
│   ├── proactive/   (controller, dto, model, repository, service)
│   ├── sensors/     (controller, dto, model, repository, service)
│   ├── settings/    (controller, dto, model, repository, service)
│   ├── skills/      (builtin, controller, dto, model, repository, service)
│   └── system/      (controller, dto, model, repository, service)
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   ├── logback-spring.xml
│   ├── META-INF/native-image/ (GraalVM configs)
│   └── static/setup/setup.js
└── src/test/
    ├── java/com/myoffgridai/ (100 unit test + 23 integration test files)
    └── resources/ (application.yml, application-test.yml)
```

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.6 (parent) | REST API framework |
| spring-boot-starter-data-jpa | 3.4.6 | JPA / Hibernate ORM |
| spring-boot-starter-security | 3.4.6 | Authentication & authorization |
| spring-boot-starter-validation | 3.4.6 | Bean validation (Jakarta) |
| spring-boot-starter-actuator | 3.4.6 | Health/metrics endpoints |
| spring-boot-starter-webflux | 3.4.6 | WebClient for async HTTP |
| spring-boot-starter-aop | 3.4.6 | AOP for audit aspect |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 | MCP Server (SSE transport) |
| jjwt-api/impl/jackson | 0.12.6 | JWT token generation/validation |
| postgresql | 42.7.7 | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector extension support |
| pdfbox | 3.0.4 | PDF text extraction |
| poi / poi-ooxml / poi-scratchpad | 5.3.0 | Office document processing |
| tess4j | 5.13.0 | OCR via Tesseract |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| bucket4j-core | 8.10.1 | Rate limiting |
| paho.client.mqttv3 | 1.2.5 | MQTT client (Mosquitto) |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger UI / OpenAPI |
| jsoup | 1.18.3 | HTML parsing (web fetch) |
| lombok | 1.18.42 | Boilerplate reduction |
| jackson-core | 2.18.6 | JSON processing (BOM override) |
| netty-codec-http/http2 | 4.1.125.Final | Netty (BOM override) |
| commons-lang3 | 3.18.0 | String utilities |

**Test dependencies:** spring-boot-starter-test, reactor-test, spring-security-test, testcontainers-postgresql (1.20.6), testcontainers-junit-jupiter (1.20.6)

**Build plugins:** spring-boot-maven-plugin, maven-compiler-plugin (Java 21), jacoco-maven-plugin (0.8.12), maven-surefire-plugin (with JVM add-opens for Mockito). GraalVM native-maven-plugin available via `native` profile.

```
Build:   ./mvnw clean compile -DskipTests
Test:    ./mvnw test
Run:     ./mvnw spring-boot:run
Package: ./mvnw clean package -DskipTests
Native:  ./mvnw clean package -Pnative -DskipTests
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Default profile `dev`, server port 8080. MCP server config (SSE endpoint `/mcp/sse`). Dev profile: PostgreSQL at `localhost:5432/myoffgridai`, Hibernate DDL `update`, Flyway disabled. Inference provider defaults to `lmstudio` at `localhost:1234`. Ollama at `localhost:11434`. Fortress and AP mode: mock=true. Rate limiting enabled. MQTT disabled in dev. Multipart max 2048MB.
- **`application-prod.yml`** — Hibernate DDL `validate`, Flyway enabled. All inference settings from env vars (`INFERENCE_PROVIDER`, `INFERENCE_BASE_URL`, `INFERENCE_MODEL`, etc.). HuggingFace models dir from `HF_MODELS_DIR`. Fortress/AP mock=false.
- **`application-test.yml`** — Flyway disabled, Hibernate DDL `create-drop`. Rate limiting disabled. MQTT disabled. Library dirs use `java.io.tmpdir`.
- **`logback-spring.xml`** — Dev: human-readable console (`HH:mm:ss.SSS [thread] level logger [requestId] — msg`). Prod/Test: structured JSON via LogstashEncoder with MDC keys (requestId, username, userId).
- **`Dockerfile`** — Multi-stage build. Build: `eclipse-temurin:21-jdk-alpine`. Runtime: `eclipse-temurin:21-jre-alpine`. Non-root user `myoffgridai`. HEALTHCHECK on `/api/system/status`. Exposes port 8080.

**Connection map:**
```
Database: PostgreSQL, localhost:5432, database: myoffgridai
Cache: None
Message Broker: MQTT (Mosquitto) via Eclipse Paho, localhost:1883 (disabled in dev)
External APIs:
  - Ollama LLM: localhost:11434 (embeddings + chat)
  - LM Studio: localhost:1234 (OpenAI-compatible inference)
  - HuggingFace Hub API: https://huggingface.co/api (model catalog)
  - Brave Search API: https://api.search.brave.com (web enrichment, optional)
  - Anthropic Claude API: https://api.anthropic.com (summarization, optional)
  - Project Gutenberg: https://gutendex.com (ebook search)
  - Kiwix: localhost:8888 (ZIM file serving)
  - Calibre Content Server: localhost:8081 (ebook metadata)
Cloud Services: None (fully offline-capable)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `com.myoffgridai.MyOffGridAiApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`
- **Startup initialization:**
  - `VectorStoreConfig` — verifies pgvector extension is installed
  - `JpaConfig` — enables JPA auditing (`@EnableJpaAuditing`)
  - `SkillSeederService` — seeds 6 built-in skill definitions into the database
  - `SensorStartupService` — resumes polling for all sensors marked active
  - `ApModeStartupService` — checks system initialization, starts AP mode if not initialized
  - `ModelHealthCheckService` — checks inference provider availability on startup
- **Scheduled tasks:**
  - `NightlyInsightJob` — `0 0 3 * * *` (3am daily) — generates proactive insights for all active users
  - `SummarizationService` — `0 0 2 * * *` (2am daily) — auto-summarizes old conversations into memories
  - `SystemHealthMonitor` — every 5 minutes (configurable) — checks disk space, Ollama availability, heap usage
  - `UsbResetWatcherService` — every 30 seconds — monitors USB mount for factory reset trigger
- **Health check:** `GET /api/system/status` returns `SystemStatusDto` with initialization state, inference provider status, user counts, and version info

---

## 6. Entity / Data Model Layer

### === Conversation.java ===
Table: `conversations`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - user: User @ManyToOne(fetch=LAZY) @JoinColumn("user_id", nullable=false)
  - title: String (255)
  - isArchived: boolean (default false)
  - messageCount: int (default 0)
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

Relationships:
  - @ManyToOne → User (JoinColumn = "user_id")

### === Message.java ===
Table: `messages`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - conversation: Conversation @ManyToOne(fetch=LAZY) @JoinColumn("conversation_id", nullable=false)
  - role: MessageRole @Enumerated(STRING)
  - content: String @Column(columnDefinition="TEXT")
  - promptTokens: Integer
  - completionTokens: Integer
  - ragContextUsed: boolean (default false)
  - thinkingContent: String @Column(columnDefinition="TEXT")
  - modelName: String
  - generationTimeMs: Long
  - createdAt: Instant @CreatedDate

Relationships:
  - @ManyToOne → Conversation (JoinColumn = "conversation_id")

### === User.java ===
Table: `users`
Primary Key: `id` UUID (GenerationType.UUID)
Implements: UserDetails

Fields:
  - username: String @Column(unique=true, nullable=false, length=50)
  - email: String (255)
  - displayName: String (100)
  - passwordHash: String @Column(nullable=false)
  - role: Role @Enumerated(STRING) @Column(nullable=false)
  - isActive: boolean (default true)
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate
  - lastLoginAt: Instant

### === ScheduledEvent.java ===
Table: `scheduled_events`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false, length=200)
  - description: String @Column(columnDefinition="TEXT")
  - eventType: EventType @Enumerated(STRING) @Column(nullable=false)
  - isEnabled: boolean (default true)
  - cronExpression: String (100)
  - recurringIntervalMinutes: Integer
  - sensorId: UUID
  - thresholdOperator: ThresholdOperator @Enumerated(STRING)
  - thresholdValue: Double
  - actionType: ActionType @Enumerated(STRING)
  - actionPayload: String @Column(columnDefinition="TEXT")
  - nextFireAt: Instant
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === KnowledgeDocument.java ===
Table: `knowledge_documents`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - filename: String @Column(nullable=false)
  - displayName: String
  - mimeType: String
  - storagePath: String
  - fileSizeBytes: Long
  - status: DocumentStatus @Enumerated(STRING)
  - errorMessage: String @Column(columnDefinition="TEXT")
  - chunkCount: int (default 0)
  - content: String @Column(columnDefinition="TEXT")
  - uploadedAt: Instant @CreatedDate
  - processedAt: Instant

### === KnowledgeChunk.java ===
Table: `knowledge_chunks`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - document: KnowledgeDocument @ManyToOne(fetch=LAZY) @JoinColumn("document_id", nullable=false)
  - userId: UUID @Column(nullable=false)
  - chunkIndex: int
  - content: String @Column(columnDefinition="TEXT", nullable=false)
  - pageNumber: Integer
  - createdAt: Instant @CreatedDate

Relationships:
  - @ManyToOne → KnowledgeDocument (JoinColumn = "document_id")

### === Ebook.java ===
Table: `ebooks`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - title: String @Column(nullable=false)
  - author: String
  - description: String @Column(columnDefinition="TEXT")
  - isbn: String
  - publisher: String
  - publishedYear: Integer
  - language: String (default "en")
  - format: EbookFormat @Enumerated(STRING) @Column(nullable=false)
  - filePath: String @Column(nullable=false)
  - fileSizeBytes: Long
  - coverImagePath: String
  - gutenbergId: String @Column(unique=true)
  - downloadCount: int (default 0)
  - uploadedBy: UUID
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === ZimFile.java ===
Table: `zim_files`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - filename: String @Column(unique=true, nullable=false)
  - displayName: String @Column(nullable=false)
  - description: String @Column(columnDefinition="TEXT")
  - language: String (default "en")
  - category: String
  - fileSizeBytes: Long
  - articleCount: Long
  - mediaCount: Long
  - creationDate: String
  - filePath: String @Column(nullable=false)
  - kiwixBookId: String
  - uploadedBy: UUID
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === McpApiToken.java ===
Table: `mcp_api_tokens`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - tokenHash: String @Column(nullable=false)
  - name: String @Column(nullable=false, length=100)
  - createdBy: UUID @Column(nullable=false)
  - lastUsedAt: Instant
  - isActive: boolean (default true)
  - createdAt: Instant @CreatedDate

### === Memory.java ===
Table: `memories`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - content: String @Column(columnDefinition="TEXT", nullable=false)
  - importance: MemoryImportance @Enumerated(STRING) (default MEDIUM)
  - tags: String
  - sourceConversationId: UUID
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate
  - lastAccessedAt: Instant
  - accessCount: int (default 0)

### === VectorDocument.java ===
Table: `vector_document`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - content: String @Column(columnDefinition="TEXT")
  - embedding: float[] @Column(columnDefinition="vector(768)")
  - sourceType: VectorSourceType @Enumerated(STRING)
  - sourceId: UUID
  - metadata: String @Column(columnDefinition="TEXT")
  - createdAt: Instant @CreatedDate

### === DeviceRegistration.java ===
Table: `device_registrations`
Primary Key: `id` UUID (GenerationType.UUID)
Unique constraint: (userId, deviceId)

Fields:
  - userId: UUID @Column(nullable=false)
  - deviceId: String @Column(nullable=false, length=100)
  - deviceName: String (100)
  - platform: String (50)
  - mqttClientId: String (100)
  - lastSeenAt: Instant
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === AuditLog.java ===
Table: `audit_logs`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID
  - username: String
  - action: String @Column(nullable=false)
  - resourceType: String
  - resourceId: String
  - httpMethod: String
  - requestPath: String
  - ipAddress: String
  - userAgent: String
  - responseStatus: Integer
  - outcome: AuditOutcome @Enumerated(STRING) @Column(nullable=false)
  - durationMs: Long
  - timestamp: Instant @CreatedDate

### === Insight.java ===
Table: `insights`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - content: String @Column(columnDefinition="TEXT", nullable=false)
  - category: InsightCategory @Enumerated(STRING)
  - isRead: boolean (default false)
  - isDismissed: boolean (default false)
  - generatedAt: Instant @CreatedDate
  - readAt: Instant

### === Notification.java ===
Table: `notifications`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - title: String @Column(nullable=false)
  - body: String @Column(columnDefinition="TEXT")
  - type: NotificationType @Enumerated(STRING) @Column(nullable=false)
  - severity: NotificationSeverity @Enumerated(STRING) (default INFO)
  - isRead: boolean (default false)
  - createdAt: Instant @CreatedDate
  - readAt: Instant
  - mqttDelivered: boolean (default false)
  - metadata: String @Column(columnDefinition="TEXT")

### === Sensor.java ===
Table: `sensors`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false, length=100)
  - type: SensorType @Enumerated(STRING) @Column(nullable=false)
  - portPath: String @Column(unique=true, nullable=false)
  - baudRate: int (default 9600)
  - dataFormat: DataFormat @Enumerated(STRING) (default CSV_LINE)
  - valueField: String (50)
  - unit: String (20)
  - isActive: boolean (default false)
  - pollIntervalSeconds: int (default 30)
  - lowThreshold: Double
  - highThreshold: Double
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === SensorReading.java ===
Table: `sensor_readings`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - sensor: Sensor @ManyToOne(fetch=LAZY) @JoinColumn("sensor_id", nullable=false)
  - value: double
  - rawData: String
  - recordedAt: Instant @CreatedDate

Relationships:
  - @ManyToOne → Sensor (JoinColumn = "sensor_id")

### === ExternalApiSettings.java ===
Table: `external_api_settings`
Primary Key: `id` UUID (GenerationType.UUID)
Singleton pattern via `singletonGuard` @Column(unique=true)

Fields:
  - singletonGuard: String (default "SINGLETON")
  - anthropicApiKey: String (encrypted via AES-256-GCM)
  - anthropicModel: String (default "claude-sonnet-4-20250514")
  - anthropicMaxTokens: int (default 1024)
  - braveApiKey: String (encrypted)
  - braveMaxResults: int (default 5)
  - huggingFaceToken: String (encrypted)
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === InventoryItem.java ===
Table: `inventory_items`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false)
  - category: InventoryCategory @Enumerated(STRING) @Column(nullable=false)
  - quantity: double
  - unit: String
  - notes: String @Column(columnDefinition="TEXT")
  - lowStockThreshold: Double
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === PlannedTask.java ===
Table: `planned_tasks`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - userId: UUID @Column(nullable=false)
  - goalDescription: String @Column(columnDefinition="TEXT")
  - title: String @Column(nullable=false)
  - steps: String @Column(columnDefinition="TEXT")
  - estimatedResources: String @Column(columnDefinition="TEXT")
  - status: TaskStatus @Enumerated(STRING) (default ACTIVE)
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === Skill.java ===
Table: `skills`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - name: String @Column(unique=true, nullable=false)
  - displayName: String @Column(nullable=false)
  - description: String @Column(columnDefinition="TEXT")
  - version: String
  - author: String
  - category: SkillCategory @Enumerated(STRING)
  - isEnabled: boolean (default true)
  - isBuiltIn: boolean (default false)
  - parametersSchema: String @Column(columnDefinition="TEXT")
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

### === SkillExecution.java ===
Table: `skill_executions`
Primary Key: `id` UUID (GenerationType.UUID)

Fields:
  - skill: Skill @ManyToOne(fetch=LAZY) @JoinColumn("skill_id", nullable=false)
  - userId: UUID @Column(nullable=false)
  - status: ExecutionStatus @Enumerated(STRING) @Column(nullable=false)
  - inputParams: String @Column(columnDefinition="TEXT")
  - outputResult: String @Column(columnDefinition="TEXT")
  - errorMessage: String @Column(columnDefinition="TEXT")
  - startedAt: Instant @CreatedDate
  - completedAt: Instant
  - durationMs: Long

Relationships:
  - @ManyToOne → Skill (JoinColumn = "skill_id")

### === SystemConfig.java ===
Table: `system_config`
Primary Key: `id` UUID (GenerationType.UUID)
Singleton (single row)

Fields:
  - initialized: boolean (default false)
  - instanceName: String (default "MyOffGridAI")
  - fortressEnabled: boolean (default false)
  - fortressEnabledByUserId: UUID
  - fortressEnabledAt: Instant
  - apModeActive: boolean (default true)
  - wifiSsid: String
  - wifiConfigured: boolean (default false)
  - aiModelName: String
  - aiTemperature: Double (default 0.7)
  - aiSimilarityThreshold: Double (default 0.3)
  - aiMemoryTopK: Integer (default 5)
  - aiMaxContextTokens: Integer (default 4096)
  - knowledgeStoragePath: String (default "/var/myoffgridai/knowledge")
  - maxUploadSizeMb: Integer (default 2048)
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

---

## 7. Enum Inventory

```
=== MessageRole.java ===
Values: USER, ASSISTANT, SYSTEM
Used in: Message

=== Role.java ===
Values: ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD
Used in: User
Has display label: NO

=== ActionType.java ===
Values: PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY
Used in: ScheduledEvent

=== EventType.java ===
Values: SCHEDULED, SENSOR_THRESHOLD, RECURRING
Used in: ScheduledEvent

=== ThresholdOperator.java ===
Values: ABOVE, BELOW, EQUALS
Used in: ScheduledEvent

=== DocumentStatus.java ===
Values: PENDING, PROCESSING, READY, FAILED
Used in: KnowledgeDocument

=== EbookFormat.java ===
Values: EPUB, PDF, MOBI, AZW, TXT, HTML
Used in: Ebook

=== MemoryImportance.java ===
Values: LOW, MEDIUM, HIGH, CRITICAL
Used in: Memory

=== VectorSourceType.java ===
Values: MEMORY, CONVERSATION, KNOWLEDGE_CHUNK
Used in: VectorDocument

=== AuditOutcome.java ===
Values: SUCCESS, FAILURE, DENIED
Used in: AuditLog

=== InsightCategory.java ===
Values: HOMESTEAD, HEALTH, RESOURCE, GENERAL
Used in: Insight

=== NotificationSeverity.java ===
Values: INFO, WARNING, CRITICAL
Used in: Notification

=== NotificationType.java ===
Values: SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL
Used in: Notification

=== DataFormat.java ===
Values: CSV_LINE, JSON_LINE
Used in: Sensor

=== SensorType.java ===
Values: TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM
Used in: Sensor

=== ExecutionStatus.java ===
Values: RUNNING, COMPLETED, FAILED
Used in: SkillExecution

=== InventoryCategory.java ===
Values: FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER
Used in: InventoryItem

=== SkillCategory.java ===
Values: HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM
Used in: Skill

=== TaskStatus.java ===
Values: ACTIVE, COMPLETED, CANCELLED
Used in: PlannedTask

=== VectorType.java (config) ===
Purpose: Hibernate UserType for pgvector float[] mapping
Used in: VectorDocument.embedding column

=== ChunkType.java (dto) ===
Values: TEXT, THINKING, DONE, ERROR, METADATA
Used in: InferenceChunk SSE streaming
```

---

## 8. Repository Layer

### === ConversationRepository.java ===
Entity: Conversation | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByUpdatedAtDesc(UUID, Pageable): Page<Conversation>
  - findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID, boolean, Pageable): Page<Conversation>
  - findByIdAndUserId(UUID, UUID): Optional<Conversation>
  - countByUserId(UUID): long
  - findByUserId(UUID): List<Conversation>
  - findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID, String, Pageable): Page<Conversation>
  - @Modifying @Query deleteByUserId(UUID): void

### === MessageRepository.java ===
Entity: Message | Extends: JpaRepository
Custom Methods:
  - findByConversationIdOrderByCreatedAtAsc(UUID): List<Message>
  - findByConversationIdOrderByCreatedAtAsc(UUID, Pageable): Page<Message>
  - findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable): List<Message>
  - countByConversationId(UUID): long
  - deleteByConversationId(UUID): void
  - @Query countByUserId(UUID): long
  - @Modifying @Query deleteByUserId(UUID): void
  - @Modifying @Query deleteMessagesAfter(UUID, UUID): void

### === UserRepository.java ===
Entity: User | Extends: JpaRepository
Custom Methods:
  - findByUsername(String): Optional<User>
  - findByEmail(String): Optional<User>
  - existsByUsername(String): boolean
  - existsByEmail(String): boolean
  - findAllByRole(Role): List<User>
  - countByIsActiveTrue(): long
  - findByIsActiveTrue(): List<User>

### === ScheduledEventRepository.java ===
Entity: ScheduledEvent | Extends: JpaRepository
Custom Methods:
  - findAllByUserId(UUID, Pageable): Page<ScheduledEvent>
  - findByIdAndUserId(UUID, UUID): Optional<ScheduledEvent>
  - findByIsEnabledTrueAndEventType(EventType): List<ScheduledEvent>
  - findAllByUserIdOrderByCreatedAtDesc(UUID): List<ScheduledEvent>
  - deleteByUserId(UUID): void
  - countByUserId(UUID): long

### === KnowledgeChunkRepository.java ===
Entity: KnowledgeChunk | Extends: JpaRepository
Custom Methods:
  - findByDocumentIdOrderByChunkIndexAsc(UUID): List<KnowledgeChunk>
  - @Modifying deleteByDocumentId(UUID): void
  - @Modifying deleteByUserId(UUID): void
  - countByDocumentId(UUID): long

### === KnowledgeDocumentRepository.java ===
Entity: KnowledgeDocument | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByUploadedAtDesc(UUID, Pageable): Page<KnowledgeDocument>
  - findByIdAndUserId(UUID, UUID): Optional<KnowledgeDocument>
  - findByUserIdAndStatus(UUID, DocumentStatus): List<KnowledgeDocument>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long

### === EbookRepository.java ===
Entity: Ebook | Extends: JpaRepository
Custom Methods:
  - @Query searchByTitleOrAuthor(String, EbookFormat, Pageable): Page<Ebook>
  - findByGutenbergId(String): Optional<Ebook>
  - existsByGutenbergId(String): boolean

### === ZimFileRepository.java ===
Entity: ZimFile | Extends: JpaRepository
Custom Methods:
  - findByFilename(String): Optional<ZimFile>
  - findAllByOrderByDisplayNameAsc(): List<ZimFile>
  - existsByFilename(String): boolean

### === McpApiTokenRepository.java ===
Entity: McpApiToken | Extends: JpaRepository
Custom Methods:
  - findByIsActiveTrue(): List<McpApiToken>
  - findByCreatedByOrderByCreatedAtDesc(UUID): List<McpApiToken>

### === MemoryRepository.java ===
Entity: Memory | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Memory>
  - findByUserIdAndImportance(UUID, MemoryImportance, Pageable): Page<Memory>
  - findByUserIdAndTagsContaining(UUID, String, Pageable): Page<Memory>
  - findByUserId(UUID): List<Memory>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long

### === VectorDocumentRepository.java ===
Entity: VectorDocument | Extends: JpaRepository
Custom Methods:
  - findByUserIdAndSourceType(UUID, VectorSourceType): List<VectorDocument>
  - @Modifying deleteBySourceIdAndSourceType(UUID, VectorSourceType): void
  - @Modifying deleteByUserId(UUID): void
  - @Query(nativeQuery) findMostSimilar(UUID, String, String, int): List<VectorDocument> — pgvector cosine distance (`<=>`)
  - @Query(nativeQuery) findMostSimilarAcrossTypes(UUID, String, int): List<VectorDocument>

### === DeviceRegistrationRepository.java ===
Entity: DeviceRegistration | Extends: JpaRepository
Custom Methods:
  - findByUserIdAndDeviceId(UUID, String): Optional<DeviceRegistration>
  - findByUserId(UUID): List<DeviceRegistration>
  - deleteByUserIdAndDeviceId(UUID, String): void

### === AuditLogRepository.java ===
Entity: AuditLog | Extends: JpaRepository
Custom Methods:
  - findAllByOrderByTimestampDesc(Pageable): Page<AuditLog>
  - findByUserIdOrderByTimestampDesc(UUID, Pageable): Page<AuditLog>
  - findByOutcomeOrderByTimestampDesc(AuditOutcome, Pageable): Page<AuditLog>
  - findByTimestampBetweenOrderByTimestampDesc(Instant, Instant, Pageable): Page<AuditLog>
  - findByUserIdAndTimestampBetween(UUID, Instant, Instant, Pageable): Page<AuditLog>
  - countByOutcomeAndTimestampBetween(AuditOutcome, Instant, Instant): long
  - @Modifying deleteByTimestampBefore(Instant): void
  - @Modifying deleteByUserId(UUID): void

### === InsightRepository.java ===
Entity: Insight | Extends: JpaRepository
Custom Methods:
  - findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID, Pageable): Page<Insight>
  - findByUserIdAndCategoryAndIsDismissedFalse(UUID, InsightCategory, Pageable): Page<Insight>
  - findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): List<Insight>
  - countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Insight>
  - countByUserId(UUID): long
  - @Modifying deleteByUserId(UUID): void

### === NotificationRepository.java ===
Entity: Notification | Extends: JpaRepository
Custom Methods:
  - findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID): List<Notification>
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Notification>
  - countByUserIdAndIsReadFalse(UUID): long
  - findByIdAndUserId(UUID, UUID): Optional<Notification>
  - @Modifying @Query markAllReadForUser(UUID, Instant): void
  - @Modifying deleteByUserId(UUID): void

### === SensorReadingRepository.java ===
Entity: SensorReading | Extends: JpaRepository
Custom Methods:
  - findBySensorIdOrderByRecordedAtDesc(UUID, Pageable): Page<SensorReading>
  - findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID, Instant): List<SensorReading>
  - findTopBySensorIdOrderByRecordedAtDesc(UUID): Optional<SensorReading>
  - @Modifying deleteBySensorId(UUID): void
  - @Modifying @Query deleteByUserId(UUID): void
  - @Query(nativeQuery) findAverageValueSince(UUID, Instant): Double

### === SensorRepository.java ===
Entity: Sensor | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<Sensor>
  - findByIdAndUserId(UUID, UUID): Optional<Sensor>
  - findByUserIdAndIsActiveTrue(UUID): List<Sensor>
  - findByPortPath(String): Optional<Sensor>
  - findByIsActiveTrue(): List<Sensor>
  - countByUserId(UUID): long
  - deleteByUserId(UUID): void

### === ExternalApiSettingsRepository.java ===
Entity: ExternalApiSettings | Extends: JpaRepository
Custom Methods:
  - findBySingletonGuard(String): Optional<ExternalApiSettings>

### === InventoryItemRepository.java ===
Entity: InventoryItem | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<InventoryItem>
  - findByUserIdAndCategory(UUID, InventoryCategory): List<InventoryItem>
  - findByUserIdAndQuantityLessThanEqual(UUID, double): List<InventoryItem>
  - findByIdAndUserId(UUID, UUID): Optional<InventoryItem>
  - @Modifying deleteByUserId(UUID): void

### === PlannedTaskRepository.java ===
Entity: PlannedTask | Extends: JpaRepository
Custom Methods:
  - findByUserIdAndStatusOrderByCreatedAtDesc(UUID, TaskStatus, Pageable): Page<PlannedTask>
  - findByIdAndUserId(UUID, UUID): Optional<PlannedTask>
  - @Modifying deleteByUserId(UUID): void

### === SkillExecutionRepository.java ===
Entity: SkillExecution | Extends: JpaRepository
Custom Methods:
  - findByUserIdOrderByStartedAtDesc(UUID, Pageable): Page<SkillExecution>
  - findBySkillIdAndUserIdOrderByStartedAtDesc(UUID, UUID, Pageable): Page<SkillExecution>
  - findByUserIdAndStatus(UUID, ExecutionStatus): List<SkillExecution>

### === SkillRepository.java ===
Entity: Skill | Extends: JpaRepository
Custom Methods:
  - findByIsEnabledTrue(): List<Skill>
  - findByIsBuiltInTrue(): List<Skill>
  - findByCategory(SkillCategory): List<Skill>
  - findByName(String): Optional<Skill>
  - findByIsEnabledTrueOrderByDisplayNameAsc(): List<Skill>

### === SystemConfigRepository.java ===
Entity: SystemConfig | Extends: JpaRepository
Custom Methods:
  - @Query("SELECT s FROM SystemConfig s") findFirst(): Optional<SystemConfig>

---

## 9. Service Layer — Full Method Signatures

### === InferenceService.java (Interface) ===
```
Public Methods:
  - chat(OllamaChatRequest): OllamaChatResponse — synchronous chat
  - streamChat(OllamaChatRequest): Flux<String> — SSE streaming chat
  - embed(String): float[] — generate embedding vector
  - listModels(): List<OllamaModelInfo> — list available models
  - isAvailable(): boolean — health check
  - getActiveModel(): ActiveModelDto — currently loaded model
```

### === OllamaInferenceService.java ===
Injects: OllamaService
Implements: InferenceService — thin wrapper delegating to OllamaService

### === LmStudioInferenceService.java ===
Injects: WebClient (sse), RestClient (sync), modelName, embedModel, maxTokens, temperature
Implements: InferenceService — OpenAI-compatible API with think-tag parsing for reasoning models

### === OllamaService.java ===
Injects: RestClient (ollamaRestClient), WebClient (ollamaWebClient), modelName, embedModel
Public Methods:
  - chat(OllamaChatRequest): OllamaChatResponse — sync chat via RestClient
  - streamChat(OllamaChatRequest): Flux<String> — SSE streaming via WebClient
  - embed(String): float[] — embedding via /api/embed
  - listModels(): List<OllamaModelInfo> — GET /api/tags
  - isAvailable(): boolean — GET /api/tags health probe

### === ChatService.java ===
Injects: ConversationRepository, MessageRepository, InferenceService, RagService, SystemPromptBuilder, ContextWindowService, MemoryExtractionService, SystemConfigService
Public Methods:
  - createConversation(UUID, String): Conversation
  - getConversation(UUID, UUID): Conversation
  - listConversations(UUID, Boolean, Pageable): Page<Conversation>
  - searchConversations(UUID, String, Pageable): Page<Conversation>
  - renameConversation(UUID, UUID, String): Conversation
  - archiveConversation(UUID, UUID): Conversation
  - deleteConversation(UUID, UUID): void
  - sendMessage(UUID, UUID, String): Message — sync message with RAG
  - streamMessage(UUID, UUID, String): Flux<String> — SSE streaming with RAG
  - editMessage(UUID, UUID, UUID, String): Message — edit + regenerate
  - branchConversation(UUID, UUID, UUID): Conversation — branch from message
  - getMessages(UUID, UUID, Pageable): Page<Message>
  - generateTitle(UUID, UUID): Conversation — auto-title via LLM

### === AgentService.java ===
Injects: InferenceService, SkillExecutorService, SystemConfigService
Public Methods:
  - executeAgentTask(UUID, String, UUID): AgentTaskResult — tool-call dispatch via regex

### === ContextWindowService.java ===
Injects: MessageRepository, TokenCounter
Public Methods:
  - prepareContext(UUID, int): List<OllamaMessage> — message selection with token budget

### === ModelHealthCheckService.java ===
Injects: InferenceService
Startup health check for inference provider

### === SystemPromptBuilder.java ===
Injects: SystemConfigService
Public Methods:
  - buildSystemPrompt(UUID, String): String — assembles system prompt with RAG context

### === AuthService.java ===
Injects: UserRepository, JwtService, PasswordEncoder, SystemConfigService
Public Methods:
  - register(String, String, String, String, Role): AuthResponse
  - login(String, String): AuthResponse
  - refreshToken(String): AuthResponse
  - logout(String): void
  - changePassword(UUID, String, String): void

### === JwtService.java ===
Injects: secret, accessExpirationMs, refreshExpirationMs
Public Methods:
  - generateAccessToken(UUID, String, String): String
  - generateRefreshToken(UUID, String, String): String
  - extractUserId(String): UUID
  - extractUsername(String): String
  - extractRole(String): String
  - isTokenValid(String): boolean
  - blacklistToken(String): void
  - isTokenBlacklisted(String): boolean

### === UserService.java ===
Injects: UserRepository, PasswordEncoder
Public Methods:
  - getAllUsers(): List<User>
  - getUserById(UUID): User
  - getUserByUsername(String): User
  - updateUser(UUID, UpdateUserRequest): User
  - deactivateUser(UUID): void
  - activateUser(UUID): void
  - deleteUser(UUID): void
  - getActiveUserCount(): long

### === ClaudeApiService.java ===
Injects: ExternalApiSettingsService
Public Methods:
  - summarize(String, String): Optional<String> — Anthropic Claude API summarization

### === WebFetchService.java ===
Injects: ClaudeApiService
Public Methods:
  - fetchUrl(String): FetchResult — fetch + Jsoup HTML parse + optional Claude summary

### === WebSearchService.java ===
Injects: ExternalApiSettingsService, WebFetchService, KnowledgeService
Public Methods:
  - search(String, int): List<SearchResultDto> — Brave Search API
  - searchAndIngestToKnowledge(UUID, String, int): SearchEnrichmentResultDto

### === ScheduledEventService.java ===
Injects: ScheduledEventRepository
Public Methods:
  - createEvent(UUID, CreateEventRequest): ScheduledEventDto
  - getEvents(UUID, Pageable): Page<ScheduledEventDto>
  - getEvent(UUID, UUID): ScheduledEventDto
  - updateEvent(UUID, UUID, UpdateEventRequest): ScheduledEventDto
  - deleteEvent(UUID, UUID): void
  - toggleEnabled(UUID, UUID): ScheduledEventDto

### === KnowledgeService.java ===
Injects: KnowledgeDocumentRepository, KnowledgeChunkRepository, VectorDocumentRepository, FileStorageService, IngestionService, EmbeddingService, SystemConfigService
Public Methods:
  - uploadDocument(UUID, MultipartFile): KnowledgeDocumentDto
  - uploadDocumentWithContent(UUID, String, String, String): KnowledgeDocumentDto
  - getDocuments(UUID, Pageable): Page<KnowledgeDocumentDto>
  - getDocument(UUID, UUID): KnowledgeDocumentDto
  - getDocumentContent(UUID, UUID): DocumentContentDto
  - updateDisplayName(UUID, UUID, String): KnowledgeDocumentDto
  - updateContent(UUID, UUID, String): KnowledgeDocumentDto
  - deleteDocument(UUID, UUID): void
  - deleteAllForUser(UUID): void

### === ChunkingService.java ===
Public Methods:
  - chunk(String, int, int): List<String> — split text into overlapping chunks

### === FileStorageService.java ===
Injects: SystemConfigService
Public Methods:
  - store(UUID, String, byte[]): String — store file, return path
  - load(String): byte[]
  - delete(String): void

### === IngestionService.java ===
Injects: ChunkingService, OcrService
Public Methods:
  - extractText(String, byte[]): ExtractionResult — PDF/DOCX/DOC/TXT/image OCR

### === OcrService.java ===
Public Methods:
  - extractText(byte[], String): String — Tesseract OCR

### === SemanticSearchService.java ===
Injects: VectorDocumentRepository, EmbeddingService, KnowledgeChunkRepository, KnowledgeDocumentRepository
Public Methods:
  - search(UUID, String, int): List<KnowledgeSearchResultDto>
  - searchForRagContext(UUID, String, int): List<String> — with source attribution

### === StorageHealthService.java ===
Injects: SystemConfigService
Public Methods:
  - getStoragePath(): String
  - getUsableSpaceMb(): long
  - isHealthy(): boolean

### === EbookService.java ===
Injects: EbookRepository, LibraryProperties, CalibreConversionService
Public Methods:
  - listEbooks(String, EbookFormat, Pageable): Page<EbookDto>
  - getEbook(UUID): EbookDto
  - uploadEbook(MultipartFile, String, String): EbookDto
  - downloadEbook(UUID): byte[]
  - deleteEbook(UUID): void
  - incrementDownloadCount(UUID): void

### === GutenbergService.java ===
Injects: EbookRepository, LibraryProperties, EbookService
Public Methods:
  - searchGutenberg(String, int): List<GutenbergBookDto>
  - downloadFromGutenberg(String): EbookDto

### === ZimFileService.java ===
Injects: ZimFileRepository, LibraryProperties
Public Methods:
  - listZimFiles(): List<ZimFileDto>
  - uploadZimFile(MultipartFile, String, String): ZimFileDto
  - deleteZimFile(UUID): void
  - getKiwixStatus(): KiwixStatusDto

### === CalibreConversionService.java ===
Public Methods:
  - convertToEpub(Path): Path — ebook-convert CLI wrapper

### === McpTokenService.java ===
Injects: McpApiTokenRepository, PasswordEncoder
Public Methods:
  - createToken(UUID, String): McpTokenCreateResult
  - listTokens(UUID): List<McpTokenSummaryDto>
  - revokeToken(UUID, UUID): void
  - validateToken(String): Optional<McpAuthentication>

### === McpToolsService.java ===
Injects: ChatService, MemoryService, KnowledgeService, SensorService
Public Methods:
  - registerTools(): List<ToolCallback> — MCP tool definitions for external AI clients

### === EmbeddingService.java ===
Injects: InferenceService
Public Methods:
  - embed(String): float[]
  - cosineSimilarity(float[], float[]): float
  - static formatEmbedding(float[]): String — pgvector format

### === MemoryExtractionService.java ===
Injects: InferenceService, MemoryService, SystemConfigService
Public Methods:
  - extractMemoriesFromMessage(UUID, String, UUID): void — async LLM extraction

### === MemoryService.java ===
Injects: MemoryRepository, VectorDocumentRepository, EmbeddingService, SystemConfigService
Public Methods:
  - createMemory(UUID, String, MemoryImportance, String, UUID): Memory
  - findRelevantMemories(UUID, String, int): List<Memory>
  - searchMemoriesWithScores(UUID, String, int): List<MemorySearchResultDto>
  - getMemory(UUID, UUID): Memory
  - updateImportance(UUID, UUID, MemoryImportance): Memory
  - updateTags(UUID, UUID, String): Memory
  - deleteMemory(UUID, UUID): void
  - deleteAllMemoriesForUser(UUID): void
  - exportMemories(UUID): List<Memory>
  - getMemories(UUID, MemoryImportance, String, Pageable): Page<Memory>

### === RagService.java ===
Injects: MemoryService, SemanticSearchService, SystemConfigService
Public Methods:
  - buildRagContext(UUID, String): RagContext
  - formatContextBlock(RagContext): String

### === SummarizationService.java ===
Injects: ConversationRepository, MessageRepository, OllamaService, MemoryService, MemoryRepository, SystemConfigService
Public Methods:
  - summarizeConversation(UUID, UUID): Memory
  - @Scheduled scheduledNightlySummarization(): void — cron 0 0 2 * * *

### === ModelCatalogService.java ===
Injects: WebClient, ExternalApiSettingsService, ObjectMapper
Public Methods:
  - searchModels(String, String, int): HfSearchResultDto
  - getModelDetails(String): HfModelDto
  - getModelFiles(String): List<HfModelFileDto>

### === ModelDownloadService.java ===
Injects: WebClient, ExternalApiSettingsService, ModelDownloadProgressRegistry, InferenceService
Public Methods:
  - startDownload(String, String): String — returns downloadId
  - getProgress(String): Optional<DownloadProgress>
  - getAllDownloads(): List<DownloadProgress>
  - cancelDownload(String): void
  - listLocalModels(): List<LocalModelFileDto>
  - deleteLocalModel(String): void
  - @Async executeDownload(String, String, String, String): void

### === ModelDownloadProgressRegistry.java ===
Public Methods:
  - subscribe(String): SseEmitter
  - emit(String, DownloadProgress): void
  - complete(String): void

### === DeviceRegistrationService.java ===
Injects: DeviceRegistrationRepository
Public Methods:
  - registerDevice(UUID, RegisterDeviceRequest): DeviceRegistrationDto
  - getDevicesForUser(UUID): List<DeviceRegistrationDto>
  - unregisterDevice(UUID, String): void
  - getTopicsForUser(UUID): List<String>

### === MqttPublisherService.java ===
Injects: MqttClient (nullable), ObjectMapper
Public Methods:
  - publishToTopic(String, NotificationPayload): boolean
  - publishToUser(String, NotificationPayload): boolean
  - publishBroadcast(NotificationPayload): boolean

### === AuditService.java ===
Injects: AuditLogRepository
Public Methods:
  - logAction(AuditLog): void
  - getAuditLogs(Pageable): Page<AuditLog>
  - getAuditLogsForUser(UUID, Pageable): Page<AuditLog>
  - getAuditLogsByOutcome(AuditOutcome, Pageable): Page<AuditLog>
  - getAuditLogsBetween(Instant, Instant, Pageable): Page<AuditLog>
  - countByOutcomeBetween(AuditOutcome, Instant, Instant): long
  - deleteByUserId(UUID): void

### === DataExportService.java ===
Injects: ConversationRepository, MessageRepository, MemoryRepository
Public Methods:
  - exportUserData(UUID, String): byte[] — AES-256-GCM encrypted ZIP

### === DataWipeService.java ===
Injects: MessageRepository, ConversationRepository, MemoryService, KnowledgeService, SensorService, InsightService, NotificationService, InventoryItemRepository, PlannedTaskRepository, AuditService
Public Methods:
  - @Transactional wipeUser(UUID): WipeResult — cascade delete in FK order (10 steps)

### === FortressService.java ===
Injects: SystemConfigService, UserRepository, mockMode flag
Public Methods:
  - enable(UUID): void — iptables block outbound
  - disable(UUID): void — iptables flush
  - getFortressStatus(): FortressStatus
  - isFortressActive(): boolean

### === SovereigntyReportService.java ===
Injects: FortressService, AuditService, multiple repositories
Public Methods:
  - generateReport(UUID): SovereigntyReport

### === InsightGeneratorService.java ===
Injects: PatternAnalysisService, OllamaService, InsightRepository, NotificationService, SystemConfigService
Public Methods:
  - generateInsightForUser(UUID): List<Insight>

### === InsightService.java ===
Injects: InsightRepository
Public Methods:
  - getInsights(UUID, Pageable): Page<Insight>
  - getInsightsByCategory(UUID, InsightCategory, Pageable): Page<Insight>
  - getUnreadInsights(UUID): List<Insight>
  - markRead(UUID, UUID): Insight
  - dismiss(UUID, UUID): Insight
  - getUnreadCount(UUID): long
  - deleteAllForUser(UUID): void

### === NightlyInsightJob.java ===
Injects: UserRepository, InsightGeneratorService
@Scheduled(cron = "0 0 3 * * *") generateNightlyInsights(): void

### === NotificationService.java ===
Injects: NotificationRepository, NotificationSseRegistry, MqttPublisherService, DeviceRegistrationService
Public Methods:
  - createNotification(UUID, String, String, NotificationType, String): Notification
  - createNotification(UUID, String, String, NotificationType, NotificationSeverity, String): Notification
  - getUnreadNotifications(UUID): List<Notification>
  - getNotifications(UUID, Pageable): Page<Notification>
  - markRead(UUID, UUID): Notification
  - markAllRead(UUID): void
  - getUnreadCount(UUID): long
  - deleteNotification(UUID, UUID): void
  - deleteAllForUser(UUID): void

### === NotificationSseRegistry.java ===
Public Methods:
  - register(UUID, SseEmitter): void
  - broadcast(UUID, Notification): void
  - broadcastUnreadCount(UUID, long): void

### === PatternAnalysisService.java ===
Injects: ConversationRepository, MemoryRepository, SensorRepository, SensorReadingRepository, InventoryItemRepository, PlannedTaskRepository
Public Methods:
  - buildPatternSummary(UUID): PatternSummary

### === SystemHealthMonitor.java ===
Injects: OllamaService, UserRepository, NotificationService, SystemConfigService
@Scheduled(fixedDelay) checkSystemHealth(): void — disk, Ollama, heap monitoring

### === SensorService.java ===
Injects: SensorRepository, SensorReadingRepository, SensorPollingService, SerialPortService, SseEmitterRegistry
Public Methods:
  - registerSensor(UUID, CreateSensorRequest): SensorDto
  - getSensors(UUID): List<SensorDto>
  - getSensor(UUID, UUID): SensorDto
  - updateThresholds(UUID, UUID, UpdateThresholdsRequest): SensorDto
  - deleteSensor(UUID, UUID): void
  - startPolling(UUID, UUID): SensorDto
  - stopPolling(UUID, UUID): SensorDto
  - testConnection(UUID, TestSensorRequest): SensorTestResult
  - getReadings(UUID, UUID, Pageable): Page<SensorReadingDto>
  - getRecentReadings(UUID, UUID, int): List<SensorReadingDto>
  - subscribeToSensorUpdates(UUID): SseEmitter
  - getAvailablePorts(): List<String>
  - deleteAllForUser(UUID): void

### === SensorPollingService.java ===
Injects: SerialPortService, SensorReadingRepository, SseEmitterRegistry, NotificationService
Manages per-sensor polling threads with threshold alerting

### === SensorStartupService.java ===
@PostConstruct — resumes polling for active sensors

### === SerialPortService.java ===
Public Methods:
  - listPorts(): List<String>
  - connect(String, int): SerialPort
  - readLine(SerialPort): Optional<String>
  - testPort(String, int): boolean

### === SseEmitterRegistry.java ===
Public Methods:
  - register(UUID, SseEmitter): void
  - emit(UUID, SensorReadingDto): void

### === ExternalApiSettingsService.java ===
Injects: ExternalApiSettingsRepository, AesEncryptionUtil
Public Methods:
  - getSettings(): ExternalApiSettingsDto (decrypted read)
  - updateSettings(UpdateExternalApiSettingsRequest): ExternalApiSettingsDto
  - getAnthropicApiKey(): Optional<String>
  - getBraveApiKey(): Optional<String>
  - getHuggingFaceToken(): Optional<String>

### === SkillExecutorService.java ===
Injects: SkillRepository, SkillExecutionRepository, builtInSkills (Map)
Public Methods:
  - executeSkill(UUID, String, Map<String,String>): SkillExecutionDto
  - getExecutionHistory(UUID, Pageable): Page<SkillExecutionDto>

### === SkillSeederService.java ===
@PostConstruct — seeds 6 built-in skills with JSON schemas

### === Built-in Skills (6) ===
  - DocumentSummarizerSkill — LLM-powered document summarization
  - InventoryTrackerSkill — CRUD for homestead inventory
  - RecipeGeneratorSkill — LLM recipe generation from inventory
  - ResourceCalculatorSkill — pure-math off-grid calculations (power/water/food)
  - TaskPlannerSkill — LLM task decomposition + CRUD
  - WeatherQuerySkill — sensor data aggregation for weather

### === SystemConfigService.java ===
Injects: SystemConfigRepository
Public Methods:
  - getConfig(): SystemConfig
  - isInitialized(): boolean
  - initialize(InitializeRequest, UUID): SystemConfig
  - setFortressEnabled(boolean, UUID): void
  - updateAiSettings(AiSettingsDto): SystemConfig
  - updateStorageSettings(StorageSettingsDto): SystemConfig
  - getAiSettings(): AiSettingsDto

### === ApModeService.java ===
Injects: mockMode flag
Public Methods:
  - startApMode(): void — hostapd/dnsmasq control
  - stopApMode(): void
  - scanWifiNetworks(): List<WifiNetwork>
  - connectToWifi(WifiConnectRequest): WifiConnectionStatus
  - getApStatus(): boolean

### === ApModeStartupService.java ===
@PostConstruct — conditional AP mode start

### === FactoryResetService.java ===
Injects: SystemConfigService, ApModeService, DataSource
Public Methods:
  - @Async performReset(FactoryResetRequest): void — drops all tables, restarts AP
  - triggerUsbReset(): void

### === NetworkTransitionService.java ===
Injects: ApModeService, SystemConfigService
Public Methods:
  - completeSetup(UUID, String): void — AP→normal network transition

### === UsbResetWatcherService.java ===
@Scheduled(fixedDelay=30000) — checks /mnt/usb for reset trigger file

---

## 10. Controller / API Layer — Method Signatures Only

### === ChatController.java ===
Base Path: /api/chat
Injects: ChatService
Endpoints:
  - createConversation() → chatService.createConversation()
  - listConversations() → chatService.listConversations()
  - getConversation() → chatService.getConversation()
  - searchConversations() → chatService.searchConversations()
  - renameConversation() → chatService.renameConversation()
  - archiveConversation() → chatService.archiveConversation()
  - deleteConversation() → chatService.deleteConversation()
  - sendMessage() → chatService.sendMessage()
  - streamMessage() → chatService.streamMessage() [SSE]
  - editMessage() → chatService.editMessage()
  - branchConversation() → chatService.branchConversation()
  - getMessages() → chatService.getMessages()
  - generateTitle() → chatService.generateTitle()

### === ModelController.java ===
Base Path: /api/models
Injects: InferenceService
Endpoints:
  - listModels() → inferenceService.listModels()
  - getHealth() → inferenceService.isAvailable()
  - getActiveModel() → inferenceService.getActiveModel()

### === AuthController.java ===
Base Path: /api/auth
Injects: AuthService
Endpoints:
  - register() → authService.register()
  - login() → authService.login()
  - refreshToken() → authService.refreshToken()
  - logout() → authService.logout()

### === UserController.java ===
Base Path: /api/users
Injects: UserService
Endpoints:
  - getAllUsers() → userService.getAllUsers() [OWNER/ADMIN only]
  - getUserById() → userService.getUserById()
  - getCurrentUser() → returns authenticated user
  - updateUser() → userService.updateUser()
  - deactivateUser() → userService.deactivateUser()
  - activateUser() → userService.activateUser()
  - deleteUser() → userService.deleteUser() [OWNER only]

### === EnrichmentController.java ===
Base Path: /api/enrichment
Injects: WebFetchService, WebSearchService
Endpoints:
  - fetchUrl() → webFetchService.fetchUrl()
  - searchWeb() → webSearchService.search()
  - searchAndIngest() → webSearchService.searchAndIngestToKnowledge()

### === ScheduledEventController.java ===
Base Path: /api/events
Injects: ScheduledEventService
Endpoints: CRUD for scheduled events (create, list, get, update, delete, toggle)

### === KnowledgeController.java ===
Base Path: /api/knowledge
Injects: KnowledgeService, SemanticSearchService
Endpoints:
  - uploadDocument() → knowledgeService.uploadDocument()
  - listDocuments() → knowledgeService.getDocuments()
  - getDocument() → knowledgeService.getDocument()
  - getDocumentContent() → knowledgeService.getDocumentContent()
  - updateDisplayName() → knowledgeService.updateDisplayName()
  - updateContent() → knowledgeService.updateContent()
  - deleteDocument() → knowledgeService.deleteDocument()
  - searchKnowledge() → semanticSearchService.search()

### === LibraryController.java ===
Base Path: /api/library
Injects: EbookService, ZimFileService, GutenbergService
Endpoints: Ebook CRUD, ZIM file management, Gutenberg search/download, Kiwix status

### === McpDiscoveryController.java ===
Base Path: /api/mcp
Endpoints: getClientConfig() — returns MCP server connection details

### === McpTokenController.java ===
Base Path: /api/mcp/tokens
Injects: McpTokenService
Endpoints: createToken(), listTokens(), revokeToken()

### === MemoryController.java ===
Base Path: /api/memories
Injects: MemoryService
Endpoints: CRUD for memories, vector search, importance/tag updates

### === ModelDownloadController.java ===
Base Path: /api/models
Injects: ModelCatalogService, ModelDownloadService, ModelDownloadProgressRegistry
Endpoints: HuggingFace search, model details/files, start/cancel download, progress SSE, local models, delete

### === DeviceRegistrationController.java ===
Base Path: /api/devices
Injects: DeviceRegistrationService
Endpoints: register, list, unregister devices

### === PrivacyController.java ===
Base Path: /api/privacy
Injects: FortressService, AuditService, DataExportService, DataWipeService, SovereigntyReportService
Endpoints: fortress enable/disable/status, audit logs, data export, data wipe, sovereignty report

### === ProactiveController.java ===
Base Path: /api/proactive
Injects: InsightService, InsightGeneratorService, NotificationService, NotificationSseRegistry
Endpoints: insights CRUD, notifications CRUD, SSE streams, generate insights

### === SensorController.java ===
Base Path: /api/sensors
Injects: SensorService
Endpoints: sensor CRUD, polling control, test connection, readings, SSE stream, available ports

### === ExternalApiSettingsController.java ===
Base Path: /api/settings/external-apis
Injects: ExternalApiSettingsService
Endpoints: getSettings(), updateSettings()

### === SkillController.java ===
Base Path: /api/skills
Injects: SkillExecutorService, SkillRepository, InventoryItemRepository
Endpoints: list skills, execute, execution history, inventory CRUD

### === CaptivePortalController.java ===
Base Path: /setup
Injects: ApModeService, NetworkTransitionService, SystemConfigService, AuthService
Endpoints: setup page, scan WiFi, connect, complete setup

### === SystemController.java ===
Base Path: /api/system
Injects: SystemConfigService, AuthService, UserService, ApModeService, FactoryResetService, InferenceService, StorageHealthService
Endpoints: status, initialize, AI settings, storage settings, factory reset

---

## 11. Security Configuration

```
Authentication: JWT (stateless, HMAC-SHA256)
Token issuer/validator: Internal (JwtService)
Password encoder: BCrypt (default rounds)

Public endpoints (no auth required):
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/refresh
  - /setup/**
  - /api/system/status
  - /api/system/initialize
  - /mcp/** (separate auth via McpAuthFilter)
  - /actuator/health
  - /swagger-ui/**, /v3/api-docs/**
  - /error

Protected endpoints (patterns):
  - DELETE /api/users/** → ROLE_OWNER only
  - GET /api/users (list all) → ROLE_OWNER, ROLE_ADMIN
  - PUT /api/users/*/activate, PUT /api/users/*/deactivate → ROLE_OWNER, ROLE_ADMIN
  - GET /api/privacy/audit → ROLE_OWNER, ROLE_ADMIN
  - POST /api/privacy/fortress/** → ROLE_OWNER, ROLE_ADMIN
  - POST /api/system/factory-reset → ROLE_OWNER
  - PUT /api/system/ai-settings → ROLE_OWNER, ROLE_ADMIN
  - All other /api/** → authenticated (any role)

CORS: Allowed origins: * (all), methods: *, headers: *

CSRF: Disabled (stateless API)

Rate limiting: Bucket4j per-IP, auth endpoints: 10 req/min, general API: 100 req/min
```

---

## 12. Custom Security Components

```
=== JwtAuthFilter.java ===
Extends: OncePerRequestFilter
Extracts token from: Authorization header (Bearer scheme)
Validates via: JwtService.isTokenValid() + blacklist check
Sets SecurityContext: YES (UsernamePasswordAuthenticationToken with role)

=== McpAuthFilter.java ===
Extends: OncePerRequestFilter
Intercepts: /mcp/** paths
Extracts token from: Authorization header (Bearer scheme)
Validates via: McpTokenService.validateToken() (BCrypt hash comparison)
Sets SecurityContext: YES (McpAuthentication)

=== RateLimitingFilter.java ===
Extends: OncePerRequestFilter
Per-IP rate limiting via Bucket4j
Auth endpoints (/api/auth/**): 10 req/min
General API (/api/**): 100 req/min (configurable)

=== CaptivePortalRedirectFilter.java ===
Extends: OncePerRequestFilter
Active only during AP mode setup
Redirects captive portal detection URLs to /setup

=== MdcFilter.java ===
Extends: OncePerRequestFilter
Populates MDC: requestId (UUID), username, userId

=== RequestResponseLoggingFilter.java ===
Extends: OncePerRequestFilter
Logs: method, path, status, duration, content-type, IP
```

---

## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler.java ===
@ControllerAdvice: YES

Exception Mappings:
  - EntityNotFoundException → 404 (ApiResponse.error)
  - DuplicateResourceException → 409 (ApiResponse.error)
  - OllamaUnavailableException → 503 (ApiResponse.error)
  - OllamaInferenceException → 502 (ApiResponse.error)
  - FortressActiveException → 403 (ApiResponse.error)
  - FortressOperationException → 500 (ApiResponse.error)
  - ApModeException → 500 (ApiResponse.error)
  - InitializationException → 409 (ApiResponse.error)
  - EmbeddingException → 500 (ApiResponse.error)
  - SkillDisabledException → 400 (ApiResponse.error)
  - StorageException → 500 (ApiResponse.error)
  - UnsupportedFileTypeException → 415 (ApiResponse.error)
  - SensorConnectionException → 502 (ApiResponse.error)
  - OcrException → 500 (ApiResponse.error)
  - AccessDeniedException → 403 (ApiResponse.error)
  - MethodArgumentNotValidException → 400 (validation errors joined)
  - MaxUploadSizeExceededException → 413 (ApiResponse.error)
  - ConstraintViolationException → 400 (ApiResponse.error)
  - HttpMessageNotReadableException → 400 (ApiResponse.error)
  - MethodArgumentTypeMismatchException → 400 (ApiResponse.error)
  - Exception (catch-all) → 500 (ApiResponse.error)

Standard error response format:
{
  "success": false,
  "message": "...",
  "data": null,
  "timestamp": "2026-03-16T...",
  "page": null,
  "totalElements": null,
  "totalPages": null
}
```

---

## 14. Mappers / DTOs

No MapStruct or ModelMapper used. All mapping is manual, typically via static factory methods (`XxxDto.from(entity)`) or inline in services. DTO structures are documented in the OpenAPI spec.

---

## 15. Utility Classes & Shared Components

```
=== AesEncryptionUtil.java ===
Methods:
  - encrypt(String): String — AES-256-GCM with random IV, Base64 output
  - decrypt(String): String — reverse of encrypt
Used by: AesAttributeConverter, ExternalApiSettingsService

=== AesAttributeConverter.java ===
JPA @Converter(autoApply=false) for transparent field-level encryption
Used by: ExternalApiSettings (API keys)

=== TokenCounter.java ===
Methods:
  - static estimateTokens(String): int — chars/4 heuristic
  - static truncateToTokenLimit(List<OllamaMessage>, int): List<OllamaMessage>
Used by: ContextWindowService, RagService

=== DeltaJsonUtils.java ===
Methods:
  - deltaToPlainText(String): String — Quill Delta JSON → plain text
  - plainTextToDelta(String): String — plain text → Quill Delta JSON
Used by: KnowledgeController (content editing)

=== ApiResponse.java ===
Generic wrapper: ApiResponse<T>
Fields: success, message, data, timestamp, page, totalElements, totalPages
Factory methods: success(T), success(T, String), success(Page<T>, Function), error(String)

=== AppConstants.java ===
528+ named constants across all domains (Ollama defaults, JWT, rate limits, MQTT topics, knowledge limits, sensor defaults, model download settings, etc.)

=== AuditAspect.java ===
AOP @Around aspect on all @RestController methods
Captures: userId, action, resource, HTTP method/path, IP, user-agent, status, duration
Persists via AuditService.logAction()
```

---

## 16. Database Schema (Live)

Database not available for live schema dump. Schema managed by Hibernate DDL `update` (dev) / `validate` (prod). All entity definitions in Section 6 represent the authoritative schema.

Tables (from entity mappings):
`conversations`, `messages`, `users`, `scheduled_events`, `knowledge_documents`, `knowledge_chunks`, `ebooks`, `zim_files`, `mcp_api_tokens`, `memories`, `vector_document`, `device_registrations`, `audit_logs`, `insights`, `notifications`, `sensors`, `sensor_readings`, `external_api_settings`, `inventory_items`, `planned_tasks`, `skills`, `skill_executions`, `system_config`

pgvector extension required for `vector_document.embedding` column (vector(768)).

---

## 17. MESSAGE BROKER DETECTION

No RabbitMQ or Kafka detected. The project uses **MQTT (Eclipse Paho)** for push notifications:

```
Broker: Mosquitto MQTT (external service)
Connection: localhost:1883 (configurable via app.mqtt.broker-url)
Disabled in dev/test (app.mqtt.enabled=false)

Topics:
  - myoffgridai/sensors/readings — sensor events
  - myoffgridai/system/alerts — system alerts
  - myoffgridai/notifications — general notifications
  - myoffgridai/insights — insight events
  - /myoffgridai/{userId}/notifications — per-user notifications
  - /myoffgridai/broadcast — broadcast to all devices

Publishers:
  - MqttPublisherService.publishToTopic() → any topic
  - MqttPublisherService.publishToUser() → user topic
  - MqttPublisherService.publishBroadcast() → broadcast topic

Consumers:
  - No MQTT consumers on the server side
  - Flutter client subscribes to user-specific topics

QoS: 1 (at least once)
```

---

## 18. Cache Layer

No Redis or caching layer detected. No `@Cacheable`, `@CacheEvict`, or `CacheManager` annotations found.

---

## 19. ENVIRONMENT VARIABLE INVENTORY

```
Variable | Used In | Default | Required in Prod
---------|---------|---------|------------------
INFERENCE_PROVIDER | application-prod.yml | lmstudio | YES
INFERENCE_BASE_URL | application-prod.yml | http://localhost:1234 | YES
INFERENCE_MODEL | application-prod.yml | (model name) | YES
INFERENCE_EMBED_MODEL | application-prod.yml | nomic-embed-text | YES
INFERENCE_TIMEOUT | application-prod.yml | 120 | NO
INFERENCE_MAX_TOKENS | application-prod.yml | 4096 | NO
INFERENCE_TEMPERATURE | application-prod.yml | 0.7 | NO
HF_MODELS_DIR | application-prod.yml | ~/.lmstudio/models | YES
LMSTUDIO_API_URL | application-prod.yml | http://localhost:1234 | YES
```

Note: Database credentials, JWT secret, and encryption key are hardcoded in dev profile and must be externalized for production (not yet configured via env vars in application-prod.yml — see Technical Debt).

---

## 20. SERVICE DEPENDENCY MAP

```
This Service → Depends On
--------------------------
Ollama: localhost:11434 (local LLM inference + embeddings)
LM Studio: localhost:1234 (alternative inference provider, OpenAI-compatible)
PostgreSQL: localhost:5432 (primary database + pgvector)
Mosquitto MQTT: localhost:1883 (push notifications, optional)
Kiwix: localhost:8888 (ZIM file serving, optional)
Calibre Content Server: localhost:8081 (ebook metadata, optional)

External APIs (internet required):
  - HuggingFace Hub API: model catalog search
  - Brave Search API: web search enrichment (optional, API key required)
  - Anthropic Claude API: content summarization (optional, API key required)
  - Project Gutenberg API: ebook search/download (optional)

Downstream Consumers:
  - MyOffGridAI Flutter client (all /api/** endpoints, MQTT subscriptions, SSE streams)
  - External AI clients (Claude Desktop, Claude Code) via MCP at /mcp/sse
```

---

## 21. Known Technical Debt & Issues

```
Issue | Location | Severity | Notes
------|----------|----------|------
Stub: USB update handler | UsbResetWatcherService.java:20,45 | Medium | "stubbed for MI-002" — update zip processing not implemented
TEMPERATURE false positive | SensorType.java:7 | Info | Grep matched "TEMPERATURE" as containing "TEMP" — not actual TODO
Missing prod DB credentials externalization | application-prod.yml | High | DB URL, username, password not configured via env vars
Missing prod JWT secret externalization | application-prod.yml | High | JWT secret not in env var form
Missing prod encryption key externalization | application-prod.yml | Critical | AES encryption key hardcoded in dev, no prod override
No optimistic locking (@Version) | All entities | Medium | No entities use @Version for concurrent write protection
No soft delete pattern | All entities | Low | Hard deletes throughout
No CI/CD pipeline | Root | Medium | No GitHub Actions, Jenkins, or GitLab CI detected
JaCoCo coverage at 82.6% | pom.xml | High | Below 100% target (threshold set to 0.80 in pom.xml)
Snyk: 1 CRITICAL vulnerability | tomcat-embed-core@10.1.41 | CRITICAL | Improper Certificate Validation — upgrade Spring Boot
Snyk: 10 HIGH vulnerabilities | Various | High | See Section 22 for full list
No API versioning | All controllers | Low | No /v1/ prefix in API paths
No HTTPS enforcement in prod | application-prod.yml | Medium | No server.ssl configuration
CORS allows all origins | SecurityConfig.java | Medium | Should restrict in production
SQL string concatenation detected | 1 location | Low | Single instance found — verify parameterized
Dev encryption key hardcoded | application.yml | Critical | 64-char hex key in dev profile
```

---

## 22. Security Vulnerability Scan (Snyk)

Scan Date: 2026-03-16T23:24:33Z
Snyk CLI Version: 1.1303.0

### Dependency Vulnerabilities (Open Source)
Critical: 1
High: 10
Medium: 7
Low: 3

| Severity | Package | Version | Vulnerability | Fix Available |
|----------|---------|---------|---------------|---------------|
| CRITICAL | tomcat-embed-core | 10.1.41 | Improper Certificate Validation | 9.0.113+ |
| HIGH | tomcat-embed-core | 10.1.41 | Allocation of Resources Without Limits or Throttling | 9.0.106+ |
| HIGH | tomcat-embed-core | 10.1.41 | Integer Overflow or Wraparound | 9.0.107+ |
| HIGH | tomcat-embed-core | 10.1.41 | Allocation of Resources Without Limits or Throttling | 9.0.107+ |
| HIGH | tomcat-embed-core | 10.1.41 | Improper Resource Shutdown or Release | 9.0.108+ |
| HIGH | tomcat-embed-core | 10.1.41 | Relative Path Traversal | 9.0.109+ |
| HIGH | tomcat-embed-core | 10.1.41 | Untrusted Search Path | 9.0.106+ |
| HIGH | tomcat-embed-core | 10.1.41 | Incorrect Authorization | 9.0.114+ |
| HIGH | spring-beans | 6.2.7 | Relative Path Traversal | 6.2.10+ |
| HIGH | spring-core | 6.2.7 | Incorrect Authorization | 6.2.11+ |
| HIGH | spring-security-core | 6.4.6 | Incorrect Authorization | 6.4.10+ |
| MEDIUM | logback-core | 1.5.18 | External Initialization of Trusted Variables | 1.3.16+ |
| MEDIUM | netty-codec-http | 4.1.125.Final | CRLF Injection | 4.1.129.Final+ |
| MEDIUM | poi-ooxml | 5.3.0 | Improper Input Validation | 5.4.0+ |
| MEDIUM | tomcat-embed-core | 10.1.41 | Auth Bypass Using Alternate Path | 9.0.106+ |
| MEDIUM | tomcat-embed-core | 10.1.41 | Session Fixation | 9.0.106+ |
| MEDIUM | tomcat-embed-core | 10.1.41 | Improper Resource Shutdown or Release | 9.0.110+ |
| MEDIUM | spring-web | 6.2.7 | HTTP Response Splitting | 6.1.21+ |
| LOW | logback-core | 1.5.18 | External Initialization of Trusted Variables | 1.5.25+ |
| LOW | reactor-netty-http | 1.2.6 | Sensitive System Information Exposure | 1.2.8+ |
| LOW | tomcat-embed-core | 10.1.41 | Improper Authorization | 9.0.113+ |

**Remediation:** Most vulnerabilities are resolved by upgrading `spring-boot-starter-parent` to a newer version (3.4.7+). The `poi-ooxml` vulnerability requires upgrading to 5.4.0.

### Code Vulnerabilities (SAST)
Snyk Code scan not available (error code 2). Flag as gap.

### IaC Findings
Not scanned (no docker-compose.yml, no Kubernetes manifests).
