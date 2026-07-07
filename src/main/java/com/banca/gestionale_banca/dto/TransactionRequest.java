package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionRequest {
    private String iban;
    private BigDecimal amount;
    private String description;
}
