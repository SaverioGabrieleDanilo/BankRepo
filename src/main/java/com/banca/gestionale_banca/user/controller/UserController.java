package com.banca.gestionale_banca.user.controller;

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

import com.banca.gestionale_banca.user.dto.AdminCreateUserRequest;
import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.dto.UserResponse;
import com.banca.gestionale_banca.user.dto.UserStatusRequest;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.user.model.Utente;
import com.banca.gestionale_banca.user.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/utenti")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/registra")
    public ResponseEntity<UserResponse> registraUtente(@Valid @RequestBody RegisterRequest request) {
        Utente utente = userService.registraUtente(request);
        return ResponseEntity.ok(UserResponse.from(utente));
    }

    @PostMapping("/admin/crea")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> creaUtenteConRuolo(@Valid @RequestBody AdminCreateUserRequest request) {
        Utente utente = userService.registraUtenteConRuolo(request, request.getRole());
        return ResponseEntity.ok(UserResponse.from(utente));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUtenteById(@PathVariable Long id) {
        return userService.findById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getUtentiPaginati(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getUtentiPaginati(pageable).map(UserResponse::from));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> modificaUtente(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Utente utente = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = jwt.getSubject().equals(utente.getKeycloakId());

        if (!isAdmin && request.getRole() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un ADMIN può modificare il ruolo");
        }
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a modificare questo utente");
        }

        return ResponseEntity.ok(UserResponse.from(userService.modificaUtente(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Void> disattivaUtente(@PathVariable Long id) {
        userService.disattivaUtente(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<UserResponse> cambiaStatoUtente(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.cambiaStatoUtente(id, request.getStato())));
    }
}
