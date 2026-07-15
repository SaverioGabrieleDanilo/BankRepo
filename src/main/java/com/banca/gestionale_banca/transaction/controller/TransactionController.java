package com.banca.gestionale_banca.transaction.controller;

import com.banca.gestionale_banca.transaction.dto.*;
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
import org.springframework.web.bind.annotation.RestController;
import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.service.TransactionService;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionservice;
    private final AuthorizationFacade authorizationFacade;

    @PostMapping("/versamento")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> versamento(@Valid @RequestBody DepositRequest request,
                                                           @AuthenticationPrincipal Jwt jwt,
                                                           Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiVersamento(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/prelievo")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> prelievo(@Valid @RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiPrelievo(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> bonifico(@Valid @RequestBody TransferRequest request,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiBonifico(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/giroconto")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> giroconto(@Valid @RequestBody GirocontoRequest request,
                                                          @AuthenticationPrincipal Jwt jwt,
                                                          Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiGiroconto(request, jwt.getSubject(), authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransazione(@PathVariable Long id) {
        return ResponseEntity.ok(transactionservice.getTransazioneById(id));
    }
}
