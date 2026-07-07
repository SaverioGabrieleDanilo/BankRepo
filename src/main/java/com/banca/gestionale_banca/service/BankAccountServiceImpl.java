package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.exception.ConflictException;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.AccountStatus;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.repository.AccountStatusRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private static final String STATO_APERTURA = "IN_ATTESA";
    private static final String STATO_ATTIVO = "ATTIVO";
    private static final String STATO_RIFIUTATO = "RIFIUTATO";
    private static final String STATO_CHIUSO = "CHIUSO";

    private final BankAccountRepository bankAccountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final UserService userService;

    @Override
    @Transactional
    public BankAccountResponse apriConto(String keycloakId) {
        Utente utente = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        AccountStatus status = accountStatusRepository.findByName(STATO_APERTURA)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto IN_ATTESA non configurato"));

        LocalDateTime now = LocalDateTime.now();
        BankAccount account = new BankAccount();
        account.setIban(generaIban());
        account.setUser(utente);
        account.setBalance(BigDecimal.ZERO);
        account.setContableBalance(BigDecimal.ZERO);
        account.setStatus(status);
        account.setOpeningDate(now);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account = bankAccountRepository.save(account);

        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .status(status.getName())
                .openingDate(account.getOpeningDate())
                .build();
    }

    @Override
    @Transactional
    public BankAccountResponse approvaConto(Long accountId, boolean approva) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        if (!STATO_APERTURA.equals(account.getStatus().getName())) {
            throw new ConflictException("Il conto non è in attesa di approvazione");
        }

        AccountStatus nuovoStato = accountStatusRepository.findByName(approva ? STATO_ATTIVO : STATO_RIFIUTATO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto non configurato"));

        account.setStatus(nuovoStato);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .status(nuovoStato.getName())
                .openingDate(account.getOpeningDate())
                .build();
    }

    @Override
    @Transactional
    public BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        if (!isEmployee && !account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a chiudere questo conto");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException("Impossibile chiudere il conto: il saldo deve essere zero");
        }

        AccountStatus chiuso = accountStatusRepository.findByName(STATO_CHIUSO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto CHIUSO non configurato"));

        account.setStatus(chiuso);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .status(chiuso.getName())
                .openingDate(account.getOpeningDate())
                .build();
    }

    @Override
    public List<BankAccountAdminResponse> listaConti() {
        return bankAccountRepository.findAll().stream()
                .map(account -> BankAccountAdminResponse.builder()
                        .id(account.getId())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .status(account.getStatus().getName())
                        .ownerId(account.getUser().getId())
                        .ownerUsername(account.getUser().getUsername())
                        .ownerFullName(account.getUser().getFirstName() + " " + account.getUser().getLastName())
                        .build())
                .toList();
    }

    private String generaIban() {
        return "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
