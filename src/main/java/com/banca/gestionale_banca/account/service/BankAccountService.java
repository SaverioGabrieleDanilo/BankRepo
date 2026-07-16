package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
import com.banca.gestionale_banca.account.model.BankAccount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BankAccountService {

    BankAccountResponse apriConto(String keycloakId);

    BankAccountResponse approvaConto(Long accountId, boolean approved);

    BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee);

    Page<BankAccountAdminResponse> listaConti(Pageable pageable);

    BankAccount lockForUpdate(String iban, String messageIfNotFound);

    List<BankAccountResponseDTO> getUserBankAccountsByUsername(String username);

}
