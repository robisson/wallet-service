package com.wallet.domain.service;

import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.Wallet;

/**
 * Port for audit service operations.
 * This interface defines the contract for audit-related operations.
 */
public interface AuditService {
    
    /**
     * Records an immutable audit log for a transaction.
     * 
     * @param transaction The transaction to audit
     * @param auditInfo Additional audit information
     */
    void auditTransaction(Transaction transaction, AuditInfo auditInfo);
    
    /**
     * Creates a wallet state snapshot for reconciliation purposes.
     * 
     * @param wallet The wallet to snapshot
     * @param auditInfo Additional audit information
     */
    void createWalletSnapshot(Wallet wallet, AuditInfo auditInfo);
    
    /**
     * Verifies the integrity of the transaction chain for a wallet.
     * 
     * @param walletId The wallet ID to verify
     * @return true if the transaction chain is valid, false otherwise
     */
    boolean verifyTransactionChainIntegrity(String walletId);
}