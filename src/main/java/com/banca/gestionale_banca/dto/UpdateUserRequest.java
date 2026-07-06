package com.banca.gestionale_banca.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class UpdateUserRequest {

    @Email
    private String email;

    private String ruolo;
}
