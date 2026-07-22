package com.banca.gestionale_banca.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    @NotBlank
    @Pattern(regexp = "ATTIVO|SOSPESO|CHIUSO", message = "Stato utente non valido")
    private String status;
}
