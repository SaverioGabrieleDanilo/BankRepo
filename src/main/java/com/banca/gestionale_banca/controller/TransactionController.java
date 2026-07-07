package com.banca.gestionale_banca.controller;

import com.banca.gestionale_banca.dto.TransferRequest;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.service.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionservice;
    private final UserService userService;

    @PostMapping("/versamento")
    @PreAuthorize("hasAnyRole('OPERATORE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> versamento(@RequestBody TransactionRequest request,
                                                           @AuthenticationPrincipal Jwt jwt,
                                                           Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiVersamento(request, jwt.getSubject(), isOperatore(authentication)));
    }

    @PostMapping("/prelievo")
    @PreAuthorize("hasAnyRole('OPERATORE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> prelievo(@RequestBody TransactionRequest request,
                                                         @AuthenticationPrincipal Jwt jwt,
                                                         Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiPrelievo(request, jwt.getSubject(), isOperatore(authentication)));
    }

    private boolean isOperatore(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATORE"));
    }

    @PostMapping("/bonifico")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> bonifico(@Valid @RequestBody TransferRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        Utente utenteCorrente = userService.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Utente autenticato non trovato"));

        return ResponseEntity.ok(transactionservice.eseguiBonifico(utenteCorrente.getId(), request));
    }
}
