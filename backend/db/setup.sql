-- One-time setup for a local PostgreSQL installation.
-- Run as the superuser the installer created:
--
--   psql -U postgres -f backend/db/setup.sql
--
-- Safe to run more than once. Flyway creates the tables on first backend start,
-- so nothing here touches the schema.

-- CREATE DATABASE cannot run inside a transaction, so it is generated and then
-- executed by psql only when the database is missing. Ownership falls to the
-- role running this script, which is the same role the application connects as.
SELECT 'CREATE DATABASE hisabkitab ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hisabkitab')\gexec
