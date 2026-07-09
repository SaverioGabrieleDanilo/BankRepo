package com.banca.gestionale_banca.repository;

import com.banca.gestionale_banca.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Utente, Long> {
    Optional<Utente> findByUsername(String username);
    Optional<Utente> findByEmail(String email);
    Optional<Utente> findByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Carica role/status/registrationStatus in join (invece che lazy) per evitare
     * LazyInitializationException quando l'entity viene mappata a UserResponse fuori
     * dalla sessione Hibernate (spring.jpa.open-in-view=false).
     */
    @Query("SELECT u FROM Utente u JOIN FETCH u.role JOIN FETCH u.status JOIN FETCH u.registrationStatus WHERE u.id = :id")
    Optional<Utente> findByIdWithDetails(@Param("id") Long id);

    @Query(value = "SELECT u FROM Utente u JOIN FETCH u.role JOIN FETCH u.status JOIN FETCH u.registrationStatus",
           countQuery = "SELECT COUNT(u) FROM Utente u")
    Page<Utente> findAllWithDetails(Pageable pageable);
}