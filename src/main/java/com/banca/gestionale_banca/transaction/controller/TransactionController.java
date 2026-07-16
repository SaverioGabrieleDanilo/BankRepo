package com.banca.gestionale_banca.transaction.controller;

import java.util.List;

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

import com.banca.gestionale_banca.transaction.dto.BankAccountTransactionRequest;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.transaction.service.TransactionService;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
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
    public ResponseEntity<TransactionResponse> versamento(@Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiVersamento(request, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/prelievo")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CUSTOMER')")
    public ResponseEntity<TransactionResponse> prelievo(@Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiPrelievo(request, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> bonifico(@Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiBonifico(request, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication)));
    }

    @PostMapping("/giroconto")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TransactionResponse> giroconto(@Valid @RequestBody GirocontoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        return ResponseEntity.ok(transactionservice.eseguiGiroconto(request, jwt.getSubject(),
                authorizationFacade.isEmployee(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
    public ResponseEntity<TransactionResponse> getTransazione(@PathVariable Long id) {
        return ResponseEntity.ok(transactionservice.getTransazioneById(id));
    }

    @GetMapping("/user-transfers")
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(@AuthenticationPrincipal Jwt jwt) {

        String username = jwt.getClaimAsString("preferred_username");

        List<TransactionResponse> transactions = transactionservice.getUserTransactions(username);
        return ResponseEntity.ok(transactions);

    }

    @PostMapping("/bank-account-transfers")
    public ResponseEntity<List<TransactionResponse>> getBankAccountTransactions(
            @RequestBody BankAccountTransactionRequest request) {

        // Estraiamo l'IBAN dal body della richiesta
        List<TransactionResponse> transactions = transactionservice.getBankAccountTransactions(request.getIban());

        return ResponseEntity.ok(transactions);
    }
}

