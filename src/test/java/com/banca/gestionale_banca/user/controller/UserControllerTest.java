package com.banca.gestionale_banca.user.controller;

import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.shared.security.SecurityConfig;
import com.banca.gestionale_banca.user.dto.UpdateUserRequest;
import com.banca.gestionale_banca.user.model.RegistrationStatus;
import com.banca.gestionale_banca.user.model.Role;
import com.banca.gestionale_banca.user.model.UserStatus;
import com.banca.gestionale_banca.user.model.User;
import com.banca.gestionale_banca.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuditLogger.class, AuthorizationFacade.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    private User contoCustomer() {
        User u = new User();
        u.setId(1L);
        u.setKeycloakId("customer-keycloak-id");
        return u;
    }

    /**
     * UserResponse.from() chiama getRole()/getStatus()/getRegistrationStatus() senza
     * controlli null: il valore restituito da updateUser() deve averli popolati,
     * altrimenti la mappatura verso la response va in NullPointerException (-> 500).
     */
    private User contoCustomerCompleto() {
        User u = contoCustomer();
        u.setUsername("mario.rossi");
        u.setEmail("mario.rossi@example.com");
        u.setFirstName("Mario");
        u.setLastName("Rossi");
        u.setRole(new Role("CUSTOMER"));
        u.setStatus(new UserStatus("ACTIVE"));
        u.setRegistrationStatus(new RegistrationStatus("APPROVED"));
        return u;
    }

    @Test
    void ownerNonAdmin_provaACambiareIlProprioRuolo_vieneRifiutatoCon403() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomer()));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("ADMIN");

        mockMvc.perform(put("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("customer-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void ownerNonAdmin_modificaSoloEmail_vieneAccettato() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomer()));
        when(userService.updateUser(eq(1L), any())).thenReturn(contoCustomerCompleto());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("nuova@email.it");

        mockMvc.perform(put("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("customer-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(1L), any());
    }

    @Test
    void admin_puoCambiareIlRuoloDiUnAltroUtente() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomer()));
        when(userService.updateUser(eq(1L), any())).thenReturn(contoCustomerCompleto());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("EMPLOYEE");

        mockMvc.perform(put("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("admin-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(1L), any());
    }

    @Test
    void owner_leggeIlProprioProfilo_e200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomerCompleto()));

        mockMvc.perform(get("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("customer-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void employee_leggeIlProprioProfilo_e200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomerCompleto()));

        mockMvc.perform(get("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("customer-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void admin_leggeProfiloDiUnAltroUtente_e200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomerCompleto()));

        mockMvc.perform(get("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("admin-keycloak-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void employee_leggeProfiloDiUnAltroUtente_e200() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomerCompleto()));

        mockMvc.perform(get("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("un-altro-employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwnerNonAdmin_leggeProfiloAltrui_vieneRifiutatoCon403() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(contoCustomer()));

        mockMvc.perform(get("/api/users/1")
                        .with(jwt().jwt(j -> j.subject("un-altro-customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaUtenti_conRuoloAdmin_e200() throws Exception {
        when(userService.getPaginatedUsers(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void listaUtenti_conRuoloEmployee_e200() throws Exception {
        when(userService.getPaginatedUsers(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void listaUtenti_conRuoloCustomer_e403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }
}
