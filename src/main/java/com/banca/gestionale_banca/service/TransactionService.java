package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;

public interface TransactionService {
    TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isOperatore);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isOperatore);

}
