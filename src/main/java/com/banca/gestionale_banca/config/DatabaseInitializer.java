package com.banca.gestionale_banca.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.banca.gestionale_banca.constants.Ruoli;
import com.banca.gestionale_banca.constants.StatiRegistrazione;
import com.banca.gestionale_banca.constants.StatiUtente;
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
                new Role(Ruoli.ADMIN),
                new Role(Ruoli.EMPLOYEE),
                new Role(Ruoli.CUSTOMER)
            ));
        }


        if (userStatusRepository.count() == 0) {
            userStatusRepository.saveAll(List.of(
                new UserStatus(StatiUtente.ATTIVO),
                new UserStatus(StatiUtente.SOSPESO),
                new UserStatus(StatiUtente.CHIUSO)
            ));
        }


        if (regStatusRepository.count() == 0) {
            regStatusRepository.saveAll(List.of(
                new RegistrationStatus(StatiRegistrazione.PENDING),
                new RegistrationStatus(StatiRegistrazione.APPROVED),
                new RegistrationStatus(StatiRegistrazione.REJECTED)
            ));
        }
    }
}
