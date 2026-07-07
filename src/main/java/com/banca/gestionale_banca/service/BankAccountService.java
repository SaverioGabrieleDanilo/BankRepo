package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.BankAccountResponse;

public interface BankAccountService {
    BankAccountResponse apriConto(String keycloakId);
}
