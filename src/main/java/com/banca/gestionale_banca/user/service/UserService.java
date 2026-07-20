package com.banca.gestionale_banca.user.service;

import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.dto.UserStatsResponse;
import com.banca.gestionale_banca.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserService {
    User registerUser(RegisterRequest request);
    User registerUserWithRole(RegisterRequest request, String role);
    Optional<User> findById(Long id);
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByKeycloakIdWithDetails(String keycloakId);
    User updateUser(Long id, UpdateUserRequest request);
    void deactivateUser(Long id);
    User changeUserStatus(Long id, String statusName);
    User changeRegistrationStatus(Long id, String statusName);
    Page<User> getPaginatedUsers(String status, Pageable pageable);

    /** Conteggi aggregati per la dashboard admin (KPI), calcolati sull'intera tabella. */
    UserStatsResponse getStats();

    /**
     * API interna ad uso del bootstrap applicativo (shared/config): inizializza
     * le tabelle di riferimento (ruoli, stati utente, stati registrazione) se vuote.
     */
    void seedBaseData();
}
