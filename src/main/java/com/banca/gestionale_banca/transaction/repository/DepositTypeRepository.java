package com.banca.gestionale_banca.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.banca.gestionale_banca.transaction.model.DepositType;

public interface DepositTypeRepository extends JpaRepository<DepositType, Integer> {
    Optional<DepositType> findByName(String name);
}
