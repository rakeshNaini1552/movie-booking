-- A cancelled show must not block a new show at the same screen and start time.
ALTER TABLE show DROP CONSTRAINT show_screen_starts_unique;

CREATE UNIQUE INDEX show_screen_starts_active_unique
    ON show (screen_id, starts_at)
    WHERE status <> 'CANCELLED';
