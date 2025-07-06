package com.wallet.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Metrics collection service for wallet operations monitoring.
 * 
 * <p>This component provides comprehensive metrics collection for:
 * <ul>
 *   <li>Business metrics - transaction counts, success rates</li>
 *   <li>Performance metrics - operation latencies, throughput</li>
 *   <li>Error metrics - business and technical error rates</li>
 *   <li>Audit metrics - audit log and snapshot counts</li>
 * </ul>
 * 
 * <p>Metrics are exposed via Micrometer and can be consumed by
 * monitoring systems like Prometheus and Grafana.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Component
public class WalletMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter walletCreatedCounter;
    private final Counter depositCounter;
    private final Counter withdrawalCounter;
    private final Counter transferCounter;
    
    // Audit metrics
    private final Counter auditLogCounter;
    private final Counter walletSnapshotCounter;
    
    private final Timer depositTimer;
    private final Timer withdrawalTimer;
    private final Timer transferTimer;
    
    /**
     * Constructs WalletMetrics with the provided meter registry.
     * 
     * <p>Initializes all counters and timers for wallet operations.
     * 
     * @param meterRegistry the Micrometer meter registry
     */
    public WalletMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.walletCreatedCounter = Counter.builder("wallet_created_total")
                .description("Total wallets created")
                .register(meterRegistry);
        
        this.depositCounter = Counter.builder("wallet_deposits_total")
                .description("Total deposits")
                .register(meterRegistry);
        
        this.withdrawalCounter = Counter.builder("wallet_withdrawals_total")
                .description("Total withdrawals")
                .register(meterRegistry);
        
        this.transferCounter = Counter.builder("wallet_transfers_total")
                .description("Total transfers")
                .register(meterRegistry);
        
        // Audit metrics
        this.auditLogCounter = Counter.builder("wallet_audit_logs_total")
                .description("Total audit logs created")
                .register(meterRegistry);
        
        this.walletSnapshotCounter = Counter.builder("wallet_snapshots_total")
                .description("Total wallet snapshots created")
                .register(meterRegistry);
        
        this.depositTimer = Timer.builder("wallet_deposit_duration_seconds")
                .description("Deposit operation duration")
                .register(meterRegistry);
        
        this.withdrawalTimer = Timer.builder("wallet_withdrawal_duration_seconds")
                .description("Withdrawal operation duration")
                .register(meterRegistry);
        
        this.transferTimer = Timer.builder("wallet_transfer_duration_seconds")
                .description("Transfer operation duration")
                .register(meterRegistry);
    }
    
    /**
     * Increments the wallet creation counter.
     */
    public void incrementWalletCreated() {
        walletCreatedCounter.increment();
    }
    
    /**
     * Increments the deposit operation counter.
     */
    public void incrementDeposit() {
        depositCounter.increment();
    }
    
    /**
     * Increments the withdrawal operation counter.
     */
    public void incrementWithdrawal() {
        withdrawalCounter.increment();
    }
    
    /**
     * Increments the transfer operation counter.
     */
    public void incrementTransfer() {
        transferCounter.increment();
    }
    
    /**
     * Increments the business error counter with error type classification.
     * 
     * @param errorType the type of business error (e.g., "insufficient_funds")
     */
    public void incrementBusinessError(String errorType) {
        Counter.builder("wallet_business_errors_total")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Increments the database error counter with operation classification.
     * 
     * @param operation the database operation that failed
     */
    public void incrementDatabaseError(String operation) {
        Counter.builder("wallet_database_errors_total")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Starts timing a deposit operation.
     * 
     * @return timer sample for duration measurement
     */
    public Timer.Sample startDepositTimer() {
        return Timer.start();
    }
    
    /**
     * Records the duration of a deposit operation.
     * 
     * @param sample the timer sample from startDepositTimer()
     */
    public void recordDepositDuration(Timer.Sample sample) {
        sample.stop(depositTimer);
    }
    
    /**
     * Starts timing a withdrawal operation.
     * 
     * @return timer sample for duration measurement
     */
    public Timer.Sample startWithdrawalTimer() {
        return Timer.start();
    }
    
    /**
     * Records the duration of a withdrawal operation.
     * 
     * @param sample the timer sample from startWithdrawalTimer()
     */
    public void recordWithdrawalDuration(Timer.Sample sample) {
        sample.stop(withdrawalTimer);
    }
    
    /**
     * Starts timing a transfer operation.
     * 
     * @return timer sample for duration measurement
     */
    public Timer.Sample startTransferTimer() {
        return Timer.start();
    }
    
    /**
     * Records the duration of a transfer operation.
     * 
     * @param sample the timer sample from startTransferTimer()
     */
    public void recordTransferDuration(Timer.Sample sample) {
        sample.stop(transferTimer);
    }
    
    /**
     * Starts timing a database operation.
     * 
     * @return timer sample for duration measurement
     */
    public Timer.Sample startDatabaseTimer() {
        return Timer.start();
    }
    
    /**
     * Records the duration of a database operation.
     * 
     * @param sample the timer sample from startDatabaseTimer()
     * @param operation the type of database operation
     */
    public void recordDatabaseDuration(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("wallet_database_duration_seconds")
                .tag("operation", operation)
                .register(meterRegistry));
    }
    
    /**
     * Increments the audit log creation counter.
     */
    public void incrementAuditLog() {
        auditLogCounter.increment();
    }
    
    /**
     * Increments the wallet snapshot creation counter.
     */
    public void incrementWalletSnapshot() {
        walletSnapshotCounter.increment();
    }
    
    /**
     * Increments the audit error counter with error type classification.
     * 
     * @param errorType the type of audit error
     */
    public void incrementAuditError(String errorType) {
        Counter.builder("wallet_audit_errors_total")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }
}