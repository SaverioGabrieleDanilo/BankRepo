package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.AccountLimits;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.transaction.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class AccountLimitsServiceImpl implements AccountLimitsService {

    private final AccountLimitsRepository accountLimitsRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuthorizationFacade authorizationFacade;

    @Override
    @Transactional
    public AccountLimitsResponse getLimiti(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato a consultare questo conto");

        AccountLimits limits = accountLimitsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Limiti non ancora configurati per questo conto"));

        return toResponseConDisponibilita(limits);
    }

    @Override
    @Transactional
    public AccountLimitsResponse impostaLimiti(Long accountId, AccountLimitsRequest request, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato a modificare i massimali di questo conto");

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
    public Optional<AccountLimitsResponse> findLimiti(Long accountId) {
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
     * Come toResponse, ma con in aggiunta quanto già consumato oggi/questo mese
     * (2 query in più) — usata solo per la GET esposta al cliente, non nel percorso
     * caldo della validazione delle transazioni (verificaLimiti/findLimiti).
     */
    private AccountLimitsResponse toResponseConDisponibilita(AccountLimits limits) {
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
                .monthlyTransferUsed(transactionRepository.sumMonthlyTransfersByAccount(accountId, monthStart, monthEnd))
                .updatedAt(limits.getUpdatedAt())
                .build();
    }
}
