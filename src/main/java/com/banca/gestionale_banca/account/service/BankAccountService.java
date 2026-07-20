package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
import com.banca.gestionale_banca.account.dto.BankAccountStatsResponse;
import com.banca.gestionale_banca.account.model.BankAccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface BankAccountService {
    BankAccountResponse apriConto(String keycloakId);
    BankAccountResponse approvaConto(Long accountId, boolean approved);
    BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee);
    BankAccountResponse getContoById(Long accountId, String keycloakId, boolean isEmployee);
    List<BankAccountResponseDTO> getUserBankAccounts(String keycloakId);
    Page<BankAccountAdminResponse> listaConti(Pageable pageable);

    /** Conteggi aggregati per la dashboard admin (KPI), calcolati sull'intera tabella. */
    BankAccountStatsResponse getStats();

    /**
     * API interna ad uso di altre feature (es. transaction): acquisisce il lock
     * pessimistico sul conto e lo restituisce nella stessa transazione del chiamante.
     */
    BankAccount lockForUpdate(String iban, String messageIfNotFound);
    void assertActive(BankAccount account, String messageIfNotActive);
    BankAccount updateBalance(BankAccount account, BigDecimal newBalance);
}
