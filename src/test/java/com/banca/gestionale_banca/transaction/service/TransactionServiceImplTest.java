package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.transaction.repository.TransactionTypeRepository;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.AccountStatus;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.model.TransactionStatus;
import com.banca.gestionale_banca.transaction.model.TransactionType;
import com.banca.gestionale_banca.user.model.Utente;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionTypeRepository transactionTypeRepository;
    @Mock
    private TransactionStatusRepository transactionStatusRepository;
    @Mock
    private AccountLimitsService accountLimitsService;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(bankAccountService, transactionRepository,
                transactionTypeRepository, transactionStatusRepository, accountLimitsService,
                new AuthorizationFacade());

        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionTypeRepository.findByName(any())).thenAnswer(invocation ->
                Optional.of(new TransactionType(invocation.getArgument(0))));
        lenient().when(transactionStatusRepository.findByName("ESEGUITA")).thenReturn(Optional.of(new TransactionStatus("ESEGUITA")));
        lenient().when(accountLimitsService.findLimiti(any())).thenReturn(Optional.empty());
        lenient().doAnswer(invocation -> null).when(bankAccountService).assertActive(any(), any());
        lenient().when(bankAccountService.updateBalance(any(), any())).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            BigDecimal newBalance = invocation.getArgument(1);
            account.setBalance(newBalance);
            return account;
        });
    }

    private BankAccount contoAttivo(String iban, BigDecimal saldo, String keycloakId) {
        Utente utente = new Utente();
        utente.setKeycloakId(keycloakId);

        AccountStatus attivo = new AccountStatus("ATTIVO");

        BankAccount account = new BankAccount();
        account.setIban(iban);
        account.setBalance(saldo);
        account.setUser(utente);
        account.setStatus(attivo);
        return account;
    }

    @Test
    void eseguiVersamento_incrementaIlSaldo() {
        BankAccount conto = contoAttivo("IT60X0000000000000000000001", new BigDecimal("100.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(conto.getIban()), any())).thenReturn(conto);

        TransactionRequest request = new TransactionRequest();
        request.setIban(conto.getIban());
        request.setAmount(new BigDecimal("50.00"));

        TransactionResponse response = service.eseguiVersamento(request, "user-1", false);

        assertEquals(new BigDecimal("150.00"), response.getUpdatedBalance());
    }

    @Test
    void eseguiPrelievo_saldoInsufficiente_lanciaConflictException() {
        BankAccount conto = contoAttivo("IT60X0000000000000000000002", new BigDecimal("50.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(conto.getIban()), any())).thenReturn(conto);

        TransactionRequest request = new TransactionRequest();
        request.setIban(conto.getIban());
        request.setAmount(new BigDecimal("100.00"));

        assertThrows(ConflictException.class, () -> service.eseguiPrelievo(request, "user-1", false));
    }

    @Test
    void eseguiPrelievo_contoInesistente_lanciaResourceNotFoundException() {
        when(bankAccountService.lockForUpdate(any(), any()))
                .thenThrow(new ResourceNotFoundException("Conto corrente non trovato"));

        TransactionRequest request = new TransactionRequest();
        request.setIban("IT60X0000000000000000000099");
        request.setAmount(new BigDecimal("10.00"));

        assertThrows(ResourceNotFoundException.class, () -> service.eseguiPrelievo(request, "user-1", false));
    }

    @Test
    void eseguiPrelievo_superaLimiteSingolaTransazione_lanciaConflictException() {
        BankAccount conto = contoAttivo("IT60X0000000000000000000003", new BigDecimal("1000.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(conto.getIban()), any())).thenReturn(conto);

        AccountLimitsResponse limiti = AccountLimitsResponse.builder()
                .singleTransactionLimit(new BigDecimal("100.00"))
                .build();
        when(accountLimitsService.findLimiti(any())).thenReturn(Optional.of(limiti));

        TransactionRequest request = new TransactionRequest();
        request.setIban(conto.getIban());
        request.setAmount(new BigDecimal("500.00"));

        assertThrows(ConflictException.class, () -> service.eseguiPrelievo(request, "user-1", false));
    }

    @Test
    void eseguiGiroconto_intestatariDiversi_lanciaConflictException() {
        BankAccount origine = contoAttivo("IT60X0000000000000000000004", new BigDecimal("200.00"), "user-1");
        origine.getUser().setId(1L);

        BankAccount destinazione = contoAttivo("IT60X0000000000000000000005", new BigDecimal("0.00"), "user-2");
        destinazione.getUser().setId(2L);

        when(bankAccountService.lockForUpdate(eq(origine.getIban()), any())).thenReturn(origine);
        when(bankAccountService.lockForUpdate(eq(destinazione.getIban()), any())).thenReturn(destinazione);

        GirocontoRequest request = new GirocontoRequest();
        request.setSourceIban(origine.getIban());
        request.setTargetIban(destinazione.getIban());
        request.setAmount(new BigDecimal("50.00"));

        assertThrows(ConflictException.class, () -> service.eseguiGiroconto(request, "user-1", false));
    }

    @Test
    void eseguiVersamento_nonProprietario_lanciaForbidden() {
        BankAccount conto = contoAttivo("IT60X0000000000000000000006", new BigDecimal("0.00"), "user-1");
        when(bankAccountService.lockForUpdate(eq(conto.getIban()), any())).thenReturn(conto);

        TransactionRequest request = new TransactionRequest();
        request.setIban(conto.getIban());
        request.setAmount(new BigDecimal("10.00"));

        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.eseguiVersamento(request, "un-altro-utente", false));

        assertEquals(403, ex.getStatusCode().value());
    }
}
