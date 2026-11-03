CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE statements
    ADD CONSTRAINT fk_statements_owner FOREIGN KEY (owner_id) REFERENCES app_users (id);

