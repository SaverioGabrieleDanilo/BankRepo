package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import com.banca.gestionale_banca.validation.Iban;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequest {

    @NotBlank
    @Iban
    private String iban;
    @NotNull
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;
    private String description;
}
