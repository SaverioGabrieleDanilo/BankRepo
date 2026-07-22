package com.banca.gestionale_banca.transaction.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.banca.gestionale_banca.validation.Iban;

@Getter
@Setter
public class InternarlTransferRequest {
    @NotBlank
    @Iban(message = "IBAN di origine non valido")
    private String sourceIban;

    @NotBlank
    @Iban(message = "IBAN di destinazione non valido")
    private String targetIban;

    @NotNull
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;
    private String description;
}
