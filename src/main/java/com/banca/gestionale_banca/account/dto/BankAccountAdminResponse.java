package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime openingDate;
}
