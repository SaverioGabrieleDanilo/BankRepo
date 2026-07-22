package com.banca.gestionale_banca.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.AccountLimits;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
class AccountLimitsServiceImpl implements AccountLimitsService {

    private final AccountLimitsRepository accountLimitsRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthorizationFacade authorizationFacade;

    @Override
    @Transactional(readOnly = true)
    public AccountLimitsResponse getBankAccountLimits(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato a consultare questo conto");

        AccountLimits limits = accountLimitsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Limiti non ancora configurati per questo conto"));

        return toResponseWithAvailability(limits);
    }

    @Override
    @Transactional
    public AccountLimitsResponse setBankAccountLimits(Long accountId, AccountLimitsRequest request, String keycloakId,
            boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato a modificare i massimali di questo conto");

        LocalDateTime now = LocalDateTime.now();
        AccountLimits limits = accountLimitsRepository.findByAccountId(accountId).orElseGet(AccountLimits::new);

        if (limits.getId() == null) {
            limits.setAccount(account);
            limits.setUser(account.getUser());
            limits.setCreatedAt(now);
        }
        limits.setDailyWithdrawalLimit(request.getDailyWithdrawalLimit());
        limits.setSingleTransactionLimit(request.getSingleTransactionLimit());
        limits.setMonthlyTransferLimit(request.getMonthlyTransferLimit());
        limits.setUpdatedAt(now);

        limits = accountLimitsRepository.save(limits);

        return toResponse(limits);
    }

    @Override
    public Optional<AccountLimitsResponse> findLimits(Long accountId) {
        return accountLimitsRepository.findByAccountId(accountId).map(this::toResponse);
    }

    private AccountLimitsResponse toResponse(AccountLimits limits) {
        return AccountLimitsResponse.builder()
                .accountId(limits.getAccount().getId())
                .dailyWithdrawalLimit(limits.getDailyWithdrawalLimit())
                .singleTransactionLimit(limits.getSingleTransactionLimit())
                .monthlyTransferLimit(limits.getMonthlyTransferLimit())
                .updatedAt(limits.getUpdatedAt())
                .build();
    }

    /**
     * Same as toResponse, but including the amount already consumed today/this
     * month
     * (2 extra queries) — used only for the GET exposed to the client, not in the
     * hot path
     * of transaction validation (verificaLimiti/findLimiti).
     */
    private AccountLimitsResponse toResponseWithAvailability(AccountLimits limits) {
        Long accountId = limits.getAccount().getId();
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
        LocalDateTime monthStart = LocalDateTime.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);

        return AccountLimitsResponse.builder()
                .accountId(accountId)
                .dailyWithdrawalLimit(limits.getDailyWithdrawalLimit())
                .singleTransactionLimit(limits.getSingleTransactionLimit())
                .monthlyTransferLimit(limits.getMonthlyTransferLimit())
                .dailyWithdrawalUsed(transactionRepository.sumDailyWithdrawalsByAccount(accountId, dayStart, dayEnd))
                .monthlyTransferUsed(
                        transactionRepository.sumMonthlyTransfersByAccount(accountId, monthStart, monthEnd))
                .updatedAt(limits.getUpdatedAt())
                .build();
    }
}
