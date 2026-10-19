CREATE INDEX IF NOT EXISTS idx_chat_messages_room_created
    ON chat_messages (chat_room_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_unread
    ON chat_messages (chat_room_id, sender_id)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_chat_rooms_user_a ON chat_rooms (user_a_id);
CREATE INDEX IF NOT EXISTS idx_chat_rooms_user_b ON chat_rooms (user_b_id);

BEGIN;

CREATE TEMP TABLE chat_room_keepers ON COMMIT DROP AS
SELECT id,
       FIRST_VALUE(id) OVER (
           PARTITION BY LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id)
           ORDER BY created_at NULLS LAST, id
           ) AS keeper
FROM chat_rooms;

UPDATE chat_messages m
SET chat_room_id = k.keeper
FROM chat_room_keepers k
WHERE m.chat_room_id = k.id AND k.id <> k.keeper;

UPDATE notifications n
SET reference_id = k.keeper
FROM chat_room_keepers k
WHERE n.type = 'NEW_CHAT_MESSAGE' AND n.reference_id = k.id AND k.id <> k.keeper;

DELETE FROM chat_rooms c
    USING chat_room_keepers k
WHERE c.id = k.id AND k.id <> k.keeper;

ALTER TABLE chat_rooms DROP COLUMN IF EXISTS exchange_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_rooms_pair
    ON chat_rooms (LEAST(user_a_id, user_b_id), GREATEST(user_a_id, user_b_id));

INSERT INTO chat_rooms (user_a_id, user_b_id, created_at)
SELECT DISTINCT ON (LEAST(e.owner_id, e.reader_id), GREATEST(e.owner_id, e.reader_id))
    e.owner_id, e.reader_id, e.created_at
FROM exchanges e
WHERE NOT EXISTS (
    SELECT 1 FROM chat_rooms r
    WHERE LEAST(r.user_a_id, r.user_b_id) = LEAST(e.owner_id, e.reader_id)
      AND GREATEST(r.user_a_id, r.user_b_id) = GREATEST(e.owner_id, e.reader_id)
)
ORDER BY LEAST(e.owner_id, e.reader_id), GREATEST(e.owner_id, e.reader_id), e.created_at;

COMMIT;