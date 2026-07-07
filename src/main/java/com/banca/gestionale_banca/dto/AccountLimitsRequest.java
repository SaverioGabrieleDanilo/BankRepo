package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountLimitsRequest {

    @NotNull
    private BigDecimal dailyWithdrawalLimit;

    @NotNull
    private BigDecimal singleTransactionLimit;

    @NotNull
    private BigDecimal monthlyTransferLimit;
}
