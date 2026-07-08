package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountLimitsRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite giornaliero deve essere positivo")
    private BigDecimal dailyWithdrawalLimit;

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite per singola transazione deve essere positivo")
    private BigDecimal singleTransactionLimit;

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite mensile deve essere positivo")
    private BigDecimal monthlyTransferLimit;
}
