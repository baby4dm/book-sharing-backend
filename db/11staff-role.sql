BEGIN;

CREATE OR REPLACE FUNCTION check_staff_role() RETURNS trigger AS $$
DECLARE
    listings_count  BIGINT;
    requests_count  BIGINT;
    exchanges_count BIGINT;
BEGIN
    IF NEW.role = 'USER' OR OLD.role <> 'USER' THEN
        RETURN NEW;
    END IF;

    SELECT count(*) INTO listings_count FROM listings WHERE owner_id = NEW.id;
    SELECT count(*) INTO requests_count FROM requests WHERE requester_id = NEW.id;
    SELECT count(*) INTO exchanges_count FROM exchanges WHERE owner_id = NEW.id OR reader_id = NEW.id;

    IF listings_count + requests_count + exchanges_count > 0 THEN
        RAISE EXCEPTION 'Роль % можна видати лише обліковому запису без книг, заявок і обмінів (оголошень: %, заявок: %, обмінів: %)',
            NEW.role, listings_count, requests_count, exchanges_count;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_staff_role ON users;
CREATE TRIGGER trg_check_staff_role
    BEFORE UPDATE OF role ON users
    FOR EACH ROW
    EXECUTE FUNCTION check_staff_role();

COMMIT;

SELECT u.email, u.role,
       (SELECT count(*) FROM listings l WHERE l.owner_id = u.id)                      AS listings,
       (SELECT count(*) FROM requests r WHERE r.requester_id = u.id)                  AS requests,
       (SELECT count(*) FROM exchanges e WHERE e.owner_id = u.id OR e.reader_id = u.id) AS exchanges
FROM users u
WHERE u.role <> 'USER';
