package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.InternarlTransferRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.repository.DepositTypeRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionTypeRepository;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.AccountStatus;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.model.DepositType;
import com.banca.gestionale_banca.transaction.model.Transaction;
import com.banca.gestionale_banca.transaction.model.TransactionStatus;
import com.banca.gestionale_banca.transaction.model.TransactionType;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.service.UserService;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private BankAccountService bankAccountService;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionTypeRepository transactionTypeRepository;
    @Mock
    private TransactionStatusRepository transactionStatusRepository;
    @Mock
    private DepositTypeRepository depositTypeRepository;
    @Mock
    private AccountLimitsService accountLimitsService;
    @Mock
    private UserService userService;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(bankAccountService, bankAccountRepository, transactionRepository,
                transactionTypeRepository, transactionStatusRepository, depositTypeRepository, accountLimitsService,
                new AuthorizationFacade(), userService);
        ReflectionTestUtils.setField(service, "feePercentage", new BigDecimal("0.02"));

        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionTypeRepository.findByName(any())).thenAnswer(invocation ->
                Optional.of(new TransactionType(invocation.getArgument(0))));
        lenient().when(depositTypeRepository.findByName(any())).thenAnswer(invocation ->
                Optional.of(new DepositType(invocation.getArgument(0))));
        lenient().when(transactionStatusRepository.findByName("ESEGUITA")).thenReturn(Optional.of(new TransactionStatus("ESEGUITA")));
        lenient().when(accountLimitsService.findLimits(any())).thenReturn(Optional.empty());
        lenient().doAnswer(invocation -> null).when(bankAccountService).assertActive(any(), any());
        lenient().when(bankAccountService.updateBalance(any(), any())).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            BigDecimal newBalance = invocation.getArgument(1);
            account.setBalance(newBalance);
            return account;
        });
    }

    private BankAccount contoAttivo(String iban, BigDecimal balance, String keycloakId) {
        User user = new User();
        user.setKeycloakId(keycloakId);

        AccountStatus active = new AccountStatus("ATTIVO");

        BankAccount account = new BankAccount();
        account.setIban(iban);
        account.setBalance(balance);
        account.setUser(user);
        account.setStatus(active);
        return account;
    }

    @Test
    void eseguiVersamento_incrementaIlSaldo() {
        BankAccount account = contoAttivo("IT60X0000000000000000000001", new BigDecimal("100.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(account.getIban()), any())).thenReturn(account);

        DepositRequest request = new DepositRequest();
        request.setIban(account.getIban());
        request.setAmount(new BigDecimal("50.00"));
        request.setDepositType("CASH");
        request.setItemsCount(1);

        TransactionResponse response = service.executeDeposit(request, "user-1", false);

        assertEquals(new BigDecimal("150.00"), response.getUpdatedBalance());
    }

    @Test
    void eseguiPrelievo_saldoInsufficiente_lanciaConflictException() {
        BankAccount account = contoAttivo("IT60X0000000000000000000002", new BigDecimal("50.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(account.getIban()), any())).thenReturn(account);

        TransactionRequest request = new TransactionRequest();
        request.setIban(account.getIban());
        request.setAmount(new BigDecimal("100.00"));

        assertThrows(ConflictException.class, () -> service.executeWithdrawal(request, "user-1", false));
    }

    @Test
    void eseguiPrelievo_contoInesistente_lanciaResourceNotFoundException() {
        when(bankAccountService.lockForUpdate(any(), any()))
                .thenThrow(new ResourceNotFoundException("Conto corrente non trovato"));

        TransactionRequest request = new TransactionRequest();
        request.setIban("IT60X0000000000000000000099");
        request.setAmount(new BigDecimal("10.00"));

        assertThrows(ResourceNotFoundException.class, () -> service.executeWithdrawal(request, "user-1", false));
    }

    @Test
    void eseguiPrelievo_superaLimiteSingolaTransazione_lanciaConflictException() {
        BankAccount account = contoAttivo("IT60X0000000000000000000003", new BigDecimal("1000.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(account.getIban()), any())).thenReturn(account);

        AccountLimitsResponse limits = AccountLimitsResponse.builder()
                .singleTransactionLimit(new BigDecimal("100.00"))
                .build();
        when(accountLimitsService.findLimits(any())).thenReturn(Optional.of(limits));

        TransactionRequest request = new TransactionRequest();
        request.setIban(account.getIban());
        request.setAmount(new BigDecimal("500.00"));

        assertThrows(ConflictException.class, () -> service.executeWithdrawal(request, "user-1", false));
    }

    @Test
    void eseguiGiroconto_intestatariDiversi_lanciaConflictException() {
        BankAccount source = contoAttivo("IT60X0000000000000000000004", new BigDecimal("200.00"), "user-1");
        source.getUser().setId(1L);

        BankAccount target = contoAttivo("IT60X0000000000000000000005", new BigDecimal("0.00"), "user-2");
        target.getUser().setId(2L);

        when(bankAccountService.lockForUpdate(eq(source.getIban()), any())).thenReturn(source);
        when(bankAccountService.lockForUpdate(eq(target.getIban()), any())).thenReturn(target);

        InternarlTransferRequest request = new InternarlTransferRequest();
        request.setSourceIban(source.getIban());
        request.setTargetIban(target.getIban());
        request.setAmount(new BigDecimal("50.00"));

        assertThrows(ConflictException.class, () -> service.executeAccountTransfer(request, "user-1", false));
    }

    @Test
    void eseguiVersamento_nonProprietario_lanciaForbidden() {
        BankAccount account = contoAttivo("IT60X0000000000000000000006", new BigDecimal("0.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(account.getIban()), any())).thenReturn(account);

        DepositRequest request = new DepositRequest();
        request.setIban(account.getIban());
        request.setAmount(new BigDecimal("10.00"));
        request.setDepositType("CASH");
        request.setItemsCount(1);

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.executeDeposit(request, "un-altro-utente", false));

        assertEquals(403, ex.getStatusCode().value());
    }

    private Transaction bonificoTraDueUtenti(User payer, User payee) {
        BankAccount payerAccount = new BankAccount();
        payerAccount.setIban("IT60X0000000000000000000010");
        payerAccount.setUser(payer);

        BankAccount payeeAccount = new BankAccount();
        payeeAccount.setIban("IT60X0000000000000000000011");
        payeeAccount.setUser(payee);

        Transaction tx = new Transaction();
        tx.setId(99L);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setType(new TransactionType(com.banca.gestionale_banca.transaction.constants.TransactionTypeEnum.TRANSFER));
        tx.setStatus(new TransactionStatus("ESEGUITA"));
        tx.setPayerAccount(payerAccount);
        tx.setPayeeAccount(payeeAccount);
        tx.setPayerUser(payer);
        tx.setPayeeUser(payee);
        tx.setTransactionDate(java.time.LocalDateTime.now());
        return tx;
    }

    @Test
    void getUserTransactions_comePagatore_mostraIlPropioIbanEUnaFee() {
        User payer = new User();
        payer.setId(1L);
        payer.setKeycloakId("payer-kc");
        User payee = new User();
        payee.setId(2L);
        payee.setKeycloakId("payee-kc");

        Transaction tx = bonificoTraDueUtenti(payer, payee);

        when(userService.findByKeycloakId("payer-kc")).thenReturn(Optional.of(payer));
        when(transactionRepository.findAllByUserId(1L)).thenReturn(List.of(tx));

        List<TransactionResponse> result = service.getUserTransactions("payer-kc");

        assertEquals(1, result.size());
        assertEquals("IT60X0000000000000000000010", result.get(0).getIban());
        assertEquals(new BigDecimal("2.00"), result.get(0).getFee());
    }

    @Test
    void getUserTransactions_comeBeneficiario_mostraIlPropioIbanSenzaFee() {
        User payer = new User();
        payer.setId(1L);
        payer.setKeycloakId("payer-kc");
        User payee = new User();
        payee.setId(2L);
        payee.setKeycloakId("payee-kc");

        Transaction tx = bonificoTraDueUtenti(payer, payee);

        when(userService.findByKeycloakId("payee-kc")).thenReturn(Optional.of(payee));
        when(transactionRepository.findAllByUserId(2L)).thenReturn(List.of(tx));

        List<TransactionResponse> result = service.getUserTransactions("payee-kc");

        assertEquals(1, result.size());
        assertEquals("IT60X0000000000000000000011", result.get(0).getIban());
        assertEquals(null, result.get(0).getFee());
    }
}
