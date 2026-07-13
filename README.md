
# BankRepo — Gestione Banca

App Spring Boot per gestione utenti bancaria (conti correnti, transazioni) con
autenticazione Keycloak.

## Stack

- Java 21, Spring Boot 4.1.0, Spring Framework 7.0.8
- Spring Data JPA, Spring Security OAuth2 Resource Server
- Keycloak 24.0.1 (admin client), PostgreSQL 15
- Flyway (migrazioni schema), Lombok, Maven

## Prerequisiti

- Docker & Docker Compose
- Java 21+
- Maven Wrapper (incluso, `mvnw`/`mvnw.cmd`)
- Windows: PowerShell 5.1+ (per `run-local.ps1`)

## Avvio da zero (nuovo ambiente/nuovo collega)

```bash
# 1. Clona e crea il file .env
git clone <url-repo> gestionale-banca
cd gestionale-banca
cp .env.example .env
```

`.env.example` ha già valori di sviluppo pronti all'uso per tutto (Postgres,
Keycloak, bootstrap del primo ADMIN) — non c'è nulla da modificare per partire
in locale. Vedi i commenti nel file se vuoi capire cosa fa ciascuna variabile,
o se questo ambiente deve mai smettere di essere "solo sul mio laptop".

```bash
# 2. Avvia PostgreSQL + Keycloak
docker compose up -d
```

Al primo avvio, Keycloak importa da solo il realm `gestionale-banca` da
`keycloak/realm-import/gestionale-banca-realm.json` (flag `--import-realm` in
`docker-compose.yml`): ruoli (`ADMIN`/`EMPLOYEE`/`CUSTOMER`), password policy,
brute-force protection, e i client `banca-spa` (frontend, pubblico/PKCE),
`banca-service` (backend, service account) e `banca-cli` (solo per test manuali,
vedi sotto) — **zero click in Admin Console**. Attendi che entrambi i
container siano `healthy`/avviati (`docker compose ps`; Keycloak impiega
~15-20s dal primo avvio).

```bash
# 3. Avvia l'app — vedi sezione "Avvio dell'app" sotto
.\run-local.ps1
```

Al primo avvio (con `BOOTSTRAP_DEFAULT_ADMIN=true`, già impostato in
`.env.example`) `DefaultAdminBootstrapper` crea da solo il primo utente ADMIN
— sia su Keycloak sia sul DB applicativo, stesso codice che usa
`POST /api/utenti/registra` — con `username=admin`, `password=AdminBanca#2026`.
Da lì in poi login e `POST /api/utenti/admin/crea` funzionano normalmente per
creare altri utenti.

**Tre comandi, niente Admin Console, niente SQL a mano.** Le sezioni sotto
spiegano cosa succede dietro le quinte e come farlo a mano se serve (volume
Docker preesistente, ambiente condiviso, debug).

## Setup Keycloak — dettaglio/fallback manuale

Se preferisci non fidarti dell'import automatico (es. un volume Docker già
esistente, dove `--import-realm` salta il realm perché esiste già), questi
sono gli stessi passi fatti a mano dall'Admin Console
(http://localhost:9090/admin, `admin`/`admin`):

1. Crea realm `gestionale-banca`, roles `ADMIN`/`EMPLOYEE`/`CUSTOMER`
2. Password policy e brute-force protection: si riallineano da sole
   all'avvio dell'app (`KeycloakRealmInitializer`, idempotente) anche se il
   realm è stato creato a mano — nessuna azione richiesta qui
3. Client `banca-spa`: pubblico (`Client authentication` OFF), `Standard
   flow` ON, `Direct access grants` OFF, redirect URI `http://localhost:4200/*`
4. Client `banca-service`: confidential, `Service accounts roles` ON, tab
   "Service accounts roles" → assegna `manage-users` e `manage-realm` del
   client `realm-management` → copia il secret in `.env` →
   `KEYCLOAK_SERVICE_CLIENT_SECRET`
5. (Opzionale) Client `banca-cli` per test manuali via Postman/
   `test-requests.http`: confidential, `Direct access grants` ON — il token
   si ottiene chiamando **direttamente Keycloak**
   (`POST /realms/gestionale-banca/protocol/openid-connect/token`), mai il
   backend applicativo, che dal login ROPC non è più raggiungibile per design

## Bootstrap del primo utente ADMIN — dettaglio/fallback manuale

Se `BOOTSTRAP_DEFAULT_ADMIN` è `false` (default fuori da `.env.example`, es.
in un ambiente condiviso) va fatto a mano, **una sola volta per ambiente**,
perché `POST /api/utenti/admin/crea` richiede già un token ADMIN:

1. **Su Keycloak** (Admin Console → realm `gestionale-banca` → Users → Add
   user): crea l'utente, scheda Credentials → imposta la password (conforme
   alla password policy, `Temporary` OFF), scheda Role mapping → assegna il
   realm role `ADMIN`.
2. **Sul database applicativo**: inserisci la riga corrispondente in `users`,
   con `keycloak_id` uguale all'ID dell'utente appena creato su Keycloak
   (visibile nell'URL della sua pagina utente in Admin Console) e `role_id`
   che punta alla riga `ADMIN` di `roles`:

   ```sql
   -- id ruolo/stato: SELECT id, name FROM roles; / user_statuses; / registration_statuses;
   INSERT INTO users (first_name, last_name, date_of_birth, email, username,
                       role_id, status_id, registration_status_id,
                       created_at, keycloak_id)
   VALUES ('Super', 'Admin', '1990-01-01', 'admin@example.com', 'admin',
           1, 1, 2, now(), '<keycloak-id-copiato-sopra>');
   ```

   (`role_id=1` → ADMIN, `status_id=1` → ATTIVO, `registration_status_id=2` →
   APPROVED, assumendo il seed di default di `DatabaseInitializer`.)

L'app e Keycloak restano così sempre coerenti (nessun utente "solo Keycloak" o
"solo DB"), stesso pattern che `UserServiceImpl.creaUtente`/
`DefaultAdminBootstrapper` applicano in automatico per tutti gli altri utenti.

## Avvio dell'app

```bash
# 3. Avvia l'app
./mvnw spring-boot:run       # Linux/macOS, se le env var sono già esportate nella shell

# Windows: usa lo script dedicato, che carica .env nella sessione PowerShell
# corrente PRIMA di lanciare Maven (mvnw.cmd spring-boot:run da solo NON legge
# .env — è un file per docker-compose/tool esterni, Spring Boot non lo carica
# in automatico senza uno starter dotenv, non presente in questo progetto)
.\run-local.ps1
```

Da rilanciare ad ogni nuova finestra di terminale: le variabili d'ambiente
PowerShell non sopravvivono alla chiusura della sessione. Log atteso a fine
avvio: `Started GestionaleBancaApplication in N seconds` su `server.port=8888`.

Se il boot fallisce con `UnsatisfiedDependencyException` su `keycloakConfig`,
la causa quasi sempre è `.env` non caricato nella sessione corrente (vedi sopra).

## Configurazione CORS

`SecurityConfig` accetta chiamate cross-origin solo dagli origin elencati in
`app.cors.allowed-origins` (property `CORS_ALLOWED_ORIGINS` in `.env`), non da
un wildcard. Default: `http://localhost:4200` (dev server Angular). In un
ambiente diverso da dev/localhost, valorizza `CORS_ALLOWED_ORIGINS` con il/i
dominio/i reali del frontend (separati da virgola se più di uno) — nessun
wildcard è accettato, solo origin esatti.

## Sicurezza account: lockout dopo tentativi falliti

Il login non passa più dal backend applicativo (vedi sopra, Authorization
Code + PKCE), quindi il lockout è applicato direttamente da **Keycloak**:
`KeycloakRealmInitializer` configura all'avvio dell'app la brute-force
protection del realm (blocco di 15 minuti dopo 5 tentativi falliti
consecutivi), idempotente e riallineata ad ogni riavvio. Nessuna azione
manuale richiesta, nessun campo applicativo da gestire lato `users`.

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

## API

Collezioni pronte all'uso, con body reali e richieste concatenate
(token/id/IBAN passati automaticamente tra una chiamata e l'altra):
- `test-requests.http` — VS Code REST Client (estensione `humao.rest-client`),
  sequenza end-to-end completa inclusi i test di sicurezza (privilege
  escalation, IBAN mittente=beneficiario)
- `postman_collection.json` — stessa API in formato Postman

| Endpoint | Metodo | Auth | Descrizione |
|----------|--------|------|-------------|
| `/api/utenti/registra` | POST | No | Registrazione self-service (ruolo sempre CUSTOMER) |
| `/api/utenti/admin/crea` | POST | ADMIN | Crea utente con ruolo esplicito (ADMIN/EMPLOYEE/CUSTOMER) |
| `/api/utenti/{id}` | GET | ADMIN | Dettaglio utente |
| `/api/utenti` | GET | ADMIN | Lista paginata utenti |
| `/api/utenti/{id}` | PUT | ADMIN o proprietario | Modifica utente (solo ADMIN può cambiare `role`) |
| `/api/utenti/{id}` | DELETE | ADMIN/EMPLOYEE | Disattiva utente |
| `/api/utenti/{id}/status` | PATCH | ADMIN/EMPLOYEE | Cambia stato utente (ATTIVO/SOSPESO/CHIUSO) |
| `/api/conti/apertura` | POST | EMPLOYEE/CUSTOMER | Apre un conto (stato iniziale IN_ATTESA) |
| `/api/conti/{id}/approve` | PATCH | EMPLOYEE/ADMIN | Approva/rifiuta apertura conto |
| `/api/conti/{id}/chiusura` | POST | CUSTOMER | Chiude un conto (richiede saldo zero) |
| `/api/conti` | GET | ADMIN | Lista paginata conti |
| `/api/conti/{id}/limits` | GET | CUSTOMER (proprietario) | Consulta i limiti operativi del conto |
| `/api/conti/{id}/limits` | PUT | EMPLOYEE/ADMIN | Imposta i limiti operativi del conto |
| `/api/transactions/versamento` | POST | EMPLOYEE/CUSTOMER | Versamento su un conto |
| `/api/transactions/prelievo` | POST | EMPLOYEE/CUSTOMER | Prelievo da un conto (soggetto a limite giornaliero) |
| `/api/transactions/transfer` | POST | CUSTOMER | Bonifico tra IBAN diversi (trattenuta 2%) |
| `/api/transactions/giroconto` | POST | CUSTOMER | Trasferimento tra conti dello stesso intestatario (no commissione) |
| `/api/transactions/{id}` | GET | EMPLOYEE/ADMIN | Dettaglio transazione |

## DB

- PostgreSQL su `localhost:5432/gestionale_banca`
- Hibernate `ddl-auto=validate` — lo schema è gestito da Flyway (`src/main/resources/db/migration`), non da `ddl-auto`
- Lo stesso Postgres ospita anche lo schema interno di Keycloak (`start-dev`,
  un solo container/volume per semplicità in dev)
