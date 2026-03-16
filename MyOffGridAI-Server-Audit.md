# MyOffGridAI-Server — Codebase Audit

**Audit Date:** 2026-03-16T21:11:08Z
**Branch:** main
**Commit:** abec7407ead6fc0f8ed7e7d9574e44250a2971fb P7-Server: Add offline library system (Kiwix ZIM, eBooks, Project Gutenberg)
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
Repository URL: (local — ~/Documents/Github/MyOffGridAI-Server)
Primary Language / Framework: Java 21 / Spring Boot 3.4.6
Java Version: 21
Build Tool + Version: Maven (spring-boot-starter-parent 3.4.6)
Current Branch: main
Latest Commit Hash: abec7407ead6fc0f8ed7e7d9574e44250a2971fb
Latest Commit Message: P7-Server: Add offline library system (Kiwix ZIM, eBooks, Project Gutenberg)
Audit Timestamp: 2026-03-16T21:11:08Z
```

---

## 2. Directory Structure

Single-module Spring Boot application. Source root at `src/main/java/com/myoffgridai/`. Organized by functional domain packages: `ai`, `auth`, `common`, `config`, `enrichment`, `events`, `knowledge`, `library`, `mcp`, `memory`, `notification`, `privacy`, `proactive`, `sensors`, `settings`, `skills`, `system`. Each domain follows controller/dto/model/repository/service layering.

```
.
├── Dockerfile
├── pom.xml
├── src/main/java/com/myoffgridai/
│   ├── MyOffGridAiApplication.java
│   ├── ai/           (controller, dto, model, repository, service)
│   ├── auth/         (controller, dto, model, repository, service)
│   ├── common/       (exception, response, util)
│   ├── config/       (security, JWT, Ollama, vector, rate-limit, logging, JPA)
│   ├── enrichment/   (controller, dto, service — Claude API, Brave Search, web fetch)
│   ├── events/       (controller, dto, model, repository, service)
│   ├── knowledge/    (controller, dto, model, repository, service, util)
│   ├── library/      (config, controller, dto, model, repository, service)
│   ├── mcp/          (config, controller, dto, model, repository, service)
│   ├── memory/       (controller, dto, model, repository, service)
│   ├── notification/ (config, controller, dto, model, repository, service)
│   ├── privacy/      (aspect, controller, dto, model, repository, service)
│   ├── proactive/    (controller, dto, model, repository, service)
│   ├── sensors/      (controller, dto, model, repository, service)
│   ├── settings/     (controller, dto, model, repository, service)
│   ├── skills/       (builtin, controller, dto, model, repository, service)
│   └── system/       (controller, dto, model, repository, service)
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   ├── logback-spring.xml
│   └── META-INF/native-image/ (proxy-config, reflect-config, resource-config)
└── src/test/
    ├── java/com/myoffgridai/ (94 unit tests + 23 integration tests)
    └── resources/ (application.yml, application-test.yml)
```

---

## 3. Build & Dependency Manifest

**Build file:** `pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.4.6 | REST API framework |
| spring-boot-starter-data-jpa | 3.4.6 | JPA/Hibernate ORM |
| spring-boot-starter-security | 3.4.6 | Authentication/authorization |
| spring-boot-starter-validation | 3.4.6 | Bean validation |
| spring-boot-starter-actuator | 3.4.6 | Health checks, metrics |
| spring-boot-starter-webflux | 3.4.6 | Reactive WebClient for SSE streaming |
| spring-boot-starter-aop | 3.4.6 | AOP for audit aspect |
| spring-ai-starter-mcp-server-webmvc | 1.1.2 | MCP Server (SSE transport) |
| jjwt-api/impl/jackson | 0.12.6 | JWT token generation/validation |
| postgresql | 42.7.7 | PostgreSQL JDBC driver |
| pgvector | 0.1.6 | pgvector Java support |
| pdfbox | 3.0.4 | PDF text extraction |
| poi/poi-ooxml/poi-scratchpad | 5.3.0 | Office document processing |
| tess4j | 5.13.0 | OCR via Tesseract |
| jSerialComm | 2.11.0 | Serial port communication (sensors) |
| bucket4j-core | 8.10.1 | Token-bucket rate limiting |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | MQTT client for push notifications |
| commons-io | 2.17.0 | File utilities |
| logstash-logback-encoder | 8.0 | Structured JSON logging (prod) |
| springdoc-openapi-starter-webmvc-ui | 2.8.4 | Swagger UI / API docs |
| jsoup | 1.18.3 | HTML parsing for web content |
| lombok | 1.18.42 | Boilerplate reduction (provided scope) |
| testcontainers (postgresql, junit-jupiter) | 1.20.6 | Integration test database |
| jacoco-maven-plugin | 0.8.12 | Code coverage reporting |

**Build plugins:** spring-boot-maven-plugin, maven-compiler-plugin (Java 21, Lombok annotation processor), jacoco-maven-plugin (80% line / 60% branch minimums), maven-surefire-plugin, GraalVM native-maven-plugin (native profile).

```
Build: mvn clean compile -DskipTests
Test:  mvn test
Run:   mvn spring-boot:run
Package: mvn clean package -DskipTests
Native: mvn -Pnative package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — Path: `src/main/resources/application.yml`. Default profile: `dev`. Server port: 8080. Flyway disabled. Multipart max: 2048MB. MCP server configured at `/mcp/sse`.
- **`application.yml` (dev profile)** — PostgreSQL at `localhost:5432/myoffgridai` user `myoffgridai`. Hibernate ddl-auto: `update`. Dev encryption key hardcoded. JWT secret: dev-only value. Ollama: `localhost:11434`, model `Qwen3-32B-GGUF:Q4_K_M`, embed model `nomic-embed-text`. Fortress mock: true. AP mock: true. MQTT disabled. Library paths: `./library/zim`, `./library/ebooks`.
- **`application-prod.yml`** — Path: `src/main/resources/application-prod.yml`. Hibernate ddl-auto: `validate`. Flyway enabled. Fortress/AP mock: false.
- **`logback-spring.xml`** — Path: `src/main/resources/logback-spring.xml`. Dev: human-readable console with requestId MDC. Prod: JSON via LogstashEncoder with requestId/username/userId MDC. Test: JSON at WARN level.
- **`Dockerfile`** — Multi-stage build. Stage 1: eclipse-temurin:21-jdk-alpine. Stage 2: eclipse-temurin:21-jre-alpine. Non-root user `myoffgridai`. Exposes 8080. HEALTHCHECK against `/api/system/status`.
- **No docker-compose.yml detected.**
- **No `.env` or `.env.example` detected.**

**Connection map:**
```
Database: PostgreSQL, localhost:5432, myoffgridai
Cache: None
Message Broker: MQTT (Eclipse Paho), localhost:1883 (conditional on app.mqtt.enabled)
External APIs: Ollama (localhost:11434), Anthropic Claude API, Brave Search API, Gutendex API, Kiwix (localhost:8888), Calibre (localhost:8081)
Cloud Services: None (fully offline-capable)
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `com.myoffgridai.MyOffGridAiApplication` — `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`
- **Startup initialization:**
  - `VectorStoreConfig.checkPgvectorExtension()` — `@EventListener(ApplicationReadyEvent)` verifies pgvector extension
  - `SkillSeederService` — Seeds built-in skills (weather-query, inventory-tracker, recipe-generator, task-planner, document-summarizer, resource-calculator) on startup
  - `SensorStartupService` — Starts polling for all active sensors on startup
  - `ApModeStartupService` — Checks if device should start in AP mode
  - `UsbResetWatcherService` — Watches USB mount point for update/factory-reset triggers
- **Scheduled tasks:**
  - `NightlyInsightJob` — Scheduled nightly insight generation
  - `SystemHealthMonitor` — Health checks at 5-minute intervals
  - `SensorPollingService` — Polls sensors at configured intervals
- **Health check:** `/api/system/status` (public), `/actuator/health` (public)

---

## 6. Entity / Data Model Layer

### User (auth)
```
=== User.java ===
Table: users
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - username: String [@Column(nullable=false, unique=true)]
  - email: String [@Column(unique=true)]
  - displayName: String [@Column(nullable=false)]
  - passwordHash: String [@Column(nullable=false)]
  - role: Role [@Enumerated(STRING), @Column(nullable=false)]
  - isActive: boolean [@Column(nullable=false)] default true
  - lastLoginAt: Instant
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
Validation: None (handled at DTO level)
Implements: UserDetails (Spring Security)
```

### Conversation (ai)
```
=== Conversation.java ===
Table: conversations
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - title: String
  - isArchived: boolean default false
  - messageCount: int default 0
Relationships:
  - @ManyToOne → User (user_id, LAZY)
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### Message (ai)
```
=== Message.java ===
Table: messages
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - role: MessageRole [@Enumerated(STRING), nullable=false]
  - content: String [TEXT, nullable=false]
  - tokenCount: Integer
  - hasRagContext: boolean default false
Relationships:
  - @ManyToOne → Conversation (conversation_id, LAZY)
Audit Fields: createdAt (@CreatedDate)
```

### Memory (memory)
```
=== Memory.java ===
Table: memories
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - importance: MemoryImportance [@Enumerated(STRING), nullable=false]
  - tags: String
  - sourceConversationId: UUID
  - lastAccessedAt: Instant
  - accessCount: int default 0
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### VectorDocument (memory)
```
=== VectorDocument.java ===
Table: vector_document
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_vector_doc_user_source_type (user_id, source_type)
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - embedding: float[] [@Type(VectorType.class), vector(768)]
  - sourceType: VectorSourceType [@Enumerated(STRING), nullable=false]
  - sourceId: UUID
  - metadata: String [TEXT]
Audit Fields: createdAt (@CreatedDate)
```

### KnowledgeDocument (knowledge)
```
=== KnowledgeDocument.java ===
Table: knowledge_documents
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_knowledge_doc_user_id (user_id)
Fields:
  - userId: UUID [nullable=false]
  - filename: String [nullable=false]
  - displayName: String
  - mimeType: String [nullable=false]
  - storagePath: String [nullable=false]
  - fileSizeBytes: long
  - status: DocumentStatus [@Enumerated(STRING), nullable=false] default PENDING
  - errorMessage: String [TEXT]
  - chunkCount: int
  - processedAt: Instant
  - content: String [TEXT]
Audit Fields: uploadedAt (@CreatedDate)
```

### KnowledgeChunk (knowledge)
```
=== KnowledgeChunk.java ===
Table: knowledge_chunks
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_knowledge_chunk_doc_id, idx_knowledge_chunk_user_id
Fields:
  - userId: UUID [nullable=false]
  - chunkIndex: int [nullable=false]
  - content: String [TEXT, nullable=false]
  - pageNumber: Integer
Relationships:
  - @ManyToOne → KnowledgeDocument (document_id, LAZY)
Audit Fields: createdAt (@CreatedDate)
```

### Sensor (sensors)
```
=== Sensor.java ===
Table: sensors
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_sensor_user_id, idx_sensor_port_path (unique)
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - type: SensorType [@Enumerated(STRING), nullable=false]
  - portPath: String [nullable=false]
  - baudRate: int [nullable=false] default 9600
  - dataFormat: DataFormat [@Enumerated(STRING), nullable=false] default CSV_LINE
  - valueField: String
  - unit: String
  - isActive: boolean default false
  - pollIntervalSeconds: int default 30
  - lowThreshold: Double
  - highThreshold: Double
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### SensorReading (sensors)
```
=== SensorReading.java ===
Table: sensor_readings
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_sensor_reading_sensor_recorded (sensor_id, recorded_at DESC)
Fields:
  - value: double [nullable=false]
  - rawData: String
  - recordedAt: Instant [nullable=false]
Relationships:
  - @ManyToOne → Sensor (sensor_id, LAZY)
Audit Fields: None
```

### ScheduledEvent (events)
```
=== ScheduledEvent.java ===
Table: scheduled_events
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_event_user_id, idx_event_enabled_type
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - description: String [TEXT]
  - eventType: EventType [@Enumerated(STRING), nullable=false]
  - isEnabled: boolean default true
  - cronExpression: String
  - recurringIntervalMinutes: Integer
  - sensorId: UUID
  - thresholdOperator: ThresholdOperator [@Enumerated(STRING)]
  - thresholdValue: Double
  - actionType: ActionType [@Enumerated(STRING), nullable=false]
  - actionPayload: String [TEXT, nullable=false]
  - lastTriggeredAt: Instant
  - nextFireAt: Instant
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### Skill (skills)
```
=== Skill.java ===
Table: skills
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_skill_name (unique), idx_skill_category
Fields:
  - name: String [nullable=false, unique]
  - displayName: String [nullable=false]
  - description: String [TEXT, nullable=false]
  - version: String [nullable=false]
  - author: String [nullable=false]
  - category: SkillCategory [@Enumerated(STRING), nullable=false]
  - isEnabled: boolean default true
  - isBuiltIn: boolean default false
  - parametersSchema: String [TEXT]
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### SkillExecution (skills)
```
=== SkillExecution.java ===
Table: skill_executions
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_skill_exec_user_id, idx_skill_exec_skill_id
Fields:
  - userId: UUID [nullable=false]
  - status: ExecutionStatus [@Enumerated(STRING), nullable=false]
  - inputParams: String [TEXT]
  - outputResult: String [TEXT]
  - errorMessage: String [TEXT]
  - completedAt: Instant
  - durationMs: Long
Relationships:
  - @ManyToOne → Skill (skill_id, LAZY)
Audit Fields: startedAt (@CreatedDate)
```

### InventoryItem (skills)
```
=== InventoryItem.java ===
Table: inventory_items
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_inventory_user_id, idx_inventory_category
Fields:
  - userId: UUID [nullable=false]
  - name: String [nullable=false]
  - category: InventoryCategory [@Enumerated(STRING), nullable=false]
  - quantity: double [nullable=false]
  - unit: String
  - notes: String [TEXT]
  - lowStockThreshold: Double
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### PlannedTask (skills)
```
=== PlannedTask.java ===
Table: planned_tasks
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_planned_task_user_id
Fields:
  - userId: UUID [nullable=false]
  - goalDescription: String [TEXT, nullable=false]
  - title: String [nullable=false]
  - steps: String [TEXT, nullable=false]
  - estimatedResources: String [TEXT]
  - status: TaskStatus [@Enumerated(STRING), nullable=false] default ACTIVE
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### Insight (proactive)
```
=== Insight.java ===
Table: insights
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_insight_user_id
Fields:
  - userId: UUID [nullable=false]
  - content: String [TEXT, nullable=false]
  - category: InsightCategory [@Enumerated(STRING), nullable=false]
  - isRead: boolean default false
  - isDismissed: boolean default false
  - readAt: Instant
Audit Fields: generatedAt (set in @PrePersist)
```

### Notification (proactive)
```
=== Notification.java ===
Table: notifications
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_notification_user_id
Fields:
  - userId: UUID [nullable=false]
  - title: String [nullable=false]
  - body: String [TEXT, nullable=false]
  - type: NotificationType [@Enumerated(STRING), nullable=false]
  - isRead: boolean default false
  - readAt: Instant
  - severity: NotificationSeverity [@Enumerated(STRING)]
  - mqttDelivered: boolean default false
  - metadata: String [TEXT]
Audit Fields: createdAt (set in @PrePersist)
```

### AuditLog (privacy)
```
=== AuditLog.java ===
Table: audit_logs
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_audit_user_timestamp, idx_audit_timestamp
Fields:
  - userId: UUID
  - username: String
  - action: String [nullable=false]
  - resourceType: String
  - resourceId: String
  - httpMethod: String [nullable=false]
  - requestPath: String [nullable=false]
  - ipAddress: String
  - userAgent: String
  - responseStatus: int
  - outcome: AuditOutcome [@Enumerated(STRING), nullable=false]
  - durationMs: long
  - timestamp: Instant [nullable=false]
Audit Fields: None (uses explicit timestamp field)
```

### DeviceRegistration (notification)
```
=== DeviceRegistration.java ===
Table: device_registrations
Primary Key: id (UUID, GenerationType.UUID)
Unique: (user_id, device_id)
Indexes: idx_device_registration_user_id
Fields:
  - userId: UUID [nullable=false]
  - deviceId: String [nullable=false]
  - deviceName: String
  - platform: String [nullable=false]
  - mqttClientId: String
  - lastSeenAt: Instant
Audit Fields: createdAt, updatedAt (via @PrePersist/@PreUpdate)
```

### SystemConfig (system)
```
=== SystemConfig.java ===
Table: system_config
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - initialized: boolean default false
  - instanceName: String
  - fortressEnabled: boolean default false
  - fortressEnabledAt: Instant
  - fortressEnabledByUserId: UUID
  - apModeEnabled: boolean default false
  - wifiConfigured: boolean default false
  - aiModel: String default "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M"
  - aiTemperature: Double default 0.7
  - aiSimilarityThreshold: Double default 0.45
  - aiMemoryTopK: Integer default 5
  - aiRagMaxContextTokens: Integer default 2048
  - aiContextSize: Integer default 4096
  - aiContextMessageLimit: Integer default 20
  - knowledgeStoragePath: String default "/var/myoffgridai/knowledge"
  - maxUploadSizeMb: Integer default 25
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### ExternalApiSettings (settings)
```
=== ExternalApiSettings.java ===
Table: external_api_settings
Primary Key: id (UUID, GenerationType.UUID)
Singleton: singletonGuard = "SINGLETON" (unique)
Fields:
  - anthropicApiKey: String [@Convert(AesAttributeConverter) — AES-256-GCM encrypted]
  - anthropicModel: String default "claude-sonnet-4-20250514"
  - anthropicEnabled: boolean default false
  - braveApiKey: String [@Convert(AesAttributeConverter) — AES-256-GCM encrypted]
  - braveEnabled: boolean default false
  - maxWebFetchSizeKb: int default 512
  - searchResultLimit: int default 5
Audit Fields: createdAt (@CreatedDate), updatedAt (@LastModifiedDate)
```

### McpApiToken (mcp)
```
=== McpApiToken.java ===
Table: mcp_api_tokens
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_mcp_token_created_by
Fields:
  - tokenHash: String [nullable=false, length=500] — BCrypt hashed
  - name: String [nullable=false]
  - createdBy: UUID [nullable=false]
  - lastUsedAt: Instant
  - isActive: boolean default true
Audit Fields: createdAt (@CreatedDate)
```

### Ebook (library)
```
=== Ebook.java ===
Table: ebooks
Primary Key: id (UUID, GenerationType.UUID)
Indexes: idx_ebooks_gutenberg_id
Fields:
  - title: String [nullable=false]
  - author: String
  - description: String [TEXT]
  - isbn: String
  - publisher: String
  - publishedYear: Integer
  - language: String
  - format: EbookFormat [@Enumerated(STRING), nullable=false]
  - fileSizeBytes: long
  - filePath: String [nullable=false]
  - coverImagePath: String
  - gutenbergId: String
  - downloadCount: int
  - uploadedBy: UUID
Audit Fields: uploadedAt (@CreatedDate)
```

### ZimFile (library)
```
=== ZimFile.java ===
Table: zim_files
Primary Key: id (UUID, GenerationType.UUID)
Fields:
  - filename: String [nullable=false, unique]
  - displayName: String [nullable=false]
  - description: String [length=1000]
  - language: String
  - category: String
  - fileSizeBytes: long
  - articleCount: int
  - mediaCount: int
  - createdDate: String
  - filePath: String [nullable=false]
  - kiwixBookId: String
  - uploadedBy: UUID
Audit Fields: uploadedAt (@CreatedDate)
```

---

## 7. Enum Inventory

| Enum | Values | Used In |
|------|--------|---------|
| Role | ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER, ROLE_VIEWER, ROLE_CHILD | User |
| MessageRole | USER, ASSISTANT, SYSTEM | Message |
| MemoryImportance | LOW, MEDIUM, HIGH, CRITICAL | Memory |
| VectorSourceType | MEMORY, CONVERSATION, KNOWLEDGE_CHUNK | VectorDocument |
| DocumentStatus | PENDING, PROCESSING, READY, FAILED | KnowledgeDocument |
| SensorType | TEMPERATURE, HUMIDITY, SOIL_MOISTURE, POWER, VOLTAGE, CUSTOM | Sensor |
| DataFormat | CSV_LINE, JSON_LINE | Sensor |
| EventType | SCHEDULED, SENSOR_THRESHOLD, RECURRING | ScheduledEvent |
| ActionType | PUSH_NOTIFICATION, AI_PROMPT, AI_SUMMARY | ScheduledEvent |
| ThresholdOperator | ABOVE, BELOW, EQUALS | ScheduledEvent |
| SkillCategory | HOMESTEAD, RESOURCE, PLANNING, KNOWLEDGE, WEATHER, CUSTOM | Skill |
| ExecutionStatus | RUNNING, COMPLETED, FAILED | SkillExecution |
| InventoryCategory | FOOD, TOOLS, MEDICAL, SUPPLIES, SEEDS, EQUIPMENT, OTHER | InventoryItem |
| TaskStatus | ACTIVE, COMPLETED, CANCELLED | PlannedTask |
| InsightCategory | HOMESTEAD, HEALTH, RESOURCE, GENERAL | Insight |
| NotificationType | SENSOR_ALERT, SYSTEM_HEALTH, INSIGHT_READY, MODEL_UPDATE, GENERAL | Notification |
| NotificationSeverity | INFO, WARNING, CRITICAL | Notification |
| AuditOutcome | SUCCESS, FAILURE, DENIED | AuditLog |
| EbookFormat | EPUB, PDF, MOBI, AZW, TXT, HTML | Ebook |

---

## 8. Repository Layer

### UserRepository
```
Entity: User | Extends: JpaRepository<User, UUID>
Custom Methods:
  - findByUsername(String): Optional<User>
  - findByEmail(String): Optional<User>
  - existsByUsername(String): boolean
  - existsByEmail(String): boolean
  - findAllByRole(Role): List<User>
  - countByIsActiveTrue(): long
  - findByIsActiveTrue(): List<User>
```

### ConversationRepository
```
Entity: Conversation | Extends: JpaRepository<Conversation, UUID>
Custom Methods:
  - findByUserIdOrderByUpdatedAtDesc(UUID, Pageable): Page<Conversation>
  - findByUserIdAndIsArchivedOrderByUpdatedAtDesc(UUID, boolean, Pageable): Page<Conversation>
  - findByIdAndUserId(UUID, UUID): Optional<Conversation>
  - countByUserId(UUID): long
  - findByUserId(UUID): List<Conversation>
  - findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID, String, Pageable): Page<Conversation>
  - @Modifying @Query deleteByUserId(UUID): void
```

### MessageRepository
```
Entity: Message | Extends: JpaRepository<Message, UUID>
Custom Methods:
  - findByConversationIdOrderByCreatedAtAsc(UUID): List<Message>
  - findByConversationIdOrderByCreatedAtAsc(UUID, Pageable): Page<Message>
  - findTopNByConversationIdOrderByCreatedAtDesc(UUID, Pageable): List<Message>
  - countByConversationId(UUID): long
  - deleteByConversationId(UUID): void
  - @Query countByUserId(UUID): long
  - @Modifying @Query deleteByUserId(UUID): void
```

### MemoryRepository
```
Entity: Memory | Extends: JpaRepository<Memory, UUID>
Custom Methods:
  - findByUserIdOrderByCreatedAtDesc(UUID, Pageable): Page<Memory>
  - findByUserIdAndImportance(UUID, MemoryImportance, Pageable): Page<Memory>
  - findByUserIdAndTagsContaining(UUID, String, Pageable): Page<Memory>
  - findByUserId(UUID): List<Memory>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long
```

### VectorDocumentRepository
```
Entity: VectorDocument | Extends: JpaRepository<VectorDocument, UUID>
Custom Methods:
  - findByUserIdAndSourceType(UUID, VectorSourceType): List<VectorDocument>
  - @Modifying deleteBySourceIdAndSourceType(UUID, VectorSourceType): void
  - @Modifying deleteByUserId(UUID): void
  - @Query(nativeQuery) findMostSimilar(UUID, String, String, int): List<VectorDocument> — pgvector cosine distance
  - @Query(nativeQuery) findMostSimilarAcrossTypes(UUID, String, int): List<VectorDocument>
```

### KnowledgeDocumentRepository
```
Entity: KnowledgeDocument | Extends: JpaRepository<KnowledgeDocument, UUID>
Custom Methods:
  - findByUserIdOrderByUploadedAtDesc(UUID, Pageable): Page<KnowledgeDocument>
  - findByIdAndUserId(UUID, UUID): Optional<KnowledgeDocument>
  - findByUserIdAndStatus(UUID, DocumentStatus): List<KnowledgeDocument>
  - @Modifying deleteByUserId(UUID): void
  - countByUserId(UUID): long
```

### KnowledgeChunkRepository
```
Entity: KnowledgeChunk | Extends: JpaRepository<KnowledgeChunk, UUID>
Custom Methods:
  - findByDocumentIdOrderByChunkIndexAsc(UUID): List<KnowledgeChunk>
  - @Modifying deleteByDocumentId(UUID): void
  - @Modifying deleteByUserId(UUID): void
  - countByDocumentId(UUID): long
```

### SensorRepository
```
Entity: Sensor | Extends: JpaRepository<Sensor, UUID>
Custom Methods:
  - findByUserIdOrderByNameAsc(UUID): List<Sensor>
  - findByIdAndUserId(UUID, UUID): Optional<Sensor>
  - findByUserIdAndIsActiveTrue(UUID): List<Sensor>
  - findByPortPath(String): Optional<Sensor>
  - findByIsActiveTrue(): List<Sensor>
  - countByUserId(UUID): long
  - deleteByUserId(UUID): void
```

### SensorReadingRepository
```
Entity: SensorReading | Extends: JpaRepository<SensorReading, UUID>
Custom Methods:
  - findBySensorIdOrderByRecordedAtDesc(UUID, Pageable): Page<SensorReading>
  - findBySensorIdAndRecordedAtAfterOrderByRecordedAtAsc(UUID, Instant): List<SensorReading>
  - findTopBySensorIdOrderByRecordedAtDesc(UUID): Optional<SensorReading>
  - @Modifying deleteBySensorId(UUID): void
  - @Modifying @Query deleteByUserId(UUID): void
  - @Query(nativeQuery) findAverageValueSince(UUID, Instant): Double
```

### ScheduledEventRepository
```
Entity: ScheduledEvent | Extends: JpaRepository<ScheduledEvent, UUID>
Custom Methods:
  - findAllByUserId(UUID, Pageable): Page<ScheduledEvent>
  - findByIdAndUserId(UUID, UUID): Optional<ScheduledEvent>
  - findByIsEnabledTrueAndEventType(EventType): List<ScheduledEvent>
  - findAllByUserIdOrderByCreatedAtDesc(UUID): List<ScheduledEvent>
  - deleteByUserId(UUID): void
  - countByUserId(UUID): long
```

### SkillRepository
```
Entity: Skill | Extends: JpaRepository<Skill, UUID>
Custom Methods:
  - findByIsEnabledTrue(): List<Skill>
  - findByIsBuiltInTrue(): List<Skill>
  - findByCategory(SkillCategory): List<Skill>
  - findByName(String): Optional<Skill>
  - findByIsEnabledTrueOrderByDisplayNameAsc(): List<Skill>
```

### SkillExecutionRepository, InventoryItemRepository, PlannedTaskRepository
```
Standard CRUD with user-scoped queries, pagination, and status filtering.
```

### InsightRepository, NotificationRepository
```
Standard CRUD with user-scoped queries, read/dismissed status filtering, and bulk operations.
NotificationRepository has @Modifying @Query markAllReadForUser(UUID, Instant).
```

### AuditLogRepository
```
Entity: AuditLog | Extends: JpaRepository<AuditLog, UUID>
Custom Methods: Paginated queries by user, outcome, time range. deleteByTimestampBefore(Instant).
```

### DeviceRegistrationRepository
```
Entity: DeviceRegistration | Extends: JpaRepository<DeviceRegistration, UUID>
Custom Methods: findByUserIdAndDeviceId, findByUserId, deleteByUserIdAndDeviceId.
```

### SystemConfigRepository
```
Entity: SystemConfig | Extends: JpaRepository<SystemConfig, UUID>
Custom Methods: @Query findFirst(): Optional<SystemConfig> — singleton row.
```

### ExternalApiSettingsRepository
```
Entity: ExternalApiSettings | Extends: JpaRepository<ExternalApiSettings, UUID>
Custom Methods: findBySingletonGuard(String): Optional<ExternalApiSettings>
```

### McpApiTokenRepository, EbookRepository, ZimFileRepository
```
McpApiTokenRepository: findByIsActiveTrue(), findByCreatedByOrderByCreatedAtDesc(UUID)
EbookRepository: @Query searchByTitleOrAuthor(String, EbookFormat, Pageable), findByGutenbergId(String), existsByGutenbergId(String)
ZimFileRepository: findByFilename(String), findAllByOrderByDisplayNameAsc(), existsByFilename(String)
```

---

## 9. Service Layer — Full Method Signatures

*Services are documented with injected dependencies and public method signatures. See source files for full implementations.*

### AuthService
```
Injects: UserRepository, PasswordEncoder, JwtService, SystemConfigRepository
Public Methods:
  - register(RegisterRequest): AuthResponse — Creates user, enforces first-user-is-OWNER
  - login(LoginRequest): AuthResponse — Authenticates, updates lastLoginAt
  - refreshToken(String refreshToken): AuthResponse
  - logout(String token): void — Blacklists token
  - isTokenBlacklisted(String token): boolean
```

### JwtService
```
Injects: @Value(app.jwt.secret, expiration-ms, refresh-expiration-ms)
Public Methods:
  - generateAccessToken(UserDetails): String
  - generateRefreshToken(UserDetails): String
  - extractUsername(String token): String
  - isTokenValid(String token, UserDetails): boolean
  - isRefreshToken(String token): boolean
```

### UserService
```
Injects: UserRepository, PasswordEncoder, @Value(spring.profiles.active)
Public Methods:
  - getAllUsers(): List<UserSummaryDto>
  - getUserById(UUID): UserDetailDto
  - updateUser(UUID, UpdateUserRequest): UserDetailDto
  - changePassword(UUID, ChangePasswordRequest): void
  - toggleUserActive(UUID): UserDetailDto
```

### ChatService
```
Injects: ConversationRepository, MessageRepository, OllamaService, ContextWindowService, SystemPromptBuilder, RagService, MemoryExtractionService, SummarizationService
Public Methods:
  - createConversation(UUID userId, CreateConversationRequest): ConversationDto
  - getConversations(UUID, Pageable, Boolean archived, String search): Page<ConversationSummaryDto>
  - getConversation(UUID userId, UUID id): ConversationDto
  - renameConversation(UUID userId, UUID id, RenameConversationRequest): ConversationDto
  - archiveConversation(UUID userId, UUID id): ConversationDto
  - deleteConversation(UUID userId, UUID id): void
  - sendMessage(UUID userId, UUID conversationId, SendMessageRequest): MessageDto
  - streamMessage(UUID userId, UUID conversationId, SendMessageRequest): Flux<String>
  - getMessages(UUID userId, UUID conversationId, Pageable): Page<MessageDto>
```

### OllamaService
```
Injects: RestClient(ollamaRestClient), WebClient(ollamaWebClient), ObjectMapper, @Qualifier(ollamaModelName), @Qualifier(ollamaEmbedModelName), SystemConfigService
Public Methods:
  - chat(List<OllamaMessage>, UUID userId): String
  - streamChat(List<OllamaMessage>, UUID userId): Flux<String>
  - embed(String text): float[]
  - isAvailable(): boolean
  - listModels(): List<OllamaModelInfo>
  - getActiveModel(): ActiveModelDto
```

### AgentService
```
Injects: OllamaService, SkillExecutorService, ObjectMapper
Public Methods:
  - executeTask(String userQuery, UUID userId, UUID conversationId): AgentTaskResult
```

### ContextWindowService
```
Injects: MessageRepository, TokenCounter
Public Methods:
  - getRecentMessagesForContext(UUID conversationId, SystemConfigService): List<OllamaMessage>
```

### SystemPromptBuilder
```
Injects: (none — utility class)
Public Methods:
  - buildSystemPrompt(String ragContext, User user): String
```

### MemoryService, EmbeddingService, MemoryExtractionService, RagService, SummarizationService
```
Memory pipeline: extract facts from conversations → store as Memory + VectorDocument → retrieve via semantic search for RAG context.
RagService orchestrates retrieval across both memory and knowledge vector stores.
```

### KnowledgeService, ChunkingService, IngestionService, SemanticSearchService, FileStorageService, OcrService, StorageHealthService
```
Knowledge pipeline: upload → extract text (PDF/Office/OCR) → chunk → embed → store in VectorDocument.
SemanticSearchService: searches knowledge chunks via pgvector cosine similarity.
```

### SensorService, SensorPollingService, SensorStartupService, SerialPortService, SseEmitterRegistry
```
Sensor pipeline: register sensors → poll serial ports → parse readings → store → emit via SSE → check thresholds.
```

### ScheduledEventService
```
Injects: ScheduledEventRepository
Public Methods: CRUD for user-scoped scheduled events with pagination.
```

### SkillExecutorService, SkillSeederService, BuiltInSkill (interface)
```
Skill system: seeder creates built-in skills on startup. Executor dispatches to BuiltInSkill implementations.
Built-in skills: WeatherQuerySkill, InventoryTrackerSkill, RecipeGeneratorSkill, TaskPlannerSkill, DocumentSummarizerSkill, ResourceCalculatorSkill.
```

### InsightService, InsightGeneratorService, NightlyInsightJob, NotificationService, NotificationSseRegistry, PatternAnalysisService, SystemHealthMonitor
```
Proactive engine: nightly job generates insights from activity patterns. SystemHealthMonitor checks disk/Ollama/DB at intervals. Notifications delivered via SSE + MQTT.
```

### AuditService, DataExportService, DataWipeService, FortressService, SovereigntyReportService, AuditAspect
```
Privacy layer: AuditAspect intercepts controller methods via AOP. Fortress mode blocks external connections. DataWipe performs cascading user data deletion. DataExport creates encrypted JSON exports.
```

### DeviceRegistrationService, MqttPublisherService, MqttConfig
```
MQTT push notifications: devices register → notifications published to user-specific MQTT topics. MQTT is conditional (app.mqtt.enabled). MqttPublisherService handles null client gracefully.
```

### SystemConfigService, ApModeService, ApModeStartupService, FactoryResetService, NetworkTransitionService, UsbResetWatcherService
```
System management: singleton SystemConfig row. AP mode for initial setup. Factory reset with confirmation phrase. USB watcher for update/reset triggers. Network transition from AP to station mode.
```

### ExternalApiSettingsService
```
Singleton settings for Anthropic/Brave API keys (AES-256-GCM encrypted at rest).
```

### McpTokenService, McpToolsService, McpServerConfig, McpAuthFilter, McpAuthentication
```
MCP integration: token-based auth for external AI clients. McpToolsService exposes server capabilities as MCP tools. SSE transport at /mcp/sse.
```

### ClaudeApiService, WebFetchService, WebSearchService
```
Enrichment services: Claude API for summarization/analysis. Brave Search for web queries. WebFetch for URL content extraction via Jsoup.
```

### EbookService, ZimFileService, GutenbergService, CalibreConversionService, LibraryProperties
```
Offline library: manage eBooks (upload, search, Gutenberg import). ZIM files for offline wiki content via Kiwix. Calibre for format conversion.
```

---

## 10. Controller / API Layer — Method Signatures Only

### AuthController
```
Base Path: /api/auth
Injects: AuthService
Endpoints:
  - register() → authService.register()
  - login() → authService.login()
  - refresh() → authService.refreshToken()
  - logout() → authService.logout()
```

### UserController
```
Base Path: /api/users
Injects: UserService
Endpoints: getAllUsers, getUserById, updateUser, changePassword, toggleActive
```

### ChatController
```
Base Path: /api/chat
Injects: ChatService
Endpoints: createConversation, getConversations, getConversation, renameConversation, archiveConversation, deleteConversation, sendMessage, streamMessage, getMessages
```

### ModelController
```
Base Path: /api/models
Injects: OllamaService, ModelHealthCheckService
Endpoints: listModels, getActiveModel, healthCheck
```

### MemoryController
```
Base Path: /api/memory
Injects: MemoryService
Endpoints: listMemories, getMemory, deleteMemory, updateImportance, updateTags, searchMemories
```

### KnowledgeController
```
Base Path: /api/knowledge
Injects: KnowledgeService
Endpoints: uploadDocument, listDocuments, getDocument, getDocumentContent, updateContent, updateDisplayName, deleteDocument, searchKnowledge
```

### SensorController
```
Base Path: /api/sensors
Injects: SensorService
Endpoints: createSensor, listSensors, getSensor, deleteSensor, activateSensor, deactivateSensor, testSensor, getReadings, updateThresholds, streamReadings (SSE)
```

### ScheduledEventController
```
Base Path: /api/events
Injects: ScheduledEventService
Endpoints: createEvent, listEvents, getEvent, updateEvent, deleteEvent, toggleEvent
```

### SkillController
```
Base Path: /api/skills
Injects: SkillExecutorService + InventoryItem CRUD
Endpoints: listSkills, executeSkill, getExecutionHistory, listInventory, createInventoryItem, updateInventoryItem, deleteInventoryItem
```

### ProactiveController
```
Base Path: /api/insights, /api/notifications
Injects: InsightService, NotificationService, PatternAnalysisService
Endpoints: getInsights, readInsight, dismissInsight, getNotifications, readNotification, markAllRead, streamNotifications (SSE), getPatternSummary
```

### PrivacyController
```
Base Path: /api/privacy
Injects: AuditService, DataExportService, DataWipeService, FortressService, SovereigntyReportService
Endpoints: getAuditLogs, exportData, wipeData, getFortressStatus, enableFortress, disableFortress, getSovereigntyReport
```

### DeviceRegistrationController
```
Base Path: /api/devices
Injects: DeviceRegistrationService
Endpoints: registerDevice, listDevices, removeDevice
```

### SystemController
```
Base Path: /api/system
Injects: SystemConfigService, ApModeService, FactoryResetService
Endpoints: getStatus, initialize, finalizeSetup, getAiSettings, updateAiSettings, getStorageSettings, updateStorageSettings, factoryReset
```

### CaptivePortalController
```
Base Path: /api/setup
Injects: ApModeService, SystemConfigService, NetworkTransitionService
Endpoints: getSetupStatus, scanWifiNetworks, connectWifi, getConnectionStatus
```

### ExternalApiSettingsController
```
Base Path: /api/settings/external-apis
Injects: ExternalApiSettingsService
Endpoints: getSettings, updateSettings
```

### McpDiscoveryController
```
Base Path: /api/mcp
Injects: (none)
Endpoints: getDiscovery — returns MCP server metadata
```

### McpTokenController
```
Base Path: /api/mcp/tokens
Injects: McpTokenService
Endpoints: createToken, listTokens, revokeToken
```

### EnrichmentController
```
Base Path: /api/enrichment
Injects: WebSearchService, WebFetchService, ClaudeApiService, ExternalApiSettingsService
Endpoints: search, fetchUrl, getStatus
```

### LibraryController
```
Base Path: /api/library
Injects: EbookService, ZimFileService, GutenbergService
Endpoints: listEbooks, uploadEbook, getEbook, deleteEbook, listZimFiles, getZimStatus, searchGutenberg, importGutenberg
```

---

## 11. Security Configuration

```
Authentication: JWT (stateless, HS256/HS384)
Token issuer/validator: Internal (JwtService using jjwt)
Password encoder: BCrypt (default rounds)

Public endpoints (no auth required):
  - /api/auth/login, /api/auth/register, /api/auth/refresh
  - /api/system/status, /api/system/initialize, /api/system/finalize-setup
  - /api/setup/**
  - /api/models, /api/models/health
  - /setup/**
  - /actuator/health
  - /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html
  - /mcp/**

Protected endpoints (patterns):
  - All other /api/** → authenticated (JWT required)
  - Role-based access via @PreAuthorize on individual methods

CORS: AllowedOriginPatterns=["*"], AllowedMethods=[GET,POST,PUT,DELETE,OPTIONS], AllowCredentials=true
CSRF: Disabled (stateless REST API)
Rate limiting: Bucket4j per-IP. Auth endpoints: 10 req/min. General API: 200 req/min.
```

---

## 12. Custom Security Components

```
=== JwtAuthFilter.java ===
Extends: OncePerRequestFilter
Purpose: Extracts Bearer token, validates via JwtService, sets SecurityContext
Extracts token from: Authorization header (Bearer prefix)
Validates via: JwtService.isTokenValid(), AuthService.isTokenBlacklisted()
Sets SecurityContext: YES

=== McpAuthFilter.java ===
Purpose: Authenticates MCP API token requests on /mcp/** paths
Validates via: McpTokenService (BCrypt hash comparison against active tokens)
Sets SecurityContext: YES (McpAuthentication)

=== CaptivePortalRedirectFilter.java ===
Order: 1
Purpose: Redirects non-API requests to setup wizard when in AP mode

=== MdcFilter.java ===
Order: 2
Purpose: Populates MDC with requestId, username, userId for structured logging

=== RateLimitingFilter.java ===
Order: 3
Purpose: Per-IP token-bucket rate limiting via Bucket4j

=== RequestResponseLoggingFilter.java ===
Order: 4
Purpose: Logs HTTP method, URI, status, duration at DEBUG level
```

---

## 13. Exception Handling & Error Responses

```
=== GlobalExceptionHandler.java ===
@ControllerAdvice: YES

Exception Mappings:
  - EntityNotFoundException → 404
  - DuplicateResourceException → 409
  - OllamaUnavailableException → 503
  - OllamaInferenceException → 502
  - StorageException → 500
  - EmbeddingException → 500
  - SkillDisabledException → 400
  - UnsupportedFileTypeException → 400
  - SensorConnectionException → 502
  - FortressActiveException → 403
  - FortressOperationException → 500
  - InitializationException → 409
  - ApModeException → 500
  - OcrException → 500
  - MethodArgumentNotValidException → 400
  - AccessDeniedException → 403
  - Exception → 500

Standard error response format:
{
  "success": false,
  "message": "...",
  "timestamp": "..."
}
```

---

## 14. Mappers / DTOs

No MapStruct or ModelMapper. All mapping is manual in service methods. DTO structures documented in OpenAPI spec.

---

## 15. Utility Classes & Shared Components

```
=== AppConstants.java ===
Purpose: Centralized constants for all magic numbers, paths, timeouts, limits. 500+ lines.

=== ApiResponse.java ===
Purpose: Standard API response wrapper — success(data), error(message), success(message, data)

=== AesEncryptionUtil.java ===
Purpose: AES-256-GCM encryption for API keys at rest
Methods: encrypt(String): String, decrypt(String): String

=== AesAttributeConverter.java ===
Purpose: JPA @Convert attribute converter using AesEncryptionUtil

=== TokenCounter.java ===
Purpose: Estimates token count from text (chars/4 approximation)
Methods: countTokens(String): int

=== DeltaJsonUtils.java ===
Purpose: Extracts plain text from Quill Delta JSON documents
Methods: deltaToPlainText(String): String
```

---

## 16. Database Schema (Live)

Database not available during audit (no running PostgreSQL instance). Schema is managed by Hibernate `ddl-auto: update` in dev. Entities define the schema via JPA annotations (see Section 6).

---

## 17. MESSAGE BROKER DETECTION

```
Broker: MQTT (Eclipse Paho v3 client)
Connection: tcp://localhost:1883 (conditional on app.mqtt.enabled)
Conditional: @ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true")

Topics:
  - myoffgridai/sensors/readings — sensor reading events
  - myoffgridai/system/alerts — system health alerts
  - myoffgridai/notifications — general notification events
  - myoffgridai/insights — insight generation events
  - /myoffgridai/{userId}/notifications — user-specific notifications
  - /myoffgridai/broadcast — broadcast to all devices

Publisher: MqttPublisherService.publishToTopic(String, NotificationPayload)
QoS: 1 (at-least-once)
Graceful degradation: If MQTT client is null or disconnected, publish is silently skipped with log warning.
```

---

## 18. Cache Layer

No Redis or caching layer detected.

---

## 19. ENVIRONMENT VARIABLE INVENTORY

| Variable | Used In | Default | Required in Prod |
|----------|---------|---------|-----------------|
| app.encryption.key | AesEncryptionUtil | dev key hardcoded | YES |
| app.jwt.secret | JwtService | dev-secret-key | YES |
| app.jwt.expiration-ms | JwtService | 86400000 | NO |
| app.jwt.refresh-expiration-ms | JwtService | 604800000 | NO |
| app.ollama.model | OllamaConfig | Qwen3-32B-GGUF:Q4_K_M | NO |
| app.ollama.embed-model | OllamaConfig | nomic-embed-text | NO |
| app.fortress.mock | FortressService | true (dev) | NO (false in prod) |
| app.ap.mock | ApModeService | true (dev) | NO (false in prod) |
| app.mqtt.enabled | MqttConfig | false (dev) | NO (true in prod) |
| app.mqtt.broker-url | MqttConfig | tcp://localhost:1883 | NO |
| app.rate-limiting.enabled | RateLimitingFilter | true | NO |
| server.port | OllamaConfig | 8080 | NO |
| spring.profiles.active | UserService | dev | YES |

---

## 20. SERVICE DEPENDENCY MAP

```
This Service → Depends On
--------------------------
Ollama LLM: http://localhost:11434 (chat, embed, model listing)
PostgreSQL: jdbc:postgresql://localhost:5432/myoffgridai
MQTT Broker: tcp://localhost:1883 (Mosquitto, optional)
Kiwix Content Server: http://localhost:8888 (optional, for ZIM files)
Calibre Content Server: http://localhost:8081 (optional, for eBook conversion)
Anthropic Claude API: https://api.anthropic.com/v1/messages (optional, external — requires API key)
Brave Search API: https://api.search.brave.com/res/v1/web/search (optional, external — requires API key)
Gutendex API: https://gutendex.com (optional, external — for Project Gutenberg catalog)

Downstream Consumers:
- Flutter mobile app (REST API + SSE + MQTT)
- External MCP clients (Claude Desktop, Claude Code) via /mcp/sse
```

---

## 21. Known Technical Debt & Issues

| Issue | Location | Severity | Notes |
|-------|----------|----------|-------|
| Stubbed USB update handler | UsbResetWatcherService.java:20,45 | Medium | Update zip handling documented as "stubbed for MI-002" |
| JaCoCo coverage threshold set to 80%/60% | pom.xml jacoco config | Medium | Should be 100% per conventions — comment says "Raise to 1.00 once Testcontainers/Docker API compatibility is resolved" |
| Dev encryption key hardcoded | application.yml dev profile | Low | Acceptable for dev, must be externalized for prod |
| No API versioning in paths | All controllers | Low | Paths use /api/... without /v1/ prefix |
| No HTTPS enforcement | application-prod.yml | Medium | No server.ssl or require-ssl configuration for prod |
| No @Version optimistic locking | All entities | Low | No concurrent write protection |
| No docker-compose.yml | Project root | Low | Manual PostgreSQL/Mosquitto/Kiwix setup required |
| Snyk: 1 CRITICAL vulnerability | tomcat-embed-core@10.1.41 | CRITICAL | Improper Certificate Validation — upgrade Spring Boot |
| Snyk: 10 HIGH vulnerabilities | Various (tomcat, spring-security, spring-beans, spring-core) | HIGH | Upgrade Spring Boot to latest patch |

---

## 22. Security Vulnerability Scan (Snyk)

Scan Date: 2026-03-16
Snyk CLI Version: 1.1303.0

### Dependency Vulnerabilities (Open Source)
Critical: 1
High: 10
Medium: 7
Low: 3

| Severity | Package | Version | Vulnerability | Fix Available |
|----------|---------|---------|---------------|---------------|
| CRITICAL | tomcat-embed-core | 10.1.41 | Improper Certificate Validation | 9.0.113+ |
| HIGH | tomcat-embed-core | 10.1.41 | Allocation of Resources Without Limits (x2) | 9.0.106+ |
| HIGH | tomcat-embed-core | 10.1.41 | Integer Overflow or Wraparound | 9.0.107+ |
| HIGH | tomcat-embed-core | 10.1.41 | Improper Resource Shutdown or Release | 9.0.108+ |
| HIGH | tomcat-embed-core | 10.1.41 | Relative Path Traversal | 9.0.109+ |
| HIGH | tomcat-embed-core | 10.1.41 | Untrusted Search Path | 9.0.106+ |
| HIGH | tomcat-embed-core | 10.1.41 | Incorrect Authorization | 9.0.114+ |
| HIGH | spring-security-core | 6.4.6 | Incorrect Authorization | 6.4.10+ |
| HIGH | spring-beans | 6.2.7 | Relative Path Traversal | 6.2.10+ |
| HIGH | spring-core | 6.2.7 | Incorrect Authorization | 6.2.11+ |
| MEDIUM | logback-core | 1.5.18 | External Init of Trusted Variables | 1.3.16+ |
| MEDIUM | netty-codec-http | 4.1.125 | CRLF Injection | 4.1.129+ |
| MEDIUM | poi-ooxml | 5.3.0 | Improper Input Validation | 5.4.0+ |
| MEDIUM | tomcat-embed-core | 10.1.41 | Auth Bypass, Session Fixation, etc. | 9.0.106+ |
| MEDIUM | spring-web | 6.2.7 | HTTP Response Splitting | 6.1.21+ |

### Code Vulnerabilities (SAST)
Snyk Code not enabled for organization — SKIPPED.

### Recommended Fix
Upgrade Spring Boot from 3.4.6 to latest 3.4.x patch to resolve the majority of Tomcat and Spring Framework vulnerabilities.
