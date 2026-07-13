-- Eseguito automaticamente da Postgres SOLO alla primissima inizializzazione
-- del volume dati (docker-entrypoint-initdb.d). Crea uno schema dedicato per
-- Keycloak cosi' lo schema "public" resta genuinamente vuoto finche' Flyway
-- non esegue le sue migration: altrimenti Flyway trova "public" gia' popolato
-- dalle tabelle interne di Keycloak e attiva baseline-on-migrate, saltando
-- V1__init.sql per intero (la tabella "users" non viene mai creata e V2 fallisce
-- con "relation users does not exist").
CREATE SCHEMA IF NOT EXISTS keycloak;
