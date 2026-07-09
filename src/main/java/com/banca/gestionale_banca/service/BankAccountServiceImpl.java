package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.constants.StatiConto;
import com.banca.gestionale_banca.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.exception.ConflictException;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.AccountStatus;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.repository.AccountStatusRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;
import com.banca.gestionale_banca.security.AuthorizationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final UserService userService;
    private final AuthorizationFacade authorizationFacade;

    @Override
    @Transactional
    public BankAccountResponse apriConto(String keycloakId) {
        Utente utente = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        AccountStatus status = accountStatusRepository.findByName(StatiConto.IN_ATTESA)
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

        return toResponse(account);
    }

    @Override
    @Transactional
    public BankAccountResponse approvaConto(Long accountId, boolean approva) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        if (!StatiConto.IN_ATTESA.equals(account.getStatus().getName())) {
            throw new ConflictException("Il conto non è in attesa di approvazione");
        }

        AccountStatus nuovoStato = accountStatusRepository.findByName(approva ? StatiConto.ATTIVO : StatiConto.RIFIUTATO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto non configurato"));

        account.setStatus(nuovoStato);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Override
    @Transactional
    public BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account, keycloakId, isEmployee, "Non autorizzato a chiudere questo conto");

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException("Impossibile chiudere il conto: il saldo deve essere zero");
        }

        AccountStatus chiuso = accountStatusRepository.findByName(StatiConto.CHIUSO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto CHIUSO non configurato"));

        account.setStatus(chiuso);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Override
    public Page<BankAccountAdminResponse> listaConti(Pageable pageable) {
        return bankAccountRepository.findAllWithUser(pageable)
                .map(account -> BankAccountAdminResponse.builder()
                        .id(account.getId())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .status(account.getStatus().getName())
                        .ownerId(account.getUser().getId())
                        .ownerUsername(account.getUser().getUsername())
                        .ownerFullName(account.getUser().getFirstName() + " " + account.getUser().getLastName())
                        .build());
    }

    private String generaIban() {
        return "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .status(account.getStatus().getName())
                .openingDate(account.getOpeningDate())
                .build();
    }
}
