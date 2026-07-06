package com.banca.gestionale_banca.repository;

import com.banca.gestionale_banca.model.AccountLimits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountLimitsRepository extends JpaRepository<AccountLimits, Long> {
    Optional<AccountLimits> findByAccountId(Long accountId);
}
