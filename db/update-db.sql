-- 1. Новий enum-тип у Postgres
CREATE TYPE settlement_type AS ENUM ('CITY', 'VILLAGE', 'SETTLEMENT');

-- 2. Users: замінюємо city на три нові колонки
ALTER TABLE users
    ADD COLUMN settlement_type settlement_type,
    ADD COLUMN region VARCHAR(255),
    ADD COLUMN settlement_name VARCHAR(255);

-- перенесення старих даних (best-effort - стара колонка мала просто текст,
-- тип і область невідомі, тому лишаємо їх NULL, переносимо тільки назву)
UPDATE users SET settlement_name = city WHERE city IS NOT NULL;

ALTER TABLE users DROP COLUMN city;

-- 3. Listings: нові опційні колонки (override)
ALTER TABLE listings
    ADD COLUMN settlement_type settlement_type,
    ADD COLUMN region VARCHAR(255),
    ADD COLUMN settlement_name VARCHAR(255);