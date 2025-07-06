package com.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WalletMetricsTest {

    private MeterRegistry meterRegistry;
    private WalletMetrics walletMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        walletMetrics = new WalletMetrics(meterRegistry);
    }

    @Test
    void shouldIncrementWalletCreatedCounter() {
        walletMetrics.incrementWalletCreated();
        
        assertEquals(1.0, meterRegistry.counter("wallet_created_total").count());
    }

    @Test
    void shouldIncrementDepositCounter() {
        walletMetrics.incrementDeposit();
        
        assertEquals(1.0, meterRegistry.counter("wallet_deposits_total").count());
    }

    @Test
    void shouldIncrementWithdrawalCounter() {
        walletMetrics.incrementWithdrawal();
        
        assertEquals(1.0, meterRegistry.counter("wallet_withdrawals_total").count());
    }

    @Test
    void shouldIncrementTransferCounter() {
        walletMetrics.incrementTransfer();
        
        assertEquals(1.0, meterRegistry.counter("wallet_transfers_total").count());
    }

    @Test
    void shouldIncrementBusinessErrorCounter() {
        walletMetrics.incrementBusinessError("validation_error");
        
        assertEquals(1.0, meterRegistry.counter("wallet_business_errors_total", "type", "validation_error").count());
    }

    @Test
    void shouldStartAndRecordDepositTimer() {
        Timer.Sample sample = walletMetrics.startDepositTimer();
        assertNotNull(sample);
        
        walletMetrics.recordDepositDuration(sample);
        
        assertTrue(meterRegistry.timer("wallet_deposit_duration_seconds").count() > 0);
    }

    @Test
    void shouldStartAndRecordTransferTimer() {
        Timer.Sample sample = walletMetrics.startTransferTimer();
        assertNotNull(sample);
        
        walletMetrics.recordTransferDuration(sample);
        
        assertTrue(meterRegistry.timer("wallet_transfer_duration_seconds").count() > 0);
    }
    
    @Test
    void shouldIncrementDatabaseErrorCounter() {
        walletMetrics.incrementDatabaseError("connection_error");
        
        assertEquals(1.0, meterRegistry.counter("wallet_database_errors_total", "operation", "connection_error").count());
    }
    
    @Test
    void shouldStartAndRecordDatabaseTimer() {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        assertNotNull(sample);
        
        walletMetrics.recordDatabaseDuration(sample, "query");
        
        assertTrue(meterRegistry.timer("wallet_database_duration_seconds", "operation", "query").count() > 0);
    }
    
    @Test
    void shouldStartAndRecordWithdrawalTimer() {
        Timer.Sample sample = walletMetrics.startWithdrawalTimer();
        assertNotNull(sample);
        
        walletMetrics.recordWithdrawalDuration(sample);
        
        assertTrue(meterRegistry.timer("wallet_withdrawal_duration_seconds").count() > 0);
    }
}