package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.constants.StatiTransazione;
import com.banca.gestionale_banca.transaction.constants.TipiTransazione;
import com.banca.gestionale_banca.transaction.model.Transaction;
import com.banca.gestionale_banca.transaction.model.TransactionStatus;
import com.banca.gestionale_banca.transaction.model.TransactionType;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionTypeRepository;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    // Legge il valore dal file application.yml (es. app.banking.fee-percentage: 0.02)
    // Se non lo trova, usa 0.02 come default
    @Value("${app.banking.fee-percentage:0.02}")
    private BigDecimal feePercentage;

    private final BankAccountService bankAccountService;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final AccountLimitsService accountLimitsService;
    private final AuthorizationFacade authorizationFacade;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        
        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato ad operare su questo conto");
        
        if (!account.isAttivo()) {
            throw new ConflictException("Il conto corrente non è attivo");
        }

        TransactionType type = trovaTipo(TipiTransazione.VERSAMENTO);
        TransactionStatus status = trovaStatoEseguita();

        // DDD: L'entità gestisce la propria logica. Nessun save() esplicito richiesto.
        account.versa(request.getAmount());

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        
        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato ad operare su questo conto");
        
        if (!account.isAttivo()) {
            throw new ConflictException("Il conto corrente non è attivo");
        }

        verificaLimiti(account, request.getAmount(), true);

        // DDD: L'entità controlla i fondi e scala i soldi
        account.preleva(request.getAmount());

        TransactionType type = trovaTipo(TipiTransazione.PRELIEVO);
        TransactionStatus status = trovaStatoEseguita();

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Il conto di origine e quello di destinazione non possono coincidere");
        }
        
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verificaProprietario(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato ad operare su questo conto");
        
        if (!sourceAccount.isAttivo()) throw new ConflictException("Il conto di origine non è attivo");
        if (!targetAccount.isAttivo()) throw new ConflictException("Il conto di destinazione non è attivo");

        BigDecimal fee = feeValue(request.getAmount(), TipiTransazione.BONIFICO);
        BigDecimal totalDebit = request.getAmount().add(fee);

        verificaLimiti(sourceAccount, request.getAmount(), false);

        // DDD: Esecuzione delle operazioni sui conti
        sourceAccount.preleva(totalDebit);
        targetAccount.versa(request.getAmount());

        TransactionType type = trovaTipo(TipiTransazione.BONIFICO);
        TransactionStatus status = trovaStatoEseguita();

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription() + " (Trattenuta: " + fee + "€)", type, status);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), fee);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Il conto di origine e quello di destinazione non possono coincidere");
        }
        
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verificaProprietario(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato ad operare su questo conto");

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new ConflictException("Il giroconto è consentito solo tra conti dello stesso intestatario");
        }

        if (!sourceAccount.isAttivo()) throw new ConflictException("Il conto di origine non è attivo");
        if (!targetAccount.isAttivo()) throw new ConflictException("Il conto di destinazione non è attivo");

        verificaLimiti(sourceAccount, request.getAmount(), false);

        sourceAccount.preleva(request.getAmount());
        targetAccount.versa(request.getAmount());

        TransactionType type = trovaTipo(TipiTransazione.GIROCONTO);
        TransactionStatus status = trovaStatoEseguita();

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription(), type, status);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransazioneById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transazione non trovata"));

        return toResponse(tx, tx.getPayerAccount().getIban(), tx.getPayerAccount().getBalance(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));

        List<Transaction> transactions = transactionRepository.findAllByUserId(user.getId());

        return transactions.stream()
                .map(t -> mapToResponse(t, user.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getBankAccountTransactions(String iban) {
        List<Transaction> transactions = transactionRepository.findAllByIban(iban);

        return transactions.stream()
                .map(t -> mapToResponseByIban(t, iban))
                .toList();
    }

    // --- METODI HELPER PRIVATI (Invisibili all'esterno) ---

    private BankAccount trovaConto(String iban, String messageIfNotFound) {
        return bankAccountService.lockForUpdate(iban, messageIfNotFound);
    }

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

    private void verificaLimiti(BankAccount account, BigDecimal amount, boolean isWithdrawal) {
        accountLimitsService.findLimiti(account.getId()).ifPresent(limits -> {
            if (amount.compareTo(limits.getSingleTransactionLimit()) > 0) {
                throw new ConflictException("Importo superiore al limite per singola transazione (" + limits.getSingleTransactionLimit() + "€)");
            }
            if (isWithdrawal) {
                LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
                BigDecimal alreadyWithdrawn = transactionRepository.sumDailyWithdrawalsByAccount(account.getId(), dayStart, dayEnd);
                if (alreadyWithdrawn.add(amount).compareTo(limits.getDailyWithdrawalLimit()) > 0) {
                    throw new ConflictException("Limite giornaliero di prelievo superato");
                }
            } else {
                LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
                BigDecimal alreadyTransferred = transactionRepository.sumMonthlyTransfersByAccount(account.getId(), monthStart, monthEnd);
                if (alreadyTransferred.add(amount).compareTo(limits.getMonthlyTransferLimit()) > 0) {
                    throw new ConflictException("Limite mensile di trasferimento superato");
                }
            }
        });
    }

    private TransactionType trovaTipo(String name) {
        return transactionTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo transazione " + name + " non configurato"));
    }

    private TransactionStatus trovaStatoEseguita() {
        return transactionStatusRepository.findByName(StatiTransazione.ESEGUITA)
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
        
        // Questo save è necessario perché stiamo inserendo una NUOVA riga a DB.
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

    private TransactionResponse mapToResponse(Transaction tx, Long currentUserId) {
        String ibanMostrato = ibanToShow(tx, currentUserId);
        BigDecimal fee = feeValue(tx.getAmount(), tx.getType().getName());
        return toResponse(tx, ibanMostrato, null, fee);
    }

    private TransactionResponse mapToResponseByIban(Transaction tx, String requestedIban) {
        String ibanMostrato = ibanToShowBankAccount(tx, requestedIban);
        BigDecimal fee = feeValue(tx.getAmount(), tx.getType().getName());
        return toResponse(tx, ibanMostrato, null, fee);
    }

    private String ibanToShow(Transaction tx, Long currentUserId) {
        if (tx.getPayerUser() != null && tx.getPayerUser().getId().equals(currentUserId)) {
            return tx.getPayeeAccount() != null ? tx.getPayeeAccount().getIban() : "";
        }
        return tx.getPayerAccount() != null ? tx.getPayerAccount().getIban() : "";
    }

    private String ibanToShowBankAccount(Transaction tx, String requestedIban) {
        if (tx.getPayerAccount() != null && requestedIban.equals(tx.getPayerAccount().getIban())) {
            return tx.getPayeeAccount() != null ? tx.getPayeeAccount().getIban() : "";
        }
        return tx.getPayerAccount() != null ? tx.getPayerAccount().getIban() : "";
    }

    private BigDecimal feeValue(BigDecimal amount, String transactionType) {
        if (TipiTransazione.BONIFICO.equals(transactionType)) {
            return amount.multiply(this.feePercentage).setScale(2, RoundingMode.HALF_EVEN);
        }
        return null;
    }
}