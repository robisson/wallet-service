package com.wallet.application.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class DTOTest {

    @Test
    void shouldCreateCreateWalletRequest() {
        CreateWalletRequest request = new CreateWalletRequest("user123");
        assertEquals("user123", request.userId());
    }

    @Test
    void shouldCreateAmountRequest() {
        AmountRequest request = new AmountRequest(BigDecimal.valueOf(100.00));
        assertEquals(BigDecimal.valueOf(100.00), request.amount());
    }

    @Test
    void shouldCreateTransactionRequest() {
        TransactionRequest request = new TransactionRequest("wallet123", BigDecimal.valueOf(50.00));
        assertEquals("wallet123", request.walletId());
        assertEquals(BigDecimal.valueOf(50.00), request.amount());
    }

    @Test
    void shouldCreateTransferRequest() {
        TransferRequest request = new TransferRequest("wallet1", "wallet2", BigDecimal.valueOf(25.00));
        assertEquals("wallet1", request.fromWalletId());
        assertEquals("wallet2", request.toWalletId());
        assertEquals(BigDecimal.valueOf(25.00), request.amount());
    }

    @Test
    void shouldCreateWalletResponse() {
        Instant now = Instant.now();
        WalletResponse response = new WalletResponse("wallet123", "user123", BigDecimal.valueOf(100.00), now, now);
        assertEquals("wallet123", response.walletId());
        assertEquals("user123", response.userId());
        assertEquals(BigDecimal.valueOf(100.00), response.balance());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
}