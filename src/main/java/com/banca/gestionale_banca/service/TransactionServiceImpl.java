package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.dto.TransactionRequest;
import com.banca.gestionale_banca.dto.TransactionResponse;
import com.banca.gestionale_banca.model.BankAccount;
import com.banca.gestionale_banca.model.Transaction;
import com.banca.gestionale_banca.model.TransactionStatus;
import com.banca.gestionale_banca.model.TransactionType;
import com.banca.gestionale_banca.repository.BankAccountRepository;
import com.banca.gestionale_banca.repository.TransactionRepository;
import com.banca.gestionale_banca.repository.TransactionStatusRepository;
import com.banca.gestionale_banca.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;

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

    private void verificaProprietario(BankAccount account, String keycloakId, boolean isOperatore) {
        if (isOperatore) {
            return;
        }
        if (!account.getUser().getKeycloakId().equals(keycloakId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad operare su questo conto");
        }
    }
}