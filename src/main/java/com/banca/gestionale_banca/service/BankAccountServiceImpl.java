package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.AccountStatus;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.repository.AccountStatusRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private static final String STATO_APERTURA = "ATTIVO";

    private final BankAccountRepository bankAccountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final UserService userService;

    @Override
    @Transactional
    public BankAccountResponse apriConto(String keycloakId) {
        Utente utente = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        AccountStatus status = accountStatusRepository.findByName(STATO_APERTURA)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto ATTIVO non configurato"));

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
        bankAccountRepository.save(account);

        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .status(status.getName())
                .openingDate(account.getOpeningDate())
                .build();
    }

    private String generaIban() {
        return "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
