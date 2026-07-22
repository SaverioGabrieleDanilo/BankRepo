package com.banca.gestionale_banca.account.controller;

import java.util.List;
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
import lombok.RequiredArgsConstructor;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.account.dto.AccountStatusRequest;
import com.banca.gestionale_banca.account.dto.ApproveAccountRequest;
import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
import com.banca.gestionale_banca.account.dto.BankAccountStatsResponse;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.service.TransactionService;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final AccountLimitsService accountLimitsService;
    private final TransactionService transactionService;
    private final AuthorizationFacade authorizationFacade;
    private final AuditLogger auditLogger;

    @PostMapping("/opening")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<BankAccountResponse> openBankAccount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.openBankAccount(jwt.getSubject()));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<BankAccountResponse> approveBankAccount(@PathVariable Long id,
            @Valid @RequestBody ApproveAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        BankAccountResponse response = bankAccountService.approveBankAccount(id, request.isApproved());
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"),
                request.isApproved() ? "APPROVA_CONTO" : "RIFIUTA_CONTO", "conto", id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<BankAccountResponse> changeBankAccountStatus(@PathVariable Long id,
            @Valid @RequestBody AccountStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        BankAccountResponse response = bankAccountService.changeBankAccountStatus(id, request.getStatus());
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "CAMBIA_STATO_CONTO", "conto",
                id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/closure")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<BankAccountResponse> closeBankAccount(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        boolean isEmployee = authorizationFacade.isEmployee(authentication);
        BankAccountResponse response = bankAccountService.closeBankAccount(id, jwt.getSubject(), isEmployee);
        if (isEmployee) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "CHIUDI_CONTO", "conto", id);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Page<BankAccountAdminResponse>> listBankAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bankAccountService.listBankAccounts(pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<BankAccountStatsResponse> getStats() {
        return ResponseEntity.ok(bankAccountService.getStats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<BankAccountResponse> getBankAccount(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(id, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/user-accounts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BankAccountResponseDTO>> getMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(bankAccountService.getUserBankAccounts(jwt.getSubject()));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE')")
    public ResponseEntity<Page<TransactionAdminResponse>> getBankAccountTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getTransazioniByConto(id, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication), pageable));
    }

    @GetMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<AccountLimitsResponse> getBankAccountLimits(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        boolean isStaff = authorizationFacade.isEmployee(authentication) || authorizationFacade.isAdmin(authentication);
        return ResponseEntity.ok(accountLimitsService.getBankAccountLimits(id, jwt.getSubject(), isStaff));
    }

    @PutMapping("/{id}/limits")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<AccountLimitsResponse> setBankAccountLimits(@PathVariable Long id,
            @Valid @RequestBody AccountLimitsRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        boolean isStaff = authorizationFacade.isEmployee(authentication) || authorizationFacade.isAdmin(authentication);
        AccountLimitsResponse response = accountLimitsService.setBankAccountLimits(id, request, jwt.getSubject(), isStaff);
        if (isStaff) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "MODIFICA_LIMITI", "conto",
                    id);
        }
        return ResponseEntity.ok(response);
    }
}
