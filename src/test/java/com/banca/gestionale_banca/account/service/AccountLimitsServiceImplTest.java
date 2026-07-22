package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.account.model.AccountLimits;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;
import com.banca.gestionale_banca.user.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLimitsServiceImplTest {

    @Mock
    private AccountLimitsRepository accountLimitsRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private AccountLimitsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountLimitsServiceImpl(accountLimitsRepository, bankAccountRepository, transactionRepository,
                new AuthorizationFacade());

        lenient().when(transactionRepository.sumDailyWithdrawalsByAccount(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(transactionRepository.sumMonthlyTransfersByAccount(any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private BankAccount contoDi(String keycloakId) {
        User user = new User();
        user.setKeycloakId(keycloakId);

        BankAccount account = new BankAccount();
        account.setId(1L);
        account.setUser(user);
        return account;
    }

    private AccountLimits limitiPer(BankAccount account) {
        AccountLimits limits = new AccountLimits();
        limits.setAccount(account);
        limits.setDailyWithdrawalLimit(new BigDecimal("1000.00"));
        limits.setSingleTransactionLimit(new BigDecimal("500.00"));
        limits.setMonthlyTransferLimit(new BigDecimal("5000.00"));
        limits.setUpdatedAt(LocalDateTime.now());
        return limits;
    }

    @Test
    void getLimiti_proprietario_restituisceDisponibilitaResidua() {
        BankAccount account = contoDi("user-1");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findByAccountId(1L)).thenReturn(Optional.of(limitiPer(account)));
        when(transactionRepository.sumDailyWithdrawalsByAccount(eq(1L), any(), any())).thenReturn(new BigDecimal("100.00"));

        AccountLimitsResponse response = service.getBankAccountLimits(1L, "user-1", false);

        assertEquals(new BigDecimal("1000.00"), response.getDailyWithdrawalLimit());
        assertEquals(new BigDecimal("100.00"), response.getDailyWithdrawalUsed());
    }

    @Test
    void getLimiti_contoInesistente_lanciaResourceNotFoundException() {
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBankAccountLimits(1L, "user-1", false));
    }

    @Test
    void getLimiti_nonProprietarioENonDipendente_lanciaForbidden() {
        BankAccount account = contoDi("user-1");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getBankAccountLimits(1L, "un-altro-utente", false));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getLimiti_limitiNonConfigurati_lanciaResourceNotFoundException() {
        BankAccount account = contoDi("user-1");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findByAccountId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getBankAccountLimits(1L, "user-1", false));
    }

    @Test
    void impostaLimiti_nessunLimitePreesistente_creaNuovaRiga() {
        BankAccount account = contoDi("user-1");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findByAccountId(1L)).thenReturn(Optional.empty());
        when(accountLimitsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(new BigDecimal("2000.00"));
        request.setSingleTransactionLimit(new BigDecimal("1000.00"));
        request.setMonthlyTransferLimit(new BigDecimal("8000.00"));

        AccountLimitsResponse response = service.setBankAccountLimits(1L, request, "user-1", false);

        assertEquals(new BigDecimal("2000.00"), response.getDailyWithdrawalLimit());
        verify(accountLimitsRepository).save(any());
    }

    @Test
    void impostaLimiti_nonProprietarioENonDipendente_lanciaForbidden() {
        BankAccount account = contoDi("user-1");
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(new BigDecimal("2000.00"));
        request.setSingleTransactionLimit(new BigDecimal("1000.00"));
        request.setMonthlyTransferLimit(new BigDecimal("8000.00"));

        assertThrows(ResponseStatusException.class, () -> service.setBankAccountLimits(1L, request, "un-altro-utente", false));
    }

    @Test
    void findLimiti_presente_restituisceOptionalConValori() {
        BankAccount account = contoDi("user-1");
        when(accountLimitsRepository.findByAccountId(1L)).thenReturn(Optional.of(limitiPer(account)));

        Optional<AccountLimitsResponse> result = service.findLimits(1L);

        assertEquals(new BigDecimal("500.00"), result.orElseThrow().getSingleTransactionLimit());
    }

    @Test
    void findLimiti_assente_restituisceOptionalVuoto() {
        when(accountLimitsRepository.findByAccountId(1L)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findLimits(1L));
    }
}
