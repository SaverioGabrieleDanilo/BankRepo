-- TEMPORANEA / SOLO LOCALE: seguito di V8, aggiunge lo stato conto CHIUSO
-- necessario per POST /api/conti/{id}/chiusura.

INSERT INTO account_statuses (name) VALUES ('CHIUSO');
