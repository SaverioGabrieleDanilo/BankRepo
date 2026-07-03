package com.banca.gestionale_banca.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.banca.gestionale_banca.dto.RegisterRequest;
import com.banca.gestionale_banca.dto.UpdateUserRequest;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/utenti")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

  


    @PostMapping("/registra")
    public ResponseEntity<Utente> registraUtente(@RequestBody RegisterRequest request) {
        Utente utente = userService.registraUtente(request);
        return ResponseEntity.ok(utente);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utente> getUtenteById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Utente>> getUtentiPaginati(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getUtentiPaginati(pageable));
    }

    

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or #id == #jwt.getClaim('user_id_locale')")
    public ResponseEntity<Utente> modificaUtente(
            @PathVariable Long id, 
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        try {
            Utente utenteAggiornato = userService.modificaUtente(id, request);
            return ResponseEntity.ok(utenteAggiornato);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

   
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
    public ResponseEntity<Void> disattivaUtente(@PathVariable Long id) {
        try {
            userService.disattivaUtente(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}