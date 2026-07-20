package com.banca.gestionale_banca.transaction.service;

import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse executeDeposit(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeWithdrawal(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeWireTransfer(TransferRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeInternalTransfer(GirocontoRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse getTransactionById(Long id);

    List<TransactionResponse> getUserTransactions(String username);

    List<TransactionResponse> getBankAccountTransactions(String iban);

}