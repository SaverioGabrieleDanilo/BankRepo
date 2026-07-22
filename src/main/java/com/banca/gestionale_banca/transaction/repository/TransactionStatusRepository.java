package com.banca.gestionale_banca.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.banca.gestionale_banca.transaction.model.TransactionStatus;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {
    Optional<TransactionStatus> findByName(String name);
}
