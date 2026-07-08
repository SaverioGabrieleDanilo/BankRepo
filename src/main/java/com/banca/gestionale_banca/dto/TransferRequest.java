package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", message = "IBAN di origine non valido")
    private String sourceIban;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$", message = "IBAN di destinazione non valido")
    private String targetIban;

    @NotNull
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;
    
    private String description;
}
