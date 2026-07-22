package com.banca.gestionale_banca.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountStatusRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|PENDING|REJECTED|CLOSED", message = "Stato conto non valido")
    private String status;
}
