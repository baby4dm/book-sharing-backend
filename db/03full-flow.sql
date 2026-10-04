-- ==========================================================
-- Повне наповнення: 50 оголошень (ті самі 50 книг, що вже є в
-- каталозі, кожна з власним реальним cover_url у book_catalog_entries
-- - тут НЕ чіпаємо самі книги, лише додаємо НОВІ оголошення на них),
-- з усіма станами заявок/обмінів між dimikoff008@gmail.com і
-- babych.dmtr@gmail.com, плюс трохи за участі третіх осіб.
--
-- dimikoff008@gmail.com = e345e90f-515b-48cd-9937-7df83c400fc6 (реальний,
--   увійшов через Google)
-- babych.dmtr@gmail.com = 1c35215e-613b-499f-8179-64a38dee5148 (реальний,
--   увійшов через Google)
-- ==========================================================

-- ---- Крок 0: обидва користувачі ВЖЕ реальні (увійшли через Google),
-- просто заповнюємо їм населений пункт - профіль поки порожній, а
-- без цього оголошення без явного override показували б порожню
-- локацію (COALESCE-fallback у ListingMapper підставляє саме профіль
-- власника, коли на самому Listing settlement_* не задано)
BEGIN;

UPDATE users SET settlement_type = 'CITY', region = 'Київська область', settlement_name = 'Київ'
WHERE id = 'e345e90f-515b-48cd-9937-7df83c400fc6';

UPDATE users SET settlement_type = 'CITY', region = 'Одеська область', settlement_name = 'Одеса'
WHERE id = '1c35215e-613b-499f-8179-64a38dee5148';

-- ==========================================================
-- Крок 1: 11 "сюжетних" оголошень - конкретні впізнавані книги,
-- шукаємо за назвою (не випадково), бо саме для них далі будуємо
-- заявки й обміни з конкретними станами
-- ==========================================================

-- L1 "Дюна": dimikoff -> заявка PENDING
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000001', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Стан хороший, кілька заломів на обкладинці.', ARRAY['PICKUP','MAIL']::text[], 'AVAILABLE', now() - interval '5 days' FROM book_catalog_entries WHERE title = 'Дюна' LIMIT 1;

-- L2 "1984": babych -> заявка REJECTED
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000002', '1c35215e-613b-499f-8179-64a38dee5148', id, 'Майже як нова.', ARRAY['PICKUP']::text[], 'AVAILABLE', now() - interval '7 days' FROM book_catalog_entries WHERE title = '1984' LIMIT 1;

-- L3 "Гаррі Поттер і філософський камінь": dimikoff -> заявка CANCELLED
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000003', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Читана один раз, без поміток.', ARRAY['PICKUP','MAIL']::text[], 'AVAILABLE', now() - interval '6 days' FROM book_catalog_entries WHERE title = 'Гаррі Поттер і філософський камінь' LIMIT 1;

-- L4 "Три мушкетери": dimikoff -> обмін HANDOVER_PENDING
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000004', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Готова до передачі.', ARRAY['PICKUP','MAIL']::text[], 'RESERVED', now() - interval '4 days' FROM book_catalog_entries WHERE title = 'Три мушкетери' LIMIT 1;

-- L5 "Гра престолів": babych -> обмін IN_READING
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000005', '1c35215e-613b-499f-8179-64a38dee5148', id, 'Кілька позначок олівцем.', ARRAY['PICKUP','MAIL']::text[], 'IN_EXCHANGE', now() - interval '10 days' FROM book_catalog_entries WHERE title = 'Гра престолів' LIMIT 1;

-- L6 "Сто років самотності": dimikoff -> обмін RETURN_PENDING
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000006', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Легкі потертості.', ARRAY['MAIL']::text[], 'IN_EXCHANGE', now() - interval '15 days' FROM book_catalog_entries WHERE title = 'Сто років самотності' LIMIT 1;

-- L7 "Великий Гетсбі": babych -> обмін COMPLETED
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000007', '1c35215e-613b-499f-8179-64a38dee5148', id, 'Чудовий стан.', ARRAY['PICKUP','MAIL']::text[], 'AVAILABLE', now() - interval '20 days' FROM book_catalog_entries WHERE title = 'Великий Гетсбі' LIMIT 1;

-- L8 "Воно": dimikoff -> обмін OVERDUE
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000008', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Прострочене повернення.', ARRAY['PICKUP']::text[], 'IN_EXCHANGE', now() - interval '25 days' FROM book_catalog_entries WHERE title = 'Воно' LIMIT 1;

-- L9 "Сяйво": babych -> обмін DISPUTED
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000009', '1c35215e-613b-499f-8179-64a38dee5148', id, 'Триває спір щодо стану книги.', ARRAY['MAIL']::text[], 'IN_EXCHANGE', now() - interval '18 days' FROM book_catalog_entries WHERE title = 'Сяйво' LIMIT 1;

-- L10 "Знедолені": dimikoff -> обмін IN_READING з третьою особою (Олег)
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000010', 'e345e90f-515b-48cd-9937-7df83c400fc6', id, 'Гарний стан.', ARRAY['PICKUP','MAIL']::text[], 'IN_EXCHANGE', now() - interval '9 days' FROM book_catalog_entries WHERE title = 'Знедолені' LIMIT 1;

-- L11 "Старий і море": Марія -> обмін COMPLETED з dimikoff як читачем
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT 'f1a10000-0000-0000-0000-000000000011', '22222222-2222-2222-2222-222222222222', id, 'Без пошкоджень.', ARRAY['PICKUP','MAIL']::text[], 'AVAILABLE', now() - interval '22 days' FROM book_catalog_entries WHERE title = 'Старий і море' LIMIT 1;

-- ==========================================================
-- Крок 1Б: 39 простих оголошень (решта книг каталогу) - без
-- заявок/обмінів, просто щоб каталог мав повні 50 записів для
-- тестування пагінації/фільтрів/пошуку
-- ==========================================================

INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at)
SELECT gen_random_uuid(), t.owner_id, bce.id, t.condition, t.delivery_methods, 'AVAILABLE', t.created_at
FROM (VALUES
          ('Кобзар',                                           '11111111-1111-1111-1111-111111111111'::uuid, 'Стан хороший.',                 ARRAY['PICKUP','MAIL']::text[], now() - interval '40 days'),
          ('Затьмарення',                                       '22222222-2222-2222-2222-222222222222'::uuid, 'Читана один раз.',              ARRAY['PICKUP']::text[],        now() - interval '38 days'),
          ('Хроніки Амбера',                                    '33333333-3333-3333-3333-333333333333'::uuid, 'Є позначки олівцем.',           ARRAY['MAIL']::text[],           now() - interval '36 days'),
          ('Атомні звички',                                     '44444444-4444-4444-4444-444444444444'::uuid, 'Майже нова.',                   ARRAY['PICKUP','MAIL']::text[], now() - interval '34 days'),
          ('Місто',                                             '55555555-5555-5555-5555-555555555555'::uuid, 'Гарний стан.',                  ARRAY['PICKUP']::text[],        now() - interval '32 days'),
          ('Дивний новий світ',                                 'e345e90f-515b-48cd-9937-7df83c400fc6'::uuid, 'Легкі потертості.',            ARRAY['PICKUP','MAIL']::text[], now() - interval '30 days'),
          ('Убити пересмішника',                                '1c35215e-613b-499f-8179-64a38dee5148'::uuid, 'Чудовий стан.',                 ARRAY['PICKUP']::text[],        now() - interval '28 days'),
          ('Олівер Твіст',                                      '11111111-1111-1111-1111-111111111111'::uuid, 'Без пошкоджень.',               ARRAY['MAIL']::text[],           now() - interval '27 days'),
          ('Джейн Ейр',                                         '22222222-2222-2222-2222-222222222222'::uuid, 'Стан хороший.',                 ARRAY['PICKUP','MAIL']::text[], now() - interval '26 days'),
          ('Грозовий Перевал',                                  '33333333-3333-3333-3333-333333333333'::uuid, 'Читана акуратно.',              ARRAY['PICKUP']::text[],        now() - interval '25 days'),
          ('Володар перснів: Братство персня',                  '44444444-4444-4444-4444-444444444444'::uuid, 'Є позначки.',                   ARRAY['PICKUP','MAIL']::text[], now() - interval '24 days'),
          ('Хроніки Нарнії: Лев, Біла Відьма та шафа',           '55555555-5555-5555-5555-555555555555'::uuid, 'Майже нова.',                   ARRAY['MAIL']::text[],           now() - interval '23 days'),
          ('Граф Монте-Крісто',                                 'e345e90f-515b-48cd-9937-7df83c400fc6'::uuid, 'Гарний стан.',                  ARRAY['PICKUP']::text[],        now() - interval '21 days'),
          ('Портрет Доріана Грея',                               '1c35215e-613b-499f-8179-64a38dee5148'::uuid, 'Легкі потертості.',            ARRAY['PICKUP','MAIL']::text[], now() - interval '19 days'),
          ('Мобі Дік',                                           '11111111-1111-1111-1111-111111111111'::uuid, 'Чудовий стан.',                 ARRAY['MAIL']::text[],           now() - interval '17 days'),
          ('Гордість і упередження',                             '22222222-2222-2222-2222-222222222222'::uuid, 'Без пошкоджень.',               ARRAY['PICKUP']::text[],        now() - interval '16 days'),
          ('Пригоди Шерлока Холмса',                             '33333333-3333-3333-3333-333333333333'::uuid, 'Стан хороший.',                 ARRAY['PICKUP','MAIL']::text[], now() - interval '14 days'),
          ('Вбивство у Східному експресі',                       '44444444-4444-4444-4444-444444444444'::uuid, 'Читана акуратно.',              ARRAY['PICKUP']::text[],        now() - interval '13 days'),
          ('Десять негренят',                                    '55555555-5555-5555-5555-555555555555'::uuid, 'Є позначки.',                   ARRAY['MAIL']::text[],           now() - interval '12 days'),
          ('Дон Кіхот',                                          'e345e90f-515b-48cd-9937-7df83c400fc6'::uuid, 'Майже нова.',                   ARRAY['PICKUP','MAIL']::text[], now() - interval '11 days'),
          ('Собор Паризької Богоматері',                         '1c35215e-613b-499f-8179-64a38dee5148'::uuid, 'Гарний стан.',                  ARRAY['PICKUP']::text[],        now() - interval '10 days'),
          ('Портрет митця замолоду',                             '11111111-1111-1111-1111-111111111111'::uuid, 'Легкі потертості.',            ARRAY['MAIL']::text[],           now() - interval '9 days'),
          ('Прощавай, зброє',                                    '22222222-2222-2222-2222-222222222222'::uuid, 'Чудовий стан.',                 ARRAY['PICKUP','MAIL']::text[], now() - interval '8 days'),
          ('Особливості національного винокуріння',              '33333333-3333-3333-3333-333333333333'::uuid, 'Без пошкоджень.',               ARRAY['PICKUP']::text[],        now() - interval '36 days'),
          ('Польові дослідження з українського сексу',           '44444444-4444-4444-4444-444444444444'::uuid, 'Стан хороший.',                 ARRAY['MAIL']::text[],           now() - interval '35 days'),
          ('Танго смерті',                                       '55555555-5555-5555-5555-555555555555'::uuid, 'Читана акуратно.',              ARRAY['PICKUP','MAIL']::text[], now() - interval '33 days'),
          ('Століття Якова',                                     'e345e90f-515b-48cd-9937-7df83c400fc6'::uuid, 'Є позначки.',                   ARRAY['PICKUP']::text[],        now() - interval '31 days'),
          ('451 градус за Фаренгейтом',                          '1c35215e-613b-499f-8179-64a38dee5148'::uuid, 'Майже нова.',                   ARRAY['MAIL']::text[],           now() - interval '29 days'),
          ('Марсіанські хроніки',                                '11111111-1111-1111-1111-111111111111'::uuid, 'Гарний стан.',                  ARRAY['PICKUP','MAIL']::text[], now() - interval '20 days'),
          ('Гіперіон',                                           '22222222-2222-2222-2222-222222222222'::uuid, 'Легкі потертості.',            ARRAY['PICKUP']::text[],        now() - interval '18 days'),
          ('Основа',                                             '33333333-3333-3333-3333-333333333333'::uuid, 'Чудовий стан.',                 ARRAY['MAIL']::text[],           now() - interval '15 days'),
          ('Кінець дитинства',                                   '44444444-4444-4444-4444-444444444444'::uuid, 'Без пошкоджень.',               ARRAY['PICKUP','MAIL']::text[], now() - interval '7 days'),
          ('Гра Ендера',                                         '55555555-5555-5555-5555-555555555555'::uuid, 'Стан хороший.',                 ARRAY['PICKUP']::text[],        now() - interval '6 days'),
          ('Нейромант',                                          'e345e90f-515b-48cd-9937-7df83c400fc6'::uuid, 'Читана акуратно.',              ARRAY['MAIL']::text[],           now() - interval '5 days'),
          ('Бен Гур',                                            '1c35215e-613b-499f-8179-64a38dee5148'::uuid, 'Є позначки.',                   ARRAY['PICKUP','MAIL']::text[], now() - interval '4 days'),
          ('Острів скарбів',                                     '11111111-1111-1111-1111-111111111111'::uuid, 'Майже нова.',                   ARRAY['PICKUP']::text[],        now() - interval '3 days'),
          ('Робінзон Крузо',                                     '22222222-2222-2222-2222-222222222222'::uuid, 'Гарний стан.',                  ARRAY['MAIL']::text[],           now() - interval '2 days'),
          ('Пригоди Тома Сойєра',                                '33333333-3333-3333-3333-333333333333'::uuid, 'Легкі потертості.',            ARRAY['PICKUP','MAIL']::text[], now() - interval '42 days'),
          ('Маленький принц',                                    '44444444-4444-4444-4444-444444444444'::uuid, 'Чудовий стан.',                 ARRAY['PICKUP']::text[],        now() - interval '44 days')
     ) AS t(title, owner_id, condition, delivery_methods, created_at)
         JOIN book_catalog_entries bce ON bce.title = t.title;

-- ==========================================================
-- Крок 2: 11 заявок (лише для "сюжетних" оголошень L1-L11)
-- ==========================================================

INSERT INTO requests (id, listing_id, requester_id, desired_deadline, preferred_delivery_method, message, status, reject_comment, created_at, decided_at) VALUES
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000001', 'f1a10000-0000-0000-0000-000000000001', '1c35215e-613b-499f-8179-64a38dee5148', current_date + interval '14 days', 'PICKUP', 'Давно хотів цю книгу прочитати!', 'PENDING', NULL, now() - interval '2 days', NULL),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000002', 'f1a10000-0000-0000-0000-000000000002', 'e345e90f-515b-48cd-9937-7df83c400fc6', current_date + interval '3 days', 'PICKUP', NULL, 'REJECTED', 'На жаль, дедлайн занадто короткий для мене', now() - interval '6 days', now() - interval '5 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000003', 'f1a10000-0000-0000-0000-000000000003', '1c35215e-613b-499f-8179-64a38dee5148', current_date + interval '10 days', 'MAIL', NULL, 'CANCELLED', NULL, now() - interval '5 days', now() - interval '4 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000004', 'f1a10000-0000-0000-0000-000000000004', '1c35215e-613b-499f-8179-64a38dee5148', current_date + interval '12 days', 'PICKUP', 'Поверну точно вчасно.', 'ACTIVE', NULL, now() - interval '4 days', now() - interval '3 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000005', 'f1a10000-0000-0000-0000-000000000005', 'e345e90f-515b-48cd-9937-7df83c400fc6', current_date + interval '8 days', 'MAIL', NULL, 'ACTIVE', NULL, now() - interval '10 days', now() - interval '9 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000006', 'f1a10000-0000-0000-0000-000000000006', '1c35215e-613b-499f-8179-64a38dee5148', current_date + interval '2 days', 'MAIL', NULL, 'ACTIVE', NULL, now() - interval '15 days', now() - interval '14 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000007', 'f1a10000-0000-0000-0000-000000000007', 'e345e90f-515b-48cd-9937-7df83c400fc6', current_date - interval '5 days', 'PICKUP', NULL, 'ACTIVE', NULL, now() - interval '20 days', now() - interval '19 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000008', 'f1a10000-0000-0000-0000-000000000008', '1c35215e-613b-499f-8179-64a38dee5148', current_date - interval '8 days', 'PICKUP', NULL, 'ACTIVE', NULL, now() - interval '25 days', now() - interval '24 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000009', 'f1a10000-0000-0000-0000-000000000009', 'e345e90f-515b-48cd-9937-7df83c400fc6', current_date - interval '2 days', 'MAIL', NULL, 'ACTIVE', NULL, now() - interval '18 days', now() - interval '17 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000010', 'f1a10000-0000-0000-0000-000000000010', '11111111-1111-1111-1111-111111111111', current_date + interval '6 days', 'PICKUP', NULL, 'ACTIVE', NULL, now() - interval '9 days', now() - interval '8 days'),
                                                                                                                                                              ('f2a20000-0000-0000-0000-000000000011', 'f1a10000-0000-0000-0000-000000000011', 'e345e90f-515b-48cd-9937-7df83c400fc6', current_date - interval '10 days', 'PICKUP', NULL, 'ACTIVE', NULL, now() - interval '22 days', now() - interval '21 days');

-- ==========================================================
-- Крок 3: 8 обмінів (із 8 ACTIVE заявок вище)
-- ==========================================================

INSERT INTO exchanges (id, request_id, listing_id, owner_id, reader_id, delivery_method, deadline, status, dispute_reason, created_at, completed_at) VALUES
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000001', 'f2a20000-0000-0000-0000-000000000004', 'f1a10000-0000-0000-0000-000000000004', 'e345e90f-515b-48cd-9937-7df83c400fc6', '1c35215e-613b-499f-8179-64a38dee5148', 'PICKUP', current_date + interval '12 days', 'HANDOVER_PENDING', NULL, now() - interval '3 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000002', 'f2a20000-0000-0000-0000-000000000005', 'f1a10000-0000-0000-0000-000000000005', '1c35215e-613b-499f-8179-64a38dee5148', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'MAIL', current_date + interval '8 days', 'IN_READING', NULL, now() - interval '9 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000003', 'f2a20000-0000-0000-0000-000000000006', 'f1a10000-0000-0000-0000-000000000006', 'e345e90f-515b-48cd-9937-7df83c400fc6', '1c35215e-613b-499f-8179-64a38dee5148', 'MAIL', current_date + interval '2 days', 'RETURN_PENDING', NULL, now() - interval '14 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000004', 'f2a20000-0000-0000-0000-000000000007', 'f1a10000-0000-0000-0000-000000000007', '1c35215e-613b-499f-8179-64a38dee5148', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'PICKUP', current_date - interval '5 days', 'COMPLETED', NULL, now() - interval '19 days', now() - interval '4 days'),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000005', 'f2a20000-0000-0000-0000-000000000008', 'f1a10000-0000-0000-0000-000000000008', 'e345e90f-515b-48cd-9937-7df83c400fc6', '1c35215e-613b-499f-8179-64a38dee5148', 'PICKUP', current_date - interval '8 days', 'OVERDUE', NULL, now() - interval '24 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000006', 'f2a20000-0000-0000-0000-000000000009', 'f1a10000-0000-0000-0000-000000000009', '1c35215e-613b-499f-8179-64a38dee5148', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'MAIL', current_date - interval '2 days', 'DISPUTED', 'Книга повернута з розірваними сторінками та плямами, яких не було задекларовано власником спочатку.', now() - interval '17 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000007', 'f2a20000-0000-0000-0000-000000000010', 'f1a10000-0000-0000-0000-000000000010', 'e345e90f-515b-48cd-9937-7df83c400fc6', '11111111-1111-1111-1111-111111111111', 'PICKUP', current_date + interval '6 days', 'IN_READING', NULL, now() - interval '8 days', NULL),
                                                                                                                                                         ('f3a30000-0000-0000-0000-000000000008', 'f2a20000-0000-0000-0000-000000000011', 'f1a10000-0000-0000-0000-000000000011', '22222222-2222-2222-2222-222222222222', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'PICKUP', current_date - interval '10 days', 'COMPLETED', NULL, now() - interval '21 days', now() - interval '6 days');

-- ==========================================================
-- Крок 4: фото обмінів (handover/return/dispute)
-- ==========================================================

INSERT INTO exchange_photos (id, exchange_id, uploaded_by, stage, url, note, created_at) VALUES
                                                                                             ('f4a40000-0000-0000-0000-000000000001', 'f3a30000-0000-0000-0000-000000000002', '1c35215e-613b-499f-8179-64a38dee5148', 'HANDOVER', 'https://picsum.photos/seed/exch2owner/400/600', 'Стан перед відправкою.', now() - interval '9 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000002', 'f3a30000-0000-0000-0000-000000000002', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch2reader/400/600', 'Отримано, усе гаразд.', now() - interval '8 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000003', 'f3a30000-0000-0000-0000-000000000003', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch3owner/400/600', NULL, now() - interval '14 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000004', 'f3a30000-0000-0000-0000-000000000003', '1c35215e-613b-499f-8179-64a38dee5148', 'HANDOVER', 'https://picsum.photos/seed/exch3reader/400/600', NULL, now() - interval '13 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000005', 'f3a30000-0000-0000-0000-000000000003', '1c35215e-613b-499f-8179-64a38dee5148', 'RETURN', 'https://picsum.photos/seed/exch3return/400/600', 'Повертаю, читала акуратно.', now() - interval '1 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000006', 'f3a30000-0000-0000-0000-000000000004', '1c35215e-613b-499f-8179-64a38dee5148', 'HANDOVER', 'https://picsum.photos/seed/exch4owner/400/600', NULL, now() - interval '19 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000007', 'f3a30000-0000-0000-0000-000000000004', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch4reader/400/600', NULL, now() - interval '18 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000008', 'f3a30000-0000-0000-0000-000000000004', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'RETURN', 'https://picsum.photos/seed/exch4return1/400/600', NULL, now() - interval '5 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000009', 'f3a30000-0000-0000-0000-000000000004', '1c35215e-613b-499f-8179-64a38dee5148', 'RETURN', 'https://picsum.photos/seed/exch4return2/400/600', 'Прийнято без зауважень.', now() - interval '4 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000010', 'f3a30000-0000-0000-0000-000000000005', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch5owner/400/600', NULL, now() - interval '24 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000011', 'f3a30000-0000-0000-0000-000000000005', '1c35215e-613b-499f-8179-64a38dee5148', 'HANDOVER', 'https://picsum.photos/seed/exch5reader/400/600', NULL, now() - interval '23 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000012', 'f3a30000-0000-0000-0000-000000000006', '1c35215e-613b-499f-8179-64a38dee5148', 'HANDOVER', 'https://picsum.photos/seed/exch6owner/400/600', NULL, now() - interval '17 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000013', 'f3a30000-0000-0000-0000-000000000006', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch6reader/400/600', NULL, now() - interval '16 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000014', 'f3a30000-0000-0000-0000-000000000006', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'RETURN', 'https://picsum.photos/seed/exch6return/400/600', NULL, now() - interval '3 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000015', 'f3a30000-0000-0000-0000-000000000006', '1c35215e-613b-499f-8179-64a38dee5148', 'DISPUTE', 'https://picsum.photos/seed/exch6dispute1/400/600', 'Сторінки порвані, плями на обкладинці.', now() - interval '2 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000016', 'f3a30000-0000-0000-0000-000000000006', '1c35215e-613b-499f-8179-64a38dee5148', 'DISPUTE', 'https://picsum.photos/seed/exch6dispute2/400/600', 'Крупний план пошкодження.', now() - interval '2 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000017', 'f3a30000-0000-0000-0000-000000000007', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch7owner/400/600', NULL, now() - interval '8 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000018', 'f3a30000-0000-0000-0000-000000000007', '11111111-1111-1111-1111-111111111111', 'HANDOVER', 'https://picsum.photos/seed/exch7reader/400/600', NULL, now() - interval '7 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000019', 'f3a30000-0000-0000-0000-000000000008', '22222222-2222-2222-2222-222222222222', 'HANDOVER', 'https://picsum.photos/seed/exch8owner/400/600', NULL, now() - interval '21 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000020', 'f3a30000-0000-0000-0000-000000000008', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'HANDOVER', 'https://picsum.photos/seed/exch8reader/400/600', NULL, now() - interval '20 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000021', 'f3a30000-0000-0000-0000-000000000008', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'RETURN', 'https://picsum.photos/seed/exch8return1/400/600', NULL, now() - interval '7 days'),
                                                                                             ('f4a40000-0000-0000-0000-000000000022', 'f3a30000-0000-0000-0000-000000000008', '22222222-2222-2222-2222-222222222222', 'RETURN', 'https://picsum.photos/seed/exch8return2/400/600', NULL, now() - interval '6 days');

-- ==========================================================
-- Крок 5: відгуки для COMPLETED обмінів
-- ==========================================================

INSERT INTO reviews (id, exchange_id, author_id, target_id, rating, comment, created_at) VALUES
                                                                                             ('f5a50000-0000-0000-0000-000000000001', 'f3a30000-0000-0000-0000-000000000004', 'e345e90f-515b-48cd-9937-7df83c400fc6', '1c35215e-613b-499f-8179-64a38dee5148', 5, 'Чудовий власник, книга була саме такою, як описано!', now() - interval '4 days'),
                                                                                             ('f5a50000-0000-0000-0000-000000000002', 'f3a30000-0000-0000-0000-000000000004', '1c35215e-613b-499f-8179-64a38dee5148', 'e345e90f-515b-48cd-9937-7df83c400fc6', 5, 'Повернув вчасно, у ідеальному стані. Рекомендую!', now() - interval '4 days'),
                                                                                             ('f5a50000-0000-0000-0000-000000000003', 'f3a30000-0000-0000-0000-000000000008', '22222222-2222-2222-2222-222222222222', 'e345e90f-515b-48cd-9937-7df83c400fc6', 5, 'Дуже приємний читач, швидко повернув книгу.', now() - interval '6 days');

-- ==========================================================
-- Крок 6: сповіщення, що відповідають подіям вище
-- ==========================================================

INSERT INTO notifications (id, user_id, type, reference_id, message, is_read, created_at) VALUES
                                                                                              ('f6a60000-0000-0000-0000-000000000001', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'NEW_REQUEST', 'f2a20000-0000-0000-0000-000000000001', 'Нова заявка на вашу книгу', false, now() - interval '2 days'),
                                                                                              ('f6a60000-0000-0000-0000-000000000002', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'REQUEST_REJECTED', 'f2a20000-0000-0000-0000-000000000002', 'Вашу заявку відхилено: На жаль, дедлайн занадто короткий для мене', true, now() - interval '5 days'),
                                                                                              ('f6a60000-0000-0000-0000-000000000003', 'e345e90f-515b-48cd-9937-7df83c400fc6', 'REVIEW_RECEIVED', 'f3a30000-0000-0000-0000-000000000004', 'Ви отримали новий відгук від Дмитра', false, now() - interval '4 days');

-- ==========================================================
-- Крок 7: фото примірників (listing_photos) для частини оголошень
-- ==========================================================

INSERT INTO listing_photos (id, listing_id, url, created_at) VALUES
                                                                 ('f7a70000-0000-0000-0000-000000000001', 'f1a10000-0000-0000-0000-000000000001', 'https://picsum.photos/seed/listing1a/400/600', now() - interval '5 days'),
                                                                 ('f7a70000-0000-0000-0000-000000000002', 'f1a10000-0000-0000-0000-000000000004', 'https://picsum.photos/seed/listing4a/400/600', now() - interval '4 days'),
                                                                 ('f7a70000-0000-0000-0000-000000000003', 'f1a10000-0000-0000-0000-000000000007', 'https://picsum.photos/seed/listing7a/400/600', now() - interval '20 days');


COMMIT;