# BankRepo — Gestione Banca

App Spring Boot 3.4.1 per gestione utenti bancaria con autenticazione Keycloak.

## Stack

- Java 21, Spring Boot 4.1.0
- Spring Data JPA, Spring Security OAuth2 Resource Server
- Keycloak 24 (admin client), PostgreSQL 15
- Lombok, Maven

## Prerequisiti

- Docker & Docker Compose
- Java 21+
- Maven Wrapper (incluso)

## Avvio

```bash
# 1. Avvia PostgreSQL + Keycloak
docker-compose up -d

# 2. Crea admin Keycloak
# Apri http://localhost:9090/ → crea utente admin/admin
# Oppure (container ricreato da zero):
# docker-compose down -v && docker-compose up -d

# 3. Avvia l'app
./mvnw spring-boot:run
```

## Setup Keycloak

Dopo aver creato l'admin user:

1. Vai su http://localhost:9090/admin → login con admin/admin
2. Crea realm: `gestionale-banca`
3. Crea realm roles: `ADMIN`, `EMPLOYEE`, `CUSTOMER`
4. Crea client `banca-client` (login utenti finali) con `Client authentication` ON, `Direct access grants` enabled
5. Copia il client secret in `.env` → `KEYCLOAK_CLIENT_SECRET`
6. Crea un **secondo client** `banca-service` (service account, usato dal backend per creare/gestire utenti — NON usare più `admin-cli`/realm `master`):
   - `Client authentication` ON, `Service accounts roles` ON, tutti gli altri flow OFF
   - Tab "Service accounts roles" → assegna i client role `manage-users` e `manage-realm` del client `realm-management`
   - Copia il client secret in `.env` → `KEYCLOAK_SERVICE_CLIENT_SECRET`

## API

| Endpoint | Metodo | Auth | Descrizione |
|----------|--------|------|-------------|
| `/api/utenti/registra` | POST | No | Registrazione utente |
| `/api/auth/login` | POST | No | Login (restituisce token) |
| `/api/utenti/{id}` | GET | ADMIN | Dettaglio utente |
| `/api/utenti` | GET | ADMIN | Lista paginata |
| `/api/utenti/{id}` | PUT | ADMIN/proprietario | Modifica utente |
| `/api/utenti/{id}` | DELETE | ADMIN/EMPLOYEE | Disattiva utente |

## DB

- PostgreSQL su `localhost:5432/gestionale_banca`
- Hibernate `ddl-auto=validate` — lo schema è gestito da Flyway (`src/main/resources/db/migration`), non da `ddl-auto`
