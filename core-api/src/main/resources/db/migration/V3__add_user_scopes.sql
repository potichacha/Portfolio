-- V3__add_user_scopes.sql
-- BACK-5 : OAuth scopes granted by the user at GitHub login (data model §7.1).
-- Stored as a text array; empty by default until the OAuth flow (BACK-6) fills it.

ALTER TABLE users
    ADD COLUMN scopes TEXT[] NOT NULL DEFAULT '{}';
