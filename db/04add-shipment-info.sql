BEGIN;

-- ==========================================================
-- shipment_info для трьох наявних MAIL-обмінів - демонструє усі
-- три статуси (PENDING/SHIPPED/DELIVERED) у різних комбінаціях
-- напрямків, щоб перевірити всі стани таймлайна.
-- ==========================================================

-- Обмін f3a30000-...0002 "Гра престолів" (IN_READING):
-- уже минув етап передачі читачу -> DELIVERED для TO_READER
INSERT INTO shipment_info (id, exchange_id, direction, recipient_name, recipient_phone, carrier, city, branch_number, waybill_photo_url, status, shipped_at, delivered_at) VALUES
    ('a1b10000-0000-0000-0000-000000000001', 'f3a30000-0000-0000-0000-000000000002', 'TO_READER', 'Dmytro Babych', '+380671112233', 'NOVA_POSHTA', 'Київ', '24', 'https://picsum.photos/seed/waybill-exch2/400/300', 'DELIVERED', now() - interval '9 days', now() - interval '7 days');

-- Обмін f3a30000-...0003 "Сто років самотності" (RETURN_PENDING):
-- передача читачу вже відбулась (DELIVERED), а повернення власнику
-- щойно почалось - читач ще навіть не створив відправлення назад
-- (PENDING, waybill_photo_url ще NULL) - демонструє "активний" стан
INSERT INTO shipment_info (id, exchange_id, direction, recipient_name, recipient_phone, carrier, city, branch_number, waybill_photo_url, status, shipped_at, delivered_at) VALUES
                                                                                                                                                                               ('a1b10000-0000-0000-0000-000000000002', 'f3a30000-0000-0000-0000-000000000003', 'TO_READER', 'Dmytro Babych', '+380671112233', 'NOVA_POSHTA', 'Одеса', '15', 'https://picsum.photos/seed/waybill-exch3a/400/300', 'DELIVERED', now() - interval '13 days', now() - interval '11 days'),
                                                                                                                                                                               ('a1b10000-0000-0000-0000-000000000003', 'f3a30000-0000-0000-0000-000000000003', 'TO_OWNER', 'Dmytro Babych', '+380671112233', 'UKRPOSHTA', 'Київ', '7', NULL, 'PENDING', NULL, NULL);

-- Обмін f3a30000-...0006 "Сяйво" (DISPUTED):
-- передача давно відбулась (DELIVERED), повернення вже В ДОРОЗІ
-- (SHIPPED, накладна є, але ще не підтверджено отримання) - саме
-- в цей момент власник відкрив спір, не чекаючи підтвердження
INSERT INTO shipment_info (id, exchange_id, direction, recipient_name, recipient_phone, carrier, city, branch_number, waybill_photo_url, status, shipped_at, delivered_at) VALUES
                                                                                                                                                                               ('a1b10000-0000-0000-0000-000000000004', 'f3a30000-0000-0000-0000-000000000006', 'TO_READER', 'Dmytro Babych', '+380671112233', 'NOVA_POSHTA', 'Київ', '24', 'https://picsum.photos/seed/waybill-exch6a/400/300', 'DELIVERED', now() - interval '16 days', now() - interval '14 days'),
                                                                                                                                                                               ('a1b10000-0000-0000-0000-000000000005', 'f3a30000-0000-0000-0000-000000000006', 'TO_OWNER', 'Dmytro Babych', '+380671112233', 'NOVA_POSHTA', 'Київ', '24', 'https://picsum.photos/seed/waybill-exch6b/400/300', 'SHIPPED', now() - interval '1 days', NULL);

COMMIT;