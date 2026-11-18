-- Local development sample data for seat-service: 8 rows x 10 seats for each show created by catalog-shows.sql.
-- NOT a Flyway migration. Nothing in the system creates seat rows for a catalog show yet, so this stands in for that.
-- The 12 movies, the 7 days and the id expression must stay identical to catalog-shows.sql: they produce the same show ids.
-- Rows A-C are SILVER, D-F GOLD, G-H PLATINUM. Safe to run again: existing seats are skipped.
--
-- Run from the deploy/ folder:
--   docker compose exec -T seat-postgres sh -c 'psql -v ON_ERROR_STOP=1 --single-transaction -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < seed/seat-shows.sql

INSERT INTO show_seat (show_id, seat_id, seat_row, seat_number, seat_type, status, version, updated_at)
SELECT md5('show:' || p.title || ':' || d.day || ':' || (p.idx / 6))::uuid,
       l.letter || n.num,
       l.letter,
       n.num,
       CASE WHEN l.letter IN ('A', 'B', 'C') THEN 'SILVER'
            WHEN l.letter IN ('D', 'E', 'F') THEN 'GOLD'
            ELSE 'PLATINUM' END,
       'AVAILABLE',
       0,
       now()
FROM (VALUES
  ('Inception', 0), ('Interstellar', 1), ('The Dark Knight', 2), ('Pulp Fiction', 3),
  ('The Matrix', 4), ('Gladiator', 5), ('Parasite', 6), ('Whiplash', 7),
  ('Joker', 8), ('Dune', 9), ('La La Land', 10), ('Barbie', 11)
) AS p(title, idx)
CROSS JOIN generate_series(0, 6) AS d(day)
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D'), ('E'), ('F'), ('G'), ('H')) AS l(letter)
CROSS JOIN generate_series(1, 10) AS n(num)
ON CONFLICT DO NOTHING;

SELECT count(*) AS seats_in_seat_service FROM show_seat;
