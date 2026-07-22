package com.banca.gestionale_banca.user.service;

import com.banca.gestionale_banca.shared.constants.Ruoli;
import com.banca.gestionale_banca.shared.exception.ConflictException;
import com.banca.gestionale_banca.shared.exception.ExternalServiceException;
import com.banca.gestionale_banca.shared.exception.ResourceNotFoundException;
import com.banca.gestionale_banca.user.constants.RegistrationStatuses;
import com.banca.gestionale_banca.user.constants.UserStatuses;
import com.banca.gestionale_banca.user.dto.RegisterRequest;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.model.RegistrationStatus;
import com.banca.gestionale_banca.user.model.Role;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.model.UserStatus;
import com.banca.gestionale_banca.user.repository.RegistrationStatusRepository;
import com.banca.gestionale_banca.user.repository.RoleRepository;
import com.banca.gestionale_banca.user.repository.UserRepository;
import com.banca.gestionale_banca.user.repository.UserStatusRepository;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Keycloak keycloak;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private RegistrationStatusRepository registrationStatusRepository;

    @Mock
    private RealmResource realmResource;
    @Mock
    private UsersResource usersResource;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, keycloak, roleRepository, userStatusRepository, registrationStatusRepository);

        lenient().when(keycloak.realm("gestionale-banca")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
    }

    private RegisterRequest richiestaValida() {
        return new RegisterRequest("mrossi", "Password#1", "Mario", "Rossi", "mario.rossi@example.com", LocalDate.of(1990, 1, 1));
    }

    @Test
    void registerUser_successo_creaUtenteSuKeycloakEInLocale() {
        RegisterRequest request = richiestaValida();

        when(userRepository.existsByUsername("mrossi")).thenReturn(false);
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(roleRepository.findByName(Ruoli.CUSTOMER)).thenReturn(Optional.of(new Role(Ruoli.CUSTOMER)));

        Response creationResponse = mock(Response.class);
        when(creationResponse.getStatus()).thenReturn(201);
        when(creationResponse.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(creationResponse.getLocation()).thenReturn(URI.create("http://keycloak/admin/realms/gestionale-banca/users/kc-123"));
        when(usersResource.create(any())).thenReturn(creationResponse);

        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(Ruoli.CUSTOMER)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

        UserResource kcUserResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);
        when(usersResource.get("kc-123")).thenReturn(kcUserResource);
        when(kcUserResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        when(userStatusRepository.findByName(UserStatuses.ACTIVE)).thenReturn(Optional.of(new UserStatus(UserStatuses.ACTIVE)));
        when(registrationStatusRepository.findByName(RegistrationStatuses.PENDING)).thenReturn(Optional.of(new RegistrationStatus(RegistrationStatuses.PENDING)));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.registerUser(request);

        assertEquals("mrossi", result.getUsername());
        assertEquals("kc-123", result.getKeycloakId());
        verify(roleScopeResource).add(any());
    }

    @Test
    void registerUser_usernameGiaInUso_lanciaConflictExceptionSenzaChiamareKeycloak() {
        RegisterRequest request = richiestaValida();
        when(userRepository.existsByUsername("mrossi")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.registerUser(request));

        verify(keycloak, never()).realm(any());
    }

    @Test
    void registerUser_realmInesistente_lanciaExternalServiceException() {
        RegisterRequest request = richiestaValida();
        when(userRepository.existsByUsername("mrossi")).thenReturn(false);
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(roleRepository.findByName(Ruoli.CUSTOMER)).thenReturn(Optional.of(new Role(Ruoli.CUSTOMER)));
        when(realmResource.toRepresentation()).thenThrow(new NotFoundException());

        assertThrows(ExternalServiceException.class, () -> service.registerUser(request));

        verify(usersResource, never()).create(any());
    }

    @Test
    void registerUser_ruoloNonAssegnabile_ripulisceUtenteOrfanoSuKeycloak() {
        RegisterRequest request = richiestaValida();
        when(userRepository.existsByUsername("mrossi")).thenReturn(false);
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(roleRepository.findByName(Ruoli.CUSTOMER)).thenReturn(Optional.of(new Role(Ruoli.CUSTOMER)));

        Response creationResponse = mock(Response.class);
        when(creationResponse.getStatus()).thenReturn(201);
        when(creationResponse.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(creationResponse.getLocation()).thenReturn(URI.create("http://keycloak/admin/realms/gestionale-banca/users/kc-999"));
        when(usersResource.create(any())).thenReturn(creationResponse);

        RolesResource rolesResource = mock(RolesResource.class);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(Ruoli.CUSTOMER)).thenThrow(new NotFoundException());

        UserResource kcUserResource = mock(UserResource.class);
        when(usersResource.get("kc-999")).thenReturn(kcUserResource);

        assertThrows(ExternalServiceException.class, () -> service.registerUser(request));

        verify(kcUserResource).remove();
    }

    @Test
    void updateUser_ruoloInesistente_lanciaResourceNotFoundSenzaToccareKeycloak() {
        User existing = new User();
        existing.setId(1L);
        existing.setKeycloakId("kc-1");
        when(userRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName("BOH")).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("BOH");

        assertThrows(ResourceNotFoundException.class, () -> service.updateUser(1L, request));

        verify(keycloak, never()).realm(any());
    }

    @Test
    void changeUserStatus_utenteAdmin_lanciaConflictExceptionSenzaToccareKeycloak() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(new Role(Ruoli.ADMIN));
        when(userRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> service.changeUserStatus(1L, UserStatuses.SUSPENDED));

        verify(keycloak, never()).realm(any());
    }

    @Test
    void seedBaseData_creaSoloLeRigheMancanti() {
        when(roleRepository.findByName(Ruoli.ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.findByName(Ruoli.EMPLOYEE)).thenReturn(Optional.of(new Role(Ruoli.EMPLOYEE)));
        when(roleRepository.findByName(Ruoli.CUSTOMER)).thenReturn(Optional.empty());

        when(userStatusRepository.findByName(any())).thenReturn(Optional.of(new UserStatus("X")));
        when(registrationStatusRepository.findByName(any())).thenReturn(Optional.of(new RegistrationStatus("X")));

        service.seedBaseData();

        verify(roleRepository).save(argThat(r -> Ruoli.ADMIN.equals(r.getName())));
        verify(roleRepository).save(argThat(r -> Ruoli.CUSTOMER.equals(r.getName())));
        verify(roleRepository, never()).save(argThat(r -> Ruoli.EMPLOYEE.equals(r.getName())));
    }
}
