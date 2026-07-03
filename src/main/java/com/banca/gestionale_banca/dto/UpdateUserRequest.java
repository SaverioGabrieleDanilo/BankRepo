package com.banca.gestionale_banca.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class UpdateUserRequest {
    private String email;
    private String ruolo;
    // getter e setter
}