package com.banca.gestionale_banca.user.service;

import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.model.Utente;
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
    Utente cambiaStatoUtente(Long id, String statoNome);
    Utente cambiaStatoRegistrazione(Long id, String statoNome);
    Page<Utente> getUtentiPaginati(Pageable pageable);

    /**
     * API interna ad uso del bootstrap applicativo (shared/config): inizializza
     * le tabelle di riferimento (ruoli, stati utente, stati registrazione) se vuote.
     */
    void seedDatiBase();
}