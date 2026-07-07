-- TEMPORANEA / SOLO LOCALE: seguito di V3, corregge ulteriori disallineamenti
-- tra la tabella transactions (stub V2) e l'entity Transaction.java.
-- Da rimuovere insieme a V3 quando arriva la migration definitiva di Gabriele.

ALTER TABLE transactions ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE transactions ALTER COLUMN value_date TYPE TIMESTAMP USING value_date::timestamp;
