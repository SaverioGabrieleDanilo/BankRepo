-- Il login via password grant (ROPC, AuthController) e' stato rimosso: il
-- frontend usa Authorization Code + PKCE via keycloak-js, che indirizza
-- l'utente sulla pagina di login di Keycloak. Il lockout applicativo su
-- questi due campi non ha piu' nessun punto di ingresso che lo aggiorni;
-- la brute-force protection e' ora configurata lato Keycloak realm
-- (vedi KeycloakRealmInitializer).
ALTER TABLE users DROP COLUMN IF EXISTS failed_login_attempts;
ALTER TABLE users DROP COLUMN IF EXISTS locked_until;
