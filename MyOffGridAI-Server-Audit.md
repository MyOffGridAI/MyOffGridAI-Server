# MyOffGridAI Server — Codebase Audit

**Audit Date:** 2026-03-16T00:39:21Z
**Branch:** main
**Commit:** 0a0f3cad50eb19537fdc07dc161f9f5c2ad23430 Handle client disconnect gracefully in GlobalExceptionHandler
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** MyOffGridAI-Server-Audit.md
**Scorecard:** MyOffGridAI-Server-Scorecard.md
**OpenAPI Spec:** MyOffGridAI-Server-OpenAPI.yaml (generated separately)

> This audit is the source of truth for the MyOffGridAI Server codebase structure, entities, services, and configuration.
> The OpenAPI spec (MyOffGridAI-Server-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: MyOffGridAI Server
Repository URL: (local — ~/Documents/GitHub/MyOffGridAI-Server)
Primary Language / Framework: Java 21 / Spring Boot 3.4.3
Java Version: 21
Build Tool + Version: Maven (spring-boot-starter-parent 3.4.3)
Current Branch: main
Latest Commit Hash: 0a0f3cad50eb19537fdc07dc161f9f5c2ad23430
Latest Commit Message: Handle client disconnect gracefully in GlobalExceptionHandler
Audit Timestamp: 2026-03-16T00:39:21Z
```

---

## 2. Directory Structure

Single-module Spring Boot project. Source root: `src/main/java/com/myoffgridai/`. Organized by feature domain:

```
src/main/java/com/myoffgridai/
├── MyOffGridAiApplication.java          ← Entry point (@SpringBootApplication, @EnableAsync, @EnableScheduling)
├── ai/                                  ← Chat, LLM, conversations, agent
│   ├── controller/ (ChatController, ModelController)
│   ├── dto/ (13 DTOs)
│   ├── model/ (Conversation, Message, MessageRole)
│   ├── repository/ (ConversationRepository, MessageRepository)
│   └── service/ (AgentService, ChatService, ContextWindowService, ModelHealthCheckService, OllamaService, SystemPromptBuilder)
├── auth/                                ← Authentication, users, JWT
│   ├── controller/ (AuthController, UserController)
│   ├── dto/ (8 DTOs)
│   ├── model/ (User, Role)
│   ├── repository/ (UserRepository)
│   └── service/ (AuthService, JwtService, UserService)
├── common/                              ← Shared exceptions, responses, utilities
│   ├── exception/ (GlobalExceptionHandler + 14 custom exceptions)
│   ├── response/ (ApiResponse)
│   └── util/ (TokenCounter)
├── config/                              ← Security, JPA, Ollama, Vector store configs
│   ├── AppConstants.java, SecurityConfig.java, JwtAuthFilter.java
│   ├── CaptivePortalRedirectFilter.java, JpaConfig.java
│   ├── OllamaConfig.java, VectorStoreConfig.java, VectorType.java
├── events/                              ← Scheduled/sensor-triggered events
│   ├── controller/ (ScheduledEventController)
│   ├── dto/ (3 DTOs)
│   ├── model/ (ScheduledEvent, EventType, ActionType, ThresholdOperator)
│   ├── repository/ (ScheduledEventRepository)
│   └── service/ (ScheduledEventService)
├── knowledge/                           ← Document ingestion, chunking, semantic search
│   ├── controller/ (KnowledgeController)
│   ├── dto/ (10 DTOs)
│   ├── model/ (KnowledgeDocument, KnowledgeChunk, DocumentStatus)
│   ├── repository/ (KnowledgeDocumentRepository, KnowledgeChunkRepository)
│   ├── service/ (KnowledgeService, IngestionService, ChunkingService, FileStorageService, OcrService, SemanticSearchService, StorageHealthService)
│   └── util/ (DeltaJsonUtils)
├── memory/                              ← Memory, embeddings, RAG, vector store
│   ├── controller/ (MemoryController)
│   ├── dto/ (6 DTOs)
│   ├── model/ (Memory, MemoryImportance, VectorDocument, VectorSourceType)
│   ├── repository/ (MemoryRepository, VectorDocumentRepository)
│   └── service/ (EmbeddingService, MemoryExtractionService, MemoryService, RagService, SummarizationService)
├── privacy/                             ← Audit logging, data export/wipe, fortress mode
│   ├── aspect/ (AuditAspect)
│   ├── controller/ (PrivacyController)
│   ├── dto/ (7 DTOs)
│   ├── model/ (AuditLog, AuditOutcome)
│   ├── repository/ (AuditLogRepository)
│   └── service/ (AuditService, DataExportService, DataWipeService, FortressService, SovereigntyReportService)
├── proactive/                           ← AI insights, notifications, health monitoring
│   ├── controller/ (ProactiveController)
│   ├── dto/ (3 DTOs)
│   ├── model/ (Insight, InsightCategory, Notification, NotificationType)
│   ├── repository/ (InsightRepository, NotificationRepository)
│   └── service/ (InsightGeneratorService, InsightService, NightlyInsightJob, NotificationService, NotificationSseRegistry, PatternAnalysisService, SystemHealthMonitor)
├── sensors/                             ← IoT sensor management, serial port polling
│   ├── controller/ (SensorController)
│   ├── dto/ (5 DTOs)
│   ├── model/ (Sensor, SensorReading, SensorType, DataFormat)
│   ├── repository/ (SensorRepository, SensorReadingRepository)
│   └── service/ (SensorService, SensorPollingService, SensorStartupService, SerialPortService, SseEmitterRegistry)
├── skills/                              ← Built-in AI skills, inventory, task planning
│   ├── builtin/ (6 skill implementations)
│   ├── controller/ (SkillController)
│   ├── dto/ (6 DTOs)
│   ├── model/ (Skill, SkillExecution, SkillCategory, ExecutionStatus, InventoryItem, InventoryCategory, PlannedTask, TaskStatus)
│   ├── repository/ (SkillRepository, SkillExecutionRepository, InventoryItemRepository, PlannedTaskRepository)
│   └── service/ (BuiltInSkill interface, SkillExecutorService, SkillSeederService)
└── system/                              ← System config, AP mode, factory reset, captive portal
    ├── controller/ (SystemController, CaptivePortalController)
    ├── dto/ (7 DTOs)
    ├── model/ (SystemConfig)
    ├── repository/ (SystemConfigRepository)
    └── service/ (SystemConfigService, ApModeService, ApModeStartupService, FactoryResetService, NetworkTransitionService, UsbResetWatcherService)
```

Test files mirror production structure under `src/test/java/com/myoffgridai/`. Integration tests in `src/test/java/com/myoffgridai/integration/` (22 files). Configuration: `src/main/resources/application.yml`, `src/main/resources/application-prod.yml`, `src/test/resources/application-test.yml`.

---

## 3. Build & Dependency Manifest

**File:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.3 (parent) | REST API |
| spring-boot-starter-data-jpa | 3.4.3 | JPA/Hibernate |
| spring-boot-starter-security | 3.4.3 | Authentication/authorization |
| spring-boot-starter-validation | 3.4.3 | Bean validation |
| spring-boot-starter-actuator | 3.4.3 | Health endpoints |
| spring-boot-starter-webflux | 3.4.3 | Reactive WebClient for SSE streaming |
| spring-boot-starter-aop | 3.4.3 | Aspect-oriented audit logging |
| jjwt-api/impl/jackson | 0.12.6 | JWT token generation/validation |
| postgresql | (managed) | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector Java support |
| pdfbox | 3.0.4 | PDF text extraction |
| poi / poi-ooxml / poi-scratchpad | 5.3.0 | Office document extraction |
| tess4j | 5.13.0 | OCR (Tesseract) |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger UI |
| lombok | 1.18.42 | Compile-time code generation |
| spring-boot-starter-test | 3.4.3 | Testing |
| reactor-test | (managed) | Reactive stream testing |
| spring-security-test | (managed) | Security test utilities |
| testcontainers postgresql | 1.20.6 | PostgreSQL containers for ITs |

**Build Plugins:** spring-boot-maven-plugin (excludes Lombok), maven-compiler-plugin (Java 21, Lombok annotation processor), maven-surefire-plugin (JVM module opens for reflection). Native profile available (GraalVM via native-maven-plugin 0.10.4).

**Build Commands:**
```
Build: mvn clean compile -DskipTests
Test: mvn test
Run: mvn spring-boot:run
Package: mvn package -DskipTests
Native: mvn -Pnative package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Default profile: `dev`. Server port: `8080`. Flyway disabled. Multipart max: 100MB. Dev DB: `jdbc:postgresql://localhost:5432/myoffgridai` (user/pass: myoffgridai). Hibernate DDL: `update`. JWT secret: dev-only hardcoded. Ollama: `localhost:11434`, model `hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M`, embed `nomic-embed-text`. Fortress mock: true. AP mock: true.
- **`application-prod.yml`** — Hibernate DDL: `validate`. Flyway enabled (`classpath:db/migration`, baseline-on-migrate). Fortress mock: false. AP mock: false.
- **`application-test.yml`** — Hibernate DDL: `create-drop`. JWT: test-only secret. AP/Fortress mock: true.

**Connection Map:**
```
Database: PostgreSQL, localhost:5432, myoffgridai
Cache: None
Message Broker: None
External APIs: Ollama LLM (localhost:11434)
Cloud Services: None
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

**Entry Point:** `com.myoffgridai.MyOffGridAiApplication` (`@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`)

**Startup Initializers:**
- `JpaConfig` — Enables JPA auditing (`@EnableJpaAuditing`)
- `VectorStoreConfig.checkPgvectorExtension()` — Verifies pgvector extension exists (warns, does not fail)
- `ApModeStartupService.onApplicationReady()` — Starts AP mode if system not initialized; stops AP mode if initialized
- `SkillSeederService.seedBuiltInSkills()` — Seeds 6 built-in skills if absent
- `ModelHealthCheckService.checkOllamaOnStartup()` — Verifies Ollama availability, logs model list
- `SensorStartupService.resumeActiveSensors()` — Resumes polling for sensors flagged active
- `StorageHealthService.checkStorageDirectory()` — Verifies/creates knowledge storage directory

**Scheduled Tasks:**
- `NightlyInsightJob.generateNightlyInsights()` — Cron `0 0 3 * * *` (3am daily) — generates AI insights for all active users
- `SummarizationService.scheduledNightlySummarization()` — Cron `0 0 2 * * *` (2am daily) — summarizes old conversations into memories
- `SystemHealthMonitor.checkSystemHealth()` — Fixed delay 5min — checks disk, Ollama, heap
- `UsbResetWatcherService.checkForTriggerFiles()` — Fixed delay 30s — watches USB for reset/update triggers

**Health Check:** `GET /actuator/health` (Spring Actuator, public endpoint)

---

## 6. Entity / Data Model Layer

### User
```
=== User.java ===
Table: users
Primary Key: id : UUID : GenerationType.UUID
Implements: UserDetails

Fields:
  - id: UUID (PK)
  - username: String @Column(nullable=false, unique=true)
  - email: String @Column(unique=true) (nullable)
  - displayName: String @Column(name="display_name", nullable=false)
  - passwordHash: String @Column(name="password_hash", nullable=false)
  - role: Role @Enumerated(STRING) @Column(nullable=false)
  - isActive: boolean @Column(nullable=false) default true
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate
  - lastLoginAt: Instant (nullable)

Relationships: None
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
Validation: None (at DTO layer)
UserDetails: getAuthorities() returns role.name() as SimpleGrantedAuthority; isAccountNonLocked() returns isActive
```

### Conversation
```
=== Conversation.java ===
Table: conversations
Primary Key: id : UUID : GenerationType.UUID

Fields:
  - id: UUID (PK)
  - user: User @ManyToOne(fetch=LAZY) @JoinColumn(name="user_id", nullable=false)
  - title: String (nullable)
  - isArchived: boolean @Column(nullable=false) default false
  - messageCount: int @Column(nullable=false) default 0
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: @ManyToOne → User (fetch=LAZY)
Audit Fields: createdAt, updatedAt
```

### Message
```
=== Message.java ===
Table: messages
Primary Key: id : UUID : GenerationType.UUID

Fields:
  - id: UUID (PK)
  - conversation: Conversation @ManyToOne(fetch=LAZY) @JoinColumn(name="conversation_id", nullable=false)
  - role: MessageRole @Enumerated(STRING) @Column(nullable=false)
  - content: String @Column(nullable=false, columnDefinition="TEXT")
  - tokenCount: Integer (nullable)
  - hasRagContext: boolean @Column(nullable=false) default false
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)

Relationships: @ManyToOne → Conversation (fetch=LAZY)
Audit Fields: createdAt
```

### Memory
```
=== Memory.java ===
Table: memories
Primary Key: id : UUID : GenerationType.UUID

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - content: String @Column(nullable=false, columnDefinition="TEXT")
  - importance: MemoryImportance @Enumerated(STRING) @Column(nullable=false)
  - tags: String (nullable)
  - sourceConversationId: UUID (nullable)
  - accessCount: int @Column(nullable=false) default 0
  - lastAccessedAt: Instant (nullable)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None (userId is non-FK reference)
Audit Fields: createdAt, updatedAt
```

### VectorDocument
```
=== VectorDocument.java ===
Table: vector_document
Primary Key: id : UUID : GenerationType.UUID
Index: idx_vector_doc_user_source_type (user_id, source_type)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - content: String @Column(nullable=false, columnDefinition="TEXT")
  - embedding: float[] @Type(VectorType.class) @Column(columnDefinition="vector(768)")
  - sourceType: VectorSourceType @Enumerated(STRING) @Column(nullable=false)
  - sourceId: UUID (nullable — polymorphic ref to Memory.id or KnowledgeChunk.id)
  - metadata: String (nullable, TEXT)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)

Relationships: None (polymorphic via sourceType + sourceId)
Audit Fields: createdAt
Custom Hibernate Type: VectorType maps float[] ↔ pgvector vector(768)
```

### KnowledgeDocument
```
=== KnowledgeDocument.java ===
Table: knowledge_documents
Primary Key: id : UUID : GenerationType.UUID
Index: idx_knowledge_doc_user_id (user_id)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - filename: String @Column(nullable=false)
  - displayName: String (nullable)
  - mimeType: String @Column(nullable=false)
  - storagePath: String @Column(nullable=false)
  - fileSizeBytes: long
  - status: DocumentStatus @Enumerated(STRING) @Column(nullable=false) default PENDING
  - errorMessage: String (nullable, TEXT)
  - chunkCount: int
  - content: String (nullable, TEXT — Quill Delta JSON for editable docs)
  - uploadedAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - processedAt: Instant (nullable)

Relationships: None
Audit Fields: uploadedAt
```

### KnowledgeChunk
```
=== KnowledgeChunk.java ===
Table: knowledge_chunks
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_knowledge_chunk_doc_id, idx_knowledge_chunk_user_id

Fields:
  - id: UUID (PK)
  - document: KnowledgeDocument @ManyToOne(fetch=LAZY) @JoinColumn(name="document_id", nullable=false)
  - userId: UUID @Column(nullable=false)
  - chunkIndex: int @Column(nullable=false)
  - content: String @Column(nullable=false, columnDefinition="TEXT")
  - pageNumber: Integer (nullable)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)

Relationships: @ManyToOne → KnowledgeDocument (fetch=LAZY)
Audit Fields: createdAt
```

### Sensor
```
=== Sensor.java ===
Table: sensors
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_sensor_user_id (user_id), idx_sensor_port_path (port_path, UNIQUE)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false)
  - type: SensorType @Enumerated(STRING) @Column(nullable=false)
  - portPath: String @Column(nullable=false) — unique index
  - baudRate: int @Column(nullable=false) default 9600
  - dataFormat: DataFormat @Enumerated(STRING) @Column(nullable=false) default CSV_LINE
  - valueField: String (nullable — JSON key for JSON_LINE format)
  - unit: String (nullable)
  - isActive: boolean @Column(nullable=false) default false
  - pollIntervalSeconds: int @Column(nullable=false) default 30
  - lowThreshold: Double (nullable)
  - highThreshold: Double (nullable)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
```

### SensorReading
```
=== SensorReading.java ===
Table: sensor_readings
Primary Key: id : UUID : GenerationType.UUID
Index: idx_sensor_reading_sensor_recorded (sensor_id, recorded_at DESC)

Fields:
  - id: UUID (PK)
  - sensor: Sensor @ManyToOne(fetch=LAZY) @JoinColumn(name="sensor_id", nullable=false)
  - value: double @Column(nullable=false)
  - rawData: String (nullable)
  - recordedAt: Instant @Column(nullable=false)

Relationships: @ManyToOne → Sensor (fetch=LAZY)
Audit Fields: None
```

### ScheduledEvent
```
=== ScheduledEvent.java ===
Table: scheduled_events
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_event_user_id, idx_event_enabled_type (is_enabled, event_type)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false)
  - description: String (nullable, TEXT)
  - eventType: EventType @Enumerated(STRING) @Column(nullable=false)
  - isEnabled: boolean @Column(nullable=false) default true
  - cronExpression: String (nullable)
  - recurringIntervalMinutes: Integer (nullable)
  - sensorId: UUID (nullable)
  - thresholdOperator: ThresholdOperator @Enumerated(STRING) (nullable)
  - thresholdValue: Double (nullable)
  - actionType: ActionType @Enumerated(STRING) @Column(nullable=false)
  - actionPayload: String @Column(nullable=false, TEXT)
  - lastTriggeredAt: Instant (nullable)
  - nextFireAt: Instant (nullable)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
```

### AuditLog
```
=== AuditLog.java ===
Table: audit_logs
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_audit_user_timestamp (user_id, timestamp DESC), idx_audit_timestamp (timestamp DESC)

Fields:
  - id: UUID (PK)
  - userId: UUID (nullable)
  - username: String (nullable)
  - action: String @Column(nullable=false)
  - resourceType: String (nullable)
  - resourceId: String (nullable)
  - httpMethod: String @Column(nullable=false)
  - requestPath: String @Column(nullable=false)
  - ipAddress: String (nullable)
  - userAgent: String (nullable)
  - responseStatus: int
  - outcome: AuditOutcome @Enumerated(STRING) @Column(nullable=false)
  - durationMs: long
  - timestamp: Instant @Column(nullable=false)

Relationships: None
Audit Fields: None (timestamp set manually by AuditAspect)
```

### Insight
```
=== Insight.java ===
Table: insights
Primary Key: id : UUID : GenerationType.UUID
Index: idx_insight_user_id (user_id)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - content: String @Column(nullable=false, TEXT)
  - category: InsightCategory @Enumerated(STRING) @Column(nullable=false)
  - isRead: boolean @Column(nullable=false) default false
  - isDismissed: boolean @Column(nullable=false) default false
  - generatedAt: Instant @Column(nullable=false, updatable=false)
  - readAt: Instant (nullable)

Relationships: None
Audit Fields: generatedAt (set via @PrePersist)
```

### Notification
```
=== Notification.java ===
Table: notifications
Primary Key: id : UUID : GenerationType.UUID
Index: idx_notification_user_id (user_id)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - title: String @Column(nullable=false)
  - body: String @Column(nullable=false, TEXT)
  - type: NotificationType @Enumerated(STRING) @Column(nullable=false)
  - isRead: boolean @Column(nullable=false) default false
  - createdAt: Instant @Column(nullable=false, updatable=false)
  - readAt: Instant (nullable)
  - metadata: String (nullable, TEXT)

Relationships: None
Audit Fields: createdAt (set via @PrePersist)
```

### Skill
```
=== Skill.java ===
Table: skills
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_skill_name (name, UNIQUE), idx_skill_category (category)

Fields:
  - id: UUID (PK)
  - name: String @Column(nullable=false, unique=true)
  - displayName: String @Column(nullable=false)
  - description: String @Column(nullable=false, TEXT)
  - version: String @Column(nullable=false)
  - author: String @Column(nullable=false)
  - category: SkillCategory @Enumerated(STRING) @Column(nullable=false)
  - isEnabled: boolean @Column(nullable=false) default true
  - isBuiltIn: boolean @Column(nullable=false) default false
  - parametersSchema: String (nullable, TEXT — JSON schema)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
```

### SkillExecution
```
=== SkillExecution.java ===
Table: skill_executions
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_skill_exec_user_id, idx_skill_exec_skill_id

Fields:
  - id: UUID (PK)
  - skill: Skill @ManyToOne(fetch=LAZY) @JoinColumn(name="skill_id", nullable=false)
  - userId: UUID @Column(nullable=false)
  - status: ExecutionStatus @Enumerated(STRING) @Column(nullable=false)
  - inputParams: String (nullable, TEXT)
  - outputResult: String (nullable, TEXT)
  - errorMessage: String (nullable, TEXT)
  - startedAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - completedAt: Instant (nullable)
  - durationMs: Long (nullable)

Relationships: @ManyToOne → Skill (fetch=LAZY)
Audit Fields: startedAt
```

### InventoryItem
```
=== InventoryItem.java ===
Table: inventory_items
Primary Key: id : UUID : GenerationType.UUID
Indexes: idx_inventory_user_id, idx_inventory_category (user_id, category)

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - name: String @Column(nullable=false)
  - category: InventoryCategory @Enumerated(STRING) @Column(nullable=false)
  - quantity: double @Column(nullable=false)
  - unit: String (nullable)
  - notes: String (nullable, TEXT)
  - lowStockThreshold: Double (nullable)
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
```

### PlannedTask
```
=== PlannedTask.java ===
Table: planned_tasks
Primary Key: id : UUID : GenerationType.UUID
Index: idx_planned_task_user_id

Fields:
  - id: UUID (PK)
  - userId: UUID @Column(nullable=false)
  - goalDescription: String @Column(nullable=false, TEXT)
  - title: String @Column(nullable=false)
  - steps: String @Column(nullable=false, TEXT)
  - estimatedResources: String (nullable, TEXT)
  - status: TaskStatus @Enumerated(STRING) @Column(nullable=false) default ACTIVE
  - createdAt: Instant @CreatedDate @Column(nullable=false, updatable=false)
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
```

### SystemConfig
```
=== SystemConfig.java ===
Table: system_config (SINGLE-ROW table)
Primary Key: id : UUID : GenerationType.UUID

Fields:
  - id: UUID (PK)
  - initialized: boolean @Column(nullable=false) default false
  - instanceName: String (nullable)
  - fortressEnabled: boolean @Column(nullable=false) default false
  - fortressEnabledAt: Instant (nullable)
  - fortressEnabledByUserId: UUID (nullable)
  - apModeEnabled: boolean @Column(nullable=false) default false
  - wifiConfigured: boolean @Column(nullable=false) default false
  - aiModel: String default "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M"
  - aiTemperature: Double default 0.7
  - aiSimilarityThreshold: Double default 0.45
  - aiMemoryTopK: Integer default 5
  - aiRagMaxContextTokens: Integer default 2048
  - aiContextSize: Integer default 4096
  - aiContextMessageLimit: Integer default 20
  - knowledgeStoragePath: String default "/var/myoffgridai/knowledge"
  - maxUploadSizeMb: Integer default 25
  - createdAt: Instant @CreatedDate
  - updatedAt: Instant @LastModifiedDate

Relationships: None
Audit Fields: createdAt, updatedAt
Design Note: All AI/storage getters have null-safe fallbacks returning defaults.
```

---

## 7. Enum Inventory

| Enum | Values | Used In |
|---|---|---|
| `Role` | ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD | User.role |
| `MessageRole` | USER, ASSISTANT, SYSTEM | Message.role |
| `MemoryImportance` | LOW, MEDIUM, HIGH, CRITICAL | Memory.importance |
| `VectorSourceType` | MEMORY, CONVERSATION, KNOWLEDGE_CHUNK | VectorDocument.sourceType |
| `DocumentStatus` | PENDING, PROCESSING, READY, FAILED | KnowledgeDocument.status |
| `SensorType` | TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM | Sensor.type |
| `DataFormat` | CSV_LINE, JSON_LINE | Sensor.dataFormat |
| `EventType` | SCHEDULED, SENSOR_THRESHOLD, RECURRING | ScheduledEvent.eventType |
| `ActionType` | PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY | ScheduledEvent.actionType |
| `ThresholdOperator` | ABOVE, BELOW, EQUALS | ScheduledEvent.thresholdOperator |
| `AuditOutcome` | SUCCESS, FAILURE, DENIED | AuditLog.outcome |
| `InsightCategory` | HOMESTEAD, HEALTH, RESOURCE, GENERAL | Insight.category |
| `NotificationType` | SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL | Notification.type |
| `SkillCategory` | HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM | Skill.category |
| `ExecutionStatus` | RUNNING, COMPLETED, FAILED | SkillExecution.status |
| `InventoryCategory` | FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER | InventoryItem.category |
| `TaskStatus` | ACTIVE, COMPLETED, CANCELLED | PlannedTask.status |

---

## 8. Repository Layer

```
=== UserRepository ===
Entity: User | Extends: JpaRepository<User, UUID>
Custom: findByUsername(String), findByEmail(String), existsByUsername(String), existsByEmail(String), findAllByRole(Role), countByIsActiveTrue(), findByIsActiveTrue()

=== ConversationRepository ===
Entity: Conversation | Extends: JpaRepository<Conversation, UUID>
Custom: findByUserIdOrderByUpdatedAtDesc(UUID, Pageable), findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID, boolean, Pageable), findByIdAndUserId(UUID, UUID), countByUserId(UUID), findByUserId(UUID), findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID, String, Pageable), @Modifying deleteByUserId(UUID)

=== MessageRepository ===
Entity: Message | Extends: JpaRepository<Message, UUID>
Custom: findByConversationIdOrderByCreatedAtAsc(UUID) [+pageable], findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable), countByConversationId(UUID), deleteByConversationId(UUID), @Query countByUserId(UUID), @Modifying @Query deleteByUserId(UUID)

=== MemoryRepository ===
Entity: Memory | Extends: JpaRepository<Memory, UUID>
Custom: findByUserIdOrderByCreatedAtDesc(UUID, Pageable), findByUserIdAndImportance(UUID, MemoryImportance, Pageable), findByUserIdAndTagsContaining(UUID, String, Pageable), findByUserId(UUID), @Modifying deleteByUserId(UUID), countByUserId(UUID)

=== VectorDocumentRepository ===
Entity: VectorDocument | Extends: JpaRepository<VectorDocument, UUID>
Custom: findByUserIdAndSourceType(UUID, VectorSourceType), @Modifying deleteBySourceIdAndSourceType(UUID, VectorSourceType), @Modifying deleteByUserId(UUID), @Query(nativeQuery) findMostSimilar(UUID, String, String, int) — pgvector cosine distance, @Query(nativeQuery) findMostSimilarAcrossTypes(UUID, String, int)

=== KnowledgeDocumentRepository ===
Entity: KnowledgeDocument | Extends: JpaRepository<KnowledgeDocument, UUID>
Custom: findByUserIdOrderByUploadedAtDesc(UUID, Pageable), findByIdAndUserId(UUID, UUID), findByUserIdAndStatus(UUID, DocumentStatus), @Modifying deleteByUserId(UUID), countByUserId(UUID)

=== KnowledgeChunkRepository ===
Entity: KnowledgeChunk | Extends: JpaRepository<KnowledgeChunk, UUID>
Custom: findByDocumentIdOrderByChunkIndexAsc(UUID), @Modifying deleteByDocumentId(UUID), @Modifying deleteByUserId(UUID), countByDocumentId(UUID)

=== SensorRepository ===
Entity: Sensor | Extends: JpaRepository<Sensor, UUID>
Custom: findByUserIdOrderByNameAsc(UUID), findByIdAndUserId(UUID, UUID), findByUserIdAndIsActiveTrue(UUID), findByPortPath(String), findByIsActiveTrue(), countByUserId(UUID), deleteByUserId(UUID)

=== SensorReadingRepository ===
Entity: SensorReading | Extends: JpaRepository<SensorReading, UUID>
Custom: findBySensorIdOrderByRecordedAtDesc(UUID, Pageable), findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID, Instant), findTopBySensorIdOrderByRecordedAtDesc(UUID), @Modifying deleteBySensorId(UUID), @Modifying @Query deleteByUserId(UUID), @Query(nativeQuery) findAverageValueSince(UUID, Instant)

=== ScheduledEventRepository ===
Entity: ScheduledEvent | Extends: JpaRepository<ScheduledEvent, UUID>
Custom: findAllByUserId(UUID, Pageable), findByIdAndUserId(UUID, UUID), findByIsEnabledTrueAndEventType(EventType), findAllByUserIdOrderByCreatedAtDesc(UUID), deleteByUserId(UUID), countByUserId(UUID)

=== AuditLogRepository ===
Entity: AuditLog | Extends: JpaRepository<AuditLog, UUID>
Custom: findAllByOrderByTimestampDesc(Pageable), findByUserIdOrderByTimestampDesc(UUID, Pageable), findByOutcomeOrderByTimestampDesc(AuditOutcome, Pageable), findByTimestampBetweenOrderByTimestampDesc(Instant, Instant, Pageable), findByUserIdAndTimestampBetween(UUID, Instant, Instant, Pageable), countByOutcomeAndTimestampBetween(AuditOutcome, Instant, Instant), @Modifying deleteByTimestampBefore(Instant), @Modifying deleteByUserId(UUID)

=== InsightRepository ===
Entity: Insight | Extends: JpaRepository<Insight, UUID>
Custom: findByUserIdAndIsDismissedFalseOrderByGeneratedAtDesc(UUID, Pageable), findByUserIdAndCategoryAndIsDismissedFalse(UUID, InsightCategory, Pageable), findByUserIdAndIsReadFalseAndIsDismissedFalse(UUID), countByUserIdAndIsReadFalseAndIsDismissedFalse(UUID), findByIdAndUserId(UUID, UUID), countByUserId(UUID), @Modifying deleteByUserId(UUID)

=== NotificationRepository ===
Entity: Notification | Extends: JpaRepository<Notification, UUID>
Custom: findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID), findByUserIdOrderByCreatedAtDesc(UUID, Pageable), countByUserIdAndIsReadFalse(UUID), findByIdAndUserId(UUID, UUID), @Modifying @Query markAllReadForUser(UUID, Instant), @Modifying deleteByUserId(UUID)

=== SkillRepository ===
Entity: Skill | Extends: JpaRepository<Skill, UUID>
Custom: findByIsEnabledTrue(), findByIsBuiltInTrue(), findByCategory(SkillCategory), findByName(String), findByIsEnabledTrueOrderByDisplayNameAsc()

=== SkillExecutionRepository ===
Entity: SkillExecution | Extends: JpaRepository<SkillExecution, UUID>
Custom: findByUserIdOrderByStartedAtDesc(UUID, Pageable), findBySkillIdAndUserIdOrderByStartedAtDesc(UUID, UUID, Pageable), findByUserIdAndStatus(UUID, ExecutionStatus)

=== InventoryItemRepository ===
Entity: InventoryItem | Extends: JpaRepository<InventoryItem, UUID>
Custom: findByUserIdOrderByNameAsc(UUID), findByUserIdAndCategory(UUID, InventoryCategory), findByUserIdAndQuantityLessThanEqual(UUID, double), findByIdAndUserId(UUID, UUID), @Modifying deleteByUserId(UUID)

=== PlannedTaskRepository ===
Entity: PlannedTask | Extends: JpaRepository<PlannedTask, UUID>
Custom: findByUserIdAndStatusOrderByCreatedAtDesc(UUID, TaskStatus, Pageable), findByIdAndUserId(UUID, UUID), @Modifying deleteByUserId(UUID)

=== SystemConfigRepository ===
Entity: SystemConfig | Extends: JpaRepository<SystemConfig, UUID>
Custom: @Query findFirst() — fetches single system config row
```

---

## 9. Service Layer — Full Method Signatures

### AuthService
```
Injects: UserRepository, JwtService, PasswordEncoder, @Value profile
Public:
  - register(RegisterRequest): AuthResponse — registers user, validates uniqueness/password, @Transactional
  - login(LoginRequest): AuthResponse — authenticates, updates lastLoginAt, @Transactional
  - refresh(String refreshToken): AuthResponse — issues new access from valid refresh
  - logout(String token): void — in-memory blacklist
  - isTokenBlacklisted(String token): boolean
  - changePassword(UUID userId, ChangePasswordRequest): void — verifies current pw, @Transactional
Private: validatePassword(String), buildAuthResponse(User), toUserSummary(User)
```

### JwtService
```
Injects: @Value secret, accessExpirationMs, refreshExpirationMs
Public:
  - generateAccessToken(UserDetails): String
  - generateRefreshToken(UserDetails): String — includes type=refresh claim
  - extractUsername(String token): String
  - extractExpiration(String token): Date
  - isTokenValid(String token, UserDetails): boolean
  - isTokenExpired(String token): boolean
  - getAccessExpirationMs(): long
Private: extractClaim(String, Function), buildToken(Map, UserDetails, long)
```

### UserService
```
Injects: UserRepository
Public:
  - listUsers(int page, int size): Page<UserSummaryDto>
  - getUserById(UUID): UserDetailDto — throws EntityNotFoundException
  - updateUser(UUID, String displayName, String email, Role role): UserDetailDto — @Transactional
  - deactivateUser(UUID): void — @Transactional
  - deleteUser(UUID): void — @Transactional
```

### ChatService
```
Injects: ConversationRepository, MessageRepository, UserRepository, OllamaService, SystemPromptBuilder, ContextWindowService, RagService, MemoryExtractionService, SystemConfigService
Public:
  - createConversation(UUID userId, String title): Conversation — @Transactional
  - getConversations(UUID, boolean includeArchived, Pageable): Page<Conversation>
  - getConversation(UUID conversationId, UUID userId): Conversation
  - archiveConversation(UUID, UUID): void — @Transactional
  - deleteConversation(UUID, UUID): void — deletes messages too, @Transactional
  - sendMessage(UUID conversationId, UUID userId, String content): Message — sync Ollama call, @Transactional
  - streamMessage(UUID, UUID, String): Flux<String> — streaming Ollama call
  - searchConversations(UUID, String query, Pageable): Page<Conversation>
  - renameConversation(UUID, UUID, String): Conversation — @Transactional
  - generateTitle(UUID, String): void — @Async, LLM title generation
Private: buildRagContextSafely(UUID, String)
```

### OllamaService
```
Injects: @Qualifier("ollamaRestClient") RestClient, @Qualifier("ollamaWebClient") WebClient
Public:
  - isAvailable(): boolean — hits /api/tags
  - listModels(): List<OllamaModelInfo> — throws OllamaUnavailableException
  - chat(OllamaChatRequest): OllamaChatResponse — sync, throws OllamaUnavailableException/OllamaInferenceException
  - chatStream(OllamaChatRequest): Flux<OllamaChatChunk> — streaming
  - embed(String text): float[] — POST /api/embeddings
  - embedBatch(List<String>): List<float[]> — sequential embedding
```

### KnowledgeService
```
Injects: KnowledgeDocumentRepository, KnowledgeChunkRepository, VectorDocumentRepository, FileStorageService, IngestionService, OcrService, ChunkingService, EmbeddingService
Public:
  - upload(UUID, MultipartFile): KnowledgeDocumentDto — stores file, @Async processes, @Transactional
  - processDocumentAsync(UUID): void — @Async pipeline: extract→chunk→embed→store
  - listDocuments(UUID, Pageable): Page<KnowledgeDocumentDto> — @Transactional(readOnly)
  - getDocument(UUID, UUID): KnowledgeDocumentDto
  - updateDisplayName(UUID, UUID, String): KnowledgeDocumentDto — @Transactional
  - deleteDocument(UUID, UUID): void — deletes chunks, vectors, file, @Transactional
  - retryProcessing(UUID, UUID): KnowledgeDocumentDto — cleans+re-queues, @Transactional
  - getChunks(UUID, UUID): List<KnowledgeChunk>
  - getDocumentContent(UUID, UUID): DocumentContentDto
  - getDocumentForDownload(UUID, UUID): KnowledgeDocument
  - createFromEditor(UUID, String, String): KnowledgeDocumentDto — rich-text, @Transactional
  - updateContent(UUID, UUID, String): KnowledgeDocumentDto — re-chunks, @Transactional
  - deleteAllForUser(UUID): void — privacy wipe, @Transactional
```

### MemoryService
```
Injects: MemoryRepository, VectorDocumentRepository, EmbeddingService, SystemConfigService
Public:
  - createMemory(UUID, String content, MemoryImportance, String tags, UUID sourceConvId): Memory — @Transactional
  - findRelevantMemories(UUID, String, int topK): List<Memory> — vector similarity, @Transactional
  - searchMemoriesWithScores(UUID, String, int topK): List<MemorySearchResultDto> — @Transactional
  - getMemory(UUID, UUID): Memory — throws EntityNotFoundException, AccessDeniedException
  - updateImportance(UUID, UUID, MemoryImportance): Memory — @Transactional
  - updateTags(UUID, UUID, String): Memory — @Transactional
  - deleteMemory(UUID, UUID): void — deletes vector doc too, @Transactional
  - deleteAllMemoriesForUser(UUID): void — @Transactional
  - exportMemories(UUID): List<Memory>
  - getMemories(UUID, MemoryImportance, String tag, Pageable): Page<Memory>
```

### FortressService
```
Injects: SystemConfigService, UserRepository, @Value mockMode
Public:
  - enable(UUID userId): void — applies iptables rules (or mock), updates config
  - disable(UUID userId): void — flushes iptables (or mock), updates config
  - getFortressStatus(): FortressStatus
  - isFortressActive(): boolean
```

### DataWipeService
```
Injects: MessageRepository, ConversationRepository, MemoryService, KnowledgeService, SensorService, InsightService, NotificationService, InventoryItemRepository, PlannedTaskRepository, AuditService
Public:
  - wipeUser(UUID): WipeResult — atomic 10-step data deletion in FK order, @Transactional
```

### SystemConfigService
```
Injects: SystemConfigRepository
Public:
  - getConfig(): SystemConfig — creates default if absent
  - save(SystemConfig): SystemConfig
  - isInitialized(): boolean
  - setInitialized(String instanceName): SystemConfig
  - setFortressEnabled(boolean, UUID): SystemConfig
  - isWifiConfigured(): boolean
  - getAiSettings(): AiSettingsDto
  - getStorageSettings(): StorageSettingsDto
  - updateStorageSettings(StorageSettingsDto): StorageSettingsDto
  - updateAiSettings(AiSettingsDto): AiSettingsDto
```

### SkillExecutorService
```
Injects: SkillRepository, SkillExecutionRepository, List<BuiltInSkill>, ObjectMapper
Public:
  - execute(UUID skillId, UUID userId, Map params): SkillExecution — by ID, @Transactional
  - executeByName(String name, UUID userId, Map params): SkillExecution — by name (for agent), @Transactional
```

### SensorService, SensorPollingService, EmbeddingService, RagService, AuditService, InsightService, NotificationService, PatternAnalysisService, ScheduledEventService — documented in full in agent outputs. Key patterns: all use constructor injection, UserID scoping, EntityNotFoundException for missing resources.

---

## 10. Controller / API Layer

```
=== AuthController (base: /api/auth) ===
Injects: AuthService
  - register() → authService.register() [PUBLIC]
  - login() → authService.login() [PUBLIC]
  - refresh() → authService.refresh() [PUBLIC]
  - logout() → authService.logout() [Auth]

=== UserController (base: /api/users) ===
Injects: UserService
  - listUsers() → userService.listUsers() [OWNER/ADMIN]
  - getUser() → userService.getUserById() [Auth, self or OWNER/ADMIN]
  - updateUser() → userService.updateUser() [OWNER/ADMIN]
  - deactivateUser() → userService.deactivateUser() [OWNER]
  - deleteUser() → userService.deleteUser() [OWNER]

=== ChatController (base: /api/chat) ===
Injects: ChatService, MessageRepository
  - createConversation() → chatService.createConversation()
  - listConversations() → chatService.getConversations()
  - searchConversations() → chatService.searchConversations()
  - getConversation() → chatService.getConversation()
  - deleteConversation() → chatService.deleteConversation()
  - archiveConversation() → chatService.archiveConversation()
  - renameConversation() → chatService.renameConversation()
  - sendMessage() → chatService.sendMessage() / streamMessage() [SSE for stream=true]
  - listMessages() → messageRepository.findByConversationIdOrderByCreatedAtAsc()

=== ModelController (base: /api/models) ===
Injects: OllamaService, SystemConfigService
  - listModels() → ollamaService.listModels() [PUBLIC]
  - getActiveModel() → systemConfigService.getAiSettings() [Auth]
  - getHealth() → ollamaService.isAvailable() [PUBLIC]

=== KnowledgeController (base: /api/knowledge) ===
Injects: KnowledgeService, SemanticSearchService, SystemConfigService, FileStorageService
  - uploadDocument(), listDocuments(), getDocument(), updateDisplayName(), deleteDocument()
  - retryProcessing(), downloadDocument(), getDocumentContent(), createDocument(), updateDocumentContent()
  - searchKnowledge() → semanticSearchService.search()

=== MemoryController (base: /api/memory) ===
Injects: MemoryService
  - listMemories(), getMemory(), deleteMemory(), updateImportance(), updateTags()
  - searchMemories() → memoryService.searchMemoriesWithScores()
  - exportMemories()

=== SensorController (base: /api/sensors) ===
Injects: SensorService, SseEmitterRegistry
  - listSensors(), getSensor(), registerSensor(), deleteSensor()
  - startSensor(), stopSensor(), getLatestReading(), getReadingHistory()
  - updateThresholds(), testConnection(), listAvailablePorts()
  - streamSensor() → SseEmitter [SSE]

=== ScheduledEventController (base: /api/events) ===
Injects: ScheduledEventService
  - listEvents(), getEvent(), createEvent(), updateEvent(), deleteEvent(), toggleEvent()

=== PrivacyController (base: /api/privacy) ===
Injects: FortressService, AuditService, SovereigntyReportService, DataExportService, DataWipeService
  - getFortressStatus(), enableFortress() [OWNER/ADMIN], disableFortress() [OWNER/ADMIN]
  - getSovereigntyReport(), getAuditLogs() [OWNER/ADMIN see all]
  - exportData() → encrypted ZIP, wipeData() [OWNER/ADMIN], wipeSelfData() [any role]

=== ProactiveController (no class-level base path) ===
Injects: InsightService, InsightGeneratorService, NotificationService, NotificationSseRegistry
  - getInsights(), generateInsights(), markInsightRead(), dismissInsight(), getInsightUnreadCount()
  - getNotifications(), markNotificationRead(), markAllNotificationsRead(), getNotificationUnreadCount()
  - deleteNotification(), streamNotifications() → SseEmitter [SSE]

=== SkillController (base: /api/skills) ===
Injects: SkillRepository, SkillExecutionRepository, InventoryItemRepository, SkillExecutorService
  - listSkills(), getSkill(), toggleSkill() [OWNER/ADMIN]
  - executeSkill(), listExecutions()
  - listInventory(), createInventoryItem(), updateInventoryItem(), deleteInventoryItem()

=== SystemController (base: /api/system) ===
Injects: SystemConfigService, AuthService, NetworkTransitionService, FactoryResetService
  - getStatus() [PUBLIC], initialize() [PUBLIC], finalizeSetup() [PUBLIC]
  - getAiSettings() [OWNER/ADMIN/MEMBER], updateAiSettings() [OWNER/ADMIN/MEMBER]
  - getStorageSettings() [OWNER/ADMIN/MEMBER], updateStorageSettings() [OWNER/ADMIN/MEMBER]
  - factoryReset() [OWNER only]

=== CaptivePortalController (no base path) ===
Injects: SystemConfigService, ApModeService
  - setupWelcome(), setupWifi(), setupAccount(), setupConfirm() [HTML forwards]
  - scanWifi(), connectWifi(), wifiStatus() [PUBLIC JSON APIs under /api/setup/]
```

---

## 11. Security Configuration

```
Authentication: JWT (stateless, HMAC-SHA via jjwt 0.12.6)
Token issuer/validator: Internal (JwtService)
Password encoder: BCrypt (default rounds)

Public endpoints (no auth required):
  - /api/auth/login, /api/auth/register, /api/auth/refresh
  - /api/system/status, /api/system/initialize, /api/system/finalize-setup
  - /api/setup/**, /setup/**
  - /api/models, /api/models/health
  - /actuator/health
  - /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html

Protected endpoints:
  - /api/** → authenticated (all others)
  - Role-based via @PreAuthorize on individual methods

CORS: All origins (*), all methods (GET/POST/PUT/DELETE/OPTIONS), credentials allowed
CSRF: Disabled (stateless REST API)
Rate limiting: None configured
```

---

## 12. Custom Security Components

```
=== JwtAuthFilter ===
Extends: OncePerRequestFilter
Extracts token from: Authorization header (Bearer prefix)
Validates via: JwtService.isTokenValid() + AuthService.isTokenBlacklisted()
Sets SecurityContext: YES

=== CaptivePortalRedirectFilter ===
Extends: OncePerRequestFilter, @Order(1)
Purpose: Redirects non-API requests to setup page when AP mode is active
Excludes: /api/**, /setup/**, /actuator/**, /v3/api-docs/**, /swagger-ui/**
```

---

## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler (@RestControllerAdvice) ===
Exception → HTTP Status:
  - MethodArgumentNotValidException → 400 (concatenated field errors)
  - UsernameNotFoundException → 404
  - BadCredentialsException → 401 ("Invalid username or password")
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
  - AsyncRequestNotUsableException → (void, client disconnected)
  - Exception (catch-all) → 500

Standard response: ApiResponse<T> with success, message, data, timestamp, requestId, pagination fields
```

---

## 14. Mappers / DTOs

No MapStruct/ModelMapper. All mapping is manual (inline in controllers/services). DTO records are in each module's `dto/` package. API request/response schemas belong in the OpenAPI spec.

---

## 15. Utility Classes & Shared Components

```
=== TokenCounter (com.myoffgridai.common.util) ===
Methods:
  - static estimateTokens(String): int — ~4 chars/token approximation
  - static truncateToTokenLimit(List<OllamaMessage>, int maxTokens): List<OllamaMessage> — preserves system+latest user msg
Used by: ContextWindowService, SystemPromptBuilder

=== DeltaJsonUtils (com.myoffgridai.knowledge.util) ===
Methods:
  - static textToDeltaJson(String): String — converts plain text to Quill Delta JSON
  - static deltaJsonToText(String): String — extracts plain text from Quill Delta JSON
Used by: KnowledgeService (processDocumentAsync, createFromEditor, updateContent)

=== AppConstants (com.myoffgridai.config) ===
Central constants class — all API paths, pagination limits, timeouts, thresholds, MIME types, role strings, sensor defaults, etc.
Used by: Every module
```

---

## 16. Database Schema (Live)

Database not available during audit. Schema is managed by Hibernate DDL `update` (dev) / `validate` (prod). Tables derive directly from JPA entities documented in Section 6. pgvector extension required for `vector(768)` columns on `vector_document` table.

---

## 17. Message Broker Configuration

No message broker detected. No RabbitMQ, Kafka, or SQS dependencies.

---

## 18. Cache Layer

No Redis or caching layer detected. No `@Cacheable`, `@CacheEvict`, or `CacheManager` usage. Token blacklist is in-memory `Set<String>` (not distributed).

---

## 19. ENVIRONMENT VARIABLE INVENTORY

| Variable | Used In | Default | Required in Prod |
|---|---|---|---|
| `app.jwt.secret` | JwtService | dev-secret-key (hardcoded in dev) | YES |
| `app.jwt.expiration-ms` | JwtService | 86400000 | NO |
| `app.jwt.refresh-expiration-ms` | JwtService | 604800000 | NO |
| `app.ollama.model` | OllamaConfig | hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M | NO |
| `app.ollama.embed-model` | OllamaConfig | nomic-embed-text | NO |
| `app.ap.mock` | ApModeService, ApModeStartupService | true | YES (false for prod) |
| `app.fortress.mock` | FortressService | true | YES (false for prod) |
| `spring.profiles.active` | AuthService, application.yml | dev | YES |
| `app.health.check.interval-ms` | SystemHealthMonitor | 300000 | NO |

---

## 20. Service Dependency Map

Standalone service — no inter-service HTTP dependencies. Depends on:
- **PostgreSQL** (localhost:5432) — primary database with pgvector extension
- **Ollama** (localhost:11434) — LLM inference and embeddings

Downstream consumers: Flutter mobile app (MyOffGridAI-App)

---

## 21. Known Technical Debt & Issues

| Issue | Location | Severity | Notes |
|---|---|---|---|
| TODO: UpdateService.applyUpdate() deferred to MI-002 | UsbResetWatcherService.java:93 | CRITICAL | Update zip detection is stubbed — logs only |
| Stub: update zip processing | UsbResetWatcherService.java:83 | CRITICAL | Documented as deferred to MI-002 |
| In-memory token blacklist | AuthService.java | High | Not distributed; lost on restart; should use Redis or DB |
| No rate limiting | SecurityConfig.java | High | No protection against brute-force attacks |
| No API versioning | All controllers | Medium | Paths use /api/ without version prefix |
| No structured logging | Application-wide | Medium | No logback-spring.xml, no JSON log format |
| No Dockerfile | Project root | Medium | No containerization config |
| No CI/CD pipeline | Project root | Medium | No .github/workflows or similar |
| No HTTPS config | application-prod.yml | Medium | No SSL/TLS configuration for production |
| No @Version (optimistic locking) | All entities | Low | No concurrent modification protection |
| Duplicate AppConstants import | SystemController.java:8-9 | Low | Redundant import (compiles fine) |
| Documentation gaps | 17 undocumented classes, 175 undocumented public methods | BLOCKING | See Scorecard CQ-09/CQ-10 |

---

## 22. Security Vulnerability Scan (Snyk)

**Scan Date:** 2026-03-16
**Snyk CLI Version:** 1.1303.0

### Dependency Vulnerabilities (Open Source)

Critical: 3 | High: 17 | Medium: 11 | Low: 4 | **Total Unique: 35**

| Severity | Package | Version | Vulnerability | Fix Available |
|---|---|---|---|---|
| CRITICAL | spring-security-core | 6.4.3 | Missing Authentication for Critical Function | 6.4.6 |
| CRITICAL | spring-security-crypto | 6.4.3 | Authentication Bypass by Primary Weakness | 6.3.8 |
| CRITICAL | tomcat-embed-core | 10.1.36 | Improper Certificate Validation | 9.0.113 |
| HIGH | jackson-core | 2.18.2 | Allocation of Resources Without Limits | 2.18.6 |
| HIGH | netty-codec-http2 | 4.1.118 | Resource Allocation / Data Amplification (2 CVEs) | 4.1.125 |
| HIGH | netty-codec-http | 4.1.118 | HTTP Smuggling / Data Amplification (2 CVEs) | 4.1.125 |
| HIGH | commons-lang3 | 3.17.0 | Uncontrolled Recursion | 3.18.0 |
| HIGH | tomcat-embed-core | 10.1.36 | 8 vulnerabilities (auth, traversal, resource) | Various |
| HIGH | postgresql | 42.7.5 | Incorrect Auth Algorithm Implementation | 42.7.7 |
| HIGH | spring-beans | 6.2.3 | Relative Path Traversal | 6.2.10 |
| HIGH | spring-core | 6.2.3 | Incorrect Authorization | 6.2.11 |
| MEDIUM | Various | Various | 11 medium-severity issues | Various |
| LOW | Various | Various | 4 low-severity issues | Various |

**Primary Fix:** Upgrade Spring Boot parent from 3.4.3 to latest 3.4.x to pull in fixed transitive dependencies.

### Code Vulnerabilities (SAST)

Snyk Code scan unavailable (error code 2). Manual review recommended.

### IaC Findings

No Dockerfile or docker-compose.yml — IaC scan not applicable.
