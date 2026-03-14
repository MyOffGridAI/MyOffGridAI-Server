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

    // ── Sensors ─────────────────────────────────────────────────────────────

    /** Default sensor polling interval in seconds */
    public static final int SENSOR_POLLING_INTERVAL_SECONDS = 30;

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

    // ── Password Validation ─────────────────────────────────────────────────

    /** Minimum password length in dev profile */
    public static final int PASSWORD_MIN_LENGTH_DEV = 4;

    /** Minimum password length in prod profile */
    public static final int PASSWORD_MIN_LENGTH_PROD = 12;
}
