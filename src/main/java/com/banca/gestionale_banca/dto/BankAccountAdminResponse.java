package com.banca.gestionale_banca.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BankAccountAdminResponse {
    private Long id;
    private String iban;
    private BigDecimal balance;
    private String status;
    private Long ownerId;
    private String ownerUsername;
    private String ownerFullName;
}
