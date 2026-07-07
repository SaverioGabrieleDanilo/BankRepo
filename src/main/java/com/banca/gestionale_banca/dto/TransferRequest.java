package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {
    private String sourceIban;
    private String targetIban;
    private BigDecimal amount;
    private String description;
}
