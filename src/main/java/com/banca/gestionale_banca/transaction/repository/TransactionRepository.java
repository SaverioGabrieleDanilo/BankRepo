package com.banca.gestionale_banca.transaction.repository;

import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  // List<TransactionResponse> findByUserId(Long userId);

    @Query("SELECT t FROM Transaction t " +
       "LEFT JOIN FETCH t.payerAccount " +
       "LEFT JOIN FETCH t.payeeAccount " +
       "LEFT JOIN FETCH t.payerUser " +
       "LEFT JOIN FETCH t.payeeUser " +
       "WHERE t.payerUser.id = :userId OR t.payeeUser.id = :userId")
List<Transaction> findAllByUserId(@Param("userId") Long userId);

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
