package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankAccountResponse {
    private Long id;
    private String iban;
    private BigDecimal balance;
    private String status;
    private LocalDateTime openingDate;
}
