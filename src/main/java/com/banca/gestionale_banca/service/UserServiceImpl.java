package com.banca.gestionale_banca.service;

import com.banca.gestionale_banca.constants.Ruoli;
import com.banca.gestionale_banca.constants.StatiRegistrazione;
import com.banca.gestionale_banca.constants.StatiUtente;
import com.banca.gestionale_banca.dto.RegisterRequest;
import com.banca.gestionale_banca.dto.UpdateUserRequest;
import com.banca.gestionale_banca.exception.ConflictException;
import com.banca.gestionale_banca.exception.ExternalServiceException;
import com.banca.gestionale_banca.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.model.RegistrationStatus;
import com.banca.gestionale_banca.model.Role;
import com.banca.gestionale_banca.model.UserStatus;
import com.banca.gestionale_banca.model.Utente;
import com.banca.gestionale_banca.repository.RegistrationStatusRepository;
import com.banca.gestionale_banca.repository.RoleRepository;
import com.banca.gestionale_banca.repository.UserRepository;
import com.banca.gestionale_banca.repository.UserStatusRepository;

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

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

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
    public Utente registraUtente(RegisterRequest request) {
        return creaUtente(request, Ruoli.CUSTOMER);
    }

    @Override
    public Utente registraUtenteConRuolo(RegisterRequest request, String ruolo) {
        return creaUtente(request, ruolo);
    }

    private Utente creaUtente(RegisterRequest request, String ruoloNome) {
        if (userrepo.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username già in uso");
        }
        if (userrepo.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email già in uso");
        }

        Role role = roleRepository.findByName(ruoloNome)
                .orElseThrow(() -> new ResourceNotFoundException("Ruolo '" + ruoloNome + "' non trovato"));

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

            UserStatus attivo = userStatusRepository.findByName(StatiUtente.ATTIVO)
                    .orElseThrow(() -> new ResourceNotFoundException("Stato ATTIVO non trovato"));
            RegistrationStatus pending = registrationStatusRepository.findByName(StatiRegistrazione.PENDING)
                    .orElseThrow(() -> new ResourceNotFoundException("Stato PENDING non trovato"));

            Utente u = new Utente();
            u.setKeycloakId(keycloakId);
            u.setUsername(request.getUsername());
            u.setEmail(request.getEmail());
            u.setFirstName(request.getFirstName());
            u.setLastName(request.getLastName());
            u.setDateOfBirth(request.getDateOfBirth());
            u.setRole(role);
            u.setStatus(attivo);
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
    public Optional<Utente> findById(Long id) { return userrepo.findByIdWithDetails(id); }

    @Override
    public Optional<Utente> findByKeycloakId(String keycloakId) { return userrepo.findByKeycloakId(keycloakId); }

    @Override
    public Utente modificaUtente(Long id, UpdateUserRequest request) {
        Utente u = userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Risolve il ruolo PRIMA di qualunque chiamata Keycloak: se il ruolo richiesto
        // non esiste, deve fallire qui senza aver già sincronizzato l'email, altrimenti
        // Keycloak e DB restano disallineati (email cambiata solo su Keycloak).
        Role nuovoRuolo = null;
        if (request.getRole() != null) {
            nuovoRuolo = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Ruolo non trovato"));
        }

        UserResource keycloakUser = realmUsers().get(u.getKeycloakId());

        if (request.getEmail() != null) {
            u.setEmail(request.getEmail());
            syncEmailToKeycloak(keycloakUser, request.getEmail());
        }
        if (nuovoRuolo != null) {
            u.setRole(nuovoRuolo);
            syncRoleToKeycloak(keycloakUser, u.getKeycloakId(), nuovoRuolo.getName());
        }
        userrepo.save(u);
        // Ri-fetch con JOIN FETCH: save() su un'entity detached fa un merge che
        // rimpiazza le associazioni lazy con nuovi proxy legati a una sessione
        // già chiusa (niente @Transactional qui, vedi nota su creaUtente sopra:
        // evitiamo di tenere una connessione DB aperta durante l'I/O verso Keycloak).
        return userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @Override
    public void disattivaUtente(Long id) {
        cambiaStatoUtente(id, StatiUtente.CHIUSO);
    }

    @Override
    public Utente cambiaStatoUtente(Long id, String statoNome) {
        Utente u = userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        UserStatus nuovoStato = userStatusRepository.findByName(statoNome)
                .orElseThrow(() -> new ResourceNotFoundException("Stato '" + statoNome + "' non valido"));

        boolean enabled = StatiUtente.ATTIVO.equals(statoNome);
        try {
            UserRepresentation rep = realmUsers().get(u.getKeycloakId()).toRepresentation();
            rep.setEnabled(enabled);
            realmUsers().get(u.getKeycloakId()).update(rep);
        } catch (Exception e) {
            log.error("Impossibile aggiornare lo stato su Keycloak per {}: {}", u.getKeycloakId(), e.getMessage());
            throw new ExternalServiceException("Impossibile aggiornare lo stato utente su Keycloak", e);
        }

        u.setStatus(nuovoStato);
        userrepo.save(u);
        // Vedi nota in modificaUtente: ri-fetch con JOIN FETCH dopo il save
        // per evitare LazyInitializationException sull'entity restituita.
        return userrepo.findByIdWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }

    @Override
    public Page<Utente> getUtentiPaginati(Pageable pageable) {
        return userrepo.findAllWithDetails(pageable);
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
