package com.wallet.infrastructure.adapter.web;

import com.wallet.application.dto.*;
import com.wallet.application.usecase.WalletUseCase;
import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.service.AuditService;
import com.wallet.infrastructure.config.LoggingInterceptor;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import(com.wallet.infrastructure.config.TestConfig.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletUseCase walletUseCase;

    @MockBean
    private WalletMetrics walletMetrics;
    
    @MockBean
    private AuditService auditService;

    @Test
    void shouldCreateWallet() throws Exception {
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.ZERO, Instant.now(), Instant.now());
        when(walletUseCase.createWallet(any(CreateWalletRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"user123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user123"));

        verify(walletMetrics).incrementWalletCreated();
    }

    @Test
    void shouldGetWallet() throws Exception {
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.valueOf(100.00), Instant.now(), Instant.now());
        when(walletUseCase.getWallet("wallet123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/wallets/wallet123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("wallet123"));
    }

    @Test
    void shouldDeposit() throws Exception {
        Timer.Sample sample = mock(Timer.Sample.class);
        when(walletMetrics.startDepositTimer()).thenReturn(sample);
        
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.valueOf(150.00), Instant.now(), Instant.now());
        when(walletUseCase.deposit(any(TransactionRequest.class))).thenReturn(response);
        
        // Mock para AuditInfo
        AuditInfo mockAuditInfo = mock(AuditInfo.class);
        doNothing().when(auditService).createWalletSnapshot(any(), any());

        mockMvc.perform(post("/api/v1/wallets/wallet123/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        verify(walletMetrics).incrementDeposit();
    }

    @Test
    void shouldWithdraw() throws Exception {
        Timer.Sample sample = mock(Timer.Sample.class);
        when(walletMetrics.startWithdrawalTimer()).thenReturn(sample);
        
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.valueOf(50.00), Instant.now(), Instant.now());
        when(walletUseCase.withdraw(any(TransactionRequest.class))).thenReturn(response);
        
        // Mock para AuditInfo
        doNothing().when(auditService).createWalletSnapshot(any(), any());

        mockMvc.perform(post("/api/v1/wallets/wallet123/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));

        verify(walletMetrics).incrementWithdrawal();
    }

    @Test
    void shouldTransfer() throws Exception {
        Timer.Sample sample = mock(Timer.Sample.class);
        when(walletMetrics.startTransferTimer()).thenReturn(sample);
        
        // Mock para AuditInfo
        doNothing().when(auditService).createWalletSnapshot(any(), any());

        mockMvc.perform(post("/api/v1/wallets/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromWalletId\":\"wallet1\",\"toWalletId\":\"wallet2\",\"amount\":25.00}"))
                .andExpect(status().isOk());

        verify(walletMetrics).incrementTransfer();
    }

    @Test
    void shouldGetHistoricalBalance() throws Exception {
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.valueOf(75.00), Instant.now(), Instant.now());
        when(walletUseCase.getHistoricalBalance(eq("wallet123"), any(Instant.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/wallets/wallet123/balance/historical")
                .param("timestamp", "2024-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.00));
    }
}