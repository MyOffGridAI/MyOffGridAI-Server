package com.myoffgridai.config;

/**
 * Centralized application constants for the MyOffGridAI server.
 *
 * <p>All magic numbers, hardcoded strings, and configuration defaults
 * are defined here. No other class should contain inline constants.
 * Constants are organized by functional domain.</p>
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // ── Server ──────────────────────────────────────────────────────────────

    /** Default HTTP server port */
    public static final int SERVER_PORT = 8080;

    /** Default Ollama LLM service port */
    public static final int OLLAMA_PORT = 11434;

    /** Default PostgreSQL database port */
    public static final int POSTGRES_PORT = 5432;

    // ── JWT ─────────────────────────────────────────────────────────────────

    /** Access token expiration in milliseconds (24 hours) */
    public static final long JWT_EXPIRATION_MS = 86_400_000L;

    /** Refresh token expiration in milliseconds (7 days) */
    public static final long JWT_REFRESH_EXPIRATION_MS = 604_800_000L;

    /** Bearer token type identifier */
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    /** Authorization header name */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer token prefix in Authorization header */
    public static final String BEARER_PREFIX = "Bearer ";

    // ── API Paths ───────────────────────────────────────────────────────────

    /** Base path for authentication endpoints */
    public static final String API_AUTH = "/api/auth";

    /** Base path for user management endpoints */
    public static final String API_USERS = "/api/users";

    /** Base path for AI conversation endpoints */
    public static final String API_AI = "/api/ai";

    /** Base path for memory management endpoints */
    public static final String API_MEMORY = "/api/memory";

    /** Base path for knowledge base endpoints */
    public static final String API_KNOWLEDGE = "/api/knowledge";

    /** Base path for skills endpoints */
    public static final String API_SKILLS = "/api/skills";

    /** Base path for sensor data endpoints */
    public static final String API_SENSORS = "/api/sensors";

    /** Base path for proactive intelligence endpoints */
    public static final String API_PROACTIVE = "/api/proactive";

    /** Base path for privacy/vault endpoints */
    public static final String API_PRIVACY = "/api/privacy";

    /** Base path for system management endpoints */
    public static final String API_SYSTEM = "/api/system";

    // ── Pagination ──────────────────────────────────────────────────────────

    /** Default number of items per page */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Maximum allowed items per page */
    public static final int MAX_PAGE_SIZE = 100;

    // ── File Upload ─────────────────────────────────────────────────────────

    /** Maximum file upload size in bytes (100 MB) */
    public static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;

    /** Default maximum upload size per file in megabytes (configurable via Settings) */
    public static final int MAX_UPLOAD_SIZE_MB_DEFAULT = 25;

    // ── Sensors ─────────────────────────────────────────────────────────────

    /** Base path for sensor management endpoints */
    public static final String SENSORS_API_PATH = "/api/sensors";

    /** Default sensor polling interval in seconds */
    public static final int DEFAULT_SENSOR_POLL_INTERVAL_SECONDS = 30;

    /** Minimum sensor polling interval in seconds */
    public static final int MIN_SENSOR_POLL_INTERVAL_SECONDS = 5;

    /** Maximum sensor polling interval in seconds */
    public static final int MAX_SENSOR_POLL_INTERVAL_SECONDS = 3600;

    /** Number of consecutive parse failures before auto-stopping a sensor */
    public static final int SENSOR_CONSECUTIVE_FAILURE_LIMIT = 5;

    /** Timeout in milliseconds for sensor connection tests */
    public static final int SENSOR_TEST_TIMEOUT_MS = 5000;

    /** Maximum number of hours of reading history to return */
    public static final int SENSOR_READING_HISTORY_MAX_HOURS = 168;

    /** SSE emitter timeout in milliseconds (30 minutes) */
    public static final long SSE_EMITTER_TIMEOUT_MS = 1_800_000L;

    /** Legacy alias — use DEFAULT_SENSOR_POLL_INTERVAL_SECONDS instead */
    public static final int SENSOR_POLLING_INTERVAL_SECONDS = DEFAULT_SENSOR_POLL_INTERVAL_SECONDS;

    // ── RAG (Retrieval-Augmented Generation) ────────────────────────────────

    /** Default number of top-K results for RAG retrieval */
    public static final int RAG_TOP_K_DEFAULT = 5;

    /** Default chunk size in tokens for document splitting */
    public static final int CHUNK_SIZE_TOKENS = 512;

    /** Default chunk overlap in tokens for document splitting */
    public static final int CHUNK_OVERLAP_TOKENS = 64;

    // ── Memory Importance Levels ────────────────────────────────────────────

    /** Low importance — routine interactions */
    public static final int MEMORY_IMPORTANCE_LOW = 1;

    /** Medium importance — notable preferences or facts */
    public static final int MEMORY_IMPORTANCE_MEDIUM = 5;

    /** High importance — critical personal data or safety info */
    public static final int MEMORY_IMPORTANCE_HIGH = 10;

    // ── Roles ───────────────────────────────────────────────────────────────

    /** Full system access, created at first boot */
    public static final String ROLE_OWNER = "ROLE_OWNER";

    /** All features, user management */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** Full AI features, own data only */
    public static final String ROLE_MEMBER = "ROLE_MEMBER";

    /** Read-only access */
    public static final String ROLE_VIEWER = "ROLE_VIEWER";

    /** Safe mode, filtered responses, no vault access */
    public static final String ROLE_CHILD = "ROLE_CHILD";

    // ── Device & System ─────────────────────────────────────────────────────

    /** Path to the device encryption key on the appliance filesystem */
    public static final String DEVICE_KEY_PATH = "/etc/myoffgridai/.device.key";

    /** SSID broadcast during initial AP-mode setup */
    public static final String AP_MODE_SSID = "MyOffGridAI-Setup";

    /** Default IP address in AP mode */
    public static final String AP_MODE_IP = "192.168.4.1";

    /** Filename for USB update zip archives */
    public static final String UPDATE_ZIP_FILENAME = "myoffgridai-update.zip";

    /** Trigger filename that initiates a factory reset when detected */
    public static final String FACTORY_RESET_TRIGGER_FILENAME = "factory-reset.trigger";

    /** USB mount point for update/reset trigger files */
    public static final String USB_MOUNT_PATH = "/media/myoffgridai/USB";

    /** Timeout in seconds for AP mode services to start */
    public static final int AP_MODE_START_TIMEOUT_SECONDS = 10;

    /** Command execution timeout in seconds for OS operations */
    public static final int COMMAND_TIMEOUT_SECONDS = 10;

    /** Confirmation phrase required for factory reset */
    public static final String FACTORY_RESET_CONFIRM_PHRASE = "RESET MY DEVICE";

    /** Delay in seconds before factory reset executes (allows HTTP response delivery) */
    public static final int FACTORY_RESET_DELAY_SECONDS = 2;

    /** Network stabilization delay in seconds after AP mode stops */
    public static final int NETWORK_TRANSITION_DELAY_SECONDS = 2;

    /** Base path for setup wizard endpoints */
    public static final String API_SETUP = "/api/setup";

    // ── Password Validation ─────────────────────────────────────────────────

    /** Minimum password length in dev profile */
    public static final int PASSWORD_MIN_LENGTH_DEV = 4;

    /** Minimum password length in prod profile */
    public static final int PASSWORD_MIN_LENGTH_PROD = 12;

    // ── Ollama ─────────────────────────────────────────────────────────────

    /** Base URL for the Ollama LLM service */
    public static final String OLLAMA_BASE_URL = "http://localhost:11434";

    /** Default Ollama chat model */
    public static final String OLLAMA_MODEL = "hf.co/Qwen/Qwen3-32B-GGUF:Q4_K_M";

    /** Default Ollama embedding model */
    public static final String OLLAMA_EMBED_MODEL = "nomic-embed-text";

    /** Ollama connection timeout in seconds */
    public static final int OLLAMA_CONNECT_TIMEOUT_SECONDS = 10;

    /** Ollama read timeout in seconds */
    public static final int OLLAMA_READ_TIMEOUT_SECONDS = 120;

    /** Maximum context window size in tokens for Ollama requests */
    public static final int OLLAMA_MAX_CONTEXT_TOKENS = 8192;

    /** Maximum number of recent messages to include in context window */
    public static final int OLLAMA_CONTEXT_WINDOW_MESSAGES = 20;

    /** Default Ollama num_ctx value (context size in tokens sent to Ollama) */
    public static final int OLLAMA_NUM_CTX_DEFAULT = 4096;

    /** Default maximum number of messages fetched from DB per conversation */
    public static final int OLLAMA_CONTEXT_MESSAGE_LIMIT_DEFAULT = 20;

    // ── Chat ───────────────────────────────────────────────────────────────

    /** Maximum length of a single user message in characters */
    public static final int MAX_MESSAGE_LENGTH = 32000;

    /** Base path for chat endpoints */
    public static final String CHAT_API_PATH = "/api/chat";

    /** Base path for model endpoints */
    public static final String MODELS_API_PATH = "/api/models";

    /** Maximum tokens for title generation responses */
    public static final int TITLE_GENERATION_MAX_TOKENS = 20;

    // ── Memory & RAG ─────────────────────────────────────────────────────

    /** Dimensionality of nomic-embed-text embedding vectors */
    public static final int EMBEDDING_DIMENSIONS = 768;

    /** Number of top-K results for RAG knowledge retrieval */
    public static final int RAG_TOP_K = 5;

    /** Number of top-K results for memory retrieval */
    public static final int MEMORY_TOP_K = 5;

    /** Minimum cosine similarity threshold for RAG/memory results */
    public static final float SIMILARITY_THRESHOLD = 0.45f;

    /** Maximum tokens for RAG context injected into system prompt */
    public static final int RAG_MAX_CONTEXT_TOKENS = 2048;

    /** Maximum number of facts to extract per conversation exchange */
    public static final int MEMORY_EXTRACTION_MAX_FACTS = 3;

    /** Tag used for conversation summary memories */
    public static final String MEMORY_SUMMARIZATION_TAG = "conversation-summary";

    /** Minimum number of messages before a conversation is eligible for summarization */
    public static final int SUMMARIZATION_MIN_MESSAGES = 10;

    /** Minimum age in days before a conversation is eligible for summarization */
    public static final int SUMMARIZATION_AGE_DAYS = 7;

    /** Base path for memory management endpoints */
    public static final String MEMORY_API_PATH = "/api/memory";

    // ── Knowledge Vault ──────────────────────────────────────────────────

    /** Base path on local filesystem for uploaded knowledge documents */
    public static final String KNOWLEDGE_STORAGE_BASE_PATH = "/var/myoffgridai/knowledge";

    /** Maximum upload file size in bytes (100 MB) */
    public static final long MAX_UPLOAD_SIZE_BYTES = 104_857_600L;

    /** Target chunk size in characters for document splitting */
    public static final int CHUNK_SIZE_CHARS = 1500;

    /** Overlap in characters between consecutive chunks */
    public static final int CHUNK_OVERLAP_CHARS = 150;

    /** Maximum number of chunks per document */
    public static final int MAX_CHUNKS_PER_DOCUMENT = 500;

    /** Minimum chunk size in characters — smaller chunks are skipped */
    public static final int MIN_CHUNK_SIZE_CHARS = 50;

    /** Base path for knowledge management endpoints */
    public static final String KNOWLEDGE_API_PATH = "/api/knowledge";

    /** MIME type for DOCX files */
    public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /** MIME type for legacy DOC files */
    public static final String MIME_DOC = "application/msword";

    /** MIME type for XLSX files */
    public static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** MIME type for legacy XLS files */
    public static final String MIME_XLS = "application/vnd.ms-excel";

    /** MIME type for PPTX files */
    public static final String MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    /** MIME type for legacy PPT files */
    public static final String MIME_PPT = "application/vnd.ms-powerpoint";

    /** MIME type for Quill Delta JSON documents created in the editor */
    public static final String MIME_QUILL_DELTA = "application/x-quill-delta";

    /** Word-processing MIME types (DOC, DOCX) */
    public static final java.util.List<String> WORD_MIME_TYPES = java.util.List.of(MIME_DOCX, MIME_DOC);

    /** Spreadsheet MIME types (XLSX, XLS) */
    public static final java.util.List<String> SPREADSHEET_MIME_TYPES = java.util.List.of(MIME_XLSX, MIME_XLS);

    /** Presentation MIME types (PPTX, PPT) */
    public static final java.util.List<String> PRESENTATION_MIME_TYPES = java.util.List.of(MIME_PPTX, MIME_PPT);

    /** MIME types that support in-app editing via the rich text editor */
    public static final java.util.List<String> EDITABLE_MIME_TYPES = java.util.List.of(
            "text/plain", "text/markdown", "text/x-markdown",
            MIME_DOCX, MIME_DOC, MIME_QUILL_DELTA
    );

    /** Supported MIME types for knowledge document upload */
    public static final java.util.List<String> SUPPORTED_MIME_TYPES = java.util.List.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "image/png",
            "image/jpeg",
            "image/tiff",
            "image/webp",
            MIME_DOCX,
            MIME_DOC,
            MIME_XLSX,
            MIME_XLS,
            MIME_PPTX,
            MIME_PPT
    );

    // ── Skills & Automation ───────────────────────────────────────────────

    /** Base path for skills management endpoints */
    public static final String SKILLS_API_PATH = "/api/skills";

    /** Base path for inventory convenience endpoints */
    public static final String INVENTORY_API_PATH = "/api/skills/inventory";

    /** Maximum number of agent tool-call iterations per task */
    public static final int AGENT_MAX_ITERATIONS = 5;

    /** Maximum number of chunks to include in document summarization */
    public static final int SUMMARIZER_MAX_CHUNKS = 10;

    /** Built-in skill name: weather query */
    public static final String SKILL_WEATHER_QUERY = "weather-query";

    /** Built-in skill name: inventory tracker */
    public static final String SKILL_INVENTORY_TRACKER = "inventory-tracker";

    /** Built-in skill name: recipe generator */
    public static final String SKILL_RECIPE_GENERATOR = "recipe-generator";

    /** Built-in skill name: task planner */
    public static final String SKILL_TASK_PLANNER = "task-planner";

    /** Built-in skill name: document summarizer */
    public static final String SKILL_DOCUMENT_SUMMARIZER = "document-summarizer";

    /** Built-in skill name: resource calculator */
    public static final String SKILL_RESOURCE_CALCULATOR = "resource-calculator";

    // ── Proactive Engine ──────────────────────────────────────────────────

    /** Base path for insights endpoints */
    public static final String INSIGHTS_API_PATH = "/api/insights";

    /** Base path for notifications endpoints */
    public static final String NOTIFICATIONS_API_PATH = "/api/notifications";

    /** Number of days of activity to analyze for insight generation */
    public static final int INSIGHT_ANALYSIS_WINDOW_DAYS = 7;

    /** Maximum number of insights to generate per run */
    public static final int MAX_INSIGHTS_PER_GENERATION = 3;

    /** Disk space alert threshold in megabytes */
    public static final long DISK_ALERT_THRESHOLD_MB = 500L;

    /** Cooldown period in minutes between repeated health alerts */
    public static final int HEALTH_ALERT_COOLDOWN_MINUTES = 60;

    /** Interval in milliseconds between system health checks */
    public static final int HEALTH_CHECK_INTERVAL_MS = 300000;

    // ── Privacy & Fortress ──────────────────────────────────────────────────

    /** Base path for fortress status endpoint */
    public static final String FORTRESS_STATUS_PATH = "/fortress/status";

    /** Base path for fortress enable endpoint */
    public static final String FORTRESS_ENABLE_PATH = "/fortress/enable";

    /** Base path for fortress disable endpoint */
    public static final String FORTRESS_DISABLE_PATH = "/fortress/disable";

    /** Audit summary window in hours */
    public static final int AUDIT_SUMMARY_WINDOW_HOURS = 24;

    /** Minimum passphrase length for data export encryption */
    public static final int EXPORT_PASSPHRASE_MIN_LENGTH = 8;

    /** Number of PBKDF2 iterations for export encryption key derivation */
    public static final int EXPORT_PBKDF2_ITERATIONS = 65536;

    /** Server version string */
    public static final String SERVER_VERSION = "1.0.0";
}
