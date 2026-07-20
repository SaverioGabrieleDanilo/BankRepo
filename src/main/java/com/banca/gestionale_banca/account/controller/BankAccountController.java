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

import java.util.List;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.account.dto.ApproveAccountRequest;
import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.service.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conti")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final AccountLimitsService accountLimitsService;
    private final TransactionService transactionService;
    private final AuthorizationFacade authorizationFacade;
    private final AuditLogger auditLogger;

    @PostMapping("/apertura")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<BankAccountResponse> apriConto(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.apriConto(jwt.getSubject()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<BankAccountResponse> approvaConto(@PathVariable Long id, @RequestBody ApproveAccountRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        BankAccountResponse response = bankAccountService.approvaConto(id, request.isApproved());
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
                request.isApproved() ? "APPROVA_CONTO" : "RIFIUTA_CONTO", "conto", id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/chiusura")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<BankAccountResponse> chiudiConto(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        boolean isEmployee = authorizationFacade.isEmployee(authentication);
        BankAccountResponse response = bankAccountService.chiudiConto(id, jwt.getSubject(), isEmployee);
        if (isEmployee) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "CHIUDI_CONTO", "conto", id);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ResponseEntity<Page<BankAccountAdminResponse>> listaConti(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bankAccountService.listaConti(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<BankAccountResponse> getConto(@PathVariable Long id,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        return ResponseEntity.ok(bankAccountService.getContoById(id, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/user-accounts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BankAccountResponseDTO>> getMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.getUserBankAccounts(jwt.getSubject()));
    }

    @GetMapping("/{id}/transazioni")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<Page<TransactionAdminResponse>> getTransazioniConto(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getTransazioniByConto(id, jwt.getSubject(), authorizationFacade.isEmployee(authentication), pageable));
    }

    @GetMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<AccountLimitsResponse> getLimiti(@PathVariable Long id,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            Authentication authentication) {
        return ResponseEntity.ok(accountLimitsService.getLimiti(id, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PutMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<AccountLimitsResponse> impostaLimiti(@PathVariable Long id,
                                                                @Valid @RequestBody AccountLimitsRequest request,
                                                                @AuthenticationPrincipal Jwt jwt,
                                                                Authentication authentication) {
        boolean isStaff = authorizationFacade.isEmployee(authentication) || authorizationFacade.isAdmin(authentication);
        AccountLimitsResponse response = accountLimitsService.impostaLimiti(id, request, jwt.getSubject(), isStaff);
        if (isStaff) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "MODIFICA_LIMITI", "conto", id);
        }
        return ResponseEntity.ok(response);
    }
}
