package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.AccountLimits;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.repository.AccountLimitsRepository;
import com.banca.gestionale_banca.repository.BankAccountRepository;
import com.banca.gestionale_banca.security.AuthorizationFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountLimitsServiceImpl implements AccountLimitsService {

    private final AccountLimitsRepository accountLimitsRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuthorizationFacade authorizationFacade;

    @Override
    public AccountLimitsResponse getLimiti(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verificaProprietario(account, keycloakId, isEmployee, "Non autorizzato a consultare questo conto");

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
