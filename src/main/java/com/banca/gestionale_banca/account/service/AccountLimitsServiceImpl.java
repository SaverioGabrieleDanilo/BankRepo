package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.model.AccountLimits;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;

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
    private final AuthorizationFacade authorizationFacade;

    @Override
    @Transactional
    public AccountLimitsResponse getLimiti(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato a consultare questo conto");

        AccountLimits limiti = accountLimitsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Limiti non ancora configurati per questo conto"));

        return toResponse(limiti);
    }

    @Override
    @Transactional
    public AccountLimitsResponse impostaLimiti(Long accountId, AccountLimitsRequest request) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        LocalDateTime now = LocalDateTime.now();
        AccountLimits limiti = accountLimitsRepository.findByAccountId(accountId).orElseGet(AccountLimits::new);

        if (limiti.getId() == null) {
            limiti.setAccount(account);
            limiti.setUser(account.getUser());
            limiti.setCreatedAt(now);
        }
        limiti.setDailyWithdrawalLimit(request.getDailyWithdrawalLimit());
        limiti.setSingleTransactionLimit(request.getSingleTransactionLimit());
        limiti.setMonthlyTransferLimit(request.getMonthlyTransferLimit());
        limiti.setUpdatedAt(now);

        limiti = accountLimitsRepository.save(limiti);

        return toResponse(limiti);
    }

    @Override
    public Optional<AccountLimitsResponse> findLimiti(Long accountId) {
        return accountLimitsRepository.findByAccountId(accountId).map(this::toResponse);
    }

    private AccountLimitsResponse toResponse(AccountLimits limiti) {
        return AccountLimitsResponse.builder()
                .accountId(limiti.getAccount().getId())
                .dailyWithdrawalLimit(limiti.getDailyWithdrawalLimit())
                .singleTransactionLimit(limiti.getSingleTransactionLimit())
                .monthlyTransferLimit(limiti.getMonthlyTransferLimit())
                .updatedAt(limiti.getUpdatedAt())
                .build();
    }
}
