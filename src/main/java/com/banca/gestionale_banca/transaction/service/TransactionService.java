package com.banca.gestionale_banca.transaction.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.InternarlTransferRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;


public interface TransactionService {
    TransactionResponse executeDeposit(DepositRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeWithdrawal(TransactionRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeTransfer(TransferRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse executeAccountTransfer(InternarlTransferRequest request, String keycloakId, boolean isEmployee);

    TransactionResponse getTransactionById(Long id);

    Page<TransactionAdminResponse> getPaginatedTransactions(Pageable pageable);

    Page<TransactionAdminResponse> getTransactionsByAccount(Long accountId, String keycloakId, boolean isEmployee,
            Pageable pageable);

    List<TransactionResponse> getUserTransactions(String keycloakId);

    List<TransactionResponse> getTransactionsByIban(String iban, String keycloakId, boolean isEmployee);
}