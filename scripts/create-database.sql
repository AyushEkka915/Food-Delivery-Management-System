-- ============================================================================
-- Smart Food Delivery Management System
-- scripts/create-database.sql
--
-- Purpose:
--   One-time MySQL 8 bootstrap: create the application database and a
--   least-privilege local application user.
--
-- How to run (choose one):
--   mysql -u root -p < scripts/create-database.sql
--   - or paste this file into MySQL Workbench and execute it as an
--     administrator account (for example, root).
--
-- WARNING:
--   Change 'CHANGE_ME_strong_password' below BEFORE using this script.
--   Use a long, random password. Re-running this script is safe: the
--   database/user use IF NOT EXISTS and ALTER USER reapplies the password.
--
-- The exact same password must be placed in .env (which is gitignored),
-- never committed, and never pasted into documentation or source control.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS food_delivery
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'foodapp'@'localhost'
    IDENTIFIED BY 'CHANGE_ME_strong_password';

ALTER USER 'foodapp'@'localhost'
    IDENTIFIED BY 'CHANGE_ME_strong_password';

-- Grant only what the application needs. CREATE, ALTER, INDEX, REFERENCES,
-- and DROP are required for Flyway schema migrations (creating and changing
-- tables, indexes, and foreign keys). In a production environment, migrate
-- with a dedicated migration account and revoke DROP/CREATE/ALTER from the
-- runtime application account after migrations complete.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP
    ON food_delivery.*
    TO 'foodapp'@'localhost';

FLUSH PRIVILEGES;

-- Verification: inspect the privileges that were actually granted.
SHOW GRANTS FOR 'foodapp'@'localhost';

-- Remember: put the same password in .env (gitignored), never in git.
