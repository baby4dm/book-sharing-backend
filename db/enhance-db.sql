-- ==========================================================
-- USERS
-- ==========================================================
INSERT INTO users (id, google_id, email, name, avatar_url, city, bio, rating_avg, books_taken, books_returned_on_time, books_overdue, books_damaged, books_given, role, status, created_at) VALUES
                                                                                                                                                                                                ('11111111-1111-1111-1111-111111111111', 'g-oleh',    'oleh.k@example.com',    'Олег Коваленко',   'https://i.pravatar.cc/150?u=oleh',    'Київ',    'Люблю фантастику і детективи.', 4.80, 5, 5, 0, 0, 8, 'USER', 'ACTIVE', now() - interval '180 days'),
                                                                                                                                                                                                ('22222222-2222-2222-2222-222222222222', 'g-maria',   'maria.s@example.com',   'Марія Шевченко',   'https://i.pravatar.cc/150?u=maria',   'Львів',   'Класика і поезія.',             4.50, 3, 2, 1, 0, 4, 'USER', 'ACTIVE', now() - interval '150 days'),
                                                                                                                                                                                                ('33333333-3333-3333-3333-333333333333', 'g-ivan',    'ivan.p@example.com',    'Іван Петренко',    'https://i.pravatar.cc/150?u=ivan',    'Одеса',   'Нон-фікшн та саморозвиток.',    3.90, 6, 4, 2, 1, 2, 'USER', 'ACTIVE', now() - interval '120 days'),
                                                                                                                                                                                                ('44444444-4444-4444-4444-444444444444', 'g-anna',    'anna.d@example.com',    'Анна Дмитренко',   'https://i.pravatar.cc/150?u=anna',    'Харків',  'Читаю все підряд.',             5.00, 2, 2, 0, 0, 6, 'USER', 'ACTIVE', now() - interval '90 days'),
                                                                                                                                                                                                ('55555555-5555-5555-5555-555555555555', 'g-taras',   'taras.m@example.com',   'Тарас Мельник',    'https://i.pravatar.cc/150?u=taras',   'Дніпро',  'Іноді забуваю повертати книги)', 3.20, 4, 1, 3, 1, 1, 'USER', 'RESTRICTED', now() - interval '200 days'),
                                                                                                                                                                                                ('66666666-6666-6666-6666-666666666666', NULL,        'moderator@bookshare.ua','Модератор Сервісу','https://i.pravatar.cc/150?u=moderator', 'Київ',  NULL, 0, 0, 0, 0, 0, 0, 'MODERATOR', 'ACTIVE', now() - interval '365 days');

UPDATE users SET restricted_until = now() + interval '14 days' WHERE id = '55555555-5555-5555-5555-555555555555';

-- ==========================================================
-- BOOK CATALOG ENTRIES
-- ==========================================================
INSERT INTO book_catalog_entries (id, isbn, title, author, description, genre, cover_url, source, created_at) VALUES
                                                                                                                  ('a1111111-0000-0000-0000-000000000001', '9789661376165', 'Кобзар',                       'Тарас Шевченко',      'Збірка поетичних творів.',                 'Поезія',    'https://upload.wikimedia.org/wikipedia/uk/3/3e/%D0%9E%D0%BA%D0%BB%D0%B0%D0%B4%D0%B8%D0%BD%D0%BA%D0%B0_%D0%9A%D0%BE%D0%B1%D0%B7%D0%B0%D1%80%D1%8F_%D0%B2%D0%B8%D0%B4%D0%B0%D0%BD%D0%BE%D0%B3%D0%BE_2002_%D1%80%D0%BE%D0%BA%D1%83.jpeg?utm_source=uk.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail_unscaled', 'GOOGLE_BOOKS', now() - interval '170 days'),
                                                                                                                  ('a1111111-0000-0000-0000-000000000002', '9786171253689', 'Затьмарення',                  'Філіп К. Дік',        'Науково-фантастичний роман.',              'Фантастика','https://upload.wikimedia.org/wikipedia/uk/8/8e/%D0%97%D0%B0%D1%82%D1%8C%D0%BC%D0%B0%D1%80%D0%B5%D0%BD%D0%BD%D1%8F_%28%D0%9A%D0%BE%D0%BC%D1%83%D0%B1%D1%83%D0%BA%2C_2016%29.png?utm_source=uk.wikipedia.org&utm_campaign=index&utm_content=original', 'GOOGLE_BOOKS', now() - interval '160 days'),
                                                                                                                  ('a1111111-0000-0000-0000-000000000003', '9786176791312', 'Хроніки Амбера',               'Роджер Желязни',      'Фентезійна сага.',                         'Фентезі',   'https://book24.ua/upload/iblock/be2/be20f760cc95abbe6447a4ace5141485.jpg', 'GOOGLE_BOOKS', now() - interval '140 days'),
                                                                                                                  ('a1111111-0000-0000-0000-000000000004', '9786177544996', 'Атомні звички',                'Джеймс Клір',         'Книга про формування корисних звичок.',    'Нон-фікшн', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSqOekSL0auEGtXu3c8evTsO-Gz5bzIqvT8dbfBMeMEl0m_Zd0gepzRjKY&s=10', 'GOOGLE_BOOKS', now() - interval '100 days'),
                                                                                                                  ('a1111111-0000-0000-0000-000000000005', '9786178012345', 'Місто',                        'Валер''ян Підмогильний','Класика української прози.',             'Класика',   'https://vivat.com.ua/storage/1.d/files/f/c/fc61305b_9786178693015.jpg', 'MANUAL', now() - interval '80 days');

-- ==========================================================
-- LISTINGS
-- ==========================================================
INSERT INTO listings (id, owner_id, book_catalog_entry_id, condition_description, delivery_methods, status, created_at) VALUES
                                                                                                                            ('b2222222-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'a1111111-0000-0000-0000-000000000002', 'Легкі потертості на обкладинці, всередині чисто.', ARRAY['PICKUP','MAIL']::text[], 'IN_EXCHANGE', now() - interval '30 days'),
                                                                                                                            ('b2222222-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'a1111111-0000-0000-0000-000000000001', 'Стан майже новий.',                                ARRAY['PICKUP']::text[],         'AVAILABLE',   now() - interval '20 days'),
                                                                                                                            ('b2222222-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', 'a1111111-0000-0000-0000-000000000004', 'Читана один раз, без поміток.',                    ARRAY['PICKUP','MAIL']::text[], 'AVAILABLE',   now() - interval '10 days'),
                                                                                                                            ('b2222222-0000-0000-0000-000000000004', '33333333-3333-3333-3333-333333333333', 'a1111111-0000-0000-0000-000000000003', 'Є підкреслення олівцем на кількох сторінках.',     ARRAY['MAIL']::text[],          'RESERVED',    now() - interval '5 days');

INSERT INTO listing_photos (listing_id, url) VALUES
                                                 ('b2222222-0000-0000-0000-000000000001', 'https://photos.example.com/listing1_1.jpg'),
                                                 ('b2222222-0000-0000-0000-000000000001', 'https://photos.example.com/listing1_2.jpg'),
                                                 ('b2222222-0000-0000-0000-000000000002', 'https://photos.example.com/listing2_1.jpg'),
                                                 ('b2222222-0000-0000-0000-000000000003', 'https://photos.example.com/listing3_1.jpg'),
                                                 ('b2222222-0000-0000-0000-000000000004', 'https://photos.example.com/listing4_1.jpg');

-- ==========================================================
-- WISHLIST
-- ==========================================================
INSERT INTO wishlist_items (user_id, book_catalog_entry_id, query_title) VALUES
                                                                             ('33333333-3333-3333-3333-333333333333', 'a1111111-0000-0000-0000-000000000001', NULL),
                                                                             ('55555555-5555-5555-5555-555555555555', NULL, 'Дюна');

-- ==========================================================
-- REQUESTS (черга на listing 4 -- три заявки, одна активна)
-- ==========================================================
INSERT INTO requests (id, listing_id, requester_id, desired_deadline, preferred_delivery_method, message, status, created_at, decided_at) VALUES
                                                                                                                                              ('c3333333-0000-0000-0000-000000000001', 'b2222222-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', current_date + interval '14 days', 'MAIL', 'Дуже хочу прочитати, поверну вчасно!', 'ACTIVE',    now() - interval '25 days', now() - interval '24 days'),
                                                                                                                                              ('c3333333-0000-0000-0000-000000000002', 'b2222222-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', current_date + interval '10 days', 'MAIL', NULL, 'ACTIVE',    now() - interval '5 days', now() - interval '4 days'),
                                                                                                                                              ('c3333333-0000-0000-0000-000000000003', 'b2222222-0000-0000-0000-000000000004', '44444444-4444-4444-4444-444444444444', current_date + interval '20 days', 'MAIL', 'Можна довше почитати?', 'PENDING', now() - interval '3 days', NULL),
                                                                                                                                              ('c3333333-0000-0000-0000-000000000004', 'b2222222-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555555', current_date + interval '7 days',  'MAIL', NULL, 'REJECTED', now() - interval '4 days', now() - interval '4 days');

UPDATE requests SET reject_comment = 'На жаль, дедлайн занадто короткий і є прострочення в історії.' WHERE id = 'c3333333-0000-0000-0000-000000000004';

-- ==========================================================
-- EXCHANGES
-- ==========================================================
INSERT INTO exchanges (id, request_id, listing_id, owner_id, reader_id, delivery_method, deadline, status, created_at) VALUES
                                                                                                                           ('d4444444-0000-0000-0000-000000000001', 'c3333333-0000-0000-0000-000000000001', 'b2222222-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'MAIL', current_date + interval '14 days', 'IN_READING', now() - interval '24 days'),
                                                                                                                           ('d4444444-0000-0000-0000-000000000002', 'c3333333-0000-0000-0000-000000000002', 'b2222222-0000-0000-0000-000000000004', '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'MAIL', current_date + interval '10 days', 'HANDOVER_PENDING', now() - interval '4 days');

INSERT INTO exchange_photos (exchange_id, uploaded_by, stage, url, note) VALUES
                                                                             ('d4444444-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'HANDOVER', 'https://photos.example.com/exch1_owner_1.jpg', 'Стан задокументовано перед відправкою.'),
                                                                             ('d4444444-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'HANDOVER', 'https://photos.example.com/exch1_reader_1.jpg', 'Отримано, недоліків не виявлено.');

INSERT INTO shipment_info (exchange_id, direction, recipient_name, recipient_phone, carrier, city, branch_number, waybill_photo_url, status, shipped_at, delivered_at) VALUES
    ('d4444444-0000-0000-0000-000000000001', 'TO_READER', 'Марія Шевченко', '+380671234567', 'NOVA_POSHTA', 'Львів', '24', 'https://photos.example.com/waybill1.jpg', 'DELIVERED', now() - interval '23 days', now() - interval '21 days');

-- ==========================================================
-- ЗАВЕРШЕНИЙ ОБМІН З ВІДГУКАМИ (для прикладу історії/рейтингу)
-- ==========================================================
INSERT INTO requests (id, listing_id, requester_id, desired_deadline, preferred_delivery_method, status, created_at, decided_at) VALUES
    ('c3333333-0000-0000-0000-000000000005', 'b2222222-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', current_date - interval '10 days', 'PICKUP', 'ACTIVE', now() - interval '60 days', now() - interval '59 days');

INSERT INTO exchanges (id, request_id, listing_id, owner_id, reader_id, delivery_method, deadline, status, created_at, completed_at) VALUES
    ('d4444444-0000-0000-0000-000000000003', 'c3333333-0000-0000-0000-000000000005', 'b2222222-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'PICKUP', current_date - interval '10 days', 'COMPLETED', now() - interval '59 days', now() - interval '12 days');

INSERT INTO reviews (exchange_id, author_id, target_id, rating, comment, created_at) VALUES
                                                                                         ('d4444444-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 5, 'Чудовий читач, повернула книгу в ідеальному стані і навіть раніше терміну!', now() - interval '12 days'),
                                                                                         ('d4444444-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 5, 'Дуже приємне спілкування, книга була саме такою, як описано.', now() - interval '12 days');

-- ==========================================================
-- DISPUTE (приклад спору по простроченому обміну Тараса)
-- ==========================================================
INSERT INTO requests (id, listing_id, requester_id, desired_deadline, preferred_delivery_method, status, created_at, decided_at) VALUES
    ('c3333333-0000-0000-0000-000000000006', 'b2222222-0000-0000-0000-000000000003', '55555555-5555-5555-5555-555555555555', current_date - interval '40 days', 'PICKUP', 'ACTIVE', now() - interval '70 days', now() - interval '69 days');

INSERT INTO exchanges (id, request_id, listing_id, owner_id, reader_id, delivery_method, deadline, status, created_at) VALUES
    ('d4444444-0000-0000-0000-000000000004', 'c3333333-0000-0000-0000-000000000006', 'b2222222-0000-0000-0000-000000000003', '44444444-4444-4444-4444-444444444444', '55555555-5555-5555-5555-555555555555', 'PICKUP', current_date - interval '40 days', 'DISPUTED', now() - interval '69 days');

INSERT INTO disputes (id, exchange_id, filed_by, description, status, moderator_id, resolution_comment, created_at, resolved_at) VALUES
    ('e5555555-0000-0000-0000-000000000001', 'd4444444-0000-0000-0000-000000000004', '44444444-4444-4444-4444-444444444444', 'Книгу повернули з пошкодженою обкладинкою та підкресленнями, яких не було задекларовано.', 'RESOLVED_FAVOR_FILER', '66666666-6666-6666-6666-666666666666', 'Підтверджено пошкодження за фото. Читачу зараховано порушення.', now() - interval '35 days', now() - interval '33 days');

INSERT INTO restrictions (user_id, type, reason, dispute_id, starts_at, ends_at) VALUES
    ('55555555-5555-5555-5555-555555555555', 'TEMPORARY', 'Прострочення повернення книги понад 30 днів та підтверджене пошкодження.', 'e5555555-0000-0000-0000-000000000001', now() - interval '33 days', now() + interval '14 days');

-- ==========================================================
-- CHAT
-- ==========================================================
INSERT INTO chat_rooms (id, exchange_id, user_a_id, user_b_id, created_at) VALUES
    ('f6666666-0000-0000-0000-000000000001', 'd4444444-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', now() - interval '24 days');

INSERT INTO chat_messages (chat_room_id, sender_id, content, created_at, read_at) VALUES
                                                                                      ('f6666666-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Привіт! Відправив книгу поштою сьогодні.', now() - interval '23 days', now() - interval '23 days' + interval '2 hours'),
                                                                                      ('f6666666-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'Дякую, чекаю на трек-номер!', now() - interval '23 days' + interval '1 hour', now() - interval '23 days' + interval '3 hours'),
                                                                                      ('f6666666-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Ось накладна, вже прикріпив у систему.', now() - interval '23 days' + interval '2 hours', NULL);

-- ==========================================================
-- FEED: POSTS & COMMENTS
-- ==========================================================
INSERT INTO posts (id, author_id, content, photo_url, created_at) VALUES
                                                                      ('11223344-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Щойно дочитав "Затьмарення" Філіпа Діка — важка, але дуже сильна книга. Рекомендую!', NULL, now() - interval '15 days'),
                                                                      ('11223344-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'Зібрала невелику поличку книг для обміну цього місяця, дивіться мої оголошення.', 'https://photos.example.com/post2.jpg', now() - interval '8 days');

INSERT INTO comments (post_id, author_id, content, created_at) VALUES
                                                                   ('11223344-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'Теж хочу прочитати, побачила у вас в оголошеннях!', now() - interval '14 days'),
                                                                   ('11223344-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333', 'Клас, а "Атомні звички" ще доступні?', now() - interval '7 days');

-- ==========================================================
-- NOTIFICATIONS
-- ==========================================================
INSERT INTO notifications (user_id, type, reference_id, message, is_read, created_at) VALUES
                                                                                          ('11111111-1111-1111-1111-111111111111', 'NEW_REQUEST', 'c3333333-0000-0000-0000-000000000001', 'Нова заявка на вашу книгу "Затьмарення".', true, now() - interval '25 days'),
                                                                                          ('33333333-3333-3333-3333-333333333333', 'NEW_REQUEST', 'c3333333-0000-0000-0000-000000000003', 'Нова заявка на вашу книгу "Хроніки Амбера".', false, now() - interval '3 days'),
                                                                                          ('55555555-5555-5555-5555-555555555555', 'DISPUTE_RESOLVED', 'e5555555-0000-0000-0000-000000000001', 'Скаргу щодо вас розглянуто модератором. Акаунт тимчасово обмежено.', false, now() - interval '33 days'),
                                                                                          ('44444444-4444-4444-4444-444444444444', 'REVIEW_RECEIVED', 'd4444444-0000-0000-0000-000000000003', 'Ви отримали новий відгук від Марії.', false, now() - interval '12 days');