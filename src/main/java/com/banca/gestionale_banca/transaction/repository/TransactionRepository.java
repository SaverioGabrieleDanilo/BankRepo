package com.banca.gestionale_banca.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.banca.gestionale_banca.transaction.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.type
            JOIN FETCH t.status
            JOIN FETCH t.payerAccount
            JOIN FETCH t.payeeAccount
            JOIN FETCH t.payerUser
            JOIN FETCH t.payeeUser
            ORDER BY t.transactionDate DESC
            """, countQuery = "SELECT COUNT(t) FROM Transaction t")
    Page<Transaction> findAllWithDetails(Pageable pageable);

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.type
            JOIN FETCH t.status
            JOIN FETCH t.payerAccount
            JOIN FETCH t.payeeAccount
            JOIN FETCH t.payerUser
            JOIN FETCH t.payeeUser
            WHERE t.payerAccount.id = :accountId OR t.payeeAccount.id = :accountId
            ORDER BY t.transactionDate DESC
            """, countQuery = "SELECT COUNT(t) FROM Transaction t WHERE t.payerAccount.id = :accountId OR t.payeeAccount.id = :accountId")
    Page<Transaction> findAllByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.type
            JOIN FETCH t.status
            JOIN FETCH t.payerAccount
            JOIN FETCH t.payeeAccount
            JOIN FETCH t.payerUser
            JOIN FETCH t.payeeUser
            LEFT JOIN FETCH t.depositType
            WHERE t.payerUser.id = :userId OR t.payeeUser.id = :userId
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findAllByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.type
            JOIN FETCH t.status
            JOIN FETCH t.payerAccount
            JOIN FETCH t.payeeAccount
            JOIN FETCH t.payerUser
            JOIN FETCH t.payeeUser
            LEFT JOIN FETCH t.depositType
            WHERE t.payerAccount.iban = :iban OR t.payeeAccount.iban = :iban
            ORDER BY t.transactionDate DESC
            """)
    List<Transaction> findAllByIban(@Param("iban") String iban);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.payerAccount.id = :accountId
              AND t.type.name = 'WITHDRAWAL'
              AND t.valueDate BETWEEN :dayStart AND :dayEnd
            """)
    BigDecimal sumDailyWithdrawalsByAccount(@Param("accountId") Long accountId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.payerAccount.id = :accountId
              AND t.type.name IN ('BANK_TRANSFER', 'INTERNAL_TRANSFER')
              AND t.valueDate BETWEEN :monthStart AND :monthEnd
            """)
    BigDecimal sumMonthlyTransfersByAccount(@Param("accountId") Long accountId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);
}
