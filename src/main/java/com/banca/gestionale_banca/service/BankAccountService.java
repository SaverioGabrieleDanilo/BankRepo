package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.dto.BankAccountResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BankAccountService {
    BankAccountResponse apriConto(String keycloakId);
    BankAccountResponse approvaConto(Long accountId, boolean approva);
    BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee);
    Page<BankAccountAdminResponse> listaConti(Pageable pageable);
}
