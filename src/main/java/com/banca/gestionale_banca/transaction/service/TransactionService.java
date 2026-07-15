package com.banca.gestionale_banca.transaction.service;

import java.util.List;

import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;

public interface TransactionService {
    TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse getTransazioneById(Long id);

    List<TransactionResponse> getUserTransactions(String user);

}
