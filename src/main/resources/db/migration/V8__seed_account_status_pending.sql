-- TEMPORANEA / SOLO LOCALE: seguito di V6, aggiunge gli stati conto necessari
-- al workflow di approvazione apertura conto (PATCH /api/conti/{id}/approva).

INSERT INTO account_statuses (name) VALUES ('IN_ATTESA'), ('RIFIUTATO');
