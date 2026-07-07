# [US-3.1 / US-3.2 / US-5.3] Implementazione bonifico — `POST /api/transactions/bonifico`

**Labels:** `feature`, `backend`, `transactions`

## User Story di riferimento

- US-3.1 — Come CUSTOMER, voglio disporre un bonifico da un mio conto verso un conto beneficiario
- US-3.2 — Come sistema, voglio verificare la capienza del conto ordinante prima di eseguire un bonifico
- US-5.3 — Come sistema, voglio verificare il limite di trasferimento mensile prima di autorizzare un bonifico

## Cosa è stato fatto

- [x] DTO `TransferRequest` (`payerAccountId`, `payeeIban`, `amount`, `description`) con validazione Bean Validation (`@NotNull`, `@DecimalMin`)
- [x] Nuova eccezione `BusinessRuleException` (HTTP 422 `UNPROCESSABLE_CONTENT`) per le violazioni di regole di dominio (conto non attivo, saldo insufficiente, limiti superati)
- [x] Handler in `GlobalExceptionHandler` per `BusinessRuleException` e per `ObjectOptimisticLockingFailureException` (409, race condition sul saldo)
- [x] `TransactionService.eseguiBonifico(Long currentUserId, TransferRequest request)` con validazioni in ordine di costo crescente:
  - esistenza conto ordinante e beneficiario
  - proprietà del conto ordinante (403 se non è dell'utente autenticato)
  - stato `ATTIVO` di entrambi i conti
  - blocco esplicito se ordinante e beneficiario coincidono → deve usare `/giroconto`
  - `single_transaction_limit` (da `AccountLimits`)
  - `monthly_transfer_limit` (somma `TRANSFER_OUT`/`GIROCONTO` del mese corrente, nuova query `sumMonthlyTransfersByAccount` in `TransactionRepository`)
  - saldo disponibile
- [x] Aggiornamento saldo con **optimistic locking** automatico via `@Version` su `BankAccount` (nessun codice manuale di lock)
- [x] Creazione delle due righe di transazione collegate (`TRANSFER_OUT` per l'ordinante, `TRANSFER_IN` per il beneficiario), stesso importo, tipo diverso — logica a "partita doppia"
- [x] Tutto racchiuso in un unico metodo `@Transactional`: in caso di qualunque eccezione, rollback completo → nessuna riga orfana in `transactions` (atomicità richiesta da US-3.2)
- [x] Endpoint `POST /api/transactions/bonifico` in `TransactionController`, coerente con `/versamento` e `/prelievo` già esistenti, riservato a `CUSTOMER`
- [x] Risposta come `TransactionResponse` (stesso DTO usato da versamento/prelievo), dal punto di vista dell'ordinante

## Cosa manca / blocker noto

- [ ] **`DatabaseInitializer` non seeda ancora** `AccountStatus`, `TransactionType`, `TransactionStatus` — senza questi dati (`TRANSFER_OUT`, `TRANSFER_IN`, `SERVITA`, `ATTIVO`, ecc.) l'endpoint fallisce a runtime con "tipo/stato non configurato". Da fare prima di poter testare end-to-end.
- [ ] Verificare compilazione (Java 21 richiesto dal progetto, non disponibile nell'ambiente di sviluppo assistito — va compilato/testato in locale)
- [ ] Test manuale/automatico dell'intero flusso

## File coinvolti

- `dto/TransferRequest.java` (nuovo)
- `exception/BusinessRuleException.java` (nuovo)
- `exception/GlobalExceptionHandler.java` (modificato)
- `repository/TransactionRepository.java` (aggiunta query limite mensile)
- `service/TransactionService.java` (nuovo metodo `eseguiBonifico`)
- `service/TransactionServiceImpl.java` (implementazione)
- `controller/TransactionController.java` (nuovo endpoint `/bonifico`)
