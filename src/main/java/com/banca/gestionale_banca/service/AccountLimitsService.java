package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.dto.AccountLimitsResponse;

public interface AccountLimitsService {
    AccountLimitsResponse getLimiti(Long accountId, String keycloakId, boolean isEmployee);
    AccountLimitsResponse impostaLimiti(Long accountId, AccountLimitsRequest request);
}
