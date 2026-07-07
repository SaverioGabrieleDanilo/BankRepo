package com.banca.gestionale_banca.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {
    @NotNull(message = "Il conto ordinante è obbligatorio")
    private Long payerAccountId;

    @NotBlank(message = "L'IBAN del beneficiario è obbligatorio")
    private String payeeIban;

    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;

    private String description;
}
