package com.banca.gestionale_banca.rubrica.controller;

import com.banca.gestionale_banca.rubrica.dto.ContactResponse;
import com.banca.gestionale_banca.rubrica.dto.CreateContactRequest;
import com.banca.gestionale_banca.rubrica.model.Contact;
import com.banca.gestionale_banca.rubrica.service.ContactService;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.service.UserService;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getContacts(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));
        List<ContactResponse> contacts = contactService.getContactsByUser(user.getId()).stream()
                .map(ContactResponse::from)
                .toList();
        return ResponseEntity.ok(contacts);
    }

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));
        Contact contact = contactService.createContact(user.getId(), request);
        return ResponseEntity.created(URI.create("/api/contacts/" + contact.getId()))
                .body(ContactResponse.from(contact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        User user = userService.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User non trovato"));
        contactService.deleteContact(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
