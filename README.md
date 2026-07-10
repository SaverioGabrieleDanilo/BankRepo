
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

Apri `.env` e valorizza subito `DB_PASSWORD`/`POSTGRES_PASSWORD` (qualunque
valore locale va bene, es. `postgres`). `KEYCLOAK_CLIENT_SECRET` e
`KEYCLOAK_SERVICE_CLIENT_SECRET` restano vuoti per ora: si ottengono nel passo
successivo. `CORS_ALLOWED_ORIGINS` può restare vuoto (default `localhost:4200`).

```bash
# 2. Avvia PostgreSQL + Keycloak
docker compose up -d
```

Attendi che entrambi i container siano `healthy`/avviati (`docker compose ps`;
Keycloak in `start-dev` impiega ~15-20s dal primo avvio).

## Setup Keycloak (una tantum, per ogni nuovo ambiente/volume Docker)

L'admin di bootstrap di Keycloak (`admin`/`admin`, definito in
`docker-compose.yml`) esiste già al primo avvio — **non** va creato a mano
dalla welcome page.

1. Vai su http://localhost:9090/admin → login con `admin`/`admin`
2. Crea realm: `gestionale-banca`
3. Crea realm roles: `ADMIN`, `EMPLOYEE`, `CUSTOMER`
4. **Password policy del realm**: viene impostata **automaticamente all'avvio
   dell'app** da `KeycloakRealmInitializer`
   (`length(8) and digits(1) and upperCase(1) and lowerCase(1) and
   specialChars(1) and notUsername`) — nessuna azione manuale richiesta qui,
   è idempotente e si riallinea ad ogni riavvio se qualcuno la modifica a mano.
5. Crea client `banca-client` (login utenti finali, grant `password`/ROPC usato
   da `/api/auth/login`) con `Client authentication` ON, `Direct access
   grants` enabled
6. Copia il client secret (tab "Credentials") in `.env` → `KEYCLOAK_CLIENT_SECRET`
7. Crea un **secondo client** `banca-service` (service account, usato dal
   backend per creare/gestire utenti — NON usare `admin-cli`/realm `master`
   per questo):
   - `Client authentication` ON, `Service accounts roles` ON, tutti gli altri
     flow OFF
   - Tab "Service accounts roles" → assegna i client role `manage-users` e
     `manage-realm` del client `realm-management`
   - Copia il client secret in `.env` → `KEYCLOAK_SERVICE_CLIENT_SECRET`

A questo punto `.env` ha tutti i valori richiesti da `application.properties`
(vedi `.env.example` per l'elenco completo e i commenti su ciascuna variabile).

## Bootstrap del primo utente ADMIN

Non esiste un utente ADMIN precaricato: `DatabaseInitializer` inizializza solo
le tabelle di lookup (ruoli, stati), non utenti applicativi. L'endpoint
`POST /api/utenti/admin/crea` — l'unico modo per creare un utente con ruolo
diverso da CUSTOMER — richiede già un token ADMIN, quindi il primissimo ADMIN
va creato manualmente, **una sola volta per ambiente**:

1. **Su Keycloak** (Admin Console → realm `gestionale-banca` → Users → Add
   user): crea l'utente (es. `admin`/email a piacere), scheda Credentials →
   imposta la password (conforme alla password policy del punto 4 sopra,
   `Temporary` OFF), scheda Role mapping → assegna il realm role `ADMIN`.
2. **Sul database applicativo**: inserisci la riga corrispondente in `users`,
   con `keycloak_id` uguale all'ID dell'utente appena creato su Keycloak
   (visibile nell'URL della sua pagina utente in Admin Console) e `role_id`
   che punta alla riga `ADMIN` di `roles`:

   ```sql
   -- id ruolo/stato: SELECT id, name FROM roles; / user_statuses; / registration_statuses;
   INSERT INTO users (first_name, last_name, date_of_birth, email, username,
                       role_id, status_id, registration_status_id,
                       failed_login_attempts, created_at, keycloak_id)
   VALUES ('Super', 'Admin', '1990-01-01', 'admin@example.com', 'admin',
           1, 1, 2, 0, now(), '<keycloak-id-copiato-sopra>');
   ```

   (`role_id=1` → ADMIN, `status_id=1` → ATTIVO, `registration_status_id=2` →
   APPROVED, assumendo il seed di default di `DatabaseInitializer`.)

L'app e Keycloak restano così sempre coerenti (nessun utente "solo Keycloak" o
"solo DB"), stesso pattern che `UserServiceImpl.creaUtente` applica in
automatico per tutti gli utenti creati via API dopo questo bootstrap iniziale.

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

`AuthController` blocca un account per 15 minuti dopo 5 tentativi di login
falliti consecutivi (`failed_login_attempts`/`locked_until` su `users`,
verificati **prima** di contattare Keycloak). Ogni tentativo fallito durante il
blocco estende la finestra di altri 15 minuti. Il contatore si azzera al primo
login riuscito. La risposta è `423 Locked`, distinta dal `401` di credenziali
errate, per non confondere le due situazioni lato client.

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
| `/api/auth/login` | POST | No | Login (proxy verso Keycloak, grant `password`); lockout dopo 5 tentativi falliti |
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
