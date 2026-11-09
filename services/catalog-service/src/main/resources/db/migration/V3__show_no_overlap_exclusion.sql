-- Two active shows on one screen must not overlap in time. The service checks this first, but two concurrent
-- requests can both pass that check; this constraint is the guard. Ranges are [starts_at, ends_at), so a show
-- that starts exactly when the previous one ends is allowed. Cancelled shows are ignored, as in the service check.
-- btree_gist lets the constraint combine equality on screen_id with range overlap.
-- This fails if the table already holds overlapping active shows on a screen; fix or cancel those first.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE show
    ADD CONSTRAINT show_screen_no_overlap
    EXCLUDE USING gist (screen_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
    WHERE (status <> 'CANCELLED');
