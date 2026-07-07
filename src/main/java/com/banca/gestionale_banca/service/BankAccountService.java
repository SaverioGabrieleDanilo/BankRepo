package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.dto.BankAccountResponse;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse apriConto(String keycloakId);
    BankAccountResponse approvaConto(Long accountId, boolean approva);
    BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee);
    List<BankAccountAdminResponse> listaConti();
}
