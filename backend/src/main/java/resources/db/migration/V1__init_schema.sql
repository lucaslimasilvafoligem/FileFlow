CREATE TABLE files (
    id            UUID PRIMARY KEY,
    owner_id      UUID,
    object_key    VARCHAR(512) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    extension     VARCHAR(32),
    content_type  VARCHAR(255) NOT NULL,
    size          BIGINT       NOT NULL,
    checksum      VARCHAR(128),
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uq_files_object_key UNIQUE (object_key),
    CONSTRAINT ck_files_size CHECK (size >= 0),
    CONSTRAINT ck_files_status CHECK (status IN ('UPLOADED', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);
CREATE INDEX idx_files_created_at ON files (created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_files_status     ON files (status)          WHERE deleted_at IS NULL;
CREATE INDEX idx_files_owner      ON files (owner_id)        WHERE deleted_at IS NULL;

CREATE TABLE jobs (
    id              UUID PRIMARY KEY,
    file_id         UUID         NOT NULL REFERENCES files (id) ON DELETE CASCADE,
    type            VARCHAR(40)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(200) NOT NULL,
    payload         JSONB,
    available_at    TIMESTAMPTZ  NOT NULL,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_jobs_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_jobs_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_jobs_status CHECK (status IN
        ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'DEAD_LETTER', 'CANCELLED'))
);
CREATE INDEX idx_jobs_file_id ON jobs (file_id);
CREATE INDEX idx_jobs_active ON jobs (status, available_at) WHERE status IN ('PENDING', 'PROCESSING');

CREATE TABLE artifacts (
    id           UUID PRIMARY KEY,
    file_id      UUID         NOT NULL REFERENCES files (id) ON DELETE CASCADE,
    job_id       UUID         NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    type         VARCHAR(40)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    object_key   VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    size         BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_artifacts_object_key UNIQUE (object_key),
    CONSTRAINT uq_artifacts_job_name UNIQUE (job_id, name),
    CONSTRAINT ck_artifacts_size CHECK (size >= 0)
);
CREATE INDEX idx_artifacts_file_id ON artifacts (file_id);

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    aggregate_id UUID        NOT NULL,
    event_type   VARCHAR(80) NOT NULL,
    payload      JSONB       NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events (created_at) WHERE published_at IS NULL;
