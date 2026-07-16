package com.banca.gestionale_banca.shared.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.banca.gestionale_banca.user.service.UserService;

import lombok.RequiredArgsConstructor;

// @Order(1): DefaultAdminBootstrapper (Order 3) dipende dai ruoli/stati seedati qui.
@Component
@RequiredArgsConstructor
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        userService.seedBaseData();
    }
}
