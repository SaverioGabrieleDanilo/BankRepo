-- STUB: tabella completa a cura di Gabriele (Epic 2 - Apertura Conto Corrente).
-- Contiene solo la PK necessaria per le FK di "transactions".
CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE transaction_types (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE transaction_statuses (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE transactions (
    id                 BIGSERIAL PRIMARY KEY,
    type_id            INTEGER NOT NULL REFERENCES transaction_types(id),
    status_id          INTEGER NOT NULL REFERENCES transaction_statuses(id),
    payer_account_id   BIGINT REFERENCES bank_accounts(id),
    payee_account_id   BIGINT REFERENCES bank_accounts(id),
    payer_user_id      BIGINT REFERENCES users(id),
    payee_user_id      BIGINT REFERENCES users(id),
    amount             DECIMAL(19,4) NOT NULL,
    value_date         DATE NOT NULL,
    transaction_date   TIMESTAMP NOT NULL
);

CREATE INDEX idx_transactions_payer_account ON transactions (payer_account_id);
CREATE INDEX idx_transactions_payee_account ON transactions (payee_account_id);
CREATE INDEX idx_transactions_value_date ON transactions (value_date);
