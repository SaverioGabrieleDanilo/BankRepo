package com.banca.gestionale_banca.repository;

import com.banca.gestionale_banca.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.payerAccount.id = :accountId
          AND t.type.name = 'PRELIEVO'
          AND t.valueDate BETWEEN :dayStart AND :dayEnd
        """)
    BigDecimal sumDailyWithdrawalsByAccount(@Param("accountId") Long accountId,
                                            @Param("dayStart") LocalDateTime dayStart,
                                            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.payerAccount.id = :accountId
          AND t.type.name IN ('BONIFICO', 'GIROCONTO')
          AND t.valueDate BETWEEN :monthStart AND :monthEnd
        """)
    BigDecimal sumMonthlyTransfersByAccount(@Param("accountId") Long accountId,
                                            @Param("monthStart") LocalDateTime monthStart,
                                            @Param("monthEnd") LocalDateTime monthEnd);
}
