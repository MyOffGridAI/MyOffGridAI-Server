# MyOffGridAI-Server — Codebase Audit

**Audit Date:** 2026-03-17T12:29:53Z
**Branch:** main
**Commit:** 5305618cb32297d02fa5451c5995f7735a465ca7 P14-Server: Replace LM Studio with native llama-server process management
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
Project Name: MyOffGridAI Server
Repository URL: (local repository)
Primary Language / Framework: Java 21 / Spring Boot 3.4.13
Java Version: 21
Build Tool + Version: Maven 3.9.9
Current Branch: main
Latest Commit Hash: 5305618cb32297d02fa5451c5995f7735a465ca7
Latest Commit Message: P14-Server: Replace LM Studio with native llama-server process management
Audit Timestamp: 2026-03-17T12:29:53Z
```

---

## 2. Directory Structure

Single-module Maven project. Source in `src/main/java/com/myoffgridai/`. 16 feature packages:

```
com.myoffgridai/
├── ai/          — Chat, inference (Ollama + llama-server), context window, agents
├── auth/        — JWT authentication, user management
├── common/      — Exceptions, ApiResponse, utilities (AES encryption, token counter)
├── config/      — Security, rate limiting, JPA, Ollama, llama-server configs
├── enrichment/  — Web fetch, web search (Brave), Claude API integration
├── events/      — Scheduled/sensor-triggered events
├── knowledge/   — Document upload, chunking, OCR, semantic search
├── library/     — eBooks, ZIM files (Kiwix), Project Gutenberg
├── mcp/         — Model Context Protocol server (SSE transport)
├── memory/      — Memories, embeddings, RAG, summarization
├── models/      — HuggingFace model catalog, GGUF download manager
├── notification/ — MQTT push notifications, device registration
├── privacy/     — Audit logging, data export/wipe, Fortress mode, sovereignty reports
├── proactive/   — AI insights, notifications, pattern analysis, system health
├── sensors/     — Serial sensor management, polling, SSE streaming
├── settings/    — External API settings (Anthropic, Brave, HuggingFace)
├── skills/      — Built-in skills (6), skill execution framework
├── system/      — Captive portal, AP mode, factory reset, system config
└── MyOffGridAiApplication.java — Entry point (@SpringBootApplication, @EnableAsync, @EnableScheduling)
```

Source files: ~317 Java files (203 main, 114 test). Config: `application.yml`, `application-prod.yml`, `logback-spring.xml`, `Dockerfile`.

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.13 | REST API |
| spring-boot-starter-data-jpa | 3.4.13 | JPA/Hibernate |
| spring-boot-starter-security | 3.4.13 | Authentication/authorization |
| spring-boot-starter-validation | 3.4.13 | Bean validation |
| spring-boot-starter-actuator | 3.4.13 | Health/metrics |
| spring-boot-starter-webflux | 3.4.13 | Reactive WebClient for SSE streaming |
| spring-boot-starter-aop | 3.4.13 | Audit aspect |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 | MCP server (SSE transport) |
| jjwt-api/impl/jackson | 0.12.6 | JWT token generation/validation |
| postgresql | 42.7.7 | Database driver |
| pgvector | 0.1.6 | Vector similarity search |
| pdfbox | 3.0.4 | PDF text extraction |
| poi/poi-ooxml/poi-scratchpad | 5.4.0 | Office document extraction |
| tess4j | 5.13.0 | OCR (Tesseract) |
| jSerialComm | 2.11.0 | Serial port communication |
| bucket4j-core | 8.10.1 | Rate limiting |
| paho.client.mqttv3 | 1.2.5 | MQTT push notifications |
| jsoup | 1.18.3 | HTML parsing |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger UI |
| lombok | 1.18.42 | Boilerplate reduction |

**Test dependencies:** spring-boot-starter-test, reactor-test, spring-security-test, testcontainers-postgresql (1.20.6), testcontainers-junit-jupiter (1.20.6)

**Build plugins:** spring-boot-maven-plugin, maven-compiler-plugin (Java 21 + Lombok), jacoco-maven-plugin (0.8.12, 100% LINE+BRANCH enforcement excluding dto/model/Application), maven-surefire-plugin (with Testcontainers Docker env vars). GraalVM native-image profile available.

**Build commands:**
```
Build: mvn clean compile -DskipTests
Test: mvn test
Run: mvn spring-boot:run
Package: mvn clean package -DskipTests
```

---

## 4. Configuration & Infrastructure Summary

**`src/main/resources/application.yml`** — Dev profile default. Server port 8080. PostgreSQL localhost:5432/myoffgridai. Hibernate DDL: `update`. Inference provider: `llama-server` (native process at port 1234). Default model: Qwen3.5-27B. Embed model: nomic-embed-text. JWT: 24h access, 7d refresh with dev-only secrets. Rate limiting enabled. MQTT disabled in dev. AP/Fortress modes mocked.

**`src/main/resources/application-prod.yml`** — All secrets via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ENCRYPTION_KEY`, `INFERENCE_*`). Hibernate DDL: `validate`. Flyway enabled. 24 env var references total.

**`src/main/resources/logback-spring.xml`** — Dev: console pattern with requestId. Prod: JSON (LogstashEncoder) with MDC fields (requestId, username, userId). Test: JSON, WARN level.

**`src/test/resources/application.yml`** — DDL: `create-drop`. Flyway disabled. JWT 1h/24h. Rate limiting disabled. MQTT disabled. AP/Fortress mocked. Library paths use temp dirs.

**`Dockerfile`** — Multi-stage build. Base: eclipse-temurin:21-jdk-alpine (build) / 21-jre-alpine (runtime). Non-root user `myoffgridai`. Port 8080. Health check: `wget http://localhost:8080/api/system/status` every 30s. Knowledge storage: `/var/myoffgridai/knowledge`.

**Connection map:**
```
Database: PostgreSQL, localhost, 5432, myoffgridai (with pgvector extension)
Cache: None
Message Broker: MQTT (Paho, conditional on app.mqtt.enabled, disabled in dev)
External APIs: Ollama (localhost:11434), llama-server (localhost:1234), Anthropic Claude API, Brave Search API, Gutendex (gutendex.com), HuggingFace API
Cloud Services: None
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry point:** `com.myoffgridai.MyOffGridAiApplication` (`@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`)

**Startup initialization:**
- `VectorStoreConfig.checkPgvectorExtension()` — Verifies pgvector extension on `ApplicationReadyEvent`
- `StorageHealthService.checkStorageDirectory()` — Creates/verifies knowledge storage dir
- `ModelHealthCheckService.checkInferenceProviderOnStartup()` — Checks inference provider and lists models
- `LlamaServerProcessService.run()` — Auto-starts llama-server binary (ApplicationRunner)
- `SkillSeederService.seedBuiltInSkills()` — Seeds 6 built-in skills
- `SensorStartupService.resumeActiveSensors()` — Resumes active sensor polling
- `ApModeStartupService.onApplicationReady()` — Starts/stops AP mode based on initialization state

**Scheduled tasks:**
- `SummarizationService.scheduledNightlySummarization()` — Cron `0 0 2 * * *` (2 AM daily)
- `NightlyInsightJob.generateNightlyInsights()` — Cron `0 0 3 * * *` (3 AM daily)
- `SystemHealthMonitor.checkSystemHealth()` — Every 300s (configurable)
- `UsbResetWatcherService.checkForTriggerFiles()` — Every 30s (USB factory reset trigger)
- `LlamaServerProcessService.monitorHealth()` — Every 30s (crash detection/auto-restart)

**Health check:** `GET /actuator/health` (Spring Actuator) + `GET /api/system/status` (custom)

---

## 6. Entity / Data Model Layer

### ai Module

=== Conversation.java ===
Table: conversations
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - user: User [@ManyToOne LAZY] (not nullable)
  - title: String (nullable)
  - isArchived: boolean (default false)
  - messageCount: int (default 0)
  - createdAt: Instant [@CreatedDate] (not nullable, not updatable)
  - updatedAt: Instant [@LastModifiedDate] (nullable)
Relationships: @ManyToOne → User (LAZY)
Audit Fields: createdAt, updatedAt ✓

=== Message.java ===
Table: messages
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - conversation: Conversation [@ManyToOne LAZY] (not nullable)
  - role: MessageRole (Enum, not nullable)
  - content: String (TEXT, not nullable)
  - tokenCount: Integer (nullable)
  - hasRagContext: boolean (default false)
  - thinkingContent: String (TEXT, nullable)
  - tokensPerSecond: Double (nullable)
  - inferenceTimeSeconds: Double (nullable)
  - stopReason: String (nullable)
  - thinkingTokenCount: Integer (nullable)
  - createdAt: Instant [@CreatedDate] (not nullable, not updatable)
Relationships: @ManyToOne → Conversation (LAZY)
Audit Fields: createdAt ✓ (no updatedAt)

### auth Module

=== User.java ===
Table: users
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - username: String (not nullable, unique)
  - email: String (nullable, unique)
  - displayName: String (not nullable)
  - passwordHash: String (not nullable)
  - role: Role (Enum, not nullable)
  - isActive: boolean (default true)
  - lastLoginAt: Instant (nullable)
  - createdAt: Instant [@CreatedDate] (not nullable, not updatable)
  - updatedAt: Instant [@LastModifiedDate] (nullable)
Implements: UserDetails (Spring Security)
Audit Fields: createdAt, updatedAt ✓

### events Module

=== ScheduledEvent.java ===
Table: scheduled_events
Indexes: idx_event_user_id (user_id), idx_event_enabled_type (is_enabled, event_type)
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - userId: UUID (not nullable)
  - name: String (not nullable)
  - description: String (TEXT, nullable)
  - eventType: EventType (Enum, not nullable)
  - isEnabled: boolean (default true)
  - cronExpression: String (nullable)
  - recurringIntervalMinutes: Integer (nullable)
  - sensorId: UUID (nullable)
  - thresholdOperator: ThresholdOperator (Enum, nullable)
  - thresholdValue: Double (nullable)
  - actionType: ActionType (Enum, not nullable)
  - actionPayload: String (TEXT, not nullable)
  - lastTriggeredAt: Instant (nullable)
  - nextFireAt: Instant (nullable)
  - createdAt: Instant [@CreatedDate], updatedAt: Instant [@LastModifiedDate]
Audit Fields: createdAt, updatedAt ✓

### knowledge Module

=== KnowledgeDocument.java ===
Table: knowledge_documents
Indexes: idx_knowledge_doc_user_id (user_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - userId: UUID (not nullable)
  - filename: String (not nullable)
  - displayName: String (nullable)
  - mimeType: String (not nullable)
  - storagePath: String (not nullable)
  - fileSizeBytes: long (nullable)
  - status: DocumentStatus (Enum, default PENDING)
  - errorMessage: String (TEXT, nullable)
  - chunkCount: int (nullable)
  - content: String (TEXT, nullable)
  - uploadedAt: Instant [@CreatedDate] (not nullable, not updatable)
  - processedAt: Instant (nullable)
Audit Fields: uploadedAt ✓

=== KnowledgeChunk.java ===
Table: knowledge_chunks
Indexes: idx_knowledge_chunk_doc_id (document_id), idx_knowledge_chunk_user_id (user_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - document: KnowledgeDocument [@ManyToOne LAZY] (not nullable)
  - userId: UUID (not nullable)
  - chunkIndex: int (not nullable)
  - content: String (TEXT, not nullable)
  - pageNumber: Integer (nullable)
  - createdAt: Instant [@CreatedDate]
Audit Fields: createdAt ✓

### library Module

=== Ebook.java ===
Table: ebooks
Indexes: idx_ebooks_gutenberg_id (gutenberg_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields: title, author, description (TEXT 2000), isbn, publisher, publishedYear, language, format (EbookFormat Enum), fileSizeBytes, filePath, coverImagePath, gutenbergId, downloadCount, uploadedAt [@CreatedDate], uploadedBy (UUID)
Audit Fields: uploadedAt ✓

=== ZimFile.java ===
Table: zim_files
Primary Key: id (UUID, GenerationType.UUID)
Fields: filename (unique), displayName, description (1000), language, category, fileSizeBytes, articleCount, mediaCount, createdDate, filePath, kiwixBookId, uploadedAt [@CreatedDate], uploadedBy (UUID)
Audit Fields: uploadedAt ✓

### mcp Module

=== McpApiToken.java ===
Table: mcp_api_tokens
Indexes: idx_mcp_token_created_by (created_by)
Primary Key: id (UUID, GenerationType.UUID)
Fields: tokenHash (500 chars, not nullable), name (not nullable), createdBy (UUID, not nullable), lastUsedAt (nullable), isActive (default true), createdAt [@CreatedDate]
Audit Fields: createdAt ✓

### memory Module

=== Memory.java ===
Table: memories
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId (not nullable), content (TEXT, not nullable), importance (MemoryImportance Enum), tags (nullable), sourceConversationId (nullable), accessCount (default 0), lastAccessedAt (nullable), createdAt [@CreatedDate], updatedAt [@LastModifiedDate]
Audit Fields: createdAt, updatedAt ✓

=== VectorDocument.java ===
Table: vector_document
Indexes: idx_vector_doc_user_source_type (user_id, source_type)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId (not nullable), content (TEXT), embedding (float[] vector(768), custom VectorType), sourceType (VectorSourceType Enum), sourceId (UUID, nullable), metadata (TEXT, nullable), createdAt [@CreatedDate]
Custom: Uses pgvector cosine distance operator `<=>` in native queries
Audit Fields: createdAt ✓

### notification Module

=== DeviceRegistration.java ===
Table: device_registrations
Unique: uk_device_registration_user_device (user_id, device_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId, deviceId, deviceName, platform, mqttClientId, lastSeenAt, createdAt, updatedAt
Audit Fields: createdAt, updatedAt (manual @PrePersist/@PreUpdate)

### privacy Module

=== AuditLog.java ===
Table: audit_logs
Indexes: idx_audit_user_timestamp (user_id, timestamp DESC), idx_audit_timestamp (timestamp DESC)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId, username, action, resourceType, resourceId, httpMethod, requestPath, ipAddress, userAgent, responseStatus, outcome (AuditOutcome Enum), durationMs, timestamp
Audit Fields: Manual timestamp only

### proactive Module

=== Insight.java ===
Table: insights
Indexes: idx_insight_user_id (user_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId, content (TEXT), category (InsightCategory Enum), isRead (default false), isDismissed (default false), generatedAt, readAt
Audit Fields: generatedAt (manual @PrePersist)

=== Notification.java ===
Table: notifications
Indexes: idx_notification_user_id (user_id)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId, title, body (TEXT), type (NotificationType Enum), isRead (default false), severity (NotificationSeverity Enum, nullable, 20 chars), mqttDelivered (default false), metadata (TEXT, nullable), createdAt, readAt
Audit Fields: createdAt (manual @PrePersist)

### sensors Module

=== Sensor.java ===
Table: sensors
Indexes: idx_sensor_user_id (user_id), idx_sensor_port_path (port_path, unique)
Primary Key: id (UUID, GenerationType.UUID)
Fields: userId, name, type (SensorType Enum), portPath (unique), baudRate (default 9600), dataFormat (DataFormat Enum, default CSV_LINE), valueField, unit, isActive (default false), pollIntervalSeconds (default 30), lowThreshold, highThreshold, createdAt [@CreatedDate], updatedAt [@LastModifiedDate]
Audit Fields: createdAt, updatedAt ✓

=== SensorReading.java ===
Table: sensor_readings
Indexes: idx_sensor_reading_sensor_recorded (sensor_id, recorded_at DESC)
Primary Key: id (UUID, GenerationType.UUID)
Fields: sensor [@ManyToOne LAZY], value (double), rawData, recordedAt
Audit Fields: None

### settings Module

=== ExternalApiSettings.java ===
Table: external_api_settings
Primary Key: id (UUID, GenerationType.UUID)
Singleton pattern via `singletonGuard` unique column
Fields: anthropicApiKey [@AesAttributeConverter encrypted], anthropicModel (default "claude-sonnet-4-20250514"), anthropicEnabled, braveApiKey [@AesAttributeConverter encrypted], braveEnabled, maxWebFetchSizeKb (default 512), searchResultLimit (default 5), huggingFaceToken [@AesAttributeConverter encrypted], huggingFaceEnabled, createdAt [@CreatedDate], updatedAt [@LastModifiedDate]
Audit Fields: createdAt, updatedAt ✓

### skills Module

=== Skill.java ===
Table: skills
Indexes: idx_skill_name (name, unique), idx_skill_category (category)
Primary Key: id (UUID, GenerationType.UUID)
Fields: name (unique), displayName, description (TEXT), version, author, category (SkillCategory Enum), isEnabled (default true), isBuiltIn (default false), parametersSchema (TEXT, nullable), createdAt, updatedAt
Audit Fields: createdAt, updatedAt ✓

=== SkillExecution.java ===
Table: skill_executions
Indexes: idx_skill_exec_user_id, idx_skill_exec_skill_id
Fields: skill [@ManyToOne LAZY], userId, status (ExecutionStatus Enum), inputParams (TEXT), outputResult (TEXT), errorMessage (TEXT), startedAt, completedAt, durationMs

=== InventoryItem.java ===
Table: inventory_items
Indexes: idx_inventory_user_id, idx_inventory_category
Fields: userId, name, category (InventoryCategory Enum), quantity (double), unit, notes (TEXT), lowStockThreshold, createdAt, updatedAt

=== PlannedTask.java ===
Table: planned_tasks
Indexes: idx_planned_task_user_id
Fields: userId, goalDescription (TEXT), title, steps (TEXT), estimatedResources (TEXT), status (TaskStatus Enum, default ACTIVE), createdAt, updatedAt

### system Module

=== SystemConfig.java ===
Table: system_config (singleton)
Fields: initialized (default false), instanceName, fortressEnabled (default false), fortressEnabledAt, fortressEnabledByUserId, apModeEnabled (default false), wifiConfigured (default false), aiModel, aiTemperature (default 0.7), aiSimilarityThreshold (default 0.45), aiMemoryTopK (default 5), aiRagMaxContextTokens (default 2048), aiContextSize (default 4096), aiContextMessageLimit (default 20), activeModelFilename, knowledgeStoragePath (default "/var/myoffgridai/knowledge"), maxUploadSizeMb (default 25), createdAt, updatedAt

---

## 7. Enum Inventory

| Enum | Values | Used In |
|------|--------|---------|
| MessageRole | USER, ASSISTANT, SYSTEM | Message.role |
| Role | ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD | User.role |
| EventType | SCHEDULED, SENSOR_THRESHOLD, RECURRING | ScheduledEvent.eventType |
| ActionType | PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY | ScheduledEvent.actionType |
| ThresholdOperator | ABOVE, BELOW, EQUALS | ScheduledEvent.thresholdOperator |
| DocumentStatus | PENDING, PROCESSING, READY, FAILED | KnowledgeDocument.status |
| EbookFormat | EPUB, PDF, MOBI, AZW, TXT, HTML | Ebook.format |
| MemoryImportance | LOW, MEDIUM, HIGH, CRITICAL | Memory.importance |
| VectorSourceType | MEMORY, CONVERSATION, KNOWLEDGE_CHUNK | VectorDocument.sourceType |
| AuditOutcome | SUCCESS, FAILURE, DENIED | AuditLog.outcome |
| InsightCategory | HOMESTEAD, HEALTH, RESOURCE, GENERAL | Insight.category |
| NotificationType | SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL | Notification.type |
| NotificationSeverity | INFO, WARNING, CRITICAL | Notification.severity |
| SensorType | TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM | Sensor.type |
| DataFormat | CSV_LINE, JSON_LINE | Sensor.dataFormat |
| ExecutionStatus | RUNNING, COMPLETED, FAILED | SkillExecution.status |
| InventoryCategory | FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER | InventoryItem.category |
| SkillCategory | HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM | Skill.category |
| TaskStatus | ACTIVE, COMPLETED, CANCELLED | PlannedTask.status |
| LlamaServerStatus | STOPPED, STARTING, RUNNING, RESTARTING, ERROR | LlamaServerProcessService |
| QuantizationType | Q2_K, Q3_K_S, Q3_K_M, Q3_K_L, Q4_0, Q4_K_S, Q4_K_M, Q5_0, Q5_K_S, Q5_K_M, Q6_K, Q8_0, F16, F32, IQ2_XXS, IQ2_XS, IQ2_S, IQ3_XXS, IQ3_XS, IQ3_S, IQ4_XS, IQ4_NL, UNKNOWN | HfModelFileDto.quantizationType |
| ChunkType | THINKING, CONTENT | InferenceChunk.type |

---

## 8. Repository Layer

=== ConversationRepository === Entity: Conversation, Extends: JpaRepository
Custom: findByUserIdOrderByUpdatedAtDesc(Pageable), findByUserIdAndIsArchivedOrderByUpdatedAtDesc(Pageable), findByIdAndUserId, countByUserId, findByUserId, findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Pageable), @Modifying deleteByUserId

=== MessageRepository === Entity: Message, Extends: JpaRepository
Custom: findByConversationIdOrderByCreatedAtAsc, findByConversationIdOrderByCreatedAtAsc(Pageable), findTopNByConversationIdOrderByCreatedAtDesc(Pageable), countByConversationId, deleteByConversationId, @Query countByUserId, @Modifying deleteByUserId, @Modifying deleteMessagesAfter

=== UserRepository === Entity: User, Extends: JpaRepository
Custom: findByUsername, findByEmail, existsByUsername, existsByEmail, findAllByRole, countByIsActiveTrue, findByIsActiveTrue

=== ScheduledEventRepository === Entity: ScheduledEvent, Extends: JpaRepository
Custom: findAllByUserId(Pageable), findByIdAndUserId, findByIsEnabledTrueAndEventType, findAllByUserIdOrderByCreatedAtDesc, deleteByUserId, countByUserId

=== KnowledgeDocumentRepository === Entity: KnowledgeDocument, Extends: JpaRepository
Custom: findByUserIdOrderByUploadedAtDesc(Pageable), findByIdAndUserId, findByUserIdAndStatus, @Modifying deleteByUserId, countByUserId

=== KnowledgeChunkRepository === Entity: KnowledgeChunk, Extends: JpaRepository
Custom: findByDocumentIdOrderByChunkIndexAsc, @Modifying deleteByDocumentId, @Modifying deleteByUserId, countByDocumentId

=== EbookRepository === Entity: Ebook, Extends: JpaRepository
Custom: @Query searchByTitleOrAuthor(search, format, Pageable), findByGutenbergId, existsByGutenbergId

=== ZimFileRepository === Entity: ZimFile, Extends: JpaRepository
Custom: findByFilename, findAllByOrderByDisplayNameAsc, existsByFilename

=== McpApiTokenRepository === Entity: McpApiToken, Extends: JpaRepository
Custom: findByIsActiveTrue, findByCreatedByOrderByCreatedAtDesc

=== MemoryRepository === Entity: Memory, Extends: JpaRepository
Custom: findByUserIdOrderByCreatedAtDesc(Pageable), findByUserIdAndImportance(Pageable), findByUserIdAndTagsContaining(Pageable), findByUserId, @Modifying deleteByUserId, countByUserId

=== VectorDocumentRepository === Entity: VectorDocument, Extends: JpaRepository
Custom: findByUserIdAndSourceType, @Modifying deleteBySourceIdAndSourceType, @Modifying deleteByUserId, @Query(native) findMostSimilar (pgvector `<=>` cosine distance), @Query(native) findMostSimilarAcrossTypes

=== DeviceRegistrationRepository === Entity: DeviceRegistration, Extends: JpaRepository
Custom: findByUserIdAndDeviceId, findByUserId, deleteByUserIdAndDeviceId

=== AuditLogRepository === Entity: AuditLog, Extends: JpaRepository
Custom: findAllByOrderByTimestampDesc(Pageable), findByUserIdOrderByTimestampDesc(Pageable), findByOutcomeOrderByTimestampDesc(Pageable), findByTimestampBetweenOrderByTimestampDesc(Pageable), findByUserIdAndTimestampBetween(Pageable), countByOutcomeAndTimestampBetween, @Modifying deleteByTimestampBefore, @Modifying deleteByUserId

=== InsightRepository === Entity: Insight, Extends: JpaRepository
Custom: findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(Pageable), findByUserIdAndCategoryAndIsDismissedFalse(Pageable), findByUserIdAndIsReadFalseAndIsDismissedFalse, countByUserIdAndIsReadFalseAndIsDismissedFalse, findByIdAndUserId, countByUserId, @Modifying deleteByUserId

=== NotificationRepository === Entity: Notification, Extends: JpaRepository
Custom: findByUserIdAndIsReadFalseOrderByCreatedAtDesc, findByUserIdOrderByCreatedAtDesc(Pageable), countByUserIdAndIsReadFalse, findByIdAndUserId, @Modifying @Query markAllReadForUser, @Modifying deleteByUserId

=== SensorRepository === Entity: Sensor, Extends: JpaRepository
Custom: findByUserIdOrderByNameAsc, findByIdAndUserId, findByUserIdAndIsActiveTrue, findByPortPath, findByIsActiveTrue, countByUserId, deleteByUserId

=== SensorReadingRepository === Entity: SensorReading, Extends: JpaRepository
Custom: findBySensorIdOrderByRecordedAtDesc(Pageable), findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc, findTopBySensorIdOrderByRecordedAtDesc, @Modifying deleteBySensorId, @Modifying @Query deleteByUserId, @Query(native) findAverageValueSince

=== ExternalApiSettingsRepository === Entity: ExternalApiSettings, Extends: JpaRepository
Custom: findBySingletonGuard

=== SkillRepository === Entity: Skill, Extends: JpaRepository
Custom: findByIsEnabledTrue, findByIsBuiltInTrue, findByCategory, findByName, findByIsEnabledTrueOrderByDisplayNameAsc

=== SkillExecutionRepository === Entity: SkillExecution, Extends: JpaRepository
Custom: findByUserIdOrderByStartedAtDesc(Pageable), findBySkillIdAndUserIdOrderByStartedAtDesc(Pageable), findByUserIdAndStatus

=== InventoryItemRepository === Entity: InventoryItem, Extends: JpaRepository
Custom: findByUserIdOrderByNameAsc, findByUserIdAndCategory, findByUserIdAndQuantityLessThanEqual, findByIdAndUserId, @Modifying deleteByUserId

=== PlannedTaskRepository === Entity: PlannedTask, Extends: JpaRepository
Custom: findByUserIdAndStatusOrderByCreatedAtDesc(Pageable), findByIdAndUserId, @Modifying deleteByUserId

=== SystemConfigRepository === Entity: SystemConfig, Extends: JpaRepository
Custom: @Query findFirst

**Common patterns:** All repositories extend JpaRepository<Entity, UUID>. No @EntityGraph usage. No projections. Wide use of userId scoping for multi-tenant isolation. Pageable support on list endpoints.

---

## 9. Service Layer — Full Method Signatures

### ai Module

=== InferenceService.java (Interface) ===
- `chat(List<OllamaMessage>, UUID userId): String`
- `streamChat(List<OllamaMessage>, UUID userId): Flux<String>`
- `streamChatWithThinking(List<OllamaMessage>, UUID userId): Flux<InferenceChunk>`
- `embed(String text): float[]`
- `isAvailable(): boolean`
- `listModels(): List<InferenceModelInfo>`
- `getActiveModel(): InferenceModelInfo`

Implementations: `LlamaServerInferenceService` (native llama.cpp), `OllamaInferenceService` (Ollama API). Selected by `app.inference.provider` property.

=== ChatService.java ===
Injects: ConversationRepository, MessageRepository, UserRepository, OllamaService, InferenceService, SystemPromptBuilder, ContextWindowService, RagService, MemoryExtractionService, SystemConfigService
- `createConversation(UUID userId, String title): Conversation` @Transactional
- `getConversations(UUID userId, boolean includeArchived, Pageable): Page<Conversation>`
- `getConversation(UUID conversationId, UUID userId): Conversation` → EntityNotFoundException
- `archiveConversation(UUID, UUID): void` @Transactional
- `deleteConversation(UUID, UUID): void` @Transactional — deletes messages then conversation
- `sendMessage(UUID conversationId, UUID userId, String content): Message` @Transactional — RAG context, system prompt, inference, memory extraction
- `streamMessage(UUID conversationId, UUID userId, String content): Flux<String>` — SSE streaming with think-tag support
- `editMessage(UUID conversationId, UUID messageId, UUID userId, String newContent): Message` @Transactional — deletes subsequent messages, re-runs inference
- `deleteMessage(UUID, UUID, UUID): void` @Transactional
- `branchConversation(UUID conversationId, UUID messageId, UUID userId, String title): Conversation` @Transactional
- `regenerateMessage(UUID, UUID, UUID): Flux<String>` — deletes assistant message, re-streams
- `getMessages(UUID userId, UUID conversationId, Pageable): Page<Message>`
- `searchConversations(UUID userId, String query, Pageable): Page<Conversation>`
- `renameConversation(UUID, UUID, String): Conversation` @Transactional

=== LlamaServerProcessService.java ===
Injects: LlamaServerProperties, SystemConfigService, ProcessBuilderFactory, InferenceService
Implements: ApplicationRunner, DisposableBean
- `start(): void` (synchronized) — resolves model, starts binary, waits for healthy
- `stop(): void` (synchronized)
- `restart(): void`
- `switchModel(String filename): LlamaServerStatusDto` → IllegalArgumentException
- `getStatus(): LlamaServerStatusDto`
- `getRecentLogLines(int n): List<String>`
- `monitorHealth(): void` @Scheduled — crash detection/auto-restart

=== ContextWindowService.java ===
Injects: MessageRepository, SystemConfigService
- `prepareMessages(UUID conversationId, String systemPrompt, String newUserMessage): List<OllamaMessage>` — truncates to token limit

=== AgentService.java ===
Injects: OllamaService, SkillExecutorService, ObjectMapper, SystemConfigService
- `executeTask(UUID userId, UUID conversationId, String taskDescription): AgentTaskResult` — step-by-step prompting with tool-call detection

=== SystemPromptBuilder.java ===
Injects: RagService
- `build(User user, String instanceName): String`
- `build(User user, String instanceName, RagContext ragContext): String` — injects RAG context

### auth Module

=== AuthService.java ===
Injects: UserRepository, JwtService, PasswordEncoder
- `register(RegisterRequest): AuthResponse` @Transactional → DuplicateResourceException, IllegalArgumentException
- `login(LoginRequest): AuthResponse` @Transactional → BadCredentialsException
- `refresh(String refreshToken): AuthResponse` → BadCredentialsException
- `logout(String token): void` — in-memory blacklist
- `isTokenBlacklisted(String): boolean`
- `changePassword(UUID, ChangePasswordRequest): void` @Transactional

=== JwtService.java ===
- `generateAccessToken(UserDetails): String`
- `generateRefreshToken(UserDetails): String`
- `extractUsername(String token): String`
- `isTokenValid(String token, UserDetails): boolean`
- `isTokenExpired(String): boolean`

=== UserService.java ===
Injects: UserRepository
- `listUsers(int page, int size): Page<UserSummaryDto>`
- `getUserById(UUID): UserDetailDto` → EntityNotFoundException
- `updateUser(UUID, String displayName, String email, Role role): UserDetailDto` @Transactional
- `deactivateUser(UUID): void` @Transactional
- `deleteUser(UUID): void` @Transactional

### enrichment Module

=== ClaudeApiService.java ===
- `isAvailable(): boolean`
- `complete(String systemPrompt, String userPrompt): Optional<String>` — graceful fallback
- `summarizeForKnowledgeBase(String rawContent, int maxChars): String`

=== WebFetchService.java ===
- `fetchUrl(String url): FetchResult` — Jsoup HTML→text extraction
- `fetchAndStore(String url, UUID userId, boolean summarizeWithClaude): KnowledgeDocumentDto`

=== WebSearchService.java ===
- `isAvailable(): boolean`
- `search(String query): List<SearchResultDto>` — Brave Search API
- `searchAndStore(String query, int fetchTopN, UUID userId, boolean summarize): List<KnowledgeDocumentDto>`

### knowledge Module

=== KnowledgeService.java ===
Injects: KnowledgeDocumentRepository, KnowledgeChunkRepository, VectorDocumentRepository, FileStorageService, IngestionService, OcrService, ChunkingService, EmbeddingService
- `upload(UUID userId, MultipartFile): KnowledgeDocumentDto` @Transactional — stores file, kicks off async processing
- `processDocumentAsync(UUID documentId): void` @Async — extract, chunk, embed
- `listDocuments(UUID userId, Pageable): Page<KnowledgeDocumentDto>` @Transactional(readOnly)
- `getDocument(UUID, UUID): KnowledgeDocumentDto`
- `updateDisplayName(UUID, UUID, String): KnowledgeDocumentDto` @Transactional
- `deleteDocument(UUID, UUID): void` @Transactional — deletes chunks, vectors, file
- `retryProcessing(UUID, UUID): KnowledgeDocumentDto` @Transactional
- `getDocumentContent(UUID, UUID): DocumentContentDto`
- `getDocumentForDownload(UUID, UUID): KnowledgeDocument`
- `createFromEditor(UUID userId, String title, String deltaJsonContent): KnowledgeDocumentDto` @Transactional
- `updateContent(UUID, UUID, String deltaJsonContent): KnowledgeDocumentDto` @Transactional
- `deleteAllForUser(UUID): void` @Transactional

=== IngestionService.java === (Stateless)
- `extractPdf(InputStream): ExtractionResult`
- `extractText(InputStream): ExtractionResult`
- `extractDocx/extractDoc/extractRtf/extractXlsx/extractXls/extractPptx/extractPpt(InputStream): ExtractionResult`

=== ChunkingService.java === (Stateless)
- `chunkText(String): List<String>` — sentence-boundary splitting with overlap

=== OcrService.java ===
- `extractFromImage(InputStream): ExtractionResult` — Tesseract OCR

=== SemanticSearchService.java ===
- `search(UUID userId, String queryText, int topK): List<KnowledgeSearchResultDto>` — pgvector cosine search
- `searchForRagContext(UUID userId, String queryText, int topK): List<String>` — with source attribution

### memory Module

=== MemoryService.java ===
Injects: MemoryRepository, VectorDocumentRepository, EmbeddingService, SystemConfigService
- `createMemory(UUID userId, String content, MemoryImportance, String tags, UUID sourceConversationId): Memory` @Transactional
- `findRelevantMemories(UUID userId, String queryText, int topK): List<Memory>` @Transactional
- `searchMemoriesWithScores(UUID userId, String queryText, int topK): List<MemorySearchResultDto>` @Transactional
- `getMemory(UUID, UUID): Memory` → EntityNotFoundException, AccessDeniedException
- `updateImportance(UUID, UUID, MemoryImportance): Memory` @Transactional
- `updateTags(UUID, UUID, String): Memory` @Transactional
- `deleteMemory(UUID, UUID): void` @Transactional
- `deleteAllMemoriesForUser(UUID): void` @Transactional
- `exportMemories(UUID): List<Memory>`
- `getMemories(UUID, MemoryImportance, String tag, Pageable): Page<Memory>`

=== RagService.java ===
- `buildRagContext(UUID userId, String queryText): RagContext` — memories + knowledge
- `formatContextBlock(RagContext): String`

=== EmbeddingService.java ===
- `embed(String): float[]` → EmbeddingException
- `embedAndFormat(String): String` — pgvector format
- `embedBatch(List<String>): List<float[]>`
- `cosineSimilarity(float[], float[]): float`

=== MemoryExtractionService.java ===
- `@Async extractAndStore(UUID userId, UUID conversationId, String userMessage, String assistantResponse): void`

=== SummarizationService.java ===
- `summarizeConversation(UUID conversationId, UUID userId): Memory`
- `@Scheduled(cron="0 0 2 * * *") scheduledNightlySummarization(): void`

### models Module

=== ModelCatalogService.java ===
- `searchModels(String query, String formatFilter, int limit): HfSearchResultDto` — HuggingFace API
- `getModelDetails(String repoId): HfModelDto`
- `getModelFiles(String repoId): List<HfModelFileDto>` — enriched with quantization metadata

=== ModelDownloadService.java ===
- `startDownload(String repoId, String filename): String` — returns download ID
- `getProgress(String): Optional<DownloadProgress>`
- `getAllDownloads(): List<DownloadProgress>`
- `cancelDownload(String): void`
- `listLocalModels(): List<LocalModelFileDto>`
- `deleteLocalModel(String filename): void`
- `@Async executeDownload(String, String, String, String): void` — SSE progress streaming

=== QuantizationRecommendationService.java ===
- `enrichFiles(List<HfModelFileDto>): List<HfModelFileDto>` — adds quant metadata + RAM recommendation
- `parseQuantType(String filename): QuantizationType`
- `estimateRam(long fileSize): long`
- `getAvailableRam(): long`

### privacy Module

=== AuditService.java === — CRUD for audit logs
=== DataExportService.java === — `exportUserData(UUID, String passphrase): byte[]` — AES-256-GCM encrypted ZIP
=== DataWipeService.java === — `wipeUser(UUID): WipeResult` @Transactional — cascade delete all user data
=== FortressService.java === — `enable/disable(UUID): void` — iptables rules (mocked in dev)
=== SovereigntyReportService.java === — `generateReport(UUID): SovereigntyReport`

### proactive Module

=== InsightGeneratorService.java === — `generateInsights(UUID): List<Insight>` — AI-powered pattern analysis
=== InsightService.java === — CRUD for insights (getInsights, markRead, dismiss)
=== NightlyInsightJob.java === — `@Scheduled(cron="0 0 3 * * *") generateNightlyInsights(): void`
=== NotificationService.java === — CRUD + SSE broadcast + MQTT delivery
=== NotificationSseRegistry.java === — SSE emitter registry for real-time notifications
=== PatternAnalysisService.java === — `buildPatternSummary(UUID): PatternSummary`
=== SystemHealthMonitor.java === — `@Scheduled checkSystemHealth(): void` — disk, Ollama, heap checks

### sensors Module

=== SensorService.java === — CRUD + start/stop polling + threshold management
=== SensorPollingService.java === — `startPolling(Sensor)/stopPolling(UUID)` — serial port read loop with SSE broadcast
=== SerialPortService.java === — jSerialComm wrapper (open, close, readLine, test)
=== SseEmitterRegistry.java === — SSE emitter registry for sensor readings

### skills Module

=== SkillExecutorService.java === — `execute(UUID skillId, UUID userId, Map params): SkillExecution` @Transactional
=== SkillSeederService.java === — Seeds 6 built-in skills on ApplicationReadyEvent
**Built-in skills:** DocumentSummarizer, InventoryTracker, RecipeGenerator, ResourceCalculator, TaskPlanner, WeatherQuery

### system Module

=== SystemConfigService.java === — Singleton config CRUD (getConfig, save, getAiSettings, updateAiSettings, getStorageSettings, setActiveModelFilename)
=== ApModeService.java === — AP mode management (start, stop, scan WiFi, connect WiFi)
=== FactoryResetService.java === — `@Async performReset(): void`
=== NetworkTransitionService.java === — `@Async finalizeSetup(): void` — AP→home network transition
=== UsbResetWatcherService.java === — `@Scheduled checkForTriggerFiles(): void` — USB factory reset

### Other services

=== McpTokenService.java === — MCP API token CRUD with BCrypt hashing
=== McpToolsService.java === — 12 @Tool-annotated MCP tools (knowledge, memory, inventory, sensors, conversations, system)
=== ExternalApiSettingsService.java === — External API key management (Anthropic, Brave, HuggingFace)
=== DeviceRegistrationService.java === — MQTT device registration
=== MqttPublisherService.java === — MQTT message publishing

---

## 10. Controller / API Layer — Method Signatures Only

=== ChatController.java === Base Path: `/api/chat`
Injects: ChatService, MessageRepository
- createConversation() → chatService.createConversation()
- listConversations() → chatService.getConversations()
- searchConversations() → chatService.searchConversations()
- getConversation() → chatService.getConversation()
- deleteConversation() → chatService.deleteConversation()
- archiveConversation() → chatService.archiveConversation()
- renameConversation() → chatService.renameConversation()
- sendMessage() → chatService.sendMessage() / streamMessage() (SSE)
- listMessages() → messageRepository.findByConversationIdOrderByCreatedAtAsc()
- editMessage() → chatService.editMessage()
- deleteMessage() → chatService.deleteMessage()
- branchConversation() → chatService.branchConversation()
- regenerateMessage() → chatService.regenerateMessage() (SSE)

=== ModelController.java === Base Path: `/api/models`
Injects: InferenceService, SystemConfigService
- listModels() → inferenceService.listModels()
- getActiveModel() → inferenceService.getActiveModel()
- getHealth() → inferenceService.isAvailable()

=== AuthController.java === Base Path: `/api/auth`
Injects: AuthService
- register() → authService.register()
- login() → authService.login()
- refresh() → authService.refresh()
- logout() → authService.logout()

=== UserController.java === Base Path: `/api/users`
Injects: UserService
- listUsers(), getUser(), updateUser(), deactivateUser(), deleteUser()

=== EnrichmentController.java === Base Path: `/api/enrichment`
Injects: WebFetchService, WebSearchService, ClaudeApiService, ExternalApiSettingsService
- fetchUrl(), search(), getStatus()

=== ScheduledEventController.java === Base Path: `/api/events`
- listEvents(), getEvent(), createEvent(), updateEvent(), deleteEvent(), toggleEvent()

=== KnowledgeController.java === Base Path: `/api/knowledge`
Injects: KnowledgeService, SemanticSearchService, SystemConfigService, FileStorageService
- uploadDocument(), listDocuments(), getDocument(), updateDisplayName(), deleteDocument(), retryProcessing(), downloadDocument(), getDocumentContent(), createDocument(), updateDocumentContent(), searchKnowledge()

=== LibraryController.java === Base Path: `/api/library`
Injects: ZimFileService, EbookService, GutenbergService
- uploadZim(), listZimFiles(), deleteZim(), kiwixStatus(), kiwixUrl()
- uploadEbook(), listEbooks(), getEbook(), deleteEbook(), downloadEbook()
- searchGutenberg(), getGutenbergBook(), importGutenberg()

=== McpDiscoveryController.java === Base Path: `/api/mcp`
- getClaudeDesktopConfig() — generates Claude Desktop config snippet

=== McpTokenController.java === Base Path: `/api/mcp/tokens`
- createToken(), listTokens(), revokeToken()

=== MemoryController.java === Base Path: `/api/memory`
- listMemories(), getMemory(), deleteMemory(), updateImportance(), updateTags(), searchMemories(), exportMemories()

=== ModelDownloadController.java === Base Path: `/api/models`
Injects: ModelCatalogService, ModelDownloadService, ModelDownloadProgressRegistry, LlamaServerProcessService
- searchCatalog(), getModelDetails(), getModelFiles()
- startDownload(), getAllDownloads(), getDownloadProgress() (SSE), cancelDownload()
- listLocalModels(), deleteLocalModel()
- setActiveModel(), getServerStatus(), restartServer()

=== DeviceRegistrationController.java === Base Path: `/api/notifications/devices`
- registerDevice(), getDevices(), unregisterDevice()

=== PrivacyController.java === Base Path: `/api/privacy`
- getFortressStatus(), enableFortress(), disableFortress()
- getSovereigntyReport()
- getAuditLogs() (3 filter variants)
- exportData(), wipeData(), wipeSelfData()

=== ProactiveController.java === Base Paths: `/api/insights`, `/api/notifications`
- getInsights(), generateInsights(), markInsightRead(), dismissInsight(), getInsightUnreadCount()
- getNotifications(), markNotificationRead(), markAllNotificationsRead(), getNotificationUnreadCount(), deleteNotification(), streamNotifications() (SSE)

=== SensorController.java === Base Path: `/api/sensors`
- listSensors(), getSensor(), registerSensor(), deleteSensor()
- startSensor(), stopSensor()
- getLatestReading(), getReadingHistory()
- updateThresholds(), testConnection(), listAvailablePorts()
- streamSensor() (SSE)

=== ExternalApiSettingsController.java === Base Path: `/api/settings/external-apis`
- getSettings(), updateSettings()

=== SkillController.java === Base Path: `/api/skills`
- listSkills(), getSkill(), toggleSkill()
- executeSkill(), listExecutions()
- listInventory(), createInventoryItem(), updateInventoryItem(), deleteInventoryItem()

=== CaptivePortalController.java === Base Paths: `/setup`, `/api/setup/wifi`
- setupWelcome(), setupWifi(), setupAccount(), setupConfirm() — forwards to static HTML
- scanWifi(), connectWifi(), wifiStatus()

=== SystemController.java === Base Path: `/api/system`
- getStatus(), initialize(), finalizeSetup()
- getAiSettings(), updateAiSettings()
- getStorageSettings(), updateStorageSettings()
- factoryReset()

---

## 11. Security Configuration

```
Authentication: JWT (stateless, Bearer token, internal issuer)
Token issuer/validator: Internal (JwtService)
Password encoder: BCrypt (default 10 rounds)

Public endpoints (no auth required):
  - /api/auth/login, /api/auth/register, /api/auth/refresh
  - /api/system/status, /api/system/initialize, /api/system/finalize-setup
  - /api/setup/**
  - /api/models, /api/models/health
  - /setup/**
  - /actuator/health
  - /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html
  - /mcp/** (uses separate McpAuthFilter with bearer token)

Protected endpoints (patterns):
  - /api/** → authenticated (any role)
  - Method-level: @PreAuthorize annotations on admin endpoints

CORS: All origins (*), all methods, all headers, credentials enabled
CSRF: Disabled (stateless REST API)
Rate limiting: 10 req/min auth endpoints, 200 req/min general (Bucket4j)
```

---

## 12. Custom Security Components

=== JwtAuthFilter.java ===
Extends: OncePerRequestFilter
Extracts token from: Authorization header (Bearer scheme)
Validates via: JwtService.isTokenValid() + AuthService.isTokenBlacklisted()
Sets SecurityContext: YES

=== McpAuthFilter.java ===
Extends: OncePerRequestFilter (filters only /mcp/** paths)
Extracts token from: Authorization header (Bearer scheme)
Validates via: McpTokenService.validateToken() (BCrypt hash matching)
Sets SecurityContext: YES (McpAuthentication with ownerUserId)

=== AuditAspect.java ===
@Aspect intercepting all controller methods
Logs: userId, action, resourceType, httpMethod, requestPath, ipAddress, responseStatus, outcome, durationMs

---

## 13. Exception Handling & Error Responses

=== GlobalExceptionHandler.java ===
@ControllerAdvice: YES

Exception Mappings:
  - MethodArgumentNotValidException → 400 (field error details)
  - UsernameNotFoundException → 404
  - BadCredentialsException → 401
  - AccessDeniedException → 403
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
  - AsyncRequestNotUsableException → (no response, DEBUG logged)
  - Exception (catch-all) → 500

Standard error response format:
```json
{
  "success": false,
  "message": "error description",
  "data": null,
  "timestamp": "2026-03-17T...",
  "requestId": "uuid"
}
```

Custom exceptions (14 total): EntityNotFoundException, DuplicateResourceException, OllamaUnavailableException, OllamaInferenceException, FortressActiveException, FortressOperationException, StorageException, EmbeddingException, UnsupportedFileTypeException, OcrException, SkillDisabledException, ApModeException, SensorConnectionException, InitializationException

---

## 14. Mappers / DTOs

Framework: Manual mapping (no MapStruct/ModelMapper). DTO conversions in service methods (`toDto`, `toSummary`, `toDetail`).

---

## 15. Utility Classes & Shared Components

=== AppConstants.java === (608 lines)
Centralized constants for all 16 feature packages: server ports, JWT config, API paths, pagination defaults, file upload limits, sensor config, RAG settings, MIME types, rate limiting, MQTT topics, HuggingFace config, quantization settings.

=== AesEncryptionUtil.java ===
AES-256-GCM encryption. 12-byte random IV prepended to ciphertext. Key from `app.encryption.key` (64-char hex). Used by AesAttributeConverter for encrypting API keys at rest.

=== AesAttributeConverter.java ===
JPA @Converter for transparent field-level encryption via AesEncryptionUtil.

=== TokenCounter.java ===
- `estimateTokens(String): int` — 1 token ≈ 4 chars approximation
- `truncateToTokenLimit(List<OllamaMessage>, int maxTokens): List<OllamaMessage>` — preserves system + last user

=== DeltaJsonUtils.java ===
- `textToDeltaJson(String): String` — plain text → Quill Delta JSON
- `deltaJsonToText(String): String` — Quill Delta JSON → plain text

=== ApiResponse.java ===
Generic response wrapper: `ApiResponse<T>` with `success`, `message`, `data`, `timestamp`, `requestId`, pagination fields. Static factories: `success()`, `error()`, `paginated()`.

---

## 16. Database Schema (Live)

Database not available for live inspection. Schema derived from JPA entities:
- 23 tables mapped from @Entity classes
- All primary keys: UUID with GenerationType.UUID
- Indexes defined on frequently-queried columns (user_id, timestamps)
- pgvector extension required for vector_document table (768-dim float arrays)
- Singleton tables: system_config, external_api_settings (unique guard columns)
- No Flyway migrations in dev (Hibernate DDL: update). Flyway enabled in prod.

---

## 17. MESSAGE BROKER DETECTION

No RabbitMQ or Kafka detected. MQTT (Eclipse Paho) used for push notifications:
```
Broker: MQTT (Paho v3)
Conditional: @ConditionalOnProperty(app.mqtt.enabled=true), disabled in dev
Topics:
  - myoffgridai/notifications/{userId} — per-user notifications
  - myoffgridai/sensors/readings — sensor data
  - myoffgridai/system/alerts — system alerts
  - myoffgridai/insights — AI insights
Publisher: MqttPublisherService
QoS: 1 (at least once)
```

---

## 18. CACHE DETECTION

No Redis or caching layer detected. No @Cacheable, @CacheEvict, or CacheManager usage.

---

## 19. ENVIRONMENT VARIABLE INVENTORY

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|-----------------|
| DB_URL | application-prod.yml | jdbc:postgresql://localhost:5432/myoffgridai | YES |
| DB_USERNAME | application-prod.yml | myoffgridai | YES |
| DB_PASSWORD | application-prod.yml | myoffgridai | YES |
| JWT_SECRET | application-prod.yml | (none) | YES |
| JWT_EXPIRATION_MS | application-prod.yml | 86400000 | NO |
| JWT_REFRESH_EXPIRATION_MS | application-prod.yml | 604800000 | NO |
| ENCRYPTION_KEY | application-prod.yml | (none) | YES |
| INFERENCE_PROVIDER | application-prod.yml | llama-server | NO |
| INFERENCE_BASE_URL | application-prod.yml | http://localhost:1234 | NO |
| INFERENCE_MODEL | application-prod.yml | Qwen3.5-27B... | NO |
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

---

## 20. SERVICE DEPENDENCY MAP

Standalone service — no inter-service dependencies. All dependencies are external:
- PostgreSQL database (required)
- Ollama or llama-server binary (required for inference)
- MQTT broker (optional, for push notifications)
- Anthropic Claude API (optional, for web enrichment)
- Brave Search API (optional, for web search)
- HuggingFace API (optional, for model catalog)
- Gutendex API (optional, for Project Gutenberg)
- Kiwix server (optional, for ZIM file serving)
- Calibre Content Server (optional, for eBook conversion)
- Tesseract OCR (optional, for image text extraction)

---

## 21. Known Technical Debt & Issues

### TODO/Placeholder/Stub Scan Results

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| "TEMPERATURE" matches TODO scan (false positive) | SensorType.java:7 | N/A | Enum value, not a TODO |
| "stubbed for MI-002" in Javadoc | UsbResetWatcherService.java:20,45 | Medium | Update functionality is documented as stubbed for future milestone MI-002; the code handles the case gracefully (logs and returns) |

### Dependency Vulnerabilities (Snyk)

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| Incorrect Authorization | tomcat-embed-core@10.1.50 | HIGH | Fix: upgrade Spring Boot (pulls tomcat 9.0.114+) |
| External Initialization of Trusted Variables | logback-core@1.5.22 | LOW | Fix: upgrade to 1.5.25 |

### Architecture Notes

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| No @Version (optimistic locking) on any entity | All entities | Medium | Could cause lost-update in concurrent writes |
| No soft-delete pattern | All entities | Low | Hard deletes only |
| CORS allows all origins in dev | SecurityConfig.java | Low | Appropriate for local network appliance |
| API paths not versioned (/api/ not /api/v1/) | All controllers | Low | Single-deployment appliance, acceptable |
| No OpenAPI/Swagger annotations | All controllers | Low | springdoc-openapi auto-generates from code |
| Token blacklist is in-memory (lost on restart) | AuthService.java | Medium | Acceptable for single-node appliance with 24h token expiry |
| SQL string concatenation | 1 occurrence found by SEC-03 check | Medium | Needs manual verification |

---

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
| HIGH | tomcat-embed-core | 10.1.50 | Incorrect Authorization | Upgrade to 9.0.114+ |
| LOW | logback-core | 1.5.22 | External Initialization of Trusted Variables | Upgrade to 1.5.25 |

### Code Vulnerabilities (SAST)
SNYK CODE: SKIPPED — Snyk Code not enabled for organization. Not a gap in the codebase.

### IaC Findings
Not applicable (no Terraform/k8s).

---

## Filter Chain Execution Order

1. @Order(1) **CaptivePortalRedirectFilter** — Redirects unrecognized requests to /setup during AP mode
2. @Order(2) **MdcFilter** — Populates MDC (requestId, username, userId)
3. @Order(3) **RateLimitingFilter** — Bucket4j rate limiting (10/200 req/min tiers)
4. @Order(4) **RequestResponseLoggingFilter** — DEBUG-level HTTP metadata logging
5. **JwtAuthFilter** — JWT validation and SecurityContext population
6. **McpAuthFilter** — MCP token validation (only /mcp/** paths)
