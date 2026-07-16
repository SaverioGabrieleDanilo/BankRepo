package com.banca.gestionale_banca.account.model;

import com.banca.gestionale_banca.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account_limits")
@Getter
@Setter
@NoArgsConstructor
public class AccountLimits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private BankAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "daily_withdrawal_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyWithdrawalLimit;

    @Column(name = "single_transaction_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal singleTransactionLimit;

    @Column(name = "monthly_transfer_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyTransferLimit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}