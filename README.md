
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

## Tema di login custom

`docker-compose.yml` monta `./keycloak/themes` dentro il container Keycloak
(`/opt/keycloak/themes`). Il tema `banca-theme` (in `keycloak/themes/banca-theme/`)
allinea la pagina di login al frontend Angular (repo `BankRepo-front`,
`src/app/features/auth/login/`):

- `theme.properties`: eredita `parent=keycloak` (form, JS, i18n) e rimappa i nomi
  delle classi CSS standard di Keycloak (`kcInputClass`, `kcButtonClass`, ecc.)
  sui nomi usati in `login-form.component.scss`. Il form vero (id/name `username`,
  `password`, `rememberMe`, `kc-login`) e la sua logica non sono toccati.
- `template.ftl`: basato sul template di default di Keycloak 24 (estratto da
  `org.keycloak.keycloak-themes-24.0.1.jar` per non reimplementarlo a memoria),
  con in più solo gli elementi che Keycloak non ha (banner con brand/back-button,
  trust-indicator) attorno alla logica originale, invariata.
- `messages/messages_en.properties`: override di due chiavi standard
  (`loginAccountTitle`, `doLogIn`) + testi custom del banner/trust-indicator.
  Aggiungere `messages_it.properties` con le stesse chiavi per l'italiano.
- `resources/css/banca-theme.css`: colori/font/spaziature presi da
  `src/styles/_tokens.scss` del frontend (valori reali, non placeholder). Le
  icone nei campi (persona/lucchetto) usano il font "Material Symbols Outlined"
  via Google Fonts — stessa tecnica a legatura testuale del frontend Angular.

**Omesso deliberatamente**: il "promo banner" con l'immagine della carta nel
design originale (hotlink a un URL Google esterno) — contenuto marketing, non
essenziale su una pagina di autenticazione. Va aggiunto con un asset self-hosted
in `login/resources/img/` se il team lo vuole comunque.

Per attivarlo su un realm: Admin Console → realm → **Realm Settings → Themes →
Login theme** → `banca-theme`. In dev mode (`start-dev`, già impostato) Keycloak
non mette in cache temi/messaggi: le modifiche sono visibili ricaricando la
pagina, senza riavviare il container.

In dev mode (`start-dev`, gia' impostato) Keycloak non mette in cache i temi:
le modifiche ai file CSS sono visibili ricaricando la pagina, senza riavviare
il container.

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
