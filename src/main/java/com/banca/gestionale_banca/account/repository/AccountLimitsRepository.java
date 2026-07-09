package com.banca.gestionale_banca.account.repository;

import com.banca.gestionale_banca.account.model.AccountLimits;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountLimitsRepository extends JpaRepository<AccountLimits, Long> {
    Optional<AccountLimits> findByAccountId(Long accountId);
}
