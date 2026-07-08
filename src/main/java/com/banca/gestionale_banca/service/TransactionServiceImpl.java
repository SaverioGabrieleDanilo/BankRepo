package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.GirocontoRequest;
import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.dto.TransferRequest;
import com.banca.gestionale_banca.exception.BusinessRuleException;
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
        BankAccount account = bankAccountRepository.findByIbanForUpdate(request.getIban())
                .orElseThrow(() -> new RuntimeException("Conto corrente non trovato"));

        verificaProprietario(account, keycloakId, isEmployee);

        if (!"ATTIVO".equals(account.getStatus().getName())) {
            throw new RuntimeException("Il conto corrente non è attivo");
        }

        TransactionType type = transactionTypeRepository.findByName("VERSAMENTO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione VERSAMENTO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName("ESEGUITA")
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        bankAccountRepository.save(account);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(account);
        transaction.setPayeeAccount(account);
        transaction.setPayerUser(account.getUser());
        transaction.setPayeeUser(account.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(account.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .updatedBalance(account.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findByIbanForUpdate(request.getIban())
                .orElseThrow(() -> new RuntimeException("Conto corrente non trovato"));

        verificaProprietario(account, keycloakId, isEmployee);

        if (!"ATTIVO".equals(account.getStatus().getName())) {
            throw new RuntimeException("Il conto corrente non è attivo");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Saldo insufficiente per completare il prelievo");
        }

        verificaLimiteSingolaTransazione(account, request.getAmount());
        verificaLimiteGiornalieroPrelievo(account, request.getAmount());

        TransactionType type = transactionTypeRepository.findByName("PRELIEVO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione PRELIEVO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName("ESEGUITA")
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(account);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(account);
        transaction.setPayeeAccount(account);
        transaction.setPayerUser(account.getUser());
        transaction.setPayeeUser(account.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(account.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .updatedBalance(account.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    private void verificaProprietario(BankAccount account, String keycloakId, boolean isEmployee) {
        if (isEmployee) {
            return;
        }
        if (!account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad operare su questo conto");
        }
    }

    private Map<String, BankAccount> lockAccountsInOrder(String sourceIban, String targetIban) {
        List<String> ordered = sourceIban.compareTo(targetIban) <= 0
                ? List.of(sourceIban, targetIban)
                : List.of(targetIban, sourceIban);

        Map<String, BankAccount> locked = new LinkedHashMap<>();
        for (String iban : ordered) {
            String label = iban.equals(sourceIban) ? "di origine" : "di destinazione";
            BankAccount account = bankAccountRepository.findByIbanForUpdate(iban)
                    .orElseThrow(() -> new RuntimeException("Conto " + label + " non trovato"));
            locked.put(iban, account);
        }
        return locked;
    }

    private void verificaLimiteSingolaTransazione(BankAccount account, BigDecimal amount) {
        accountLimitsRepository.findByAccountId(account.getId()).ifPresent(limiti -> {
            if (amount.compareTo(limiti.getSingleTransactionLimit()) > 0) {
                throw new BusinessRuleException(
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
                throw new BusinessRuleException("Limite giornaliero di prelievo superato (limite: "
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
                throw new BusinessRuleException("Limite mensile di trasferimento superato (limite: "
                        + limiti.getMonthlyTransferLimit() + "€, già trasferito questo mese: " + giaTrasferito + "€)");
            }
        });
    }

    @Override
    @Transactional
    public TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee) {
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        verificaProprietario(sourceAccount, keycloakId, isEmployee);

        if (!"ATTIVO".equals(sourceAccount.getStatus().getName()) || !"ATTIVO".equals(targetAccount.getStatus().getName())) {
            throw new RuntimeException("Uno o entrambi i conti correnti non sono attivi");
        }

        BigDecimal fee = request.getAmount().multiply(FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new RuntimeException("Saldo insufficiente. Il bonifico richiede: " + totalDebit + "€ compresa trattenuta");
        }

        verificaLimiteSingolaTransazione(sourceAccount, request.getAmount());
        verificaLimiteMensileTrasferimenti(sourceAccount, request.getAmount());

        TransactionType type = transactionTypeRepository.findByName("BONIFICO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione BONIFICO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName(STATO_ESEGUITA)
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription() + " (Trattenuta: " + fee + "€)");
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(sourceAccount);
        transaction.setPayeeAccount(targetAccount);
        transaction.setPayerUser(sourceAccount.getUser());
        transaction.setPayeeUser(targetAccount.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(sourceAccount.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .fee(fee)
                .updatedBalance(sourceAccount.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee) {
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        verificaProprietario(sourceAccount, keycloakId, isEmployee);

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new RuntimeException("Il giroconto è consentito solo tra conti dello stesso intestatario");
        }

        if (!"ATTIVO".equals(sourceAccount.getStatus().getName()) || !"ATTIVO".equals(targetAccount.getStatus().getName())) {
            throw new RuntimeException("Uno o entrambi i conti non sono attivi");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Saldo insufficiente per completare il giroconto");
        }

        verificaLimiteSingolaTransazione(sourceAccount, request.getAmount());
        verificaLimiteMensileTrasferimenti(sourceAccount, request.getAmount());

        TransactionType type = transactionTypeRepository.findByName("GIROCONTO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione GIROCONTO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName(STATO_ESEGUITA)
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(sourceAccount);
        transaction.setPayeeAccount(targetAccount);
        transaction.setPayerUser(sourceAccount.getUser());
        transaction.setPayeeUser(targetAccount.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(sourceAccount.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .updatedBalance(sourceAccount.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    @Override
    public TransactionResponse getTransazioneById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transazione non trovata"));

        return TransactionResponse.builder()
                .transactionId(tx.getId())
                .iban(tx.getPayerAccount().getIban())
                .type(tx.getType().getName())
                .amount(tx.getAmount())
                .updatedBalance(tx.getPayerAccount().getBalance())
                .status(tx.getStatus().getName())
                .timestamp(tx.getTransactionDate())
                .build();
    }
}