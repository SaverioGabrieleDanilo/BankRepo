package com.banca.gestionale_banca.transaction.model;

import com.banca.gestionale_banca.account.model.BankAccount;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "value_date", nullable = false)
    private LocalDateTime valueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account", nullable = false)
    private BankAccount payerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_account", nullable = false)
    private BankAccount payeeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_user", nullable = false)
    private User payerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_user", nullable = false)
    private User payeeUser;

    @Column(nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_type_id", nullable = true)
    private DepositType depositType;

    @Column(name = "items_count", nullable = true)
    private Integer itemsCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}