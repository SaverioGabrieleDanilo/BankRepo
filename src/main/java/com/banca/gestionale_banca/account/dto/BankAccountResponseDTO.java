package com.banca.gestionale_banca.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountResponseDTO {
    private Long id;
    private String iban;
    private BigDecimal balance;
    private BigDecimal contableBalance;
    private Long userId;
    private String statusName;
    private LocalDateTime openingDate;
    private LocalDateTime createdAt;
}
