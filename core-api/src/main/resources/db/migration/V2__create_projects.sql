-- V2__create_projects.sql
-- Stores user projects shown on public profiles.

CREATE TABLE projects (
    id          UUID                     NOT NULL DEFAULT gen_random_uuid(),
    owner_id    UUID                     NOT NULL,
    title       VARCHAR(120)             NOT NULL,
    visibility  VARCHAR(20)              NOT NULL,
    live_url    TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_projects_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE INDEX idx_projects_public_owner_created_at
    ON projects (owner_id, created_at DESC)
    WHERE visibility = 'PUBLIC' AND live_url IS NOT NULL AND deleted_at IS NULL;
