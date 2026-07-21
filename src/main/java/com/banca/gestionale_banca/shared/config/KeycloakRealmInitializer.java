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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// Ridondante rispetto a keycloak/realm-import/gestionale-banca-realm.json (che gia'
// crea ruoli/policy al primo avvio del container Keycloak), ma resta utile come rete
// di sicurezza idempotente per volumi Docker preesistenti a quel file di import.
// @Order(2): dopo DatabaseInitializer (1), prima di DefaultAdminBootstrapper (3), che
// crea l'utente sia su questo realm sia sul DB applicativo.
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class KeycloakRealmInitializer implements CommandLineRunner {

    private static final String REALM = "gestionale-banca";
    private static final List<String> REALM_ROLES = List.of(Ruoli.ADMIN, Ruoli.EMPLOYEE, Ruoli.CUSTOMER);

    // length(8): coerente con @Pattern su RegisterRequest.password.
    // notUsername: la password non puo' contenere lo username.
    private static final String PASSWORD_POLICY =
            "length(8) and digits(1) and upperCase(1) and lowerCase(1) and specialChars(1) and notUsername";

    // Stessa soglia del vecchio lockout applicativo in AuthController (rimosso: il
    // login e' ora Authorization Code + PKCE via keycloak-js, quindi e' Keycloak
    // stesso, non piu' il nostro backend, a dover applicare il blocco).
    private static final int FAILURE_FACTOR = 5;
    private static final int WAIT_SECONDS = 15 * 60;

    private final Keycloak keycloak;

    @Override
    public void run(String... args) {
        RealmResource realmResource = keycloak.realm(REALM);

        creaRuoliMancanti(realmResource.roles());
        allineaPasswordPolicy(realmResource);
        allineaBruteForceProtection(realmResource);
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

    private void allineaBruteForceProtection(RealmResource realmResource) {
        RealmRepresentation realm = realmResource.toRepresentation();
        boolean alreadyAligned = Boolean.TRUE.equals(realm.isBruteForceProtected())
                && Integer.valueOf(FAILURE_FACTOR).equals(realm.getFailureFactor())
                && Integer.valueOf(WAIT_SECONDS).equals(realm.getWaitIncrementSeconds())
                && Integer.valueOf(WAIT_SECONDS).equals(realm.getMaxFailureWaitSeconds());

        if (!alreadyAligned) {
            realm.setBruteForceProtected(true);
            realm.setPermanentLockout(false);
            realm.setFailureFactor(FAILURE_FACTOR);
            realm.setWaitIncrementSeconds(WAIT_SECONDS);
            realm.setMaxFailureWaitSeconds(WAIT_SECONDS);
            realm.setMinimumQuickLoginWaitSeconds(WAIT_SECONDS);
            realmResource.update(realm);
            log.info("Brute-force protection del realm '{}' allineata: blocco {} min dopo {} tentativi falliti",
                    REALM, WAIT_SECONDS / 60, FAILURE_FACTOR);
        }
    }
}
