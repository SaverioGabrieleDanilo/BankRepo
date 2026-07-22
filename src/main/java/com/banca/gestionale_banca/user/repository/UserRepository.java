package com.banca.gestionale_banca.user.repository;

import com.banca.gestionale_banca.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByStatus_Name(String statusName);

    /**
     * Carica role/status/registrationStatus in join (invece che lazy) per evitare
     * LazyInitializationException quando l'entity viene mappata a UserResponse
     * fuori dalla sessione Hibernate (spring.jpa.open-in-view=false).
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role JOIN FETCH u.status JOIN FETCH u.registrationStatus WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.role JOIN FETCH u.status JOIN FETCH u.registrationStatus WHERE u.keycloakId = :keycloakId")
    Optional<User> findByKeycloakIdWithDetails(@Param("keycloakId") String keycloakId);

    @Query(value = "SELECT u FROM User u JOIN FETCH u.role JOIN FETCH u.status JOIN FETCH u.registrationStatus " +
           "WHERE (:status IS NULL OR u.status.name = :status) " +
           "ORDER BY CASE u.status.name WHEN 'ACTIVE' THEN 0 WHEN 'SUSPENDED' THEN 1 WHEN 'CLOSED' THEN 2 ELSE 3 END, " +
           "u.firstName, u.lastName",
           countQuery = "SELECT COUNT(u) FROM User u WHERE (:status IS NULL OR u.status.name = :status)")
    Page<User> findAllWithDetails(@Param("status") String status, Pageable pageable);
}
