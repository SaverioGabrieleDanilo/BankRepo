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
import com.banca.gestionale_banca.user.dto.UserStatsResponse;
import com.banca.gestionale_banca.user.dto.RegistrationStatusRequest;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuditLogger auditLogger;
    private final AuthorizationFacade authorizationFacade;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUserWithRole(@Valid @RequestBody AdminCreateUserRequest request) {
        User user = userService.registerUserWithRole(request, request.getRole());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.findByKeycloakIdWithDetails(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        Authentication authentication) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));

        boolean isAdmin = authorizationFacade.isAdmin(authentication);
        boolean isEmployee = authorizationFacade.isEmployee(authentication);
        boolean isOwner = jwt.getSubject().equals(user.getKeycloakId());

        if (!isAdmin && !isEmployee && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a consultare questo utente");
        }

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<UserStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getStats());
    }

    @GetMapping("/check")
    public ResponseEntity<String> testRoute(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok("Connessione OK! Token valido per l'utente: " + username);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Page<UserResponse>> getPaginatedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.getPaginatedUsers(status, pageable).map(UserResponse::from));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        User user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));

        boolean isAdmin = authorizationFacade.isAdmin(authentication);
        boolean isOwner = jwt.getSubject().equals(user.getKeycloakId());

        if (!isAdmin && request.getRole() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un ADMIN può modificare il ruolo");
        }
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a modificare questo utente");
        }

        return ResponseEntity.ok(UserResponse.from(userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        userService.deactivateUser(id);
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "DISATTIVA_UTENTE", "utente", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request,
                                                          @AuthenticationPrincipal Jwt jwt) {
        UserResponse response = UserResponse.from(userService.changeUserStatus(id, request.getStatus()));
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "CAMBIA_STATO_UTENTE", "utente", id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/registration-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<UserResponse> changeRegistrationStatus(@PathVariable Long id, @Valid @RequestBody RegistrationStatusRequest request,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        UserResponse response = UserResponse.from(userService.changeRegistrationStatus(id, request.getRegistrationStatus()));
        auditLogger.log(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), "CAMBIA_STATO_REGISTRAZIONE", "utente", id);
        return ResponseEntity.ok(response);
    }
}
