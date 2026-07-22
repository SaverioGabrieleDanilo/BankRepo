package com.banca.gestionale_banca.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.dto.BankAccountSummaryResponse;
import com.banca.gestionale_banca.account.dto.BankAccountStatsResponse;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.account.constants.BankAccountStatus;
import com.banca.gestionale_banca.account.model.AccountStatus;
import com.banca.gestionale_banca.account.model.BankAccount;
import com.banca.gestionale_banca.account.repository.AccountStatusRepository;
import com.banca.gestionale_banca.account.repository.BankAccountRepository;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.user.service.UserService;
import com.banca.gestionale_banca.utils.IbanGenerator;

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
        public BankAccountResponse openBankAccount(String keycloakId) {
                User user = userService.findByKeycloakId(keycloakId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                AccountStatus status = accountStatusRepository.findByName(BankAccountStatus.PENDING)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Account status PENDING not configured"));

                LocalDateTime now = LocalDateTime.now();
                String newIban = IbanGenerator.generateItalianIban();
                BankAccount account = new BankAccount();
                account.setIban(newIban);
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
        public BankAccountResponse approveBankAccount(Long accountId, boolean approved) {
                BankAccount account = bankAccountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

                if (!BankAccountStatus.PENDING.equals(account.getStatus().getName())) {
                        throw new ConflictException("The account is not pending approval");
                }

                AccountStatus newStatus = accountStatusRepository
                                .findByName(approved ? BankAccountStatus.ACTIVE : BankAccountStatus.REJECTED)
                                .orElseThrow(() -> new ResourceNotFoundException("Account status not configured"));

                account.setStatus(newStatus);
                account.setUpdatedAt(LocalDateTime.now());
                account = bankAccountRepository.save(account);

                return toResponse(account);
        }

        @Override
        @Transactional
        public BankAccountResponse closeBankAccount(Long accountId, String keycloakId, boolean isEmployee) {
                BankAccount account = bankAccountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

                authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                                "Unauthorized to close this account");

                if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        throw new ConflictException("Unable to close account: balance must be zero");
                }

                AccountStatus closed = accountStatusRepository.findByName(BankAccountStatus.CLOSED)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Account status CLOSED not configured"));

                account.setStatus(closed);
                account.setUpdatedAt(LocalDateTime.now());
                account = bankAccountRepository.save(account);

                return toResponse(account);
        }

        @Override
        @Transactional
        public BankAccountResponse changeBankAccountStatus(Long accountId, String statusName) {
                BankAccount account = bankAccountRepository.findById(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

                AccountStatus newStatus = accountStatusRepository.findByName(statusName)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Invalid account status '" + statusName + "'"));

                if (BankAccountStatus.CLOSED.equals(statusName) && account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                        throw new ConflictException("Unable to close account: balance must be zero");
                }

                account.setStatus(newStatus);
                account.setUpdatedAt(LocalDateTime.now());
                account = bankAccountRepository.save(account);

                return toResponse(account);
        }

        @Override
        @Transactional(readOnly = true)
        public BankAccountResponse getBankAccountById(Long accountId, String keycloakId, boolean isEmployee) {
                BankAccount account = bankAccountRepository.findByIdWithUser(accountId)
                                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

                authorizationFacade.verifyOwnership(account.getUser().getKeycloakId(), keycloakId, isEmployee,
                                "Unauthorized to view this account");

                return toResponse(account);
        }

        @Override
        @Transactional(readOnly = true)
        public List<BankAccountSummaryResponse> getUserBankAccounts(String keycloakId) {
                User user = userService.findByKeycloakId(keycloakId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                return bankAccountRepository.findByUserId(user.getId()).stream()
                                .map(this::toSummaryResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<BankAccountSummaryResponse> getUserBankAccountsByUsername(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with username: " + username));

                List<BankAccount> accounts = bankAccountRepository.findByUserId(user.getId());

                return accounts.stream()
                                .map(this::toSummaryResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public Page<BankAccountAdminResponse> listBankAccounts(Pageable pageable) {
                return bankAccountRepository.findAllWithUser(pageable)
                                .map(account -> BankAccountAdminResponse.builder()
                                                .id(account.getId())
                                                .iban(account.getIban())
                                                .balance(account.getBalance())
                                                .status(account.getStatus().getName())
                                                .ownerId(account.getUser().getId())
                                                .ownerUsername(account.getUser().getUsername())
                                                .ownerFullName(account.getUser().getFirstName() + " "
                                                                + account.getUser().getLastName())
                                                .openingDate(account.getOpeningDate())
                                                .build());
        }

        @Override
        @Transactional(readOnly = true)
        public BankAccountStatsResponse getStats() {
                return BankAccountStatsResponse.builder()
                                .pendingApprovals(bankAccountRepository.countByStatus_Name(BankAccountStatus.PENDING))
                                .totalManagedAssets(
                                                bankAccountRepository.sumBalanceByStatusName(BankAccountStatus.ACTIVE))
                                .build();
        }

        @Override
        public BankAccount lockForUpdate(String iban, String messageIfNotFound) {
                return bankAccountRepository.findByIbanForUpdate(iban)
                                .orElseThrow(() -> new ResourceNotFoundException(messageIfNotFound));
        }

        @Override
        public void assertActive(BankAccount account, String messageIfNotActive) {
                if (!BankAccountStatus.ACTIVE.equals(account.getStatus().getName())) {
                        throw new ConflictException(messageIfNotActive);
                }
        }

        @Override
        @Transactional
        public BankAccount updateBalance(BankAccount account, BigDecimal newBalance) {
                account.setBalance(newBalance);
                return bankAccountRepository.save(account);
        }

        private String generateIban() {
                String countryCode = "IT";
                String bban = UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();

                String rearranged = bban + countryCode + "00";
                String numeric = rearranged.chars()
                                .mapToObj(c -> Character.isDigit(c) ? String.valueOf((char) c)
                                                : String.valueOf(c - 'A' + 10))
                                .collect(java.util.stream.Collectors.joining());
                int checkDigits = 98
                                - new java.math.BigInteger(numeric).mod(java.math.BigInteger.valueOf(97)).intValue();
                return String.format("%s%02d%s", countryCode, checkDigits, bban);
        }

        private BankAccountSummaryResponse toSummaryResponse(BankAccount account) {
                return BankAccountSummaryResponse.builder()
                                .id(account.getId())
                                .iban(account.getIban())
                                .balance(account.getBalance())
                                .contableBalance(account.getContableBalance())
                                .userId(account.getUser().getId())
                                .statusName(account.getStatus() != null ? account.getStatus().getName() : null)
                                .openingDate(account.getOpeningDate())
                                .createdAt(account.getCreatedAt())
                                .build();
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
