package com.banca.gestionale_banca.account.repository;

import com.banca.gestionale_banca.account.model.BankAccount;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    @EntityGraph(attributePaths = {"status"})
    List<BankAccount> findByUserId(Long userId);
    Optional<BankAccount> findByIban(String iban);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT b FROM BankAccount b WHERE b.iban = :iban")
    Optional<BankAccount> findByIbanForUpdate(@Param("iban") String iban);

    @Query(value = "SELECT b FROM BankAccount b JOIN FETCH b.user JOIN FETCH b.status",
           countQuery = "SELECT COUNT(b) FROM BankAccount b")
    Page<BankAccount> findAllWithUser(Pageable pageable);

    @Query("SELECT b FROM BankAccount b JOIN FETCH b.user JOIN FETCH b.status WHERE b.id = :id")
    Optional<BankAccount> findByIdWithUser(@Param("id") Long id);
}
