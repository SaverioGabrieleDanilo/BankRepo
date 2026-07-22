package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.repository.AccountStatusRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.account.model.AccountStatus;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private AccountStatusRepository accountStatusRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    private BankAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BankAccountServiceImpl(bankAccountRepository, accountStatusRepository,
                userService, new AuthorizationFacade(), userRepository);
    }

    @Test
    void apriConto_creaContoConSaldoZeroEStatoInAttesa() {
        User user = new User();
        user.setKeycloakId("user-1");
        when(userService.findByKeycloakId("user-1")).thenReturn(Optional.of(user));
        when(accountStatusRepository.findByName("IN_ATTESA")).thenReturn(Optional.of(new AccountStatus("IN_ATTESA")));
        when(bankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = service.apriConto("user-1");

        assertEquals("IN_ATTESA", response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getBalance());
    }

    @Test
    void chiudiConto_saldoDiversoDaZero_lanciaConflictException() {
        User user = new User();
        user.setKeycloakId("user-1");

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setBalance(new BigDecimal("10.00"));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(ConflictException.class, () -> service.chiudiConto(1L, "user-1", false));
    }

    @Test
    void chiudiConto_nonProprietario_lanciaForbidden() {
        User user = new User();
        user.setKeycloakId("user-1");

        BankAccount account = new BankAccount();
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.chiudiConto(1L, "un-altro-user", false));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void approvaConto_giaApprovato_lanciaConflictException() {
        BankAccount account = new BankAccount();
        account.setStatus(new AccountStatus("ATTIVO"));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(ConflictException.class, () -> service.approvaConto(1L, true));
    }

    @Test
    void changeAccountStatus_chiusuraConSaldoDiversoDaZero_lanciaConflictException() {
        BankAccount account = new BankAccount();
        account.setStatus(new AccountStatus("ATTIVO"));
        account.setBalance(new BigDecimal("10.00"));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountStatusRepository.findByName("CHIUSO")).thenReturn(Optional.of(new AccountStatus("CHIUSO")));

        assertThrows(ConflictException.class, () -> service.changeAccountStatus(1L, "CHIUSO"));
    }

    @Test
    void changeAccountStatus_chiusuraConSaldoZero_aggiornaStato() {
        BankAccount account = new BankAccount();
        account.setStatus(new AccountStatus("ATTIVO"));
        account.setBalance(BigDecimal.ZERO);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountStatusRepository.findByName("CHIUSO")).thenReturn(Optional.of(new AccountStatus("CHIUSO")));
        when(bankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = service.changeAccountStatus(1L, "CHIUSO");

        assertEquals("CHIUSO", response.getStatus());
    }

    @Test
    void changeAccountStatus_riattivazione_nonRichiedeSaldoZero() {
        BankAccount account = new BankAccount();
        account.setStatus(new AccountStatus("CHIUSO"));
        account.setBalance(BigDecimal.ZERO);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountStatusRepository.findByName("ATTIVO")).thenReturn(Optional.of(new AccountStatus("ATTIVO")));
        when(bankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = service.changeAccountStatus(1L, "ATTIVO");

        assertEquals("ATTIVO", response.getStatus());
    }
}
