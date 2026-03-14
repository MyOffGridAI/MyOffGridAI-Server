-- MyOffGridAI Production Schema Migration V1
-- Generated from Hibernate DDL auto output against PostgreSQL 16 + pgvector
-- Tables: users, conversations, messages, memories, vector_document,
--         knowledge_documents, knowledge_chunks, skills, skill_executions,
--         inventory_items, planned_tasks, sensors, sensor_readings,
--         insights, notifications, audit_logs, system_config

CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255) NOT NULL,
    email character varying(255),
    is_active boolean NOT NULL,
    last_login_at timestamp(6) with time zone,
    password_hash character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    username character varying(255) NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_username_unique UNIQUE (username),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('ROLE_OWNER','ROLE_ADMIN','ROLE_MEMBER','ROLE_VIEWER','ROLE_CHILD'))
);

CREATE TABLE public.conversations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    is_archived boolean NOT NULL,
    message_count integer NOT NULL,
    title character varying(255),
    updated_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    CONSTRAINT conversations_pkey PRIMARY KEY (id),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES public.users(id)
);

CREATE TABLE public.messages (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    has_rag_context boolean NOT NULL,
    role character varying(255) NOT NULL,
    token_count integer,
    conversation_id uuid NOT NULL,
    CONSTRAINT messages_pkey PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES public.conversations(id),
    CONSTRAINT messages_role_check CHECK (role IN ('USER','ASSISTANT','SYSTEM'))
);

CREATE TABLE public.memories (
    id uuid NOT NULL,
    access_count integer NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    importance character varying(255) NOT NULL,
    last_accessed_at timestamp(6) with time zone,
    source_conversation_id uuid,
    tags character varying(255),
    updated_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    CONSTRAINT memories_pkey PRIMARY KEY (id),
    CONSTRAINT memories_importance_check CHECK (importance IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE TABLE public.vector_document (
    id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    embedding public.vector(768),
    metadata text,
    source_id uuid,
    source_type character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT vector_document_pkey PRIMARY KEY (id),
    CONSTRAINT vector_document_source_type_check CHECK (source_type IN ('MEMORY','CONVERSATION','KNOWLEDGE_CHUNK'))
);

CREATE TABLE public.knowledge_documents (
    id uuid NOT NULL,
    chunk_count integer,
    display_name character varying(255),
    error_message text,
    file_size_bytes bigint,
    filename character varying(255) NOT NULL,
    mime_type character varying(255) NOT NULL,
    processed_at timestamp(6) with time zone,
    status character varying(255) NOT NULL,
    storage_path character varying(255) NOT NULL,
    uploaded_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT knowledge_documents_pkey PRIMARY KEY (id),
    CONSTRAINT knowledge_documents_status_check CHECK (status IN ('PENDING','PROCESSING','READY','FAILED'))
);

CREATE TABLE public.knowledge_chunks (
    id uuid NOT NULL,
    chunk_index integer NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    page_number integer,
    user_id uuid NOT NULL,
    document_id uuid NOT NULL,
    CONSTRAINT knowledge_chunks_pkey PRIMARY KEY (id),
    CONSTRAINT fk_knowledge_chunks_document FOREIGN KEY (document_id) REFERENCES public.knowledge_documents(id)
);

CREATE TABLE public.skills (
    id uuid NOT NULL,
    author character varying(255) NOT NULL,
    category character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description text NOT NULL,
    display_name character varying(255) NOT NULL,
    is_built_in boolean NOT NULL,
    is_enabled boolean NOT NULL,
    name character varying(255) NOT NULL,
    parameters_schema text,
    updated_at timestamp(6) with time zone,
    version character varying(255) NOT NULL,
    CONSTRAINT skills_pkey PRIMARY KEY (id),
    CONSTRAINT skills_name_unique UNIQUE (name),
    CONSTRAINT skills_category_check CHECK (category IN ('HOMESTEAD','RESOURCE','PLANNING','KNOWLEDGE','WEATHER','CUSTOM'))
);

CREATE TABLE public.skill_executions (
    id uuid NOT NULL,
    completed_at timestamp(6) with time zone,
    duration_ms bigint,
    error_message text,
    input_params text,
    output_result text,
    started_at timestamp(6) with time zone NOT NULL,
    status character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    skill_id uuid NOT NULL,
    CONSTRAINT skill_executions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_skill_executions_skill FOREIGN KEY (skill_id) REFERENCES public.skills(id),
    CONSTRAINT skill_executions_status_check CHECK (status IN ('RUNNING','COMPLETED','FAILED'))
);

CREATE TABLE public.inventory_items (
    id uuid NOT NULL,
    category character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    low_stock_threshold double precision,
    name character varying(255) NOT NULL,
    notes text,
    quantity double precision NOT NULL,
    unit character varying(255),
    updated_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    CONSTRAINT inventory_items_pkey PRIMARY KEY (id),
    CONSTRAINT inventory_items_category_check CHECK (category IN ('FOOD','TOOLS','MEDICAL','SUPPLIES','SEEDS','EQUIPMENT','OTHER'))
);

CREATE TABLE public.planned_tasks (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    estimated_resources text,
    goal_description text NOT NULL,
    status character varying(255) NOT NULL,
    steps text NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    CONSTRAINT planned_tasks_pkey PRIMARY KEY (id),
    CONSTRAINT planned_tasks_status_check CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED'))
);

CREATE TABLE public.sensors (
    id uuid NOT NULL,
    baud_rate integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    data_format character varying(255) NOT NULL,
    high_threshold double precision,
    is_active boolean NOT NULL,
    low_threshold double precision,
    name character varying(255) NOT NULL,
    poll_interval_seconds integer NOT NULL,
    port_path character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    unit character varying(255),
    updated_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    value_field character varying(255),
    CONSTRAINT sensors_pkey PRIMARY KEY (id),
    CONSTRAINT sensors_port_path_unique UNIQUE (port_path),
    CONSTRAINT sensors_data_format_check CHECK (data_format IN ('CSV_LINE','JSON_LINE')),
    CONSTRAINT sensors_type_check CHECK (type IN ('TEMPERATURE','HUMIDITY','SOIL_MOISTURE','POWER','VOLTAGE','CUSTOM'))
);

CREATE TABLE public.sensor_readings (
    id uuid NOT NULL,
    raw_data character varying(255),
    recorded_at timestamp(6) with time zone NOT NULL,
    value double precision NOT NULL,
    sensor_id uuid NOT NULL,
    CONSTRAINT sensor_readings_pkey PRIMARY KEY (id),
    CONSTRAINT fk_sensor_readings_sensor FOREIGN KEY (sensor_id) REFERENCES public.sensors(id)
);

CREATE TABLE public.insights (
    id uuid NOT NULL,
    category character varying(255) NOT NULL,
    content text NOT NULL,
    generated_at timestamp(6) with time zone NOT NULL,
    is_dismissed boolean NOT NULL,
    is_read boolean NOT NULL,
    read_at timestamp(6) with time zone,
    user_id uuid NOT NULL,
    CONSTRAINT insights_pkey PRIMARY KEY (id),
    CONSTRAINT insights_category_check CHECK (category IN ('HOMESTEAD','HEALTH','RESOURCE','GENERAL'))
);

CREATE TABLE public.notifications (
    id uuid NOT NULL,
    body text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    is_read boolean NOT NULL,
    metadata text,
    read_at timestamp(6) with time zone,
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_type_check CHECK (type IN ('SENSOR_ALERT','SYSTEM_HEALTH','INSIGHT_READY','MODEL_UPDATE','GENERAL'))
);

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    action character varying(255) NOT NULL,
    duration_ms bigint,
    http_method character varying(255) NOT NULL,
    ip_address character varying(255),
    outcome character varying(255) NOT NULL,
    request_path character varying(255) NOT NULL,
    resource_id character varying(255),
    resource_type character varying(255),
    response_status integer,
    "timestamp" timestamp(6) with time zone NOT NULL,
    user_agent character varying(255),
    user_id uuid,
    username character varying(255),
    CONSTRAINT audit_logs_pkey PRIMARY KEY (id),
    CONSTRAINT audit_logs_outcome_check CHECK (outcome IN ('SUCCESS','FAILURE','DENIED'))
);

CREATE TABLE public.system_config (
    id uuid NOT NULL,
    ap_mode_enabled boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    fortress_enabled boolean NOT NULL,
    fortress_enabled_at timestamp(6) with time zone,
    fortress_enabled_by_user_id uuid,
    initialized boolean NOT NULL,
    instance_name character varying(255),
    updated_at timestamp(6) with time zone,
    wifi_configured boolean NOT NULL,
    CONSTRAINT system_config_pkey PRIMARY KEY (id)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_audit_timestamp ON public.audit_logs USING btree ("timestamp" DESC);
CREATE INDEX idx_audit_user_timestamp ON public.audit_logs USING btree (user_id, "timestamp" DESC);
CREATE INDEX idx_insight_user_id ON public.insights USING btree (user_id);
CREATE INDEX idx_inventory_category ON public.inventory_items USING btree (user_id, category);
CREATE INDEX idx_inventory_user_id ON public.inventory_items USING btree (user_id);
CREATE INDEX idx_knowledge_chunk_doc_id ON public.knowledge_chunks USING btree (document_id);
CREATE INDEX idx_knowledge_chunk_user_id ON public.knowledge_chunks USING btree (user_id);
CREATE INDEX idx_knowledge_doc_user_id ON public.knowledge_documents USING btree (user_id);
CREATE INDEX idx_notification_user_id ON public.notifications USING btree (user_id);
CREATE INDEX idx_planned_task_user_id ON public.planned_tasks USING btree (user_id);
CREATE INDEX idx_sensor_reading_sensor_recorded ON public.sensor_readings USING btree (sensor_id, recorded_at DESC);
CREATE INDEX idx_sensor_user_id ON public.sensors USING btree (user_id);
CREATE INDEX idx_skill_category ON public.skills USING btree (category);
CREATE INDEX idx_skill_exec_skill_id ON public.skill_executions USING btree (skill_id);
CREATE INDEX idx_skill_exec_user_id ON public.skill_executions USING btree (user_id);
CREATE INDEX idx_vector_doc_user_source_type ON public.vector_document USING btree (user_id, source_type);
