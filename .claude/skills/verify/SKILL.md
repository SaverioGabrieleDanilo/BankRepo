---
name: verify
description: How to build, launch and drive gestionale-banca (Spring Boot + Keycloak) for runtime verification of backend changes.
---

# Verify: gestionale-banca backend

## Prerequisiti
- `docker compose up -d` (postgres su 5432, keycloak su 9090) — di solito già attivi, controlla con `docker ps`.
- `.env` presente in root (copiato da `.env.example` e valorizzato: `DB_PASSWORD`, `KEYCLOAK_SERVICE_CLIENT_SECRET`, `BOOTSTRAP_DEFAULT_ADMIN=true` per avere subito un admin).

## Avviare il backend
**Niente spring-boot-devtools**: le modifiche al codice richiedono un riavvio completo, non c'è hot-reload.

```powershell
.\run-local.ps1   # carica .env nell'ambiente e lancia mvnw spring-boot:run
```
NON usare `mvn spring-boot:run` direttamente in una shell "vergine": senza `.env` caricato, `KEYCLOAK_SERVICE_CLIENT_SECRET`/`DB_PASSWORD` mancano e il boot fallisce (o silenziosamente usa credenziali sbagliate — vedi `GestionaleBancaApplicationTests.contextLoads`, che fallisce sempre se lanciato da una shell senza `.env`).

Porta backend: **8080** (non 8888, nonostante `test-requests.http` usi ancora quel valore). Pronto quando risponde `POST /api/utenti/registra` con 400 invece di connection-refused (~7s di boot).

Se un processo java è già in ascolto sulla 8080 ma è più vecchio delle tue modifiche (`Get-Process -Id <pid> | select StartTime`), va riavviato — killalo e rilancia `run-local.ps1`.

## Ottenere un token reale (Keycloak, non il backend)
Login applicativo rimosso: si autentica sempre direttamente contro Keycloak col client di test `banca-cli`.

```bash
curl -s -X POST http://localhost:9090/realms/gestionale-banca/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=banca-cli&client_secret=dev-only-banca-cli-secret&username=<user>&password=<pwd>" \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p'
```

Credenziali admin di bootstrap: `admin` / `AdminBanca#2026` (create automaticamente al boot se `BOOTSTRAP_DEFAULT_ADMIN=true`).

Per creare un EMPLOYEE di test (serve un token ADMIN):
```bash
curl -s -X POST http://localhost:8080/api/utenti/admin/crea \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"...", "password":"...", "firstName":"...", "lastName":"...", "email":"...", "dateOfBirth":"1990-01-01", "role":"EMPLOYEE"}'
```
Per un CUSTOMER, endpoint pubblico senza token: `POST /api/utenti/registra` (ruolo CUSTOMER forzato lato server).

## Drive: endpoint interessanti per i permessi EMPLOYEE
`GET /api/conti`, `GET /api/utenti`, `GET /api/transactions` (liste), `GET/POST /api/conti/{id}*` (dettaglio/chiusura/limiti/transazioni di un conto) sono i punti dove la logica `AuthorizationFacade.isEmployee`/`@PreAuthorize` va verificata con un vero token EMPLOYEE — non basta il test `@WebMvcTest` (mocka il service), serve un utente reale + dati reali per controllare anche i 401/403/404/409 di business.

## Note
- Utenti di test lasciati nel DB da sessioni precedenti: `testcustomer1/2`, `testemployee1` (vedi `test-requests.http`), `verify.employee1`, `verify.customer1` (creati durante la verifica del 2026-07-20). Password sempre nel formato `<Ruolo>#2026`.
- `GestionaleBancaApplicationTests.contextLoads` fallisce se lanciato senza `.env` caricato nell'ambiente — non è un bug del codice, è un limite dell'ambiente di test.
