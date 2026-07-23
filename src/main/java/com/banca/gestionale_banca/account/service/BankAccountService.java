package com.banca.gestionale_banca.account.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountSummaryResponse;
import com.banca.gestionale_banca.account.dto.BankAccountStatsResponse;
import com.banca.gestionale_banca.account.model.BankAccount;

public interface BankAccountService {

    BankAccountResponse openBankAccount(String keycloakId, BigDecimal initialBalance);

    BankAccountResponse approveBankAccount(Long accountId, boolean approved);

    BankAccountResponse closeBankAccount(Long accountId, String keycloakId, boolean isEmployee);

    BankAccountResponse changeBankAccountStatus(Long accountId, String statusName);

    BankAccountResponse getBankAccountById(Long accountId, String keycloakId, boolean isEmployee);

    List<BankAccountSummaryResponse> getUserBankAccounts(String keycloakId);

    List<BankAccountSummaryResponse> getUserBankAccountsByUsername(String username);

    Page<BankAccountAdminResponse> listBankAccounts(Pageable pageable);

    /**
     * Conteggi aggregati per la dashboard admin (KPI), calcolati sull'intera
     * tabella.
     */
    BankAccountStatsResponse getStats();

    /**
     * API interna ad uso di altre feature (es. transaction): acquisisce il lock
     * pessimistico sul conto e lo restituisce nella stessa transazione del
     * chiamante.
     */
    BankAccount lockForUpdate(String iban, String messageIfNotFound);

    void assertActive(BankAccount account, String messageIfNotActive);

    BankAccount updateBalance(BankAccount account, BigDecimal newBalance);
}
