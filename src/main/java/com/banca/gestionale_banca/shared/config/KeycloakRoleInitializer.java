package com.banca.gestionale_banca.shared.config;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakRoleInitializer implements CommandLineRunner {

    private static final String REALM = "gestionale-banca";
    private static final List<String> REALM_ROLES = List.of(Ruoli.ADMIN, Ruoli.EMPLOYEE, Ruoli.CUSTOMER);

    private final Keycloak keycloak;

    @Override
    public void run(String... args) {
        RolesResource rolesResource = keycloak.realm(REALM).roles();

        for (String roleName : REALM_ROLES) {
            try {
                rolesResource.get(roleName).toRepresentation();
            } catch (NotFoundException e) {
                RoleRepresentation role = new RoleRepresentation();
                role.setName(roleName);
                rolesResource.create(role);
                log.info("Creato ruolo realm '{}' su Keycloak (realm '{}')", roleName, REALM);
            }
        }
    }
}
