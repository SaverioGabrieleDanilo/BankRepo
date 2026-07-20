package com.banca.gestionale_banca.account.service;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponseDTO;
import com.banca.gestionale_banca.account.dto.BankAccountStatsResponse;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.constants.StatiConto;
import com.banca.gestionale_banca.account.model.AccountStatus;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountStatusRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final UserService userService;
    private final AuthorizationFacade authorizationFacade;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BankAccountResponse apriConto(String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        AccountStatus status = accountStatusRepository.findByName(StatiConto.IN_ATTESA)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto IN_ATTESA non configurato"));

        LocalDateTime now = LocalDateTime.now();
        BankAccount account = new BankAccount();
        account.setIban(generaIban());
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);
        account.setContableBalance(BigDecimal.ZERO);
        account.setStatus(status);
        account.setOpeningDate(now);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        account = bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Override
    @Transactional
    public BankAccountResponse approvaConto(Long accountId, boolean approved) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        if (!StatiConto.IN_ATTESA.equals(account.getStatus().getName())) {
            throw new ConflictException("Il conto non è in attesa di approvazione");
        }

        AccountStatus newStatus = accountStatusRepository
                .findByName(approved ? StatiConto.ATTIVO : StatiConto.RIFIUTATO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto non configurato"));

        account.setStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Override
    @Transactional
    public BankAccountResponse chiudiConto(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                "Non autorizzato a chiudere questo conto");

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ConflictException("Impossibile chiudere il conto: il saldo deve essere zero");
        }

        AccountStatus closed = accountStatusRepository.findByName(StatiConto.CHIUSO)
                .orElseThrow(() -> new ResourceNotFoundException("Stato conto CHIUSO non configurato"));

        account.setStatus(closed);
        account.setUpdatedAt(LocalDateTime.now());
        account = bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Override
    public BankAccountResponse getContoById(Long accountId, String keycloakId, boolean isEmployee) {
        BankAccount account = bankAccountRepository.findByIdWithUser(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conto corrente non trovato"));

        authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee, "Non autorizzato a consultare questo conto");

        return toResponse(account);
    }

    @Override
    public List<BankAccountResponseDTO> getUserBankAccounts(String keycloakId) {
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return bankAccountRepository.findByUserId(user.getId()).stream()
                .map(acc -> new BankAccountResponseDTO(
                        acc.getId(),
                        acc.getIban(),
                        acc.getBalance(),
                        acc.getContableBalance(),
                        acc.getUser().getId(),
                        acc.getStatus() != null ? acc.getStatus().getName() : null,
                        acc.getOpeningDate(),
                        acc.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponseDTO> getUserBankAccountsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con username: " + username));

        List<BankAccount> accounts = bankAccountRepository.findByUserId(user.getId());

        return accounts.stream()
                .map(acc -> new BankAccountResponseDTO(
                        acc.getId(),
                        acc.getIban(),
                        acc.getBalance(),
                        acc.getContableBalance(),
                        acc.getUser().getId(),
                        acc.getStatus() != null ? acc.getStatus().getName() : null,
                        acc.getOpeningDate(),
                        acc.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankAccountAdminResponse> listaConti(Pageable pageable) {
        return bankAccountRepository.findAllWithUser(pageable)
                .map(account -> BankAccountAdminResponse.builder()
                        .id(account.getId())
                        .iban(account.getIban())
                        .balance(account.getBalance())
                        .status(account.getStatus().getName())
                        .ownerId(account.getUser().getId())
                        .ownerUsername(account.getUser().getUsername())
                        .ownerFullName(account.getUser().getFirstName() + " " + account.getUser().getLastName())
                        .openingDate(account.getOpeningDate())
                        .build());
    }

    @Override
    public BankAccountStatsResponse getStats() {
        return BankAccountStatsResponse.builder()
                .pendingApprovals(bankAccountRepository.countByStatus_Name(StatiConto.IN_ATTESA))
                .totalManagedAssets(bankAccountRepository.sumBalanceByStatusName(StatiConto.ATTIVO))
                .build();
    }

    @Override
    public BankAccount lockForUpdate(String iban, String messageIfNotFound) {
        return bankAccountRepository.findByIbanForUpdate(iban)
                .orElseThrow(() -> new ResourceNotFoundException(messageIfNotFound));
    }

    @Override
    public void assertActive(BankAccount account, String messageIfNotActive) {
        if (!StatiConto.ATTIVO.equals(account.getStatus().getName())) {
            throw new ConflictException(messageIfNotActive);
        }
    }

    @Override
    @Transactional
    public BankAccount updateBalance(BankAccount account, BigDecimal newBalance) {
        account.setBalance(newBalance);
        return bankAccountRepository.save(account);
    }

    private String generaIban() {
        String countryCode = "IT";
        String bban = UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();

        String rearranged = bban + countryCode + "00";
        String numeric = rearranged.chars()
                .mapToObj(c -> Character.isDigit(c) ? String.valueOf((char) c) : String.valueOf(c - 'A' + 10))
                .collect(java.util.stream.Collectors.joining());
        int checkDigits = 98 - new java.math.BigInteger(numeric).mod(java.math.BigInteger.valueOf(97)).intValue();
        return String.format("%s%02d%s", countryCode, checkDigits, bban);
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .iban(account.getIban())
                .balance(account.getBalance())
                .contableBalance(account.getContableBalance())
                .status(account.getStatus().getName())
                .openingDate(account.getOpeningDate())
                .build();
    }
}
