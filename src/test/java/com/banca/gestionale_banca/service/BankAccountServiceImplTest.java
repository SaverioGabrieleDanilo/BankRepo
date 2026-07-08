package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.exception.ConflictException;
import com.banca.gestionale_banca.model.AccountStatus;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.repository.AccountStatusRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;
import com.banca.gestionale_banca.security.AuthorizationFacade;

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

    private BankAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BankAccountServiceImpl(bankAccountRepository, accountStatusRepository,
                userService, new AuthorizationFacade());
    }

    @Test
    void apriConto_creaContoConSaldoZeroEStatoInAttesa() {
        Utente utente = new Utente();
        utente.setKeycloakId("user-1");
        when(userService.findByKeycloakId("user-1")).thenReturn(Optional.of(utente));
        when(accountStatusRepository.findByName("IN_ATTESA")).thenReturn(Optional.of(new AccountStatus("IN_ATTESA")));
        when(bankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = service.apriConto("user-1");

        assertEquals("IN_ATTESA", response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getBalance());
    }

    @Test
    void chiudiConto_saldoDiversoDaZero_lanciaConflictException() {
        Utente utente = new Utente();
        utente.setKeycloakId("user-1");

        BankAccount conto = new BankAccount();
        conto.setUser(utente);
        conto.setBalance(new BigDecimal("10.00"));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(conto));

        assertThrows(ConflictException.class, () -> service.chiudiConto(1L, "user-1", false));
    }

    @Test
    void chiudiConto_nonProprietario_lanciaForbidden() {
        Utente utente = new Utente();
        utente.setKeycloakId("user-1");

        BankAccount conto = new BankAccount();
        conto.setUser(utente);
        conto.setBalance(BigDecimal.ZERO);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(conto));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.chiudiConto(1L, "un-altro-utente", false));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void approvaConto_giaApprovato_lanciaConflictException() {
        BankAccount conto = new BankAccount();
        conto.setStatus(new AccountStatus("ATTIVO"));

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(conto));

        assertThrows(ConflictException.class, () -> service.approvaConto(1L, true));
    }
}
