BEGIN;

INSERT INTO listing_photos (listing_id, url)
SELECT
    l.id,
    (ARRAY[
        'https://picsum.photos/seed/bookshare1/600/800',
        'https://picsum.photos/seed/bookshare2/600/800',
        'https://picsum.photos/seed/bookshare3/600/800',
        'https://picsum.photos/seed/bookshare4/600/800'
        ])[g.n]
FROM listings l
         CROSS JOIN LATERAL generate_series(
        (SELECT COUNT(*) FROM listing_photos p WHERE p.listing_id = l.id)::int + 1,
        4
                            ) AS g(n)
WHERE (SELECT COUNT(*) FROM listing_photos p WHERE p.listing_id = l.id) < 4;

COMMIT;