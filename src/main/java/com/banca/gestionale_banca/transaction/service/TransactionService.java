package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.transaction.dto.*;

public interface TransactionService {
    TransactionResponse eseguiVersamento(DepositRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse getTransazioneById(Long id);

}
