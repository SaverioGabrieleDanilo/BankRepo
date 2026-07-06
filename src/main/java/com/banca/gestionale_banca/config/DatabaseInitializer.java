package com.banca.gestionale_banca.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.banca.gestionale_banca.model.RegistrationStatus;
import com.banca.gestionale_banca.model.Role;
import com.banca.gestionale_banca.model.UserStatus;
import com.banca.gestionale_banca.repository.RegistrationStatusRepository;
import com.banca.gestionale_banca.repository.RoleRepository;
import com.banca.gestionale_banca.repository.UserStatusRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final RegistrationStatusRepository regStatusRepository;

    @Override
    @Transactional
    public void run(String... args) {

        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                new Role("ADMIN"),
                new Role("OPERATORE"),
                new Role("CUSTOMER")
            ));
        }


        if (userStatusRepository.count() == 0) {
            userStatusRepository.saveAll(List.of(
                new UserStatus("ATTIVO"),
                new UserStatus("SOSPESO"),
                new UserStatus("CHIUSO")
            ));
        }


        if (regStatusRepository.count() == 0) {
            regStatusRepository.saveAll(List.of(
                new RegistrationStatus("PENDING"),
                new RegistrationStatus("APPROVED"),
                new RegistrationStatus("REJECTED")
            ));
        }
    }
}
