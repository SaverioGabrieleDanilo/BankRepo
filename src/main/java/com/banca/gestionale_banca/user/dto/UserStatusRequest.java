package com.banca.gestionale_banca.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserStatusRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|SUSPENDED|CLOSED", message = "Stato utente non valido")
    private String status;
}
