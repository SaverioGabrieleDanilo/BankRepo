package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.model.BankAccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface BankAccountService {
    BankAccountResponse apriConto(String keycloakId);
    BankAccountResponse approvaConto(Long accountId, boolean approva);
    BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee);
    Page<BankAccountAdminResponse> listaConti(Pageable pageable);

    /**
     * API interna ad uso di altre feature (es. transaction): acquisisce il lock
     * pessimistico sul conto e lo restituisce nella stessa transazione del chiamante.
     */
    BankAccount lockForUpdate(String iban, String messaggioSeNonTrovato);
    void assertActive(BankAccount account, String messaggioSeNonAttivo);
    BankAccount updateBalance(BankAccount account, BigDecimal newBalance);
}
