package com.banca.gestionale_banca.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.banca.gestionale_banca.dto.AdminCreateUserRequest;
import com.banca.gestionale_banca.dto.RegisterRequest;
import com.banca.gestionale_banca.dto.UpdateUserRequest;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/utenti")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/registra")
    public ResponseEntity<Utente> registraUtente(@Valid @RequestBody RegisterRequest request) {
        Utente utente = userService.registraUtente(request);
        return ResponseEntity.ok(utente);
    }

    @PostMapping("/admin/crea")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utente> creaUtenteConRuolo(@Valid @RequestBody AdminCreateUserRequest request) {
        Utente utente = userService.registraUtenteConRuolo(request, request.getRuolo());
        return ResponseEntity.ok(utente);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Utente> getUtenteById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
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
    public ResponseEntity<Utente> modificaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Utente utente = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = jwt.getSubject().equals(utente.getKeycloakId());

        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a modificare questo utente");
        }

        return ResponseEntity.ok(userService.modificaUtente(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATORE')")
    public ResponseEntity<Void> disattivaUtente(@PathVariable Long id) {
        userService.disattivaUtente(id);
        return ResponseEntity.noContent().build();
    }
}
