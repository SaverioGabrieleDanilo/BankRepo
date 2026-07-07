-- TEMPORANEA / SOLO LOCALE: seguito di V3/V4, aggiunge la colonna iban mancante
-- su bank_accounts (usata da TransactionServiceImpl per la ricerca del conto).
-- Da rimuovere quando arriva la migration definitiva di Gabriele (Epic 2).

ALTER TABLE bank_accounts ADD COLUMN iban VARCHAR(34) NOT NULL UNIQUE;
