-- Local development sample data for the catalog: 3 theaters, 6 screens and shows for 12 movies over the next 7 days.
-- NOT a Flyway migration. Run catalog-movies.sql first (the shows look the movies up by title), then this file, then
-- seat-shows.sql (against the seat database) so the same shows have seats. Safe to run again: ids are derived from names,
-- and a row that already exists is skipped.
--
-- Show times are 14:00 and 19:00 UTC; the list of 12 movies and the id expression must stay identical to seat-shows.sql.
--
-- Run from the deploy/ folder:
--   docker compose exec -T catalog-postgres sh -c 'psql -v ON_ERROR_STOP=1 --single-transaction -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < seed/catalog-shows.sql

INSERT INTO theater (id, name, city, address, created_at, updated_at)
SELECT md5('theater:' || v.name)::uuid, v.name, v.city, v.address, now(), now()
FROM (VALUES
  ('Lakeside Cinemas', 'Frisco', '100 Sample Street'),
  ('Downtown Screens', 'Dallas', '200 Sample Avenue'),
  ('Riverside Multiplex', 'Plano', '300 Sample Road')
) AS v(name, city, address)
ON CONFLICT DO NOTHING;

INSERT INTO screen (id, theater_id, name, seat_rows, seats_per_row, created_at, updated_at)
SELECT md5('screen:' || t.name || ':' || s.name)::uuid, t.id, s.name, 8, 10, now(), now()
FROM theater t
CROSS JOIN (VALUES ('Screen 1'), ('Screen 2')) AS s(name)
WHERE t.name IN ('Lakeside Cinemas', 'Downtown Screens', 'Riverside Multiplex')
ON CONFLICT DO NOTHING;

WITH screens AS (
  SELECT s.id AS screen_id, (row_number() OVER (ORDER BY t.name, s.name) - 1)::int AS idx
  FROM screen s
  JOIN theater t ON t.id = s.theater_id
  WHERE t.name IN ('Lakeside Cinemas', 'Downtown Screens', 'Riverside Multiplex')
),
picks AS (
  SELECT * FROM (VALUES
    ('Inception', 0), ('Interstellar', 1), ('The Dark Knight', 2), ('Pulp Fiction', 3),
    ('The Matrix', 4), ('Gladiator', 5), ('Parasite', 6), ('Whiplash', 7),
    ('Joker', 8), ('Dune', 9), ('La La Land', 10), ('Barbie', 11)
  ) AS p(title, idx)
)
INSERT INTO show (id, movie_id, screen_id, starts_at, ends_at, base_price, status, created_at, updated_at)
SELECT md5('show:' || p.title || ':' || d.day || ':' || (p.idx / 6))::uuid,
       m.id,
       sc.screen_id,
       t.ts,
       t.ts + make_interval(mins => m.duration_minutes + 15),
       (10 + (p.idx % 3) * 2.5)::numeric(10, 2),
       'SCHEDULED',
       now(),
       now()
FROM picks p
CROSS JOIN generate_series(0, 6) AS d(day)
JOIN movie m ON m.title = p.title
JOIN screens sc ON sc.idx = p.idx % 6
CROSS JOIN LATERAL (
  SELECT ((current_date + d.day) + (CASE WHEN p.idx < 6 THEN time '14:00' ELSE time '19:00' END)) AS ts
) AS t
ON CONFLICT DO NOTHING;

SELECT count(*) AS shows_in_catalog FROM show;
