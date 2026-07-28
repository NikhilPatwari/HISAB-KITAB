-- One-time setup for a local PostgreSQL installation.
-- Run as a superuser (the `postgres` account created by the installer):
--
--   psql -U postgres -f backend/db/setup.sql
--
-- Safe to run more than once. Flyway creates the tables on first backend start,
-- so nothing here touches the schema.

-- The role the application connects as. Change the password here and in
-- DB_PASSWORD together; do not leave this default on anything reachable.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'hisabkitab') THEN
        CREATE ROLE hisabkitab WITH LOGIN PASSWORD 'hisabkitab';
    END IF;
END
$$;

-- CREATE DATABASE cannot run inside a transaction, so it is generated and then
-- executed by psql only when the database is missing.
SELECT 'CREATE DATABASE hisabkitab OWNER hisabkitab ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hisabkitab')\gexec
