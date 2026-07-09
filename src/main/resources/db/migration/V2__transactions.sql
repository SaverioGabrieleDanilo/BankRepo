CREATE TABLE account_statuses (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE bank_accounts (
    id                BIGSERIAL PRIMARY KEY,
    iban              VARCHAR(34) NOT NULL UNIQUE,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    balance           DECIMAL(19,4) NOT NULL,
    contable_balance  DECIMAL(19,4) NOT NULL,
    status_id         INTEGER NOT NULL REFERENCES account_statuses(id),
    opening_date      TIMESTAMP NOT NULL,
    version           INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMP
);

CREATE TABLE account_limits (
    id                        BIGSERIAL PRIMARY KEY,
    account_id                BIGINT NOT NULL UNIQUE REFERENCES bank_accounts(id),
    user_id                   BIGINT NOT NULL REFERENCES users(id),
    daily_withdrawal_limit    DECIMAL(19,4) NOT NULL,
    single_transaction_limit  DECIMAL(19,4) NOT NULL,
    monthly_transfer_limit    DECIMAL(19,4) NOT NULL,
    created_at                TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transaction_type (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE transaction_status (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE transactions (
    id                BIGSERIAL PRIMARY KEY,
    type_id           INTEGER NOT NULL REFERENCES transaction_type(id),
    status            INTEGER NOT NULL REFERENCES transaction_status(id),
    payer_account     BIGINT NOT NULL REFERENCES bank_accounts(id),
    payee_account     BIGINT NOT NULL REFERENCES bank_accounts(id),
    payer_user        BIGINT NOT NULL REFERENCES users(id),
    payee_user        BIGINT NOT NULL REFERENCES users(id),
    amount            DECIMAL(19,4) NOT NULL,
    description       VARCHAR(255),
    value_date        TIMESTAMP NOT NULL,
    transaction_date  TIMESTAMP NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_payer_account ON transactions (payer_account);
CREATE INDEX idx_transactions_payee_account ON transactions (payee_account);
CREATE INDEX idx_transactions_value_date ON transactions (value_date);

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

INSERT INTO account_statuses (name) VALUES ('ATTIVO'), ('IN_ATTESA'), ('RIFIUTATO'), ('CHIUSO');

INSERT INTO transaction_type (name) VALUES ('VERSAMENTO'), ('PRELIEVO'), ('BONIFICO'), ('GIROCONTO');

INSERT INTO transaction_status (name) VALUES ('ESEGUITA');
