package com.banca.gestionale_banca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.service.BankAccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conti")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping("/apertura")
    @PreAuthorize("hasAnyRole('OPERATORE','CUSTOMER')")
    public ResponseEntity<BankAccountResponse> apriConto(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.apriConto(jwt.getSubject()));
    }
}
