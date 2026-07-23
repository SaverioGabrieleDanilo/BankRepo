package com.banca.gestionale_banca.rubrica.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContactRequest {

    @NotBlank(message = "Nome obbligatorio")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Cognome obbligatorio")
    @Size(max = 100)
    private String surname;

    @NotBlank(message = "IBAN obbligatorio")
    @Size(max = 34)
    private String iban;

    @Email(message = "Email non valida")
    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String note;
}
