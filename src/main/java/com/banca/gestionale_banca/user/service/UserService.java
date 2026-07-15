package com.banca.gestionale_banca.user.service;

import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserService {
    User registraUtente(RegisterRequest request);
    User registraUtenteConRuolo(RegisterRequest request, String role);
    Optional<User> findById(Long id);
    Optional<User> findByKeycloakId(String keycloakId);
    User modificaUtente(Long id, UpdateUserRequest request);
    void disattivaUtente(Long id);
    User cambiaStatoUtente(Long id, String statusName);
    User cambiaStatoRegistrazione(Long id, String statusName);
    Page<User> getUtentiPaginati(Pageable pageable);

    /**
     * API interna ad uso del bootstrap applicativo (shared/config): inizializza
     * le tabelle di riferimento (ruoli, stati utente, stati registrazione) se vuote.
     */
    void seedDatiBase();
}