package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenAccountRequest {

    @DecimalMin(value = "0.0", message = "Il saldo iniziale non può essere negativo")
    @DecimalMax(value = "1000000.00", message = "Il saldo iniziale non può superare 1000000€")
    private BigDecimal initialBalance;
}
