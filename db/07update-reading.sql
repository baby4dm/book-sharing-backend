
BEGIN;

ALTER TABLE requests ADD COLUMN IF NOT EXISTS reading_days INT;
UPDATE requests
SET reading_days = GREATEST(1, LEAST(90, desired_deadline - created_at::date))
WHERE reading_days IS NULL;
ALTER TABLE requests ALTER COLUMN reading_days SET NOT NULL;
ALTER TABLE requests ADD CONSTRAINT chk_requests_reading_days CHECK (reading_days BETWEEN 1 AND 90);
ALTER TABLE requests DROP COLUMN desired_deadline;

ALTER TABLE exchanges ADD COLUMN IF NOT EXISTS reading_days INT;
ALTER TABLE exchanges ADD COLUMN IF NOT EXISTS reading_started_at TIMESTAMP;
ALTER TABLE exchanges ADD COLUMN IF NOT EXISTS return_prepared_at TIMESTAMP;
ALTER TABLE exchanges ADD COLUMN IF NOT EXISTS returned_at TIMESTAMP;
ALTER TABLE exchanges ADD COLUMN IF NOT EXISTS reading_paused_days INT NOT NULL DEFAULT 0;

UPDATE exchanges e SET reading_days = r.reading_days
    FROM requests r WHERE r.id = e.request_id AND e.reading_days IS NULL;
UPDATE exchanges SET reading_days = 14 WHERE reading_days IS NULL;
ALTER TABLE exchanges ALTER COLUMN reading_days SET NOT NULL;

ALTER TABLE exchanges ALTER COLUMN deadline DROP NOT NULL;

UPDATE exchanges e SET reading_started_at = p.first_at
    FROM (SELECT exchange_id, uploaded_by, MIN(created_at) AS first_at
      FROM exchange_photos WHERE stage = 'HANDOVER'
      GROUP BY exchange_id, uploaded_by) p
WHERE p.exchange_id = e.id AND p.uploaded_by = e.reader_id AND e.reading_started_at IS NULL;

UPDATE exchanges SET deadline = NULL, extended_deadline = NULL
WHERE status = 'HANDOVER_PENDING';

UPDATE exchanges e SET return_prepared_at = p.first_at
    FROM (SELECT exchange_id, uploaded_by, MIN(created_at) AS first_at
      FROM exchange_photos WHERE stage = 'RETURN'
      GROUP BY exchange_id, uploaded_by) p
WHERE p.exchange_id = e.id AND p.uploaded_by = e.reader_id AND e.return_prepared_at IS NULL;

UPDATE exchanges e SET returned_at = s.shipped_at
    FROM shipment_info s
WHERE s.exchange_id = e.id AND s.direction = 'TO_OWNER' AND s.shipped_at IS NOT NULL
  AND e.delivery_method = 'MAIL' AND e.returned_at IS NULL;
UPDATE exchanges SET returned_at = return_prepared_at
WHERE delivery_method <> 'MAIL' AND returned_at IS NULL AND return_prepared_at IS NOT NULL;

ALTER TABLE deadline_extension_requests ADD COLUMN IF NOT EXISTS extra_days INT;
UPDATE deadline_extension_requests d
SET extra_days = GREATEST(1, d.requested_new_deadline - COALESCE(e.deadline, d.created_at::date))
    FROM exchanges e
WHERE e.id = d.exchange_id AND d.extra_days IS NULL;
UPDATE deadline_extension_requests SET extra_days = 7 WHERE extra_days IS NULL;
ALTER TABLE deadline_extension_requests ALTER COLUMN extra_days SET NOT NULL;
ALTER TABLE deadline_extension_requests ADD CONSTRAINT chk_extension_extra_days CHECK (extra_days BETWEEN 1 AND 60);
ALTER TABLE deadline_extension_requests ALTER COLUMN requested_new_deadline DROP NOT NULL;

COMMIT;

ALTER TABLE deadline_extension_requests ADD COLUMN IF NOT EXISTS decision_comment TEXT;