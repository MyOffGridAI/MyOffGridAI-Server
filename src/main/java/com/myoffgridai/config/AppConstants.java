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
    public static final int OLLAMA_READ_TIMEOUT_SECONDS = 600;

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

    /** MIME type for RTF files */
    public static final String MIME_RTF = "application/rtf";

    /** MIME type for Quill Delta JSON documents created in the editor */
    public static final String MIME_QUILL_DELTA = "application/x-quill-delta";

    /** Word-processing MIME types (DOC, DOCX, RTF) */
    public static final java.util.List<String> WORD_MIME_TYPES = java.util.List.of(MIME_DOCX, MIME_DOC, MIME_RTF);

    /** Spreadsheet MIME types (XLSX, XLS) */
    public static final java.util.List<String> SPREADSHEET_MIME_TYPES = java.util.List.of(MIME_XLSX, MIME_XLS);

    /** Presentation MIME types (PPTX, PPT) */
    public static final java.util.List<String> PRESENTATION_MIME_TYPES = java.util.List.of(MIME_PPTX, MIME_PPT);

    /** MIME types that support in-app editing via the rich text editor */
    public static final java.util.List<String> EDITABLE_MIME_TYPES = java.util.List.of(
            "text/plain", "text/markdown", "text/x-markdown",
            MIME_DOCX, MIME_DOC, MIME_RTF, MIME_QUILL_DELTA
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
            MIME_RTF,
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

    // ── Events ─────────────────────────────────────────────────────────────

    /** Base path for scheduled event management endpoints */
    public static final String EVENTS_API_PATH = "/api/events";

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

    // ── Rate Limiting ────────────────────────────────────────────────────────

    /** Maximum requests per minute for authentication endpoints (/api/auth/**) */
    public static final int RATE_LIMIT_AUTH_CAPACITY = 10;

    /** Maximum requests per minute for general API endpoints */
    public static final int RATE_LIMIT_API_CAPACITY = 200;

    /** Rate limit refill period in minutes */
    public static final int RATE_LIMIT_REFILL_MINUTES = 1;

    /** HTTP 429 error message returned when rate limit is exceeded */
    public static final String RATE_LIMIT_EXCEEDED_MESSAGE = "Rate limit exceeded. Please try again shortly.";

    // ── External APIs & Enrichment ──────────────────────────────────────────

    /** Base path for external API settings endpoints */
    public static final String API_SETTINGS_EXTERNAL = "/api/settings/external-apis";

    /** Base path for per-user settings endpoints */
    public static final String API_USER_SETTINGS = "/api/users/me/settings";

    /** Base path for enrichment endpoints */
    public static final String API_ENRICHMENT = "/api/enrichment";

    /** Anthropic Messages API URL */
    public static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    /** Anthropic API version header value */
    public static final String ANTHROPIC_API_VERSION = "2023-06-01";

    /** Maximum tokens for Anthropic API responses */
    public static final int ANTHROPIC_MAX_TOKENS = 4096;

    /** Timeout in seconds for Anthropic API calls */
    public static final int ANTHROPIC_TIMEOUT_SECONDS = 60;

    /** Brave Search API base URL */
    public static final String BRAVE_SEARCH_API_URL = "https://api.search.brave.com/res/v1/web/search";

    /** Timeout in seconds for web fetch requests */
    public static final int WEB_FETCH_TIMEOUT_SECONDS = 30;

    // ── MCP (Model Context Protocol) ────────────────────────────────────────

    /** Base path for MCP token management endpoints */
    public static final String API_MCP_TOKENS = "/api/mcp/tokens";

    /** Base path for MCP discovery endpoints */
    public static final String API_MCP = "/api/mcp";

    // ── MQTT ─────────────────────────────────────────────────────────────────

    /** MQTT topic for sensor reading events */
    public static final String MQTT_TOPIC_SENSOR_READINGS = "myoffgridai/sensors/readings";

    /** MQTT topic for system alerts */
    public static final String MQTT_TOPIC_SYSTEM_ALERTS = "myoffgridai/system/alerts";

    /** MQTT topic for notification push events */
    public static final String MQTT_TOPIC_NOTIFICATIONS = "myoffgridai/notifications";

    /** MQTT topic for insight generation events */
    public static final String MQTT_TOPIC_INSIGHTS = "myoffgridai/insights";

    /** Default MQTT broker port */
    public static final int MQTT_DEFAULT_PORT = 1883;

    /** Default MQTT WebSocket port */
    public static final int MQTT_WEBSOCKET_PORT = 9001;

    /** MQTT topic pattern for user-specific notifications (format with userId) */
    public static final String MQTT_TOPIC_USER_NOTIFICATIONS = "/myoffgridai/%s/notifications";

    /** MQTT topic for broadcast notifications to all connected devices */
    public static final String MQTT_TOPIC_BROADCAST = "/myoffgridai/broadcast";

    /** Default MQTT client ID for the server publisher */
    public static final String MQTT_SERVER_CLIENT_ID = "myoffgridai-server";

    /** MQTT QoS level: at-least-once delivery */
    public static final int MQTT_QOS = 1;

    // ── Library ──────────────────────────────────────────────────────────────

    /** Base path for library endpoints */
    public static final String LIBRARY_API_PATH = "/api/library";

    /** Gutendex API base URL */
    public static final String GUTENBERG_API_BASE = "https://gutendex.com";

    /** Default search result limit for Gutenberg searches */
    public static final int GUTENBERG_SEARCH_LIMIT = 20;

    /** Maximum eBook upload size in bytes (2 GB) */
    public static final long MAX_EBOOK_UPLOAD_BYTES = 2L * 1024 * 1024 * 1024;

    /** Supported eBook file extensions */
    public static final java.util.List<String> SUPPORTED_EBOOK_EXTENSIONS =
            java.util.List.of("epub", "pdf", "mobi", "azw", "txt", "html");

    /** Timeout in seconds for Calibre ebook-convert operations */
    public static final int CALIBRE_CONVERSION_TIMEOUT_SECONDS = 120;

    // ── Inference Provider ─────────────────────────────────────────────────

    /** Inference provider value for llama-server (llama.cpp standalone HTTP binary) */
    public static final String INFERENCE_PROVIDER_LLAMA_SERVER = "llama-server";

    /** Inference provider value for Ollama */
    public static final String INFERENCE_PROVIDER_OLLAMA = "ollama";

    /** Inference provider value for native java-llama.cpp JNI bindings */
    public static final String INFERENCE_PROVIDER_NATIVE = "native";

    /** llama-server chat completions endpoint (OpenAI-compatible) */
    public static final String LLAMA_SERVER_CHAT_ENDPOINT = "/v1/chat/completions";

    /** llama-server models listing endpoint (OpenAI-compatible) */
    public static final String LLAMA_SERVER_MODELS_ENDPOINT = "/v1/models";

    /** Interval in milliseconds between startup health polls for llama-server */
    public static final int LLAMA_SERVER_STARTUP_POLL_INTERVAL_MS = 2000;

    /** Maximum number of log lines to retain in the llama-server log buffer */
    public static final int LLAMA_SERVER_LOG_BUFFER_LINES = 100;

    /** Opening tag for reasoning model thinking traces */
    public static final String THINK_TAG_OPEN = "<think>";

    /** Closing tag for reasoning model thinking traces */
    public static final String THINK_TAG_CLOSE = "</think>";

    /** Buffer size for detecting think tags split across SSE chunks */
    public static final int THINK_TAG_PARSE_BUFFER_SIZE = 16;

    // ── HuggingFace API ─────────────────────────────────────────────────────

    /** HuggingFace API base URL */
    public static final String HF_API_BASE = "https://huggingface.co/api";

    /** HuggingFace models search API URL */
    public static final String HF_API_MODELS = "https://huggingface.co/api/models";

    /** HuggingFace CDN base URL for file downloads */
    public static final String HF_CDN_BASE = "https://huggingface.co";

    /** Default number of results for HuggingFace model searches */
    public static final int HF_SEARCH_DEFAULT_LIMIT = 20;

    /** Maximum number of results for HuggingFace model searches */
    public static final int HF_SEARCH_MAX_LIMIT = 50;

    /** Buffer size in bytes for download chunk writes (64KB) */
    public static final int HF_DOWNLOAD_BUFFER_SIZE = 65536;

    /** Maximum model file size allowed for download (50GB) */
    public static final long HF_MAX_MODEL_SIZE_BYTES = 50L * 1024 * 1024 * 1024;

    /** Timeout in minutes for model file downloads */
    public static final int HF_DOWNLOAD_TIMEOUT_MINUTES = 120;

    // ── Model Quantization Recommendation ──────────────────────────────────

    /** RAM overhead multiplier for loaded models (accounts for KV cache, runtime buffers) */
    public static final double MODEL_RAM_OVERHEAD_FACTOR = 1.2;

    /** RAM reserved for OS and other processes in bytes (4 GB) */
    public static final long MODEL_RAM_OS_RESERVE_BYTES = 4L * 1024 * 1024 * 1024;

    // ── Judge Model ──────────────────────────────────────────────────────────

    /** Default port for the judge llama-server instance */
    public static final int JUDGE_DEFAULT_PORT = 1235;

    /** Default minimum score threshold; below this the judge recommends cloud refinement */
    public static final double JUDGE_DEFAULT_SCORE_THRESHOLD = 7.5;

    /** Default timeout in seconds for judge model inference and health checks */
    public static final int JUDGE_DEFAULT_TIMEOUT_SECONDS = 30;

    /** Context window size in tokens for the judge model */
    public static final int JUDGE_CONTEXT_SIZE = 4096;

    /** SSE event type emitted when the judge begins evaluating a local response */
    public static final String JUDGE_EVALUATING_EVENT = "JUDGE_EVALUATING";

    /** SSE event type emitted when judge evaluation is complete */
    public static final String JUDGE_RESULT_EVENT = "JUDGE_RESULT";

    /** SSE event type for tokens streamed from a cloud frontier model */
    public static final String ENHANCED_CONTENT_EVENT = "ENHANCED_CONTENT";

    /** SSE event type emitted when the enhanced cloud response stream is complete */
    public static final String ENHANCED_DONE_EVENT = "ENHANCED_DONE";

    // ── Frontier API Providers ───────────────────────────────────────────────

    /** Grok (xAI) API base URL (OpenAI-compatible) */
    public static final String GROK_API_BASE_URL = "https://api.x.ai/v1";

    /** Default Grok model for frontier completions */
    public static final String GROK_DEFAULT_MODEL = "grok-3-mini";

    /** OpenAI API base URL */
    public static final String OPENAI_API_BASE_URL = "https://api.openai.com/v1";

    /** Default OpenAI model for frontier completions */
    public static final String OPENAI_DEFAULT_MODEL = "gpt-4o-mini";

    // ── Hybrid Search ────────────────────────────────────────────────────────

    /** Weight applied to vector (cosine) similarity in hybrid search scoring */
    public static final double HYBRID_SEARCH_VECTOR_WEIGHT = 0.7;

    /** Weight applied to BM25 keyword relevance in hybrid search scoring */
    public static final double HYBRID_SEARCH_BM25_WEIGHT = 0.3;

    // ── Inference Retry ──────────────────────────────────────────────────────

    /** Maximum number of retry attempts for transient inference errors */
    public static final int INFERENCE_MAX_RETRIES = 3;

    /** Base delay in milliseconds for exponential backoff between retries */
    public static final long INFERENCE_BACKOFF_BASE_MS = 500L;

    /** Maximum backoff delay in milliseconds (caps exponential growth) */
    public static final long INFERENCE_BACKOFF_CAP_MS = 10_000L;

    // ── Memory Compaction ────────────────────────────────────────────────────

    /** Ratio of context window usage that triggers compaction (0.80 = 80%) */
    public static final double COMPACTION_THRESHOLD_RATIO = 0.80;

    /** Minimum message count before compaction is allowed */
    public static final int COMPACTION_MIN_MESSAGES = 10;

    /** Maximum tokens for compaction summary output */
    public static final int COMPACTION_SUMMARY_MAX_TOKENS = 512;

    // ── Token Usage ──────────────────────────────────────────────────────────

    /** Default page size for token usage history queries */
    public static final int TOKEN_USAGE_PAGE_SIZE_DEFAULT = 30;

    // ── Heartbeat ────────────────────────────────────────────────────────────

    /** Default interval in seconds between heartbeat task evaluations */
    public static final int HEARTBEAT_DEFAULT_INTERVAL_SECONDS = 300;

    /** Maximum context tokens for heartbeat prompt execution */
    public static final int HEARTBEAT_MAX_CONTEXT_TOKENS = 2048;
}
