CREATE TABLE contacts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    name        VARCHAR(100) NOT NULL,
    surname     VARCHAR(100) NOT NULL,
    iban        VARCHAR(34) NOT NULL,
    email       VARCHAR(255),
    note        VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_contacts_user_id ON contacts(user_id);
