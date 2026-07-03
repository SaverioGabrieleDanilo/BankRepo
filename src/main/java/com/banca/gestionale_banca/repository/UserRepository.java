package com.banca.gestionale_banca.repository;

import com.banca.gestionale_banca.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByUsername(String username);
    Optional<Utente> findByEmail(String email);
    Optional<Utente> findByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}