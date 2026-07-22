package com.banca.gestionale_banca.transaction.controller;

import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.shared.security.SecurityConfig;
import com.banca.gestionale_banca.transaction.dto.DepositRequest;
import com.banca.gestionale_banca.transaction.dto.InternarlTransferRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionResponse;
import com.banca.gestionale_banca.transaction.dto.TransferRequest;
import com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse;
import com.banca.gestionale_banca.transaction.service.TransactionService;
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
import java.math.BigDecimal;
import java.util.List;

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
@Import({SecurityConfig.class, AuthorizationFacade.class, AuditLogger.class})
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
        request.setIban("IT66X0100001000000000000001");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    private DepositRequest depositoRequest() {
        DepositRequest request = new DepositRequest();
        request.setIban("IT66X0100001000000000000001");
        request.setAmount(BigDecimal.valueOf(100));
        request.setDepositType("CASH");
        request.setItemsCount(1);
        return request;
    }

    private TransferRequest transferRequest() {
        TransferRequest request = new TransferRequest();
        request.setSourceIban("IT66X0100001000000000000001");
        request.setTargetIban("IT39X0100001000000000000002");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    private InternarlTransferRequest girocontoRequest() {
        InternarlTransferRequest request = new InternarlTransferRequest();
        request.setSourceIban("IT66X0100001000000000000001");
        request.setTargetIban("IT39X0100001000000000000002");
        request.setAmount(BigDecimal.valueOf(100));
        return request;
    }

    @Test
    void versamento_conRuoloCustomer_e200() throws Exception {
        when(transactionservice.executeDeposit(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/deposit")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void versamento_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/deposit")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void versamento_senzaAutenticazione_e401() throws Exception {
        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositoRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prelievo_conRuoloEmployee_e200() throws Exception {
        when(transactionservice.executeWithdrawal(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/withdraw")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void prelievo_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/withdraw")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movimentoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void bonifico_conRuoloCustomer_e200() throws Exception {
        when(transactionservice.executeTransfer(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

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
        when(transactionservice.executeAccountTransfer(any(), any(), anyBoolean())).thenReturn(movimentoResponse());

        mockMvc.perform(post("/api/transactions/internal-transfer")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(girocontoRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void giroconto_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/transactions/internal-transfer")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(girocontoRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransazione_conRuoloEmployee_e200() throws Exception {
        when(transactionservice.getTransactionById(eq(1L))).thenReturn(movimentoResponse());

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

    @Test
    void listaTransazioni_conRuoloEmployee_e200() throws Exception {
        when(transactionservice.getPaginatedTransactions(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/transactions")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void listaTransazioni_conRuoloCustomer_e403() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }
}
