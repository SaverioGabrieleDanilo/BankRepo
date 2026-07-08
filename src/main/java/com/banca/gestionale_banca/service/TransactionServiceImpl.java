package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.GirocontoRequest;
import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.dto.TransferRequest;
import com.banca.gestionale_banca.exception.ConflictException;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Transaction;
import com.banca.gestionale_banca.model.TransactionStatus;
import com.banca.gestionale_banca.model.TransactionType;
import com.banca.gestionale_banca.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;
import com.banca.gestionale_banca.repository.TransactionRepository;
import com.banca.gestionale_banca.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.02");
    private static final String STATO_ESEGUITA = "ESEGUITA";

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final AccountLimitsRepository accountLimitsRepository;

    @Override
    @Transactional
    public TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        verificaProprietario(account, keycloakId, isEmployee);
        verificaContoAttivo(account, "Il conto corrente non è attivo");

        TransactionType type = trovaTipo("VERSAMENTO");
        TransactionStatus status = trovaStatoEseguita();

        account.setBalance(account.getBalance().add(request.getAmount()));
        bankAccountRepository.save(account);

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        verificaProprietario(account, keycloakId, isEmployee);
        verificaContoAttivo(account, "Il conto corrente non è attivo");

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Saldo insufficiente per completare il prelievo");
        }

        verificaLimiteSingolaTransazione(account, request.getAmount());
        verificaLimiteGiornalieroPrelievo(account, request.getAmount());

        TransactionType type = trovaTipo("PRELIEVO");
        TransactionStatus status = trovaStatoEseguita();

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(account);

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee) {
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        verificaProprietario(sourceAccount, keycloakId, isEmployee);
        verificaContoAttivo(sourceAccount, "Il conto di origine non è attivo");
        verificaContoAttivo(targetAccount, "Il conto di destinazione non è attivo");

        BigDecimal fee = request.getAmount().multiply(FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new ConflictException("Saldo insufficiente. Il bonifico richiede: " + totalDebit + "€ compresa trattenuta");
        }

        verificaLimiteSingolaTransazione(sourceAccount, request.getAmount());
        verificaLimiteMensileTrasferimenti(sourceAccount, request.getAmount());

        TransactionType type = trovaTipo("BONIFICO");
        TransactionStatus status = trovaStatoEseguita();

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription() + " (Trattenuta: " + fee + "€)", type, status);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), fee);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee) {
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        verificaProprietario(sourceAccount, keycloakId, isEmployee);

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new ConflictException("Il giroconto è consentito solo tra conti dello stesso intestatario");
        }

        verificaContoAttivo(sourceAccount, "Il conto di origine non è attivo");
        verificaContoAttivo(targetAccount, "Il conto di destinazione non è attivo");

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Saldo insufficiente per completare il giroconto");
        }

        verificaLimiteSingolaTransazione(sourceAccount, request.getAmount());
        verificaLimiteMensileTrasferimenti(sourceAccount, request.getAmount());

        TransactionType type = trovaTipo("GIROCONTO");
        TransactionStatus status = trovaStatoEseguita();

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), null);
    }

    @Override
    public TransactionResponse getTransazioneById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transazione non trovata"));

        return toResponse(tx, tx.getPayerAccount().getIban(), tx.getPayerAccount().getBalance(), null);
    }

    private void verificaProprietario(BankAccount account, String keycloakId, boolean isEmployee) {
        if (isEmployee) {
            return;
        }
        if (!account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad operare su questo conto");
        }
    }

    private BankAccount trovaConto(String iban, String messaggioSeNonTrovato) {
        return bankAccountRepository.findByIbanForUpdate(iban)
                .orElseThrow(() -> new ResourceNotFoundException(messaggioSeNonTrovato));
    }

    /**
     * Acquisisce il lock pessimistico su entrambi i conti in ordine deterministico
     * (per IBAN) per evitare deadlock quando due bonifici incrociati (A->B e B->A)
     * vengono eseguiti in concorrenza.
     */
    private Map<String, BankAccount> lockAccountsInOrder(String sourceIban, String targetIban) {
        List<String> ordered = sourceIban.compareTo(targetIban) <= 0
                ? List.of(sourceIban, targetIban)
                : List.of(targetIban, sourceIban);

        Map<String, BankAccount> locked = new LinkedHashMap<>();
        for (String iban : ordered) {
            String label = iban.equals(sourceIban) ? "di origine" : "di destinazione";
            locked.put(iban, trovaConto(iban, "Conto " + label + " non trovato"));
        }
        return locked;
    }

    private void verificaContoAttivo(BankAccount account, String messaggioSeNonAttivo) {
        if (!"ATTIVO".equals(account.getStatus().getName())) {
            throw new ConflictException(messaggioSeNonAttivo);
        }
    }

    private void verificaLimiteSingolaTransazione(BankAccount account, BigDecimal amount) {
        accountLimitsRepository.findByAccountId(account.getId()).ifPresent(limiti -> {
            if (amount.compareTo(limiti.getSingleTransactionLimit()) > 0) {
                throw new ConflictException(
                        "Importo superiore al limite per singola transazione consentito (" + limiti.getSingleTransactionLimit() + "€)");
            }
        });
    }

    private void verificaLimiteGiornalieroPrelievo(BankAccount account, BigDecimal amount) {
        accountLimitsRepository.findByAccountId(account.getId()).ifPresent(limiti -> {
            LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
            BigDecimal giaPrelevato = transactionRepository.sumDailyWithdrawalsByAccount(account.getId(), dayStart, dayEnd);
            if (giaPrelevato.add(amount).compareTo(limiti.getDailyWithdrawalLimit()) > 0) {
                throw new ConflictException("Limite giornaliero di prelievo superato (limite: "
                        + limiti.getDailyWithdrawalLimit() + "€, già prelevato oggi: " + giaPrelevato + "€)");
            }
        });
    }

    private void verificaLimiteMensileTrasferimenti(BankAccount account, BigDecimal amount) {
        accountLimitsRepository.findByAccountId(account.getId()).ifPresent(limiti -> {
            LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
            BigDecimal giaTrasferito = transactionRepository.sumMonthlyTransfersByAccount(account.getId(), monthStart, monthEnd);
            if (giaTrasferito.add(amount).compareTo(limiti.getMonthlyTransferLimit()) > 0) {
                throw new ConflictException("Limite mensile di trasferimento superato (limite: "
                        + limiti.getMonthlyTransferLimit() + "€, già trasferito questo mese: " + giaTrasferito + "€)");
            }
        });
    }

    private TransactionType trovaTipo(String nome) {
        return transactionTypeRepository.findByName(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo transazione " + nome + " non configurato"));
    }

    private TransactionStatus trovaStatoEseguita() {
        return transactionStatusRepository.findByName(STATO_ESEGUITA)
                .orElseThrow(() -> new ResourceNotFoundException("Stato transazione ESEGUITA non configurato"));
    }

    private Transaction registraMovimento(BankAccount payer, BankAccount payee, BigDecimal amount,
                                           String description, TransactionType type, TransactionStatus status) {
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(payer);
        transaction.setPayeeAccount(payee);
        transaction.setPayerUser(payer.getUser());
        transaction.setPayeeUser(payee.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }

    private TransactionResponse toResponse(Transaction tx, String iban, BigDecimal updatedBalance, BigDecimal fee) {
        return TransactionResponse.builder()
                .transactionId(tx.getId())
                .iban(iban)
                .type(tx.getType().getName())
                .amount(tx.getAmount())
                .fee(fee)
                .updatedBalance(updatedBalance)
                .status(tx.getStatus().getName())
                .timestamp(tx.getTransactionDate())
                .build();
    }
}
