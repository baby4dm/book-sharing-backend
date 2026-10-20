
CREATE TABLE IF NOT EXISTS feedback (
                                        id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           VARCHAR(20)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    link            VARCHAR(512),
    screenshot_url  VARCHAR(512),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    ip_address      VARCHAR(64),
    status          VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    moderator_note  TEXT,
    handled_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    CONSTRAINT chk_feedback_topic  CHECK (topic IN ('QUESTION', 'BUG', 'IDEA', 'COMPLAINT')),
    CONSTRAINT chk_feedback_status CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED'))
    );

CREATE INDEX IF NOT EXISTS idx_feedback_status_created ON feedback (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_ip_created ON feedback (ip_address, created_at);
CREATE INDEX IF NOT EXISTS idx_feedback_email_created ON feedback (lower(email), created_at);