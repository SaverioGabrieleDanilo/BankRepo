package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.InternarlTransferRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionDetailsResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.constants.TransactionStatusEnum;
import com.banca.gestionale_banca.transaction.constants.TransactionTypeEnum;
import com.banca.gestionale_banca.transaction.model.DepositType;
import com.banca.gestionale_banca.transaction.model.Transaction;
import com.banca.gestionale_banca.transaction.model.TransactionStatus;
import com.banca.gestionale_banca.transaction.model.TransactionType;
import com.banca.gestionale_banca.transaction.repository.DepositTypeRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionTypeRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Value("${app.transaction.fee-percentage}")
    private BigDecimal feePercentage;

    private final BankAccountService bankAccountService;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final DepositTypeRepository depositTypeRepository;
    private final AccountLimitsService accountLimitsService;
    private final AuthorizationFacade authorizationFacade;
    private final UserService userService;

    @Override
    @Transactional
    public TransactionResponse executeDeposit(DepositRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = findAccount(request.getIban(), "Bank account not found");
        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Unauthorized to operate on this account");
        bankAccountService.assertActive(account, "The bank account is not active");

        TransactionType type = findType(TransactionTypeEnum.DEPOSIT);
        TransactionStatus status = findExecutedStatus();
        DepositType depositType = findDepositType(request.getDepositType());

        account = bankAccountService.updateBalance(account, account.getBalance().add(request.getAmount()));

        Transaction transaction = registerMovement(account, account, request.getAmount(),
                request.getDescription(), type, status, depositType, request.getItemsCount());

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse executeWithdrawal(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = findAccount(request.getIban(), "Bank account not found");
        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Unauthorized to operate on this account");
        bankAccountService.assertActive(account, "The bank account is not active");

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Insufficient balance to complete the withdrawal");
        }

        verifyLimits(account, request.getAmount(), true);

        TransactionType type = findType(TransactionTypeEnum.WITHDRAWAL);
        TransactionStatus status = findExecutedStatus();

        account = bankAccountService.updateBalance(account, account.getBalance().subtract(request.getAmount()));

        Transaction transaction = registerMovement(account, account, request.getAmount(),
                request.getDescription(), type, status, null, null);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse executeTransfer(TransferRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Source and destination accounts cannot be the same");
        }
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verifyOwnership(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee, "Unauthorized to operate on this account");
        bankAccountService.assertActive(sourceAccount, "Source account is not active");
        bankAccountService.assertActive(targetAccount, "Destination account is not active");

        BigDecimal fee = request.getAmount().multiply(feePercentage).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new ConflictException("Insufficient balance. Transfer requires: " + totalDebit + "€ including fee");
        }

        verifyLimits(sourceAccount, request.getAmount(), false);

        TransactionType type = findType(TransactionTypeEnum.TRANSFER);
        TransactionStatus status = findExecutedStatus();

        sourceAccount = bankAccountService.updateBalance(sourceAccount, sourceAccount.getBalance().subtract(totalDebit));
        targetAccount = bankAccountService.updateBalance(targetAccount, targetAccount.getBalance().add(request.getAmount()));

        Transaction transaction = registerMovement(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription() + " (Fee: " + fee + "€)", type, status, null, null);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), fee);
    }

    @Override
    @Transactional
    public TransactionResponse executeAccountTransfer(InternarlTransferRequest request, String keycloakId, boolean isEmployee) {
        if (request.getSourceIban().equals(request.getTargetIban())) {
            throw new ConflictException("Source and destination accounts cannot be the same");
        }
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verifyOwnership(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee, "Unauthorized to operate on this account");

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new ConflictException("Internal account transfers are only allowed between accounts of the same owner");
        }

        bankAccountService.assertActive(sourceAccount, "Source account is not active");
        bankAccountService.assertActive(targetAccount, "Destination account is not active");

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Insufficient balance to complete the internal transfer");
        }

        verifyLimits(sourceAccount, request.getAmount(), false);

        TransactionType type = findType(TransactionTypeEnum.INTERNAL_TRANSFER);
        TransactionStatus status = findExecutedStatus();

        sourceAccount = bankAccountService.updateBalance(sourceAccount, sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount = bankAccountService.updateBalance(targetAccount, targetAccount.getBalance().add(request.getAmount()));

        Transaction transaction = registerMovement(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription(), type, status, null, null);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), null);
    }

@Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        return toResponse(tx, tx.getPayerAccount().getIban(), tx.getPayerAccount().getBalance(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailsResponse getTransactionDetails(Long id, String keycloakId, boolean isEmployee) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User payer = tx.getPayerUser();
        User payee = tx.getPayeeUser();
        boolean isParty = (payer != null && payer.getKeycloakId().equals(keycloakId))
                || (payee != null && payee.getKeycloakId().equals(keycloakId));
        if (!isEmployee && !isParty) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to view this transaction");
        }

        BankAccount payerAccount = tx.getPayerAccount();
        BankAccount payeeAccount = tx.getPayeeAccount();

        return TransactionDetailsResponse.builder()
                .id(tx.getId().toString())
                .amount(tx.getAmount())
                .date(tx.getTransactionDate())
                .cause(tx.getDescription())
                .sender(TransactionDetailsResponse.PartyDto.builder()
                        .firstName(payer != null ? payer.getFirstName() : null)
                        .lastName(payer != null ? payer.getLastName() : null)
                        .iban(payerAccount != null ? payerAccount.getIban() : null)
                        .build())
                .recipient(TransactionDetailsResponse.PartyDto.builder()
                        .firstName(payee != null ? payee.getFirstName() : null)
                        .lastName(payee != null ? payee.getLastName() : null)
                        .iban(payeeAccount != null ? payeeAccount.getIban() : null)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionAdminResponse> getPaginatedTransactions(Pageable pageable) {
        return transactionRepository.findAllWithDetails(pageable).map(this::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionAdminResponse> getTransactionsByAccount(Long accountId, String keycloakId, boolean isEmployee, Pageable pageable) {
        BankAccount account = bankAccountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Unauthorized to view transactions for this account");

        return transactionRepository.findAllByAccountId(accountId, pageable).map(this::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return transactionRepository.findAllByUserId(user.getId()).stream()
                .map(tx -> {
                    boolean isPayer = tx.getPayerUser().getId().equals(user.getId());
                    String iban = isPayer ? tx.getPayerAccount().getIban() : tx.getPayeeAccount().getIban();

                    BigDecimal fee = null;
                    if (isPayer && TransactionTypeEnum.TRANSFER.equals(tx.getType().getName())) {
                        fee = tx.getAmount().multiply(feePercentage).setScale(2, RoundingMode.HALF_EVEN);
                    }

                    return toResponse(tx, iban, null, fee);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByIban(String iban, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findByIbanWithUser(iban)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Unauthorized to view transactions for this account");

        return transactionRepository.findAllByIban(iban).stream()
                .map(tx -> {
                    boolean isPayer = tx.getPayerAccount().getIban().equals(iban);

                    BigDecimal fee = null;
                    if (isPayer && TransactionTypeEnum.TRANSFER.equals(tx.getType().getName())) {
                        fee = tx.getAmount().multiply(feePercentage).setScale(2, RoundingMode.HALF_EVEN);
                    }

                    return toResponse(tx, iban, null, fee);
                })
                .toList();
    }

    private TransactionAdminResponse toAdminResponse(Transaction tx) {
        return TransactionAdminResponse.builder()
                .id(tx.getId())
                .type(tx.getType().getName())
                .amount(tx.getAmount())
                .status(tx.getStatus().getName())
                .description(tx.getDescription())
                .transactionDate(tx.getTransactionDate())
                .payerIban(tx.getPayerAccount().getIban())
                .payerUsername(tx.getPayerUser().getUsername())
                .payerFullName(tx.getPayerUser().getFirstName() + " " + tx.getPayerUser().getLastName())
                .payeeIban(tx.getPayeeAccount().getIban())
                .payeeUsername(tx.getPayeeUser().getUsername())
                .payeeFullName(tx.getPayeeUser().getFirstName() + " " + tx.getPayeeUser().getLastName())
                .build();
    }

    private BankAccount findAccount(String iban, String messageIfNotFound) {
        return bankAccountService.lockForUpdate(iban, messageIfNotFound);
    }

    /**
     * Acquires a pessimistic lock on both accounts in a deterministic order
     * (by IBAN) to prevent deadlocks when two cross-transfers (A->B and B->A)
     * are executed concurrently.
     */
    private Map<String, BankAccount> lockAccountsInOrder(String sourceIban, String targetIban) {
        List<String> ordered = sourceIban.compareTo(targetIban) <= 0
                ? List.of(sourceIban, targetIban)
                : List.of(targetIban, sourceIban);

        Map<String, BankAccount> locked = new LinkedHashMap<>();
        for (String iban : ordered) {
            String label = iban.equals(sourceIban) ? "source" : "destination";
            locked.put(iban, findAccount(iban, "Bank account " + label + " not found"));
        }
        return locked;
    }

    /**
     * Single AccountLimits fetch per operation (previously queried twice:
     * once for single transaction limit, once for daily/monthly limit).
     */
    private void verifyLimits(BankAccount account, BigDecimal amount, boolean isWithdrawal) {
        accountLimitsService.findLimits(account.getId()).ifPresent(limits -> {
            if (amount.compareTo(limits.getSingleTransactionLimit()) > 0) {
                throw new ConflictException(
                        "Amount exceeds the allowed single transaction limit (" + limits.getSingleTransactionLimit() + "€)");
            }
            if (isWithdrawal) {
                LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
                BigDecimal alreadyWithdrawn = transactionRepository.sumDailyWithdrawalsByAccount(account.getId(), dayStart, dayEnd);
                if (alreadyWithdrawn.add(amount).compareTo(limits.getDailyWithdrawalLimit()) > 0) {
                    throw new ConflictException("Daily withdrawal limit exceeded (limit: "
                            + limits.getDailyWithdrawalLimit() + "€, already withdrawn today: " + alreadyWithdrawn + "€)");
                }
            } else {
                LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
                BigDecimal alreadyTransferred = transactionRepository.sumMonthlyTransfersByAccount(account.getId(), monthStart, monthEnd);
                if (alreadyTransferred.add(amount).compareTo(limits.getMonthlyTransferLimit()) > 0) {
                    throw new ConflictException("Monthly transfer limit exceeded (limit: "
                            + limits.getMonthlyTransferLimit() + "€, already transferred this month: " + alreadyTransferred + "€)");
                }
            }
        });
    }

    private TransactionType findType(String name) {
        return transactionTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction type " + name + " not configured"));
    }

    private DepositType findDepositType(String name) {
        return depositTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit type " + name + " not configured"));
    }

    private TransactionStatus findExecutedStatus() {
        return transactionStatusRepository.findByName(TransactionStatusEnum.EXECUTED)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction status EXECUTED not configured"));
    }

    private Transaction registerMovement(BankAccount payer, BankAccount payee, BigDecimal amount,
                                        String description, TransactionType type, TransactionStatus status,
                                        DepositType depositType, Integer itemsCount) {
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
        transaction.setDepositType(depositType);
        transaction.setItemsCount(itemsCount);
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
                .depositType(tx.getDepositType() != null ? tx.getDepositType().getName() : null)
                .itemsCount(tx.getItemsCount())
                .build();
    }
}