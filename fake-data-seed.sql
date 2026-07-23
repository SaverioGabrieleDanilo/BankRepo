-- =====================================================================
-- Popolazione fittizia del DB: ~10 utenti per ruolo (ADMIN, EMPLOYEE,
-- CUSTOMER), conti correnti e transazioni coerenti tra loro.
--
-- Come eseguirlo (container docker-compose gia' su, DB gestionale_banca):
--   docker exec -i banca-postgres psql -U postgres -d gestionale_banca -f - < fake-data-seed.sql
-- oppure da dentro il container:
--   docker exec -it banca-postgres psql -U postgres -d gestionale_banca
--   \i /path/fake-data-seed.sql
--
-- ATTENZIONE:
-- - Gli utenti qui hanno GIA' un account reale su Keycloak (realm
--   gestionale-banca), creato via kcadm con lo stesso username/email e
--   keycloak_id corrispondente (UUID reale, non piu' placeholder). Password
--   condivisa di sviluppo per tutti: SeedDemo#2026 (rispetta la password
--   policy del realm: 8+ caratteri, maiuscola, minuscola, cifra, speciale).
--   Login possibile da subito con qualunque username di questo file.
-- - Le tabelle di riferimento (roles, user_statuses, ecc.) di norma le
--   popola gia' l'app all'avvio (UserServiceImpl.seedDatiBase()); qui le
--   reinseriamo con ON CONFLICT DO NOTHING cosi' lo script funziona anche
--   su un DB appena creato senza aver mai avviato l'app.
-- - Rieseguire lo script duplica conti/transazioni (non hanno una unique
--   key naturale). Per ripartire pulito:
--     TRUNCATE transactions, account_limits, bank_accounts RESTART IDENTITY CASCADE;
--     DELETE FROM users WHERE username LIKE 'admin%' OR username LIKE 'employee%'
--       OR username IN ('mrossi','gverdi','lbianchi','aneri','fcolombo','dgalli','schiara','mesposito','vromano','lferri');
--
-- Riepilogo dati creati:
--   10 ADMIN (admin1..admin10), 10 EMPLOYEE (employee1..employee10) — solo
--   utenti, senza conti (sono staff, non intestatari).
--   10 CUSTOMER, di cui:
--     - 6 con conto ATTIVO e movimenti (mrossi ha 2 conti, per il giroconto)
--     - 2 con conto IN_ATTESA, saldo zero, nessun movimento (dgalli, lferri)
--       -> utili per testare la pagina Approvals
--     - 1 con conto CHIUSO, saldo zero (aneri, anche stato utente SOSPESO)
--     - 2 senza alcun conto, registrazione ancora PENDING (lbianchi, vromano)
--       -> utili per testare "Pending Verification" nella pagina Users
--   12 transazioni reali tra i 6 conti attivi, saldi finali coerenti con
--   la storia dei movimenti (vedi commento sopra ogni blocco).
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Tabelle di riferimento (idempotenti)
-- ---------------------------------------------------------------------
INSERT INTO roles (name) VALUES ('ADMIN'), ('EMPLOYEE'), ('CUSTOMER')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO user_statuses (name) VALUES ('ATTIVO'), ('SOSPESO'), ('CHIUSO')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO registration_statuses (name) VALUES ('PENDING'), ('APPROVED'), ('REJECTED')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO account_statuses (name) VALUES ('ATTIVO'), ('IN_ATTESA'), ('RIFIUTATO'), ('CHIUSO')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO transaction_type (name) VALUES ('DEPOSIT'), ('WITHDRAWAL'), ('BANK_TRANSFER'), ('INTERNAL_TRANSFER')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO transaction_status (name) VALUES ('ESEGUITA')
    ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------
-- 10 ADMIN
-- ---------------------------------------------------------------------
INSERT INTO users (first_name, last_name, date_of_birth, email, username, role_id, status_id, registration_status_id, keycloak_id, created_at)
VALUES
    ('Admin', 'Sistema', '1985-01-01', 'admin1@example.com', 'admin1',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'a69bf555-42a2-4490-9885-b44f8139112f', now() - interval '90 days'),
    ('Paolo', 'Conti', '1979-03-11', 'paolo.conti@example.com', 'admin2',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '209d6a67-a352-4c91-b06d-96b87c08a2a9', now() - interval '88 days'),
    ('Chiara', 'Marino', '1982-06-23', 'chiara.marino@example.com', 'admin3',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '9daa3ab3-18b9-4aa8-83c1-ed42c30a4a0e', now() - interval '85 days'),
    ('Roberto', 'Greco', '1975-11-02', 'roberto.greco@example.com', 'admin4',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'c729b7c4-36ba-4e72-9c79-9fc969be00af', now() - interval '80 days'),
    ('Silvia', 'Bruno', '1988-02-14', 'silvia.bruno@example.com', 'admin5',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '226cae0c-f176-486e-9e99-e83f551ea10b', now() - interval '75 days'),
    ('Fabio', 'Costa', '1983-09-05', 'fabio.costa@example.com', 'admin6',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '5e46f15e-82e8-4850-9fcd-91a18e68e685', now() - interval '70 days'),
    ('Elisa', 'Fontana', '1990-12-19', 'elisa.fontana@example.com', 'admin7',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '0ded3a66-08f6-4335-b801-a07f8dce7126', now() - interval '65 days'),
    ('Marco', 'Serra', '1978-04-28', 'marco.serra@example.com', 'admin8',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '060672d6-b2fe-47fd-a923-bb42f26fe085', now() - interval '60 days'),
    ('Laura', 'Ricci', '1986-07-16', 'laura.ricci@example.com', 'admin9',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '2be0a2fe-9f74-4e9b-ad07-2b5d02ffa89a', now() - interval '55 days'),
    ('Alessandro', 'Villa', '1981-10-08', 'alessandro.villa@example.com', 'admin10',
        (SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '113d1570-5003-4bb8-a07b-eb0137083173', now() - interval '50 days')
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------
-- 10 EMPLOYEE
-- ---------------------------------------------------------------------
INSERT INTO users (first_name, last_name, date_of_birth, email, username, role_id, status_id, registration_status_id, keycloak_id, created_at)
VALUES
    ('Elena', 'Ferrari', '1990-03-15', 'elena.ferrari@example.com', 'employee1',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'bc1e6a46-d069-4dd0-bc9a-25d0292ea748', now() - interval '80 days'),
    ('Giorgio', 'Moretti', '1984-05-21', 'giorgio.moretti@example.com', 'employee2',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'cf7bb860-92b9-45c2-89f8-e30ed63d0bdd', now() - interval '78 days'),
    ('Federica', 'Gallo', '1991-08-30', 'federica.gallo@example.com', 'employee3',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'a3d3ea31-4ec6-4ebb-89bb-0087789d968c', now() - interval '76 days'),
    ('Simone', 'Rinaldi', '1987-01-12', 'simone.rinaldi@example.com', 'employee4',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'c5778677-a638-48e6-aba2-815f05c2dfbb', now() - interval '74 days'),
    ('Martina', 'Longo', '1993-06-07', 'martina.longo@example.com', 'employee5',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '1a02ad32-a0f5-446a-9697-b560cb8af31c', now() - interval '72 days'),
    ('Andrea', 'Barbieri', '1989-09-24', 'andrea.barbieri@example.com', 'employee6',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'a5411817-b519-4571-b66b-cda93fd4d05d', now() - interval '70 days'),
    ('Cristina', 'Leone', '1992-11-16', 'cristina.leone@example.com', 'employee7',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '4bdb531f-946e-4c31-a3f5-27cf209c7f6e', now() - interval '68 days'),
    ('Nicola', 'Santoro', '1985-02-27', 'nicola.santoro@example.com', 'employee8',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '18c6585e-daf8-48c1-a910-2178629acd0a', now() - interval '66 days'),
    ('Beatrice', 'Pellegrini', '1994-04-03', 'beatrice.pellegrini@example.com', 'employee9',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '41fcf29a-5b0d-408e-a06d-c45c56e642bf', now() - interval '64 days'),
    ('Stefano', 'Ferraro', '1986-12-09', 'stefano.ferraro@example.com', 'employee10',
        (SELECT id FROM roles WHERE name = 'EMPLOYEE'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '56901854-4d4b-452f-86c1-e8df96666e71', now() - interval '62 days')
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------
-- 10 CUSTOMER (validati dal primo employee, salvo dove annotato)
-- ---------------------------------------------------------------------
INSERT INTO users (first_name, last_name, date_of_birth, email, username, validated_by_id, role_id, status_id, registration_status_id, keycloak_id, created_at)
VALUES
    ('Mario', 'Rossi', '1988-05-12', 'mario.rossi@example.com', 'mrossi',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '648745ce-8e73-431c-9c26-7b1dfce84461', now() - interval '60 days'),

    ('Giulia', 'Verdi', '1992-02-20', 'giulia.verdi@example.com', 'gverdi',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '9a92620d-b8b0-4ca0-b9f4-801ee6438d51', now() - interval '45 days'),

    -- Registrazione ancora da approvare: nessun conto associato
    ('Luca', 'Bianchi', '1995-11-03', 'luca.bianchi@example.com', 'lbianchi',
        NULL,
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'PENDING'), '6f6d8462-a204-4dae-81f9-009eaf4e3ede', now() - interval '3 days'),

    -- Utente sospeso: conto associato chiuso
    ('Anna', 'Neri', '1980-07-30', 'anna.neri@example.com', 'aneri',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'SOSPESO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '3f309f09-9897-4485-aa48-a4e72debf078', now() - interval '120 days'),

    ('Francesca', 'Colombo', '1991-09-18', 'francesca.colombo@example.com', 'fcolombo',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '12b99336-269e-4171-9c61-224fe0a75380', now() - interval '55 days'),

    -- Conto aperto ma ancora IN_ATTESA di approvazione (utile per la pagina Approvals)
    ('Davide', 'Galli', '1987-04-25', 'davide.galli@example.com', 'dgalli',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'a1f07978-9bc3-48a6-99db-4ca5ca17eaf7', now() - interval '20 days'),

    ('Sara', 'Chiari', '1993-12-02', 'sara.chiari@example.com', 'schiara',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), 'e5407c63-3ceb-4a0c-bda1-8e45ec492aa7', now() - interval '33 days'),

    ('Matteo', 'Esposito', '1985-06-14', 'matteo.esposito@example.com', 'mesposito',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '12258ad5-8758-4583-af37-319f9d297273', now() - interval '70 days'),

    -- Registrazione ancora da approvare: nessun conto associato
    ('Valentina', 'Romano', '1996-08-09', 'valentina.romano@example.com', 'vromano',
        NULL,
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'PENDING'), '0cae970e-7e23-4e72-abc3-75b01bcd13d0', now() - interval '1 days'),

    -- Conto aperto ma ancora IN_ATTESA di approvazione (secondo caso per Approvals)
    ('Lorenzo', 'Ferri', '1990-01-27', 'lorenzo.ferri@example.com', 'lferri',
        (SELECT id FROM users WHERE username = 'employee1'),
        (SELECT id FROM roles WHERE name = 'CUSTOMER'), (SELECT id FROM user_statuses WHERE name = 'ATTIVO'),
        (SELECT id FROM registration_statuses WHERE name = 'APPROVED'), '822089ec-a280-4c97-b7dc-9c1e60e46de3', now() - interval '15 days')
ON CONFLICT (username) DO NOTHING;

-- ---------------------------------------------------------------------
-- Conti correnti (9 conti per 8 dei 10 customer — mrossi ne ha 2 per il
-- giroconto; lbianchi e vromano restano senza conto, registrazione PENDING)
-- ---------------------------------------------------------------------
INSERT INTO bank_accounts (iban, user_id, balance, contable_balance, status_id, opening_date, created_at, updated_at)
VALUES
    ('IT60X0542811101000000100001', (SELECT id FROM users WHERE username = 'mrossi'),
        544.00, 544.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '60 days', now() - interval '60 days', now() - interval '1 days'),

    ('IT60X0542811101000000100002', (SELECT id FROM users WHERE username = 'mrossi'),
        500.00, 500.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '40 days', now() - interval '40 days', now() - interval '6 days'),

    ('IT60X0542811101000000100003', (SELECT id FROM users WHERE username = 'gverdi'),
        1100.00, 1100.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '45 days', now() - interval '45 days', now() - interval '1 days'),

    ('IT60X0542811101000000100004', (SELECT id FROM users WHERE username = 'aneri'),
        0.00, 0.00, (SELECT id FROM account_statuses WHERE name = 'CHIUSO'),
        now() - interval '120 days', now() - interval '120 days', now() - interval '10 days'),

    ('IT60X0542811101000000100005', (SELECT id FROM users WHERE username = 'fcolombo'),
        892.00, 892.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '55 days', now() - interval '55 days', now() - interval '8 days'),

    ('IT60X0542811101000000100006', (SELECT id FROM users WHERE username = 'dgalli'),
        0.00, 0.00, (SELECT id FROM account_statuses WHERE name = 'IN_ATTESA'),
        now() - interval '20 days', now() - interval '20 days', now() - interval '20 days'),

    ('IT60X0542811101000000100007', (SELECT id FROM users WHERE username = 'schiara'),
        950.00, 950.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '33 days', now() - interval '33 days', now() - interval '3 days'),

    ('IT60X0542811101000000100008', (SELECT id FROM users WHERE username = 'mesposito'),
        1490.00, 1490.00, (SELECT id FROM account_statuses WHERE name = 'ATTIVO'),
        now() - interval '70 days', now() - interval '70 days', now() - interval '12 days'),

    ('IT60X0542811101000000100009', (SELECT id FROM users WHERE username = 'lferri'),
        0.00, 0.00, (SELECT id FROM account_statuses WHERE name = 'IN_ATTESA'),
        now() - interval '15 days', now() - interval '15 days', now() - interval '15 days')
ON CONFLICT (iban) DO NOTHING;

-- ---------------------------------------------------------------------
-- Limiti conto (solo per i conti ATTIVI — i conti IN_ATTESA non possono
-- ancora operare, assertActive li blocca comunque)
-- ---------------------------------------------------------------------
INSERT INTO account_limits (account_id, user_id, daily_withdrawal_limit, single_transaction_limit, monthly_transfer_limit, created_at, updated_at)
VALUES
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM users WHERE username = 'mrossi'), 500.00, 1000.00, 5000.00, now() - interval '60 days', now() - interval '60 days'),
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100002'),
        (SELECT id FROM users WHERE username = 'mrossi'), 500.00, 1000.00, 5000.00, now() - interval '40 days', now() - interval '40 days'),
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM users WHERE username = 'gverdi'), 500.00, 1000.00, 5000.00, now() - interval '45 days', now() - interval '45 days'),
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM users WHERE username = 'fcolombo'), 500.00, 1500.00, 6000.00, now() - interval '55 days', now() - interval '55 days'),
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM users WHERE username = 'schiara'), 400.00, 800.00, 4000.00, now() - interval '33 days', now() - interval '33 days'),
    ((SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100008'),
        (SELECT id FROM users WHERE username = 'mesposito'), 700.00, 2000.00, 8000.00, now() - interval '70 days', now() - interval '70 days')
ON CONFLICT (account_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Transazioni (12, in ordine cronologico — saldi finali sopra ricalcolati
-- a mano per essere coerenti con questa esatta sequenza):
--   A1 (mrossi/100001):    +1000 -150 +500(bonifico in, netto) -500(giroconto out) -306(bonifico out, fee 6) => 544.00
--   A2 (mrossi/100002):    +500 (giroconto in)                                                                => 500.00
--   A3 (gverdi/100003):    +300 (bonifico in) +800                                                             => 1100.00
--   A5 (fcolombo/100005):  +1500 -200 -408 (bonifico out, fee 8)                                               => 892.00
--   A7 (schiara/100007):   +600 +400 (bonifico in) -50                                                         => 950.00
--   A8 (mesposito/100008): +2000 -510 (bonifico out, fee 10)                                                   => 1490.00
-- ---------------------------------------------------------------------
INSERT INTO transactions (type_id, status, payer_account, payee_account, payer_user, payee_user, amount, description, value_date, transaction_date, created_at)
VALUES
    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100008'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100008'),
        (SELECT id FROM users WHERE username = 'mesposito'), (SELECT id FROM users WHERE username = 'mesposito'),
        2000.00, 'Versamento stipendio', now() - interval '30 days', now() - interval '30 days', now() - interval '30 days'),

    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM users WHERE username = 'fcolombo'), (SELECT id FROM users WHERE username = 'fcolombo'),
        1500.00, 'Versamento contanti', now() - interval '25 days', now() - interval '25 days', now() - interval '25 days'),

    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM users WHERE username = 'mrossi'), (SELECT id FROM users WHERE username = 'mrossi'),
        1000.00, 'Versamento contanti', now() - interval '20 days', now() - interval '20 days', now() - interval '20 days'),

    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM users WHERE username = 'schiara'), (SELECT id FROM users WHERE username = 'schiara'),
        600.00, 'Versamento contanti', now() - interval '18 days', now() - interval '18 days', now() - interval '18 days'),

    ((SELECT id FROM transaction_type WHERE name = 'WITHDRAWAL'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM users WHERE username = 'fcolombo'), (SELECT id FROM users WHERE username = 'fcolombo'),
        200.00, 'Prelievo sportello', now() - interval '15 days', now() - interval '15 days', now() - interval '15 days'),

    ((SELECT id FROM transaction_type WHERE name = 'BANK_TRANSFER'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100008'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM users WHERE username = 'mesposito'), (SELECT id FROM users WHERE username = 'mrossi'),
        500.00, 'Rimborso spese comuni (Trattenuta: 10.00€)', now() - interval '12 days', now() - interval '12 days', now() - interval '12 days'),

    ((SELECT id FROM transaction_type WHERE name = 'WITHDRAWAL'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM users WHERE username = 'mrossi'), (SELECT id FROM users WHERE username = 'mrossi'),
        150.00, 'Prelievo sportello', now() - interval '10 days', now() - interval '10 days', now() - interval '10 days'),

    ((SELECT id FROM transaction_type WHERE name = 'BANK_TRANSFER'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM users WHERE username = 'fcolombo'), (SELECT id FROM users WHERE username = 'schiara'),
        400.00, 'Quota affitto condiviso (Trattenuta: 8.00€)', now() - interval '8 days', now() - interval '8 days', now() - interval '8 days'),

    ((SELECT id FROM transaction_type WHERE name = 'INTERNAL_TRANSFER'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100002'),
        (SELECT id FROM users WHERE username = 'mrossi'), (SELECT id FROM users WHERE username = 'mrossi'),
        500.00, 'Giroconto risparmio', now() - interval '6 days', now() - interval '6 days', now() - interval '6 days'),

    ((SELECT id FROM transaction_type WHERE name = 'BANK_TRANSFER'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM users WHERE username = 'mrossi'), (SELECT id FROM users WHERE username = 'gverdi'),
        300.00, 'Bonifico affitto (Trattenuta: 6.00€)', now() - interval '4 days', now() - interval '4 days', now() - interval '4 days'),

    ((SELECT id FROM transaction_type WHERE name = 'WITHDRAWAL'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM users WHERE username = 'schiara'), (SELECT id FROM users WHERE username = 'schiara'),
        50.00, 'Prelievo sportello', now() - interval '3 days', now() - interval '3 days', now() - interval '3 days'),

    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM users WHERE username = 'gverdi'), (SELECT id FROM users WHERE username = 'gverdi'),
        800.00, 'Versamento stipendio', now() - interval '1 days', now() - interval '1 days', now() - interval '1 days'),

    -- Transazioni recenti: popolano le card "Volume (1h)" e "Volume (24h)" della
    -- dashboard monitoring, altrimenti sempre a zero perche' nessuna riga sopra
    -- cade in quelle finestre temporali rispetto al momento in cui si guarda la UI.
    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100001'),
        (SELECT id FROM users WHERE username = 'mrossi'), (SELECT id FROM users WHERE username = 'mrossi'),
        250.00, 'Versamento contanti', now() - interval '10 minutes', now() - interval '10 minutes', now() - interval '10 minutes'),

    ((SELECT id FROM transaction_type WHERE name = 'WITHDRAWAL'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100005'),
        (SELECT id FROM users WHERE username = 'fcolombo'), (SELECT id FROM users WHERE username = 'fcolombo'),
        80.00, 'Prelievo sportello', now() - interval '45 minutes', now() - interval '45 minutes', now() - interval '45 minutes'),

    ((SELECT id FROM transaction_type WHERE name = 'DEPOSIT'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100007'),
        (SELECT id FROM users WHERE username = 'schiara'), (SELECT id FROM users WHERE username = 'schiara'),
        500.00, 'Versamento contanti', now() - interval '3 hours', now() - interval '3 hours', now() - interval '3 hours'),

    ((SELECT id FROM transaction_type WHERE name = 'WITHDRAWAL'), (SELECT id FROM transaction_status WHERE name = 'ESEGUITA'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM bank_accounts WHERE iban = 'IT60X0542811101000000100003'),
        (SELECT id FROM users WHERE username = 'gverdi'), (SELECT id FROM users WHERE username = 'gverdi'),
        120.00, 'Prelievo sportello', now() - interval '15 hours', now() - interval '15 hours', now() - interval '15 hours');

COMMIT;

-- ---------------------------------------------------------------------
-- Query rapide per controllare i dati appena inseriti
-- ---------------------------------------------------------------------
SELECT u.username, u.first_name, u.last_name, r.name AS role, us.name AS status, rs.name AS registration_status
FROM users u
JOIN roles r ON r.id = u.role_id
JOIN user_statuses us ON us.id = u.status_id
JOIN registration_statuses rs ON rs.id = u.registration_status_id
ORDER BY r.name, u.id;

SELECT b.iban, u.username AS owner, b.balance, ast.name AS status
FROM bank_accounts b
JOIN users u ON u.id = b.user_id
JOIN account_statuses ast ON ast.id = b.status_id
ORDER BY b.id;

SELECT t.id, tt.name AS type, pa.iban AS payer_iban, pe.iban AS payee_iban, t.amount, t.description, t.value_date
FROM transactions t
JOIN transaction_type tt ON tt.id = t.type_id
JOIN bank_accounts pa ON pa.id = t.payer_account
JOIN bank_accounts pe ON pe.id = t.payee_account
ORDER BY t.value_date;

-- Conteggio per ruolo, utile per verificare i "10 per ruolo" richiesti
SELECT r.name AS role, COUNT(*) AS totale
FROM users u JOIN roles r ON r.id = u.role_id
GROUP BY r.name
ORDER BY r.name;
