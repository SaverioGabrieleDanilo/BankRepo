package com.banca.gestionale_banca.rubrica.repository;

import com.banca.gestionale_banca.rubrica.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Contact> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
