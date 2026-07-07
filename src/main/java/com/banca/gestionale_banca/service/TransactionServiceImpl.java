package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.dto.TransferRequest;
import com.banca.gestionale_banca.exception.BusinessRuleException;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.*;
import com.banca.gestionale_banca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.banca.gestionale_banca.repository.AccountLimitsRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final AccountLimitsRepository accountLimitsRepository;

    @Override
    @Transactional
    public TransactionResponse eseguiVersamento(TransactionRequest request, String keycloakId, boolean isOperatore) {
        BankAccount account = bankAccountRepository.findByIban(request.getIban())
                .orElseThrow(() -> new RuntimeException("Conto corrente non trovato"));

        verificaProprietario(account, keycloakId, isOperatore);

        if (!"ATTIVO".equals(account.getStatus().getName())) {
            throw new RuntimeException("Il conto corrente non è attivo");
        }

        TransactionType type = transactionTypeRepository.findByName("VERSAMENTO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione VERSAMENTO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName("ESEGUITA")
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        bankAccountRepository.save(account);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(account);
        transaction.setPayeeAccount(account);
        transaction.setPayerUser(account.getUser());
        transaction.setPayeeUser(account.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(account.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .updatedBalance(account.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse eseguiPrelievo(TransactionRequest request, String keycloakId, boolean isOperatore) {
        BankAccount account = bankAccountRepository.findByIban(request.getIban())
                .orElseThrow(() -> new RuntimeException("Conto corrente non trovato"));

        verificaProprietario(account, keycloakId, isOperatore);

        if (!"ATTIVO".equals(account.getStatus().getName())) {
            throw new RuntimeException("Il conto corrente non è attivo");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Saldo insufficiente per completare il prelievo");
        }

        TransactionType type = transactionTypeRepository.findByName("PRELIEVO")
                .orElseThrow(() -> new RuntimeException("Tipo transazione PRELIEVO non configurato"));

        TransactionStatus status = transactionStatusRepository.findByName("ESEGUITA")
                .orElseThrow(() -> new RuntimeException("Stato transazione ESEGUITA non configurato"));

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        bankAccountRepository.save(account);

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(now);
        transaction.setValueDate(now);
        transaction.setCreatedAt(now);
        transaction.setPayerAccount(account);
        transaction.setPayeeAccount(account);
        transaction.setPayerUser(account.getUser());
        transaction.setPayeeUser(account.getUser());
        transaction.setType(type);
        transaction.setStatus(status);
        transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .iban(account.getIban())
                .type(type.getName())
                .amount(request.getAmount())
                .updatedBalance(account.getBalance())
                .status(status.getName())
                .timestamp(transaction.getTransactionDate())
                .build();
    }

    @Override
    @Transactional
    public TransactionResponse eseguiBonifico(Long currentUserId, TransferRequest request) {

        // 1. il conto ordinante esiste?
        BankAccount payerAccount = bankAccountRepository.findById(request.getPayerAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Conto ordinante non trovato"));

        // 2. il conto ordinante appartiene davvero a chi ha fatto la richiesta?
        if (!payerAccount.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Il conto ordinante non appartiene all'utente autenticato");
        }

        // 3. il conto ordinante è ATTIVO?
        if (!"ATTIVO".equals(payerAccount.getStatus().getName())) {
            throw new BusinessRuleException("Il conto ordinante non è attivo");
        }

        // 4. il conto beneficiario esiste?
        BankAccount payeeAccount = bankAccountRepository.findByIban(request.getPayeeIban())
                .orElseThrow(() -> new ResourceNotFoundException("Conto beneficiario non trovato"));

        // 5. il conto beneficiario è ATTIVO?
        if (!"ATTIVO".equals(payeeAccount.getStatus().getName())) {
            throw new BusinessRuleException("Il conto beneficiario non è attivo");
        }

        // 6. stesso titolare per ordinante e beneficiario? Va usato /transactions/giroconto
        if (payerAccount.getUser().getId().equals(payeeAccount.getUser().getId())) {
            throw new BusinessRuleException("Per trasferire fondi tra due tuoi conti usa il giroconto");
        }

        // 7. limite per singola operazione
        AccountLimits limits = accountLimitsRepository.findByAccountId(payerAccount.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Limiti operativi non configurati per il conto ordinante"));

        if (request.getAmount().compareTo(limits.getSingleTransactionLimit()) > 0) {
            throw new BusinessRuleException("Importo superiore al limite per singola operazione");
        }

        // 8. limite di trasferimento mensile (US-5.3)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        BigDecimal trasferitoQuestoMese =
                transactionRepository.sumMonthlyTransfersByAccount(payerAccount.getId(), monthStart, now);

        if (trasferitoQuestoMese.add(request.getAmount()).compareTo(limits.getMonthlyTransferLimit()) > 0) {
            throw new BusinessRuleException("Superato il limite di trasferimento mensile");
        }

        // 9. saldo disponibile (US-3.2)
        if (payerAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessRuleException("Saldo insufficiente");
        }

        // 10. aggiornamento saldo: il controllo di concorrenza è automatico grazie a @Version
        payerAccount.setBalance(payerAccount.getBalance().subtract(request.getAmount()));
        payeeAccount.setBalance(payeeAccount.getBalance().add(request.getAmount()));
        bankAccountRepository.save(payerAccount);
        bankAccountRepository.save(payeeAccount);

        // Passaggio 4: creazione delle due righe di transazione
        TransactionType transferOutType = transactionTypeRepository.findByName("TRANSFER_OUT")
                .orElseThrow(() -> new ResourceNotFoundException("Tipo transazione TRANSFER_OUT non configurato"));
        TransactionType transferInType = transactionTypeRepository.findByName("TRANSFER_IN")
                .orElseThrow(() -> new ResourceNotFoundException("Tipo transazione TRANSFER_IN non configurato"));
        TransactionStatus servita = transactionStatusRepository.findByName("SERVITA")
                .orElseThrow(() -> new ResourceNotFoundException("Stato transazione SERVITA non configurato"));

        LocalDateTime adesso = LocalDateTime.now();

        Transaction transferOut = new Transaction();
        transferOut.setType(transferOutType);
        transferOut.setStatus(servita);
        transferOut.setAmount(request.getAmount());
        transferOut.setPayerAccount(payerAccount);
        transferOut.setPayeeAccount(payeeAccount);
        transferOut.setPayerUser(payerAccount.getUser());
        transferOut.setPayeeUser(payeeAccount.getUser());
        transferOut.setDescription(request.getDescription());
        transferOut.setValueDate(adesso);
        transferOut.setTransactionDate(adesso);
        transferOut.setCreatedAt(adesso);
        transactionRepository.save(transferOut);

        Transaction transferIn = new Transaction();
        transferIn.setType(transferInType);
        transferIn.setStatus(servita);
        transferIn.setAmount(request.getAmount());
        transferIn.setPayerAccount(payerAccount);
        transferIn.setPayeeAccount(payeeAccount);
        transferIn.setPayerUser(payerAccount.getUser());
        transferIn.setPayeeUser(payeeAccount.getUser());
        transferIn.setDescription(request.getDescription());
        transferIn.setValueDate(adesso);
        transferIn.setTransactionDate(adesso);
        transferIn.setCreatedAt(adesso);
        transactionRepository.save(transferIn);

        return TransactionResponse.builder()
                .transactionId(transferOut.getId())
                .iban(payerAccount.getIban())
                .type(transferOutType.getName())
                .amount(request.getAmount())
                .updatedBalance(payerAccount.getBalance())
                .status(servita.getName())
                .timestamp(transferOut.getTransactionDate())
                .build();
    }


    private void verificaProprietario(BankAccount account, String keycloakId, boolean isOperatore) {
        if (isOperatore) {
            return;
        }
        if (!account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad operare su questo conto");
        }
    }
}