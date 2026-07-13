-- Colonna residua di un design pre-Keycloak: la password vive solo su Keycloak,
-- nessuna entity/migration attuale la referenzia. NOT NULL senza default blocca
-- ogni nuovo INSERT su users (UserServiceImpl non la valorizza piu').
-- IF EXISTS: no-op sugli ambienti che non hanno mai avuto la colonna.
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
