package com.banca.gestionale_banca.shared.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.banca.gestionale_banca.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        userService.seedDatiBase();
    }
}
