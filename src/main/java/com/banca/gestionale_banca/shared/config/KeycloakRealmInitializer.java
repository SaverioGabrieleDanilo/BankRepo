package com.banca.gestionale_banca.shared.config;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakRealmInitializer implements CommandLineRunner {

    private static final String REALM = "gestionale-banca";
    private static final List<String> REALM_ROLES = List.of(Ruoli.ADMIN, Ruoli.EMPLOYEE, Ruoli.CUSTOMER);

    // length(8): coerente con @Pattern su RegisterRequest.password.
    // notUsername: la password non puo' contenere lo username.
    private static final String PASSWORD_POLICY =
            "length(8) and digits(1) and upperCase(1) and lowerCase(1) and specialChars(1) and notUsername";

    private final Keycloak keycloak;

    @Override
    public void run(String... args) {
        RealmResource realmResource = keycloak.realm(REALM);

        creaRuoliMancanti(realmResource.roles());
        allineaPasswordPolicy(realmResource);
    }

    private void creaRuoliMancanti(RolesResource rolesResource) {
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

    private void allineaPasswordPolicy(RealmResource realmResource) {
        RealmRepresentation realm = realmResource.toRepresentation();
        if (!PASSWORD_POLICY.equals(realm.getPasswordPolicy())) {
            realm.setPasswordPolicy(PASSWORD_POLICY);
            realmResource.update(realm);
            log.info("Password policy del realm '{}' allineata a: {}", REALM, PASSWORD_POLICY);
        }
    }
}
