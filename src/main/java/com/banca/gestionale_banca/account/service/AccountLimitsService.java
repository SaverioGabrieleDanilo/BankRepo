package com.banca.gestionale_banca.account.service;

import java.util.Optional;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;

public interface AccountLimitsService {
    AccountLimitsResponse getBankAccountLimits(Long accountId, String keycloakId, boolean isEmployee);

    AccountLimitsResponse setBankAccountLimits(Long accountId, AccountLimitsRequest request, String keycloakId,
            boolean isEmployee);

    /**
     * API interna ad uso di altre feature (es. transaction): lettura delle soglie
     * configurate senza controllo di autorizzazione (già effettuato dal chiamante).
     */
    Optional<AccountLimitsResponse> findLimits(Long accountId);
}
