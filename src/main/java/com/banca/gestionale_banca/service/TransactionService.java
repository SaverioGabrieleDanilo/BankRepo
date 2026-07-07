package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.dto.TransferRequest;
import com.banca.gestionale_banca.model.Transaction;

public interface TransactionService {
    TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isOperatore);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isOperatore);
    TransactionResponse eseguiBonifico(Long currentUserId, TransferRequest request);

}
