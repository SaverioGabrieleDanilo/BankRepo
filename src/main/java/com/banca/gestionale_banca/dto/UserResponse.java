package com.banca.gestionale_banca.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.banca.gestionale_banca.model.Utente;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String role;
    private String stato;
    private String registrationStatus;
    private LocalDateTime createdAt;

    public static UserResponse from(Utente u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .dateOfBirth(u.getDateOfBirth())
                .role(u.getRole().getName())
                .stato(u.getStatus().getName())
                .registrationStatus(u.getRegistrationStatus().getName())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
