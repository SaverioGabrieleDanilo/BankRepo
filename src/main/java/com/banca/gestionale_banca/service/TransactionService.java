package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.GirocontoRequest;
import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.dto.TransferRequest;

public interface TransactionService {
    TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse getTransazioneById(Long id);

}
