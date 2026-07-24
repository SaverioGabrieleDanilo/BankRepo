package com.banca.gestionale_banca.user.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest extends RegisterRequest {

    @NotBlank
    private String role;

    // Se true (e role == CUSTOMER), oltre all'utente viene aperto anche un conto
    // corrente PENDING per lui, cosi' compare subito in Account Approvals invece
    // di richiedere un secondo passaggio manuale di apertura conto.
    private boolean openAccount;

    @DecimalMin(value = "0.0", message = "Il saldo iniziale non può essere negativo")
    @DecimalMax(value = "1000000.00", message = "Il saldo iniziale non può superare 1000000€")
    private BigDecimal initialBalance;
}
