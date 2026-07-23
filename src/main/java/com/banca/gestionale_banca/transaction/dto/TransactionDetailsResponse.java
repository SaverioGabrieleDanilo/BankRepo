package com.banca.gestionale_banca.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDetailsResponse {

    private String id;
    private BigDecimal amount;
    private LocalDateTime date;
    private String cause;
    private PartyDto sender;
    private PartyDto recipient;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PartyDto {
        private String firstName;
        private String lastName;
        private String iban;
    }
}
