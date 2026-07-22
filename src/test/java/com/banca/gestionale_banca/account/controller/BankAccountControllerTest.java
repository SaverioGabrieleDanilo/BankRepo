package com.banca.gestionale_banca.account.controller;

import com.banca.gestionale_banca.account.dto.AccountLimitsRequest;
import com.banca.gestionale_banca.account.dto.AccountLimitsResponse;
import com.banca.gestionale_banca.account.dto.AccountStatusRequest;
import com.banca.gestionale_banca.account.dto.ApproveAccountRequest;
import com.banca.gestionale_banca.account.dto.BankAccountAdminResponse;
import com.banca.gestionale_banca.account.dto.BankAccountResponse;
import com.banca.gestionale_banca.account.service.AccountLimitsService;
import com.banca.gestionale_banca.account.service.BankAccountService;
import com.banca.gestionale_banca.shared.security.AuditLogger;
import com.banca.gestionale_banca.shared.security.AuthorizationFacade;
import com.banca.gestionale_banca.shared.security.SecurityConfig;
import com.banca.gestionale_banca.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica che i @PreAuthorize su BankAccountController blocchino/consentano
 * le richieste lungo tutta la catena HTTP (non solo a livello di service).
 */
@WebMvcTest(BankAccountController.class)
@Import({SecurityConfig.class, AuthorizationFacade.class, AuditLogger.class})
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BankAccountService bankAccountService;

    @MockitoBean
    private AccountLimitsService accountLimitsService;

    @MockitoBean
    private TransactionService transactionService;

    private BankAccountResponse contoResponse() {
        return BankAccountResponse.builder().id(1L).iban("IT60X0542811101000000123456").build();
    }

    @Test
    void apriConto_conRuoloCustomer_e200() throws Exception {
        when(bankAccountService.openBankAccount(any())).thenReturn(contoResponse());

        mockMvc.perform(post("/api/bank-accounts/opening")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void apriConto_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(post("/api/bank-accounts/opening")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void apriConto_senzaAutenticazione_e401() throws Exception {
        mockMvc.perform(post("/api/bank-accounts/opening"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvaConto_conRuoloEmployee_e200() throws Exception {
        when(bankAccountService.approveBankAccount(eq(1L), eq(true))).thenReturn(contoResponse());

        ApproveAccountRequest request = new ApproveAccountRequest();
        request.setApproved(true);

        mockMvc.perform(patch("/api/bank-accounts/1/approve")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void approvaConto_conRuoloCustomer_e403() throws Exception {
        ApproveAccountRequest request = new ApproveAccountRequest();
        request.setApproved(true);

        mockMvc.perform(patch("/api/bank-accounts/1/approve")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chiudiConto_conRuoloCustomer_e200() throws Exception {
        when(bankAccountService.closeBankAccount(eq(1L), any(), anyBoolean())).thenReturn(contoResponse());

        mockMvc.perform(post("/api/bank-accounts/1/closure")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void chiudiConto_conRuoloEmployee_e200() throws Exception {
        when(bankAccountService.closeBankAccount(eq(1L), any(), anyBoolean())).thenReturn(contoResponse());

        mockMvc.perform(post("/api/bank-accounts/1/closure")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void chiudiConto_senzaRuolo_e403() throws Exception {
        mockMvc.perform(post("/api/bank-accounts/1/closure")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConto_conRuoloCustomer_e200() throws Exception {
        when(bankAccountService.getBankAccountById(eq(1L), any(), anyBoolean())).thenReturn(contoResponse());

        mockMvc.perform(get("/api/bank-accounts/1")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getConto_conRuoloEmployee_e200() throws Exception {
        when(bankAccountService.getBankAccountById(eq(1L), any(), anyBoolean())).thenReturn(contoResponse());

        mockMvc.perform(get("/api/bank-accounts/1")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void getConto_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(get("/api/bank-accounts/1")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransazioniConto_conRuoloCustomer_e200() throws Exception {
        Page<com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse> page = new PageImpl<>(List.of());
        when(transactionService.getTransactionsByAccount(eq(1L), any(), anyBoolean(), any())).thenReturn(page);

        mockMvc.perform(get("/api/bank-accounts/1/transactions")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getTransazioniConto_conRuoloEmployee_e200() throws Exception {
        Page<com.banca.gestionale_banca.transaction.dto.TransactionAdminResponse> page = new PageImpl<>(List.of());
        when(transactionService.getTransactionsByAccount(eq(1L), any(), anyBoolean(), any())).thenReturn(page);

        mockMvc.perform(get("/api/bank-accounts/1/transactions")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void getTransazioniConto_conRuoloAdmin_e403() throws Exception {
        mockMvc.perform(get("/api/bank-accounts/1/transactions")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listaConti_conRuoloAdmin_e200() throws Exception {
        Page<BankAccountAdminResponse> page = new PageImpl<>(List.of());
        when(bankAccountService.listBankAccounts(any())).thenReturn(page);

        mockMvc.perform(get("/api/bank-accounts")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void listaConti_conRuoloEmployee_e200() throws Exception {
        Page<BankAccountAdminResponse> page = new PageImpl<>(List.of());
        when(bankAccountService.listBankAccounts(any())).thenReturn(page);

        mockMvc.perform(get("/api/bank-accounts")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void listaConti_conRuoloCustomer_e403() throws Exception {
        mockMvc.perform(get("/api/bank-accounts")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLimiti_conRuoloCustomer_e200() throws Exception {
        when(accountLimitsService.getBankAccountLimits(eq(1L), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(get("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getLimiti_conRuoloAdmin_e200() throws Exception {
        when(accountLimitsService.getBankAccountLimits(eq(1L), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(get("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getLimiti_conRuoloEmployee_e200() throws Exception {
        when(accountLimitsService.getBankAccountLimits(eq(1L), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(get("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk());
    }

    @Test
    void impostaLimiti_conRuoloEmployee_e200() throws Exception {
        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(java.math.BigDecimal.valueOf(1000));
        request.setSingleTransactionLimit(java.math.BigDecimal.valueOf(500));
        request.setMonthlyTransferLimit(java.math.BigDecimal.valueOf(5000));

        when(accountLimitsService.setBankAccountLimits(eq(1L), any(), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(put("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void impostaLimiti_conRuoloAdmin_e200() throws Exception {
        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(java.math.BigDecimal.valueOf(1000));
        request.setSingleTransactionLimit(java.math.BigDecimal.valueOf(500));
        request.setMonthlyTransferLimit(java.math.BigDecimal.valueOf(5000));

        when(accountLimitsService.setBankAccountLimits(eq(1L), any(), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(put("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void impostaLimiti_conRuoloCustomer_proprietario_e200() throws Exception {
        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(java.math.BigDecimal.valueOf(1000));
        request.setSingleTransactionLimit(java.math.BigDecimal.valueOf(500));
        request.setMonthlyTransferLimit(java.math.BigDecimal.valueOf(5000));

        when(accountLimitsService.setBankAccountLimits(eq(1L), any(), any(), anyBoolean()))
                .thenReturn(AccountLimitsResponse.builder().accountId(1L).build());

        mockMvc.perform(put("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void impostaLimiti_conValoreOltreDecimalMax_e400() throws Exception {
        AccountLimitsRequest request = new AccountLimitsRequest();
        request.setDailyWithdrawalLimit(java.math.BigDecimal.valueOf(999999));
        request.setSingleTransactionLimit(java.math.BigDecimal.valueOf(500));
        request.setMonthlyTransferLimit(java.math.BigDecimal.valueOf(5000));

        mockMvc.perform(put("/api/bank-accounts/1/limits")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiaStatoConto_conRuoloEmployee_e200() throws Exception {
        AccountStatusRequest request = new AccountStatusRequest();
        request.setStatus("CLOSED");

        when(bankAccountService.changeBankAccountStatus(eq(1L), eq("CLOSED"))).thenReturn(contoResponse());

        mockMvc.perform(patch("/api/bank-accounts/1/status")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void cambiaStatoConto_conRuoloAdmin_e200() throws Exception {
        AccountStatusRequest request = new AccountStatusRequest();
        request.setStatus("ACTIVE");

        when(bankAccountService.changeBankAccountStatus(eq(1L), eq("ACTIVE"))).thenReturn(contoResponse());

        mockMvc.perform(patch("/api/bank-accounts/1/status")
                        .with(jwt().jwt(j -> j.subject("admin-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void cambiaStatoConto_conRuoloCustomer_e403() throws Exception {
        AccountStatusRequest request = new AccountStatusRequest();
        request.setStatus("CLOSED");

        mockMvc.perform(patch("/api/bank-accounts/1/status")
                        .with(jwt().jwt(j -> j.subject("customer-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiaStatoConto_conStatoNonValido_e400() throws Exception {
        AccountStatusRequest request = new AccountStatusRequest();
        request.setStatus("BOH");

        mockMvc.perform(patch("/api/bank-accounts/1/status")
                        .with(jwt().jwt(j -> j.subject("employee-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
