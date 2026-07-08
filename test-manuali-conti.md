# Piano di test manuale — endpoint conti (`/api/conti/*`)

> Nota ambiente: questo piano è pensato per essere eseguito **in locale** (Java 21 + Docker Compose + Keycloak), non nel sandbox di Claude — qui non sono disponibili Java 21/Docker/Keycloak per far girare lo stack. Segui i passi qui sotto sulla tua macchina.

Route coperte (tabella originale → endpoint reale nel codice):

| # | Endpoint reale | Ruolo atteso | Metodo/Classe |
|---|---|---|---|
| 1 | `PATCH /api/conti/{id}/approve` | EMPLOYEE, ADMIN | `BankAccountService.approvaConto` |
| 2 | `POST /api/conti/{id}/chiusura` | CUSTOMER | `BankAccountService.chiudiConto` |
| 3 | `GET /api/conti/{id}/limits` | CUSTOMER | `AccountLimitsService.getLimiti` |
| 4 | `PUT /api/conti/{id}/limits` | EMPLOYEE, ADMIN | `AccountLimitsService.impostaLimiti` |
| 5 | `GET /api/conti` | ADMIN | `BankAccountService.listaConti` |

## 0. Prerequisito bloccante

`DatabaseInitializer` semina solo `Role`, `UserStatus`, `RegistrationStatus` — **non** `AccountStatus`. Senza le righe `IN_ATTESA`, `ATTIVO`, `RIFIUTATO`, `CHIUSO` in tabella `account_status`, gli endpoint 1 e 2 falliscono con errore a runtime (`ResourceNotFoundException`) anche se il codice è corretto.

Prima di testare, verifica che le migration Flyway `V8__seed_account_status_pending.sql` e `V9__seed_account_status_chiuso.sql` coprano tutti e 4 gli stati (a occhio ne mancano almeno "ATTIVO" e "RIFIUTATO" — vanno controllate/completate).

## 1. Setup ambiente locale

```bash
docker-compose up -d          # avvia postgres + keycloak
./mvnw spring-boot:run         # richiede Java 21
```

Configura Keycloak (una tantum, vedi README):
1. http://localhost:9090/admin → login admin/admin
2. Realm `gestionale-banca`, client `banca-client` (Direct access grants ON), client secret in `application.properties`
3. Ruoli realm `ADMIN`, `EMPLOYEE`, `CUSTOMER` (creati automaticamente da `KeycloakRoleInitializer` all'avvio se mancanti)

## 2. Creare utenti di test (uno per ruolo)

Non esistono utenti seed: vanno creati.

```bash
# Utente CUSTOMER (autoregistrazione)
curl -X POST http://localhost:8080/api/utenti/registra \
  -H "Content-Type: application/json" \
  -d '{"username":"cust1","password":"Test123!","email":"cust1@test.it","firstName":"Mario","lastName":"Rossi"}'
```

Per EMPLOYEE/ADMIN serve un utente ADMIN già esistente che chiama `POST /api/utenti/admin/crea` con `role` valorizzato, oppure assegna il ruolo realm direttamente da Keycloak admin console (Users → il tuo utente → Role mapping).

## 3. Ottenere i token

```bash
get_token() {
  curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r .access_token
}

TOKEN_CUSTOMER=$(get_token cust1 'Test123!')
TOKEN_EMPLOYEE=$(get_token emp1 'Test123!')
TOKEN_ADMIN=$(get_token admin1 'Test123!')
```

## 4. Casi di test per endpoint

Per ogni endpoint: 1 caso "happy path" con il ruolo corretto, 1 caso con ruolo sbagliato (atteso `403`), poi i casi limite di business logic.

### 4.1 `PATCH /api/conti/{id}/approve`

```bash
# happy path (EMPLOYEE approva un conto IN_ATTESA)
curl -i -X PATCH http://localhost:8080/api/conti/1/approve \
  -H "Authorization: Bearer $TOKEN_EMPLOYEE" \
  -H "Content-Type: application/json" \
  -d '{"approva": true}'
```

| Caso | Ruolo | Stato conto | Atteso |
|---|---|---|---|
| Approvazione ok | EMPLOYEE o ADMIN | IN_ATTESA | 200, status → ATTIVO |
| Rifiuto | EMPLOYEE o ADMIN | IN_ATTESA | 200, status → RIFIUTATO (`"approva": false`) |
| Ruolo non autorizzato | CUSTOMER | IN_ATTESA | 403 |
| Conto già gestito | EMPLOYEE | ATTIVO (già approvato) | 409 (`ConflictException`: "non è in attesa di approvazione") |
| Conto inesistente | EMPLOYEE | — | 404 |

### 4.2 `POST /api/conti/{id}/chiusura`

```bash
curl -i -X POST http://localhost:8080/api/conti/1/chiusura \
  -H "Authorization: Bearer $TOKEN_CUSTOMER"
```

| Caso | Ruolo | Condizione | Atteso |
|---|---|---|---|
| Chiusura ok | CUSTOMER proprietario | saldo = 0 | 200, status → CHIUSO |
| Saldo diverso da zero | CUSTOMER proprietario | saldo ≠ 0 | 409 (`ConflictException`: "saldo deve essere zero") |
| Conto di un altro utente | CUSTOMER non proprietario | — | 403 |
| Ruolo non autorizzato | EMPLOYEE/ADMIN | — | 403 (endpoint riservato a `hasRole('CUSTOMER')`, verificare se è voluto: un EMPLOYEE non può chiudere per conto del cliente) |
| Conto inesistente | CUSTOMER | — | 404 |

### 4.3 `GET /api/conti/{id}/limits`

```bash
curl -i http://localhost:8080/api/conti/1/limits \
  -H "Authorization: Bearer $TOKEN_CUSTOMER"
```

| Caso | Ruolo | Condizione | Atteso |
|---|---|---|---|
| Lettura ok | CUSTOMER proprietario | limiti configurati | 200 con `dailyWithdrawalLimit`, `singleTransactionLimit`, `monthlyTransferLimit` |
| Conto di un altro utente | CUSTOMER non proprietario | — | 403 |
| Limiti non configurati | CUSTOMER proprietario | nessun `AccountLimits` per il conto | 404 (`ResourceNotFoundException`) |
| Ruolo non autorizzato | EMPLOYEE/ADMIN | — | 403 (nota: endpoint è `hasRole('CUSTOMER')` puro, un EMPLOYEE non può leggere i limiti di un cliente da qui — verificare se è la UX voluta o se serve un endpoint gemello per operatori) |

### 4.4 `PUT /api/conti/{id}/limits`

```bash
curl -i -X PUT http://localhost:8080/api/conti/1/limits \
  -H "Authorization: Bearer $TOKEN_EMPLOYEE" \
  -H "Content-Type: application/json" \
  -d '{"dailyWithdrawalLimit": 1000, "singleTransactionLimit": 500, "monthlyTransferLimit": 5000}'
```

| Caso | Ruolo | Atteso |
|---|---|---|
| Creazione limiti (prima volta) | EMPLOYEE/ADMIN | 200, nuovo `AccountLimits` creato |
| Aggiornamento limiti esistenti | EMPLOYEE/ADMIN | 200, valori aggiornati, `updatedAt` cambiato |
| Ruolo non autorizzato | CUSTOMER | 403 |
| Campo mancante (es. `singleTransactionLimit` null) | EMPLOYEE | 400 (Bean Validation `@NotNull`) |
| Conto inesistente | EMPLOYEE | 404 |

### 4.5 `GET /api/conti`

```bash
curl -i http://localhost:8080/api/conti \
  -H "Authorization: Bearer $TOKEN_ADMIN"
```

| Caso | Ruolo | Atteso |
|---|---|---|
| Lista completa | ADMIN | 200, array di `BankAccountAdminResponse` (tutti i conti, ogni ruolo proprietario incluso) |
| Ruolo non autorizzato | CUSTOMER/EMPLOYEE | 403 |
| Nessun conto a sistema | ADMIN | 200, array vuoto |

## 5. Punti da tenere d'occhio durante l'esecuzione

- Gli endpoint 1 e 2 dipendono dai record `AccountStatus` seedati (vedi punto 0) — se falliscono con errore generico 500 invece di 404/409, molto probabilmente è quello.
- L'endpoint 3 (`GET limits`) è ad oggi raggiungibile solo da CUSTOMER: un EMPLOYEE che vuole controllare i limiti di un cliente non ha una via diretta. Segnalalo se non è il comportamento voluto — non è un bug, è una scelta di design da confermare.
- Nessuna paginazione su `GET /api/conti` (endpoint 5): con molti conti a sistema la risposta cresce senza limiti — da tenere a mente se il dataset di test diventa grande.

## 6. Esito

Compila dopo l'esecuzione:

| Endpoint | Esito | Note |
|---|---|---|
| PATCH approve | ⬜ Pass / ⬜ Fail | |
| POST chiusura | ⬜ Pass / ⬜ Fail | |
| GET limits | ⬜ Pass / ⬜ Fail | |
| PUT limits | ⬜ Pass / ⬜ Fail | |
| GET conti | ⬜ Pass / ⬜ Fail | |
