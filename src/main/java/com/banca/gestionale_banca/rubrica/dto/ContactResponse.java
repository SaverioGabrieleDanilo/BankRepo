package com.banca.gestionale_banca.rubrica.dto;

import java.time.LocalDateTime;

import com.banca.gestionale_banca.rubrica.model.Contact;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContactResponse {
    private Long id;
    private String name;
    private String surname;
    private String iban;
    private String email;
    private String note;
    private LocalDateTime createdAt;

    public static ContactResponse from(Contact c) {
        return ContactResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .surname(c.getSurname())
                .iban(c.getIban())
                .email(c.getEmail())
                .note(c.getNote())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
