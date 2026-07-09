package com.banca.gestionale_banca.transaction.controller;

import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.shared.security.SecurityConfig;
import com.banca.gestionale_banca.transaction.dto.GirocontoRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica che i @PreAuthorize su TransactionController blocchino/consentano
 * le richieste lungo tutta la catena HTTP (non solo a livello di service).
 */
@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, AuthorizationFacade.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionservice;

    private TransactionResponse movimentoResponse() {
        return TransactionResponse.builder().transactionId(1L).build();
    }

    private TransactionRequest movimentoRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setIban("IT60X0542811101000000123456");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    private TransferRequest transferRequest() {
        TransferRequest request = new TransferRequest();
        request.setSourceIban("IT60X0542811101000000123456");
        request.setTargetIban("IT60X0542811101000000654321");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    private GirocontoRequest girocontoRequest() {
        GirocontoRequest request = new GirocontoRequest();
        request.setSourceIban("IT60X0542811101000000123456");
        request.setTargetIban("IT60X0542811101000000654321");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    @Test
    void versamento_conRuoloCustomer_e200() throws Exception {
        when(transactionservice.eseguiVersamento(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/versamento")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void versamento_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/versamento")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void versamento_senzaAutenticazione_e401() throws Exception {
        mockMvc.perform(post("/api/transactions/versamento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prelievo_conRuoloEmployee_e200() throws Exception {
        when(transactionservice.eseguiPrelievo(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/prelievo")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void prelievo_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/prelievo")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void bonifico_conRuoloCustomer_e200() throws Exception {
        when(transactionservice.eseguiBonifico(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/transfer")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void bonifico_conRuoloEmployee_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void giroconto_conRuoloCustomer_e200() throws Exception {
        when(transactionservice.eseguiGiroconto(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/giroconto")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(girocontoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void giroconto_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/giroconto")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(girocontoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransazione_conRuoloEmployee_e200() throws Exception {
        when(transactionservice.getTransazioneById(eq(1L))).thenReturn(movimentoResponse());

        mockMvc.perform(get("/api/transactions/1")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void getTransazione_conRuoloCustomer_e403() throws Exception {
        mockMvc.perform(get("/api/transactions/1")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }
}
