-- A poster is optional: existing movies stay valid with NULL and the frontend falls back to a generated poster.
-- Holds either a path served by the frontend (/posters/dune.jpg) or an absolute https URL; the API validates the shape.
ALTER TABLE movie ADD COLUMN poster_url varchar(500);
