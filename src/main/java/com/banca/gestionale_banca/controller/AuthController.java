package com.banca.gestionale_banca.controller;

import com.banca.gestionale_banca.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RestTemplate restTemplate;


    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {

        String tokenUrl = issuerUri + "/protocol/openid-connect/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "password");
        params.add("username", loginRequest.getUsername());
        params.add("password", loginRequest.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            log.debug("Richiesta token a: {}", tokenUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            return ResponseEntity.ok(response.getBody());

        } catch (HttpStatusCodeException e) {
           
            log.error("Login fallito su Keycloak. Status: {} - Risposta: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login fallito: credenziali non valide");

        } catch (Exception e) {
            log.error("Errore imprevisto durante il login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Servizio di autenticazione non disponibile");
        }
    }
}