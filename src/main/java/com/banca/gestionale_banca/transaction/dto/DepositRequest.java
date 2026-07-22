package com.banca.gestionale_banca.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

import com.banca.gestionale_banca.validation.Iban;

@Getter
@Setter
public class DepositRequest {
    @NotBlank
    @Iban
    private String iban;

    @NotNull
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;

    private String description;

    @NotBlank
    private String depositType;

    @NotNull
    @Min(value = 1, message = "Il numero di banconote o assegni deve essere positivo ")
    private Integer itemsCount;
}
