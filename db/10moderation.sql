
BEGIN;

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS hidden_at     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS hidden_reason TEXT,
    ADD COLUMN IF NOT EXISTS hidden_by     UUID REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_listings_hidden
    ON listings (hidden_at)
    WHERE hidden_at IS NOT NULL;

ALTER TABLE restrictions
    ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS lifted_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS lifted_by  UUID REFERENCES users (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS moderation_actions (
                                                  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moderator_id   UUID REFERENCES users (id) ON DELETE SET NULL,
    action         VARCHAR(40) NOT NULL,
    target_type    VARCHAR(20) NOT NULL,
    target_id      UUID,
    target_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    reason         TEXT,
    details        TEXT,
    created_at     TIMESTAMP   NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_moderation_actions_created
    ON moderation_actions (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_moderation_actions_target_user
    ON moderation_actions (target_user_id, created_at DESC);

COMMIT;
