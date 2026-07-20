package com.banca.gestionale_banca.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private Long transactionId;
    private String iban;
    private String type;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal updatedBalance;
    private String status;
    private LocalDateTime timestamp;
    private String depositType;
    private Integer itemsCount;
}
