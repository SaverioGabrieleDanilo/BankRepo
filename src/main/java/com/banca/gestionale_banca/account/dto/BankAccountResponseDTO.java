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
    private Long userId;                  // Evita di esporre l'intero oggetto User
    private String statusName;            // Prende il nome dalla tabella dello stato
    private LocalDateTime openingDate;
    private LocalDateTime createdAt;
}
