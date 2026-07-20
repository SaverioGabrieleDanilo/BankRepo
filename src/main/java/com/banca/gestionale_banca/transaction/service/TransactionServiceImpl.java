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
    public TransactionResponse executeDeposit(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = findAccountOrThrow(request.getIban(), "Bank account not found");
        
        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Not authorized to operate on this account");
        
        if (!account.isActive()) {
            throw new ConflictException("The bank account is not active");
        }

        TransactionType type = findTransactionType(TipiTransazione.VERSAMENTO);
        TransactionStatus status = findExecutedStatus();

        account.deposit(request.getAmount());

        Transaction transaction = recordTransaction(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return buildResponse(transaction, account.getIban(), null, true);
    }

    @Override
    @Transactional
    public TransactionResponse executeWithdrawal(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = findAccountOrThrow(request.getIban(), "Bank account not found");
        
        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Not authorized to operate on this account");
        
        if (!account.isActive()) {
            throw new ConflictException("The bank account is not active");
        }

        checkTransactionLimits(account, request.getAmount(), true);

        account.withdraw(request.getAmount());

        TransactionType type = findTransactionType(TipiTransazione.PRELIEVO);
        TransactionStatus status = findExecutedStatus();

        Transaction transaction = recordTransaction(account, account, request.getAmount(),
                request.getDescription(), type, status);

        return buildResponse(transaction, account.getIban(), null, false);
    }

    @Override
    @Transactional
    public TransactionResponse executeWireTransfer(TransferRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Source and target accounts cannot be the same");
        }
        
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verifyOwnership(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Not authorized to operate on this account");
        
        if (!sourceAccount.isActive()) throw new ConflictException("Source account is not active");
        if (!targetAccount.isActive()) throw new ConflictException("Target account is not active");

        BigDecimal fee = calculateFee(request.getAmount(), TipiTransazione.BONIFICO);
        BigDecimal totalDebit = request.getAmount().add(fee);

        checkTransactionLimits(sourceAccount, request.getAmount(), false);

        sourceAccount.withdraw(totalDebit);
        targetAccount.deposit(request.getAmount());

        TransactionType type = findTransactionType(TipiTransazione.BONIFICO);
        TransactionStatus status = findExecutedStatus();

        Transaction transaction = recordTransaction(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription() + " (Fee: €" + fee + ")", type, status);

        return buildResponse(transaction, sourceAccount.getIban(), fee, false);
    }

    @Override
    @Transactional
    public TransactionResponse executeInternalTransfer(GirocontoRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Source and target accounts cannot be the same");
        }
        
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verifyOwnership(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Not authorized to operate on this account");

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new ConflictException("Internal transfers are only allowed between accounts of the same owner");
        }

        if (!sourceAccount.isActive()) throw new ConflictException("Source account is not active");
        if (!targetAccount.isActive()) throw new ConflictException("Target account is not active");

        checkTransactionLimits(sourceAccount, request.getAmount(), false);

        sourceAccount.withdraw(request.getAmount());
        targetAccount.deposit(request.getAmount());

        TransactionType type = findTransactionType(TipiTransazione.GIROCONTO);
        TransactionStatus status = findExecutedStatus();

        Transaction transaction = recordTransaction(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription(), type, status);

        return buildResponse(transaction, sourceAccount.getIban(), null, false);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        boolean isIncoming = tx.getPayeeAccount() != null && tx.getPayerAccount() != null
                && tx.getPayerAccount().getId().equals(tx.getPayeeAccount().getId()) 
                && TipiTransazione.VERSAMENTO.equals(tx.getType().getName());

        String displayedIban = tx.getPayerAccount() != null ? tx.getPayerAccount().getIban() : "";
        BigDecimal fee = calculateFee(tx.getAmount(), tx.getType().getName());

        // FIX: Uso displayedIban e fee, prima erano ignorati
        return buildResponse(tx, displayedIban, fee, isIncoming);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<Transaction> transactions = transactionRepository.findAllByUserId(user.getId());

        return transactions.stream()
                .map(t -> mapToUserResponse(t, user.getId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getBankAccountTransactions(String iban) {
        List<Transaction> transactions = transactionRepository.findAllByIban(iban);

        return transactions.stream()
                .map(t -> mapToAccountResponse(t, iban))
                .toList();
    }

    // --- PRIVATE HELPER METHODS ---

    private BankAccount findAccountOrThrow(String iban, String errorMessage) {
        return bankAccountService.lockForUpdate(iban, errorMessage);
    }

    private Map<String, BankAccount> lockAccountsInOrder(String sourceIban, String targetIban) {
        List<String> orderedIbans = sourceIban.compareTo(targetIban) <= 0
                ? List.of(sourceIban, targetIban)
                : List.of(targetIban, sourceIban);

        Map<String, BankAccount> lockedAccounts = new LinkedHashMap<>();
        for (String iban : orderedIbans) {
            String label = iban.equals(sourceIban) ? "Source" : "Target";
            lockedAccounts.put(iban, findAccountOrThrow(iban, label + " account not found"));
        }
        return lockedAccounts;
    }

    private void checkTransactionLimits(BankAccount account, BigDecimal amount, boolean isWithdrawal) {
        accountLimitsService.findLimiti(account.getId()).ifPresent(limits -> {
            if (amount.compareTo(limits.getSingleTransactionLimit()) > 0) {
                throw new ConflictException("Amount exceeds single transaction limit (€" + limits.getSingleTransactionLimit() + ")");
            }
            if (isWithdrawal) {
                LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
                BigDecimal alreadyWithdrawn = transactionRepository.sumDailyWithdrawalsByAccount(account.getId(), dayStart, dayEnd);
                
                if (alreadyWithdrawn.add(amount).compareTo(limits.getDailyWithdrawalLimit()) > 0) {
                    throw new ConflictException("Daily withdrawal limit exceeded");
                }
            } else {
                LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
                BigDecimal alreadyTransferred = transactionRepository.sumMonthlyTransfersByAccount(account.getId(), monthStart, monthEnd);
                
                if (alreadyTransferred.add(amount).compareTo(limits.getMonthlyTransferLimit()) > 0) {
                    throw new ConflictException("Monthly transfer limit exceeded");
                }
            }
        });
    }

    private TransactionType findTransactionType(String name) {
        return transactionTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type " + name + " not configured"));
    }

    private TransactionStatus findExecutedStatus() {
        return transactionStatusRepository.findByName(StatiTransazione.ESEGUITA)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction status EXECUTED not configured"));
    }

    private Transaction recordTransaction(BankAccount payer, BankAccount payee, BigDecimal amount,
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

    private TransactionResponse buildResponse(Transaction tx, String iban, BigDecimal fee, Boolean isIncoming) {
        return TransactionResponse.builder()
                .transactionId(tx.getId())
                .iban(iban)
                .type(tx.getType().getName())
                .amount(tx.getAmount())
                .fee(fee)
                .status(tx.getStatus().getName())
                .timestamp(tx.getTransactionDate())
                .incoming(isIncoming != null ? isIncoming : false)
                .build();
    }

    private TransactionResponse mapToUserResponse(Transaction tx, Long currentUserId) {
        String counterpartyIban = getCounterpartyIbanForUser(tx, currentUserId);
        BigDecimal fee = calculateFee(tx.getAmount(), tx.getType().getName());

        boolean isIncoming = tx.getPayeeUser() != null && tx.getPayeeUser().getId().equals(currentUserId);
        
        return buildResponse(tx, counterpartyIban, fee, isIncoming);
    }

    private TransactionResponse mapToAccountResponse(Transaction tx, String requestedIban) {
        String counterpartyIban = getCounterpartyIbanForAccount(tx, requestedIban);
        BigDecimal fee = calculateFee(tx.getAmount(), tx.getType().getName());

        boolean isIncoming = tx.getPayeeAccount() != null && requestedIban.equals(tx.getPayeeAccount().getIban());

        return buildResponse(tx, counterpartyIban, fee, isIncoming);
    }

    private String getCounterpartyIbanForUser(Transaction tx, Long currentUserId) {
        if (tx.getPayerUser() != null && tx.getPayerUser().getId().equals(currentUserId)) {
            return tx.getPayeeAccount() != null ? tx.getPayeeAccount().getIban() : "";
        }
        return tx.getPayerAccount() != null ? tx.getPayerAccount().getIban() : "";
    }

    private String getCounterpartyIbanForAccount(Transaction tx, String requestedIban) {
        if (tx.getPayerAccount() != null && requestedIban.equals(tx.getPayerAccount().getIban())) {
            return tx.getPayeeAccount() != null ? tx.getPayeeAccount().getIban() : "";
        }
        return tx.getPayerAccount() != null ? tx.getPayerAccount().getIban() : "";
    }

    private BigDecimal calculateFee(BigDecimal amount, String transactionType) {
        if (TipiTransazione.BONIFICO.equals(transactionType)) {
            return amount.multiply(this.feePercentage).setScale(2, RoundingMode.HALF_EVEN);
        }
        return null;
    }
}