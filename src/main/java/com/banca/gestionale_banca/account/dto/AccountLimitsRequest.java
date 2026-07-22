package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountLimitsRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite giornaliero deve essere positivo")
    @DecimalMax(value = "10000.00", message = "Il limite giornaliero non può superare 10000€")
    private BigDecimal dailyWithdrawalLimit;

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite per singola transazione deve essere positivo")
    @DecimalMax(value = "20000.00", message = "Il limite per singola transazione non può superare 20000€")
    private BigDecimal singleTransactionLimit;

    @NotNull
    @DecimalMin(value = "0.01", message = "Il limite mensile deve essere positivo")
    @DecimalMax(value = "50000.00", message = "Il limite mensile non può superare 50000€")
    private BigDecimal monthlyTransferLimit;
}
