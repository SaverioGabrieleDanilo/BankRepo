package com.banca.gestionale_banca.repository;

import com.banca.gestionale_banca.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RegistrationStatusRepository extends JpaRepository<RegistrationStatus, Integer> {
    Optional<RegistrationStatus> findByName(String name);
}