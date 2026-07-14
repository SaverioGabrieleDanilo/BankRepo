package com.banca.gestionale_banca.account.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.account.dto.ApproveAccountRequest;
import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conti")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final AccountLimitsService accountLimitsService;
    private final AuthorizationFacade authorizationFacade;

    @PostMapping("/apertura")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<BankAccountResponse> apriConto(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.apriConto(jwt.getSubject()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<BankAccountResponse> approvaConto(@PathVariable Long id, @RequestBody ApproveAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.approvaConto(id, request.isApproved()));
    }

    @PostMapping("/{id}/chiusura")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BankAccountResponse> chiudiConto(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        return ResponseEntity.ok(bankAccountService.chiudiConto(id, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BankAccountAdminResponse>> listaConti(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bankAccountService.listaConti(pageable));
    }

    @GetMapping("/{id}/limits")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AccountLimitsResponse> getLimiti(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        return ResponseEntity.ok(accountLimitsService.getLimiti(id, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PutMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<AccountLimitsResponse> impostaLimiti(@PathVariable Long id, @Valid @RequestBody AccountLimitsRequest request) {
        return ResponseEntity.ok(accountLimitsService.impostaLimiti(id, request));
    }
}
