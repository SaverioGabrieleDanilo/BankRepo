package com.banca.gestionale_banca.shared.config;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(4)
public class TestUsersBootstrapper implements CommandLineRunner {

    @Value("${app.bootstrap.test-users.enabled:false}")
    private boolean enabled;

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        createUser("admin", "Admin@123", "Super", "Admin", "admin@test.com", Ruoli.ADMIN);
        createUser("employee", "Employee@123", "Mario", "Rossi", "employee@test.com", Ruoli.EMPLOYEE);
        createUser("customer", "Customer@123", "Luca", "Bianchi", "customer@test.com", Ruoli.CUSTOMER);
    }

    private void createUser(String username, String password, String firstName, String lastName, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            log.info("Utente '{}' gia' esistente, skip.", username);
            return;
        }

        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setDateOfBirth(LocalDate.of(1995, 6, 15));

        try {
            userService.registerUserWithRole(request, role);
            log.warn("Creato utente test: username='{}' password='{}' ruolo='{}'", username, password, role);
        } catch (Exception e) {
            log.error("Creazione utente test '{}' fallita: {}", username, e.getMessage());
        }
    }
}
