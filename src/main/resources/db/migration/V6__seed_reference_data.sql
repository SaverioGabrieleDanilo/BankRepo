-- TEMPORANEA / SOLO LOCALE: dati di riferimento minimi necessari per apertura
-- conto e transazioni (versamento/prelievo). Da rivedere quando arriva la
-- migration definitiva di Gabriele (Epic 2).

INSERT INTO account_statuses (name) VALUES ('ATTIVO');

INSERT INTO transaction_type (name) VALUES ('VERSAMENTO'), ('PRELIEVO');

INSERT INTO transaction_status (name) VALUES ('ESEGUITA');
