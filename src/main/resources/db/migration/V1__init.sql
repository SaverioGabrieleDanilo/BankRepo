CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE user_statuses (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE registration_statuses (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE users (
    id                       BIGSERIAL PRIMARY KEY,
    first_name               VARCHAR(255) NOT NULL,
    last_name                VARCHAR(255) NOT NULL,
    date_of_birth             DATE NOT NULL,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    username                  VARCHAR(255) NOT NULL UNIQUE,
    validated_by_id           BIGINT REFERENCES users(id),
    role_id                   INTEGER NOT NULL REFERENCES roles(id),
    status_id                 INTEGER NOT NULL REFERENCES user_statuses(id),
    registration_status_id    INTEGER NOT NULL REFERENCES registration_statuses(id),
    failed_login_attempts     INTEGER NOT NULL DEFAULT 0,
    locked_until              TIMESTAMP,
    created_at                TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP,
    keycloak_id               VARCHAR(255) NOT NULL UNIQUE
);
