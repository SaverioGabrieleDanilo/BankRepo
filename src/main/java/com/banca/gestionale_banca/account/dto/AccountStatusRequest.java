package com.banca.gestionale_banca.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountStatusRequest {

    @NotBlank
    private String status;
}
