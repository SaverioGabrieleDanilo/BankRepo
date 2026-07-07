package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.RegisterRequest;
import com.banca.gestionale_banca.dto.UpdateUserRequest;
import com.banca.gestionale_banca.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserService {
    Utente registraUtente(RegisterRequest request);
    Utente registraUtenteConRuolo(RegisterRequest request, String ruolo);
    Optional<Utente> findById(Long id);
    Optional<Utente> findByKeycloakId(String keycloakId);
    Utente modificaUtente(Long id, UpdateUserRequest request);
    void disattivaUtente(Long id);
    Page<Utente> getUtentiPaginati(Pageable pageable);
}