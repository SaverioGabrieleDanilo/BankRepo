package com.banca.gestionale_banca.user.service;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import com.banca.gestionale_banca.user.constants.StatiRegistrazione;
import com.banca.gestionale_banca.user.constants.StatiUtente;
import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ExternalServiceException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.user.model.RegistrationStatus;
import com.banca.gestionale_banca.user.model.Role;
import com.banca.gestionale_banca.user.model.UserStatus;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.repository.RegistrationStatusRepository;
import com.banca.gestionale_banca.user.repository.RoleRepository;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.user.repository.UserStatusRepository;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository userrepo;
    private final Keycloak keycloak; // Assicurati che questo bean punti al realm corretto
    private final RoleRepository roleRepository;
    private final UserStatusRepository userStatusRepository;
    private final RegistrationStatusRepository registrationStatusRepository;

    // Costante per il nome del realm target (dove vivono gli utenti applicativi)
    private static final String REALM = "gestionale-banca";

    // creaUtente() chiama Keycloak (I/O di rete) prima del salvataggio locale:
    // niente @Transactional qui, per non tenere aperta una connessione DB per
    // tutta la durata della chiamata esterna (vedi audit UserServiceImpl).
    @Override
    public User registerUser(RegisterRequest request) {
        return creaUtente(request, Ruoli.CUSTOMER);
    }

    @Override
    public User registerUserWithRole(RegisterRequest request, String role) {
        return creaUtente(request, role);
    }

    private User creaUtente(RegisterRequest request, String roleName) {
        if (userrepo.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username già in uso");
        }
        if (userrepo.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email già in uso");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Ruolo '" + roleName + "' non trovato"));

        // Verifica esplicita che il realm target esista PRIMA di provare a creare l'utente.
        // Questo trasforma un 404 criptico in un errore chiaro e diagnosticabile.
        RealmResource realmResource;
        try {
            realmResource = keycloak.realm(REALM);
            realmResource.toRepresentation(); // forza una chiamata reale, così un realm inesistente fallisce qui
        } catch (NotFoundException e) {
            log.error("Il realm '{}' non esiste su Keycloak. Crealo dalla Admin Console prima di registrare utenti.", REALM);
            throw new ExternalServiceException("Configurazione Keycloak non valida: realm '" + REALM + "' non trovato", e);
        } catch (Exception e) {
            log.error("Impossibile contattare Keycloak: {}", e.getMessage());
            throw new ExternalServiceException("Servizio di autenticazione non raggiungibile", e);
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getPassword());
        cred.setTemporary(false);
        user.setCredentials(Collections.singletonList(cred));

        log.info("Tentativo di creazione utente nel realm: {}", REALM);

        UsersResource usersResource = realmResource.users();
        String keycloakId = null;

        try (Response response = usersResource.create(user)) {
            int status = response.getStatus();

            if (status == 404) {
                // A questo punto il realm esiste (verificato sopra), quindi un 404 qui
                // indica quasi sempre un endpoint/client sbagliato (es. clientId admin-cli
                // senza permessi 'manage-users' sul realm target, oppure server URL errato).
                log.error("404 dalla creazione utente su Keycloak nonostante il realm '{}' esista. " +
                        "Verifica i permessi del client admin e l'URL del server Keycloak.", REALM);
                throw new ExternalServiceException(
                        "Errore nella creazione utente su Keycloak: 404 - verificare permessi del client admin sul realm '" + REALM + "'");
            }

            if (status != 201) {
                String body = response.hasEntity() ? response.readEntity(String.class) : "";
                log.error("Errore Keycloak: status {} - body: {}", status, body);
                throw new ExternalServiceException("Errore nella creazione utente su Keycloak: " + status);
            }

            keycloakId = CreatedResponseUtil.getCreatedId(response);

            // Assegnazione ruolo
            assignRole(realmResource, usersResource, keycloakId, role.getName());

            UserStatus active = userStatusRepository.findByName(StatiUtente.ATTIVO)
                    .orElseThrow(() -> new ResourceNotFoundException("Stato ATTIVO non trovato"));
            RegistrationStatus pending = registrationStatusRepository.findByName(StatiRegistrazione.PENDING)
                    .orElseThrow(() -> new ResourceNotFoundException("Stato PENDING non trovato"));

            User u = new User();
            u.setKeycloakId(keycloakId);
            u.setUsername(request.getUsername());
            u.setEmail(request.getEmail());
            u.setFirstName(request.getFirstName());
            u.setLastName(request.getLastName());
            u.setDateOfBirth(request.getDateOfBirth());
            u.setRole(role);
            u.setStatus(active);
            u.setRegistrationStatus(pending);

            return userrepo.save(u);

        } catch (RuntimeException e) {
            // Se l'utente è già stato creato su Keycloak ma qualcosa dopo è fallito
            // (assegnazione ruolo, stati non trovati, save su DB), elimina l'utente
            // orfano su Keycloak per evitare disallineamento tra i due sistemi.
            if (keycloakId != null) {
                try {
                    usersResource.get(keycloakId).remove();
                    log.warn("Utente Keycloak {} rimosso dopo fallimento della registrazione locale", keycloakId);
                } catch (Exception cleanupEx) {
                    log.error("Impossibile ripulire l'utente Keycloak orfano {}: {}", keycloakId, cleanupEx.getMessage());
                }
            }
            throw e;
        }
    }

    private void assignRole(RealmResource realmResource, UsersResource usersResource, String userId, String roleName) {
        try {
            RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
            usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(realmRole));
        } catch (NotFoundException e) {
            log.error("Il ruolo realm '{}' non esiste su Keycloak nel realm target", roleName);
            throw new ExternalServiceException("Ruolo '" + roleName + "' non trovato su Keycloak", e);
        } catch (Exception e) {
            log.error("Errore durante l'assegnazione del ruolo: {}", e.getMessage());
            throw new ExternalServiceException("Impossibile assegnare il ruolo all'utente creato", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) { return userrepo.findByIdWithDetails(id); }

    @Override
    public Optional<User> findByKeycloakId(String keycloakId) { return userrepo.findByKeycloakId(keycloakId); }

    @Override
    public User updateUser(Long id, UpdateUserRequest request) {
        User u = userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Risolve il ruolo PRIMA di qualunque chiamata Keycloak: se il ruolo richiesto
        // non esiste, deve fallire qui senza aver già sincronizzato l'email, altrimenti
        // Keycloak e DB restano disallineati (email cambiata solo su Keycloak).
        Role newRole = null;
        if (request.getRole() != null) {
            newRole = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Ruolo non trovato"));
        }

        UserResource keycloakUser = realmUsers().get(u.getKeycloakId());

        if (request.getEmail() != null) {
            u.setEmail(request.getEmail());
            syncEmailToKeycloak(keycloakUser, request.getEmail());
        }
        if (newRole != null) {
            u.setRole(newRole);
            syncRoleToKeycloak(keycloakUser, u.getKeycloakId(), newRole.getName());
        }
        userrepo.save(u);
        // Ri-fetch con JOIN FETCH: save() su un'entity detached fa un merge che
        // rimpiazza le associazioni lazy con nuovi proxy legati a una sessione
        // già chiusa (niente @Transactional qui, vedi nota su creaUtente sopra:
        // evitiamo di tenere una connessione DB aperta durante l'I/O verso Keycloak).
        return userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @Override
    public void deactivateUser(Long id) {
        changeUserStatus(id, StatiUtente.CHIUSO);
    }

    @Override
    public User changeUserStatus(Long id, String statusName) {
        User u = userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        UserStatus newStatus = userStatusRepository.findByName(statusName)
                .orElseThrow(() -> new ResourceNotFoundException("Stato '" + statusName + "' non valido"));

        boolean enabled = StatiUtente.ATTIVO.equals(statusName);
        try {
            UserRepresentation rep = realmUsers().get(u.getKeycloakId()).toRepresentation();
            rep.setEnabled(enabled);
            realmUsers().get(u.getKeycloakId()).update(rep);
        } catch (Exception e) {
            log.error("Impossibile aggiornare lo stato su Keycloak per {}: {}", u.getKeycloakId(), e.getMessage());
            throw new ExternalServiceException("Impossibile aggiornare lo stato utente su Keycloak", e);
        }

        u.setStatus(newStatus);
        userrepo.save(u);
        // Vedi nota in updateUser: ri-fetch con JOIN FETCH dopo il save
        // per evitare LazyInitializationException sull'entity restituita.
        return userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @Override
    public User changeRegistrationStatus(Long id, String statusName) {
        User u = userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        RegistrationStatus newStatus = registrationStatusRepository.findByName(statusName)
                .orElseThrow(() -> new ResourceNotFoundException("Stato di registrazione '" + statusName + "' non valido"));

        u.setRegistrationStatus(newStatus);
        userrepo.save(u);
        // Vedi nota in updateUser: ri-fetch con JOIN FETCH dopo il save
        // per evitare LazyInitializationException sull'entity restituita.
        return userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @Override
    public Page<User> getPaginatedUsers(Pageable pageable) {
        return userrepo.findAllWithDetails(pageable);
    }

    // Crea solo le righe mancanti (non "solo se la tabella e' vuota"): un
    // count()==0 a livello di tabella lascerebbe 'ADMIN' permanentemente
    // assente se la tabella ha gia' qualche riga da un seed precedente
    // incompleto, facendo fallire DefaultAdminBootstrapper piu' avanti.
    @Override
    @Transactional
    public void seedBaseData() {
        creaSeMancante(roleRepository::findByName, roleRepository::save, Role::new,
                Ruoli.ADMIN, Ruoli.EMPLOYEE, Ruoli.CUSTOMER);

        creaSeMancante(userStatusRepository::findByName, userStatusRepository::save, UserStatus::new,
                StatiUtente.ATTIVO, StatiUtente.SOSPESO, StatiUtente.CHIUSO);

        creaSeMancante(registrationStatusRepository::findByName, registrationStatusRepository::save, RegistrationStatus::new,
                StatiRegistrazione.PENDING, StatiRegistrazione.APPROVED, StatiRegistrazione.REJECTED);
    }

    private <T> void creaSeMancante(java.util.function.Function<String, Optional<T>> findByName,
                                     java.util.function.Function<T, T> save,
                                     java.util.function.Function<String, T> factory,
                                     String... names) {
        for (String name : names) {
            if (findByName.apply(name).isEmpty()) {
                save.apply(factory.apply(name));
            }
        }
    }

    private UsersResource realmUsers() {
        return keycloak.realm(REALM).users();
    }

    private void syncEmailToKeycloak(UserResource keycloakUser, String email) {
        try {
            UserRepresentation rep = keycloakUser.toRepresentation();
            rep.setEmail(email);
            keycloakUser.update(rep);
        } catch (Exception e) {
            log.error("Impossibile aggiornare l'email su Keycloak: {}", e.getMessage());
            throw new ExternalServiceException("Impossibile aggiornare l'email su Keycloak", e);
        }
    }

    private void syncRoleToKeycloak(UserResource keycloakUser, String keycloakId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(REALM);
            keycloakUser.roles().realmLevel().listAll()
                    .forEach(r -> keycloakUser.roles().realmLevel().remove(Collections.singletonList(r)));
            RoleRepresentation realmRole = realmResource.roles().get(roleName).toRepresentation();
            keycloakUser.roles().realmLevel().add(Collections.singletonList(realmRole));
        } catch (NotFoundException e) {
            throw new ExternalServiceException("Ruolo '" + roleName + "' non trovato su Keycloak", e);
        } catch (Exception e) {
            log.error("Impossibile aggiornare il ruolo su Keycloak per {}: {}", keycloakId, e.getMessage());
            throw new ExternalServiceException("Impossibile aggiornare il ruolo su Keycloak", e);
        }
    }
}
