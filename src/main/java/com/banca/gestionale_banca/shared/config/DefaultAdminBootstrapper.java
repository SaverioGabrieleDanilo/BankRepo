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

// Automatizza il passo "Bootstrap del primo utente ADMIN" del README (altrimenti
// manuale: creazione utente su Keycloak + INSERT a mano coerente su 'users').
// Riusa UserServiceImpl.registerUserWithRole cosi' la creazione (Keycloak + DB,
// con cleanup dell'utente Keycloak orfano in caso di fallimento) resta un solo
// percorso di codice, non duplicato qui.
@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class DefaultAdminBootstrapper implements CommandLineRunner {

    private static final String USERNAME = "admin";

    @Value("${app.bootstrap.default-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.default-admin.password:}")
    private String password;

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (password == null || password.isBlank()) {
            log.error("app.bootstrap.default-admin.enabled=true ma app.bootstrap.default-admin.password non impostata: bootstrap ADMIN saltato.");
            return;
        }
        if (userRepository.existsByUsername(USERNAME)) {
            return;
        }

        RegisterRequest request = new RegisterRequest();
        request.setUsername(USERNAME);
        request.setPassword(password);
        request.setFirstName("Super");
        request.setLastName("Admin");
        request.setEmail("admin@example.com");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        try {
            userService.registerUserWithRole(request, Ruoli.ADMIN);
            log.warn("Creato utente ADMIN di bootstrap (username='{}') - SOLO SVILUPPO LOCALE. " +
                    "Disabilita 'app.bootstrap.default-admin.enabled' prima di qualunque ambiente condiviso.",
                    USERNAME);
        } catch (Exception e) {
            log.error("Bootstrap ADMIN di default fallito: {}", e.getMessage(), e);
        }
    }
}
