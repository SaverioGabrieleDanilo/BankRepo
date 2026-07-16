package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.constants.StatiTransazione;
import com.banca.gestionale_banca.transaction.constants.TipiTransazione;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
class TransactionServiceImpl implements TransactionService {

    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.02");

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
    public TransactionResponse eseguiVersamento(DepositRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato ad operare su questo conto");
        bankAccountService.assertActive(account, "Il conto corrente non è attivo");

        TransactionType type = trovaTipo(TipiTransazione.VERSAMENTO);
        TransactionStatus status = trovaStatoEseguita();
        DepositType depositType = trovaDepositType(request.getDepositType());

        account = bankAccountService.updateBalance(account, account.getBalance().add(request.getAmount()));

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status, depositType, request.getItemsCount());

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = trovaConto(request.getIban(), "Conto corrente non trovato");
        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato ad operare su questo conto");
        bankAccountService.assertActive(account, "Il conto corrente non è attivo");

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Saldo insufficiente per completare il prelievo");
        }

        verificaLimiti(account, request.getAmount(), true);

        TransactionType type = trovaTipo(TipiTransazione.PRELIEVO);
        TransactionStatus status = trovaStatoEseguita();

        account = bankAccountService.updateBalance(account, account.getBalance().subtract(request.getAmount()));

        Transaction transaction = registraMovimento(account, account, request.getAmount(),
                request.getDescription(), type, status, null, null);

        return toResponse(transaction, account.getIban(), account.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee) {
        if(request.getSourceIban().equals(request.getTargetIban())){
            throw new ConflictException("Il conto di origine e quello di destinazione non possono coincidere");
        }
        Map<String, BankAccount> lockedAccounts = lockAccountsInOrder(request.getSourceIban(), request.getTargetIban());
        BankAccount sourceAccount = lockedAccounts.get(request.getSourceIban());
        BankAccount targetAccount = lockedAccounts.get(request.getTargetIban());

        authorizationFacade.verificaProprietario(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato ad operare su questo conto");
        bankAccountService.assertActive(sourceAccount, "Il conto di origine non è attivo");
        bankAccountService.assertActive(targetAccount, "Il conto di destinazione non è attivo");

        BigDecimal fee = request.getAmount().multiply(FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new ConflictException("Saldo insufficiente. Il bonifico richiede: " + totalDebit + "€ compresa trattenuta");
        }

        verificaLimiti(sourceAccount, request.getAmount(), false);

        TransactionType type = trovaTipo(TipiTransazione.BONIFICO);
        TransactionStatus status = trovaStatoEseguita();

        sourceAccount = bankAccountService.updateBalance(sourceAccount, sourceAccount.getBalance().subtract(totalDebit));
        targetAccount = bankAccountService.updateBalance(targetAccount, targetAccount.getBalance().add(request.getAmount()));

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription() + " (Trattenuta: " + fee + "€)", type, status, null, null);

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

        authorizationFacade.verificaProprietario(sourceAccount.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato ad operare su questo conto");

        if (!sourceAccount.getUser().getId().equals(targetAccount.getUser().getId())) {
            throw new ConflictException("Il giroconto è consentito solo tra conti dello stesso intestatario");
        }

        bankAccountService.assertActive(sourceAccount, "Il conto di origine non è attivo");
        bankAccountService.assertActive(targetAccount, "Il conto di destinazione non è attivo");

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ConflictException("Saldo insufficiente per completare il giroconto");
        }

        verificaLimiti(sourceAccount, request.getAmount(), false);

        TransactionType type = trovaTipo(TipiTransazione.GIROCONTO);
        TransactionStatus status = trovaStatoEseguita();

        sourceAccount = bankAccountService.updateBalance(sourceAccount, sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount = bankAccountService.updateBalance(targetAccount, targetAccount.getBalance().add(request.getAmount()));

        Transaction transaction = registraMovimento(sourceAccount, targetAccount, request.getAmount(),
                request.getDescription(), type, status, null, null);

        return toResponse(transaction, sourceAccount.getIban(), sourceAccount.getBalance(), null);
    }

    @Override
    @Transactional
    public TransactionResponse getTransazioneById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transazione non trovata"));

        return toResponse(tx, tx.getPayerAccount().getIban(), tx.getPayerAccount().getBalance(), null);
    }

    @Override
    public Page<TransactionAdminResponse> getTransazioniPaginate(Pageable pageable) {
        return transactionRepository.findAllWithDetails(pageable).map(this::toAdminResponse);
    }

    @Override
    public Page<TransactionAdminResponse> getTransazioniByConto(Long accountId, String keycloakId, boolean isEmployee, Pageable pageable) {
        BankAccount account = bankAccountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato a consultare le transazioni di questo conto");

        return transactionRepository.findAllByAccountId(accountId, pageable).map(this::toAdminResponse);
    }

    @Override
    public List<TransactionResponse> getUserTransactions(String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return transactionRepository.findAllByUserId(user.getId()).stream()
                .map(tx -> {
                    boolean isPayer = tx.getPayerUser().getId().equals(user.getId());
                    String iban = isPayer ? tx.getPayerAccount().getIban() : tx.getPayeeAccount().getIban();

                    BigDecimal fee = null;
                    if (isPayer && TipiTransazione.BONIFICO.equals(tx.getType().getName())) {
                        fee = tx.getAmount().multiply(FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_EVEN);
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

    private BankAccount trovaConto(String iban, String messageIfNotFound) {
        return bankAccountService.lockForUpdate(iban, messageIfNotFound);
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

    /**
     * Un solo fetch di AccountLimits per operazione (prima veniva interrogato due volte:
     * una per il limite di singola transazione, una per quello giornaliero/mensile).
     */
    private void verificaLimiti(BankAccount account, BigDecimal amount, boolean isWithdrawal) {
        accountLimitsService.findLimiti(account.getId()).ifPresent(limits -> {
            if (amount.compareTo(limits.getSingleTransactionLimit()) > 0) {
                throw new ConflictException(
                        "Importo superiore al limite per singola transazione consentito (" + limits.getSingleTransactionLimit() + "€)");
            }
            if (isWithdrawal) {
                LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
                BigDecimal alreadyWithdrawn = transactionRepository.sumDailyWithdrawalsByAccount(account.getId(), dayStart, dayEnd);
                if (alreadyWithdrawn.add(amount).compareTo(limits.getDailyWithdrawalLimit()) > 0) {
                    throw new ConflictException("Limite giornaliero di prelievo superato (limite: "
                            + limits.getDailyWithdrawalLimit() + "€, già prelevato oggi: " + alreadyWithdrawn + "€)");
                }
            } else {
                LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);
                BigDecimal alreadyTransferred = transactionRepository.sumMonthlyTransfersByAccount(account.getId(), monthStart, monthEnd);
                if (alreadyTransferred.add(amount).compareTo(limits.getMonthlyTransferLimit()) > 0) {
                    throw new ConflictException("Limite mensile di trasferimento superato (limite: "
                            + limits.getMonthlyTransferLimit() + "€, già trasferito questo mese: " + alreadyTransferred + "€)");
                }
            }
        });
    }

    private TransactionType trovaTipo(String name) {
        return transactionTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo transazione " + name + " non configurato"));
    }

    private DepositType trovaDepositType(String name) {
        return depositTypeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo deposito " + name + " non configurato"));
    }

    private TransactionStatus trovaStatoEseguita() {
        return transactionStatusRepository.findByName(StatiTransazione.ESEGUITA)
                .orElseThrow(() -> new ResourceNotFoundException("Stato transazione ESEGUITA non configurato"));
    }

    private Transaction registraMovimento(BankAccount payer, BankAccount payee, BigDecimal amount,
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
