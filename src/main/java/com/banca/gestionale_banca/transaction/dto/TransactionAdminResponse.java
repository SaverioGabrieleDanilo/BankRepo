package com.banca.gestionale_banca.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionAdminResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String status;
    private String description;
    private LocalDateTime transactionDate;

    private String payerIban;
    private String payerUsername;
    private String payerFullName;

    private String payeeIban;
    private String payeeUsername;
    private String payeeFullName;
}
