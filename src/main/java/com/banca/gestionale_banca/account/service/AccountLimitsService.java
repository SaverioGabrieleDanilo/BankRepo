package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;

import java.util.Optional;

public interface AccountLimitsService {
    AccountLimitsResponse getLimiti(Long accountId, String keycloakId, boolean isEmployee);
    AccountLimitsResponse impostaLimiti(Long accountId, AccountLimitsRequest request);

    /**
     * API interna ad uso di altre feature (es. transaction): lettura delle soglie
     * configurate senza controllo di autorizzazione (già effettuato dal chiamante).
     */
    Optional<AccountLimitsResponse> findLimiti(Long accountId);
}
