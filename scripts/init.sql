-- PostgreSQL init script
-- Runs once when the DB container is first created.
-- Schema is managed by Hibernate (ddl-auto=update in prod),
-- but we set up extensions here.

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- enables fast LIKE/ILIKE with GIN index
CREATE EXTENSION IF NOT EXISTS unaccent;  -- normalise accented characters in search

-- Optional: GIN index for fast full-text search on opportunity title
-- (run after first Hibernate migration creates the table)
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_opp_title_trgm
--     ON opportunities USING gin (title gin_trgm_ops);
