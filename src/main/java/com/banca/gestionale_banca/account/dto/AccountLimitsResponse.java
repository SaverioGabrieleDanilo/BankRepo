package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountLimitsResponse {
    private Long accountId;
    private BigDecimal dailyWithdrawalLimit;
    private BigDecimal singleTransactionLimit;
    private BigDecimal monthlyTransferLimit;
    private BigDecimal dailyWithdrawalUsed;
    private BigDecimal monthlyTransferUsed;
    private LocalDateTime updatedAt;
}
