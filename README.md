# MyOffGridAI-Server

## Environment Variable Inventory

| Variable | Required in Prod | Default | Description |
|---|---|---|---|
| `DB_URL` | Yes | `jdbc:postgresql://localhost:5432/myoffgridai` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | `myoffgridai` | Database username |
| `DB_PASSWORD` | Yes | `myoffgridai` | Database password |
| `JWT_SECRET` | Yes | — | HMAC signing key for JWT tokens (min 256-bit) |
| `JWT_EXPIRATION_MS` | No | `86400000` | Access token TTL in milliseconds (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | No | `604800000` | Refresh token TTL in milliseconds (7d) |
| `ENCRYPTION_KEY` | Yes | — | AES-256-GCM key for encrypting API keys at rest |
| `INFERENCE_PROVIDER` | No | `lmstudio` | Inference backend (`lmstudio` or `ollama`) |
| `INFERENCE_BASE_URL` | No | `http://localhost:1234` | Inference server URL |
| `INFERENCE_MODEL` | No | (see application-prod.yml) | Default inference model |
| `INFERENCE_EMBED_MODEL` | No | `nomic-embed-text` | Embedding model name |
| `INFERENCE_TIMEOUT` | No | `120` | Inference timeout in seconds |
| `INFERENCE_MAX_TOKENS` | No | `4096` | Max tokens per inference |
| `INFERENCE_TEMPERATURE` | No | `0.7` | Inference temperature |
| `HF_MODELS_DIR` | No | `~/.lmstudio/models` | HuggingFace model download directory |
| `LMSTUDIO_API_URL` | No | `http://localhost:1234` | LM Studio API base URL |