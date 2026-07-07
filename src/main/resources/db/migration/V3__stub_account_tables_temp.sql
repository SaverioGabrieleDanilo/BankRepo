-- TEMPORANEA / SOLO LOCALE: da rimuovere quando Gabriele consegna la migration
-- definitiva per l'Epic 2 (Apertura Conto Corrente). Allinea lo schema alle
-- entity Java gia' presenti sul branch (BankAccount, AccountLimits, AccountStatus,
-- Transaction, TransactionType, TransactionStatus) e corregge i disallineamenti
-- di nomi tabella/colonna rispetto allo stub V2 (tutte le tabelle coinvolte sono vuote).

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

CREATE TABLE account_statuses (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

ALTER TABLE bank_accounts
    ADD COLUMN user_id           BIGINT NOT NULL REFERENCES users(id),
    ADD COLUMN balance           DECIMAL(19,4) NOT NULL,
    ADD COLUMN contable_balance  DECIMAL(19,4) NOT NULL,
    ADD COLUMN status_id         INTEGER NOT NULL REFERENCES account_statuses(id),
    ADD COLUMN opening_date      TIMESTAMP NOT NULL,
    ADD COLUMN version           INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN created_at        TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN deleted_at        TIMESTAMP;

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

ALTER TABLE transaction_types RENAME TO transaction_type;
ALTER TABLE transaction_statuses RENAME TO transaction_status;

ALTER TABLE transactions RENAME COLUMN status_id TO status;
ALTER TABLE transactions RENAME COLUMN payer_account_id TO payer_account;
ALTER TABLE transactions RENAME COLUMN payee_account_id TO payee_account;
ALTER TABLE transactions RENAME COLUMN payer_user_id TO payer_user;
ALTER TABLE transactions RENAME COLUMN payee_user_id TO payee_user;
ALTER TABLE transactions ADD COLUMN description VARCHAR(255);
ALTER TABLE transactions ALTER COLUMN payer_account SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN payee_account SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN payer_user SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN payee_user SET NOT NULL;
