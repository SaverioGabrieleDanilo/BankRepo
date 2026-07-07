package com.banca.gestionale_banca.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.banca.gestionale_banca.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.dto.ApproveAccountRequest;
import com.banca.gestionale_banca.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.dto.BankAccountResponse;
import com.banca.gestionale_banca.service.AccountLimitsService;
import com.banca.gestionale_banca.service.BankAccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conti")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final AccountLimitsService accountLimitsService;

    @PostMapping("/apertura")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<BankAccountResponse> apriConto(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.apriConto(jwt.getSubject()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<BankAccountResponse> approvaConto(@PathVariable Long id, @RequestBody ApproveAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.approvaConto(id, request.isApprova()));
    }

    @PostMapping("/{id}/chiusura")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BankAccountResponse> chiudiConto(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        return ResponseEntity.ok(bankAccountService.chiudiConto(id, jwt.getSubject(), isEmployee(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BankAccountAdminResponse>> listaConti() {
        return ResponseEntity.ok(bankAccountService.listaConti());
    }

    @GetMapping("/{id}/limits")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountLimitsResponse> getLimiti(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        return ResponseEntity.ok(accountLimitsService.getLimiti(id, jwt.getSubject(), isEmployee(authentication)));
    }

    @PutMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<AccountLimitsResponse> impostaLimiti(@PathVariable Long id, @RequestBody AccountLimitsRequest request) {
        return ResponseEntity.ok(accountLimitsService.impostaLimiti(id, request));
    }

    private boolean isEmployee(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }
}
