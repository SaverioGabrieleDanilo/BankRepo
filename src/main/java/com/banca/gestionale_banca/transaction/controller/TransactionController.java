package com.banca.gestionale_banca.transaction.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.transaction.service.TransactionService;
import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final AuthorizationFacade authorizationFacade;
    private final AuditLogger auditLogger;

    @PostMapping("/versamento")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> versamento(@Valid @RequestBody DepositRequest request,
                                                           @AuthenticationPrincipal Jwt jwt,
                                                           Authentication authentication) {
        boolean isEmployee = authorizationFacade.isEmployee(authentication);
        TransactionResponse response = transactionService.eseguiVersamento(request, jwt.getSubject(), isEmployee);
        if (isEmployee) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "VERSAMENTO", "conto", request.getIban());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/prelievo")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> prelievo(@Valid @RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        boolean isEmployee = authorizationFacade.isEmployee(authentication);
        TransactionResponse response = transactionService.eseguiPrelievo(request, jwt.getSubject(), isEmployee);
        if (isEmployee) {
            auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "PRELIEVO", "conto", request.getIban());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> bonifico(@Valid @RequestBody TransferRequest request,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        return ResponseEntity.ok(transactionService.eseguiBonifico(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/giroconto")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> giroconto(@Valid @RequestBody GirocontoRequest request,
                                                          @AuthenticationPrincipal Jwt jwt,
                                                          Authentication authentication) {
        return ResponseEntity.ok(transactionService.eseguiGiroconto(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/user-transfers")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(transactionService.getUserTransactions(jwt.getSubject()));
    }

    @GetMapping("/by-iban")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<TransactionResponse>> getTransazioniByIban(@RequestParam String iban,
                                                                           @AuthenticationPrincipal Jwt jwt,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(transactionService.getTransazioniByIban(iban, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransazione(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransazioneById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<Page<TransactionAdminResponse>> listaTransazioni(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getTransazioniPaginate(pageable));
    }
}
