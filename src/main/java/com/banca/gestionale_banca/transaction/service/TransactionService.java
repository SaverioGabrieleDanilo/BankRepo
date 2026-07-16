package com.banca.gestionale_banca.transaction.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;

public interface TransactionService {
    TransactionResponse eseguiVersamento(DepositRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse eseguiBonifico(TransferRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse eseguiGiroconto(GirocontoRequest request, String keycloakId, boolean isEmployee);
    TransactionResponse getTransazioneById(Long id);
    Page<TransactionAdminResponse> getTransazioniPaginate(Pageable pageable);
    Page<TransactionAdminResponse> getTransazioniByConto(Long accountId, String keycloakId, boolean isEmployee, Pageable pageable);
    List<TransactionResponse> getUserTransactions(String keycloakId);

}
