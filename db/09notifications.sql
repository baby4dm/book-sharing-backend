
BEGIN;

DELETE FROM notifications WHERE type = 'NEW_CHAT_MESSAGE';

UPDATE notifications SET is_read = false WHERE is_read IS NULL;
UPDATE notifications SET created_at = now() WHERE created_at IS NULL;
ALTER TABLE notifications
    ALTER COLUMN is_read SET NOT NULL,
ALTER COLUMN created_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id)
    WHERE is_read = false;

CREATE TABLE IF NOT EXISTS deadline_reminders (
                                                  exchange_id UUID        NOT NULL REFERENCES exchanges (id) ON DELETE CASCADE,
    kind        VARCHAR(20) NOT NULL CHECK (kind IN ('SOON', 'LAST_DAY', 'OVERDUE')),
    due_date    DATE        NOT NULL,
    sent_at     TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (exchange_id, kind, due_date)
    );

COMMIT;