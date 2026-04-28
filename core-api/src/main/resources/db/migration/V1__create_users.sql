-- V1__create_users.sql
-- Creates the users table for PortLoko MVP

CREATE TABLE users (
    id           UUID                        NOT NULL DEFAULT gen_random_uuid(),
    handle       VARCHAR(39)                 NOT NULL,
    email        VARCHAR(255)                NOT NULL,
    avatar_url   TEXT,
    bio          TEXT,
    github_user_id BIGINT                    NOT NULL,
    github_login VARCHAR(255)                NOT NULL,
    -- scopes TEXT[] added by V2__add_user_scopes.sql (auth module BACK-001)
    created_at   TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_handle UNIQUE (handle),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_github_user_id UNIQUE (github_user_id),
    CONSTRAINT ck_users_bio_length CHECK (char_length(bio) <= 300)
);

CREATE INDEX idx_users_handle      ON users (handle)         WHERE deleted_at IS NULL;
CREATE INDEX idx_users_github_id   ON users (github_user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at  ON users (created_at DESC);
