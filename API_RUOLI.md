# Divisione delle API per Ruolo

Suddivisione delle chiamate API valide e possibili per ciascun ruolo del sistema (CUSTOMER, EMPLOYEE, ADMIN), basata sulle user story e integrata con le valutazioni fatte insieme.

## CUSTOMER

| Metodo | Endpoint | Descrizione |
|---|---|---|
| POST | `/users/register` | Registrazione nuovo utente |
| POST | `/auth/login` | Accesso al sistema |
| POST | `/accounts/open` | Richiesta apertura nuovo conto corrente |
| POST | `/accounts/{account_id}/close` | Richiesta chiusura conto (solo se balance = 0) |
| POST | `/transactions/transfer` | Bonifico verso un conto beneficiario |
| POST | `/transactions/giroconto` | Giroconto tra due conti propri |
| POST | `/transactions/withdraw` | Prelievo contante |
| POST | `/transactions/deposit` | Versamento contante |
| GET | `/accounts/{account_id}/limits` | Consultazione dei propri limiti operativi |

## EMPLOYEE

| Metodo | Endpoint | Descrizione |
|---|---|---|
| POST | `/auth/login` | Accesso al sistema |
| PATCH | `/users/{user_id}/status` | Attivazione/sospensione/chiusura di un utente |
| PATCH | `/accounts/{account_id}/approve` | Approvazione/rifiuto apertura conto |
| GET | `/transactions/{transaction_id}` | Consultazione stato di contabilizzazione di una transazione |
| PUT | `/accounts/{account_id}/limits` | Impostazione limiti operativi di un conto |

## ADMIN

L'ADMIN eredita tutte le API dell'EMPLOYEE, più le proprie:

| Metodo | Endpoint | Descrizione |
|---|---|---|
| POST | `/auth/login` | Accesso al sistema |
| GET | `/accounts` | Elenco di tutti i conti correnti con saldo e titolare, per valutazioni sull'andamento della banca |
| PATCH | `/users/{user_id}/status` | Attivazione/sospensione/chiusura di un utente *(ereditata da EMPLOYEE)* |
| PATCH | `/accounts/{account_id}/approve` | Approvazione/rifiuto apertura conto *(ereditata da EMPLOYEE)* |
| GET | `/transactions/{transaction_id}` | Consultazione stato di contabilizzazione di una transazione *(ereditata da EMPLOYEE)* |
| PUT | `/accounts/{account_id}/limits` | Impostazione limiti operativi di un conto *(ereditata da EMPLOYEE)* |

## Tabella riepilogativa

| Endpoint | CUSTOMER | EMPLOYEE | ADMIN |
|---|:---:|:---:|:---:|
| POST /users/register | X | | |
| POST /auth/login | X | X | X |
| PATCH /users/{user_id}/status | | X | X |
| POST /accounts/open | X | | |
| PATCH /accounts/{account_id}/approve | | X | X |
| POST /accounts/{account_id}/close | X | | |
| GET /accounts | | | X |
| POST /transactions/transfer | X | | |
| POST /transactions/giroconto | X | | |
| POST /transactions/withdraw | X | | |
| POST /transactions/deposit | X | | |
| GET /transactions/{transaction_id} | | X | X |
| PUT /accounts/{account_id}/limits | | X | X |
| GET /accounts/{account_id}/limits | X | | |
