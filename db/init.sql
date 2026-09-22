-- QuickBite Delivery — one-time database setup.
-- Safe to run more than once: everything here skips creation if it
-- already exists, instead of failing with "already exists" errors.
-- Run as the postgres superuser: psql -U postgres -f db/init.sql

DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'quickbite') THEN
      CREATE ROLE quickbite LOGIN PASSWORD 'quickbite';
      RAISE NOTICE 'Created role: quickbite';
   ELSE
      RAISE NOTICE 'Role quickbite already exists, skipping';
   END IF;
END
$$;

SELECT 'CREATE DATABASE authdb OWNER quickbite'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec

SELECT 'CREATE DATABASE restaurantdb OWNER quickbite'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'restaurantdb')\gexec

SELECT 'CREATE DATABASE orderdb OWNER quickbite'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec

SELECT 'CREATE DATABASE paymentdb OWNER quickbite'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec

GRANT ALL PRIVILEGES ON DATABASE authdb TO quickbite;
GRANT ALL PRIVILEGES ON DATABASE restaurantdb TO quickbite;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO quickbite;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO quickbite;
