package com.banca.gestionale_banca.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.banca.gestionale_banca.transaction.model.TransactionType;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {
    Optional<TransactionType> findByName(String name);
}
