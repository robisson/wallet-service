package com.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a financial transaction in the wallet system.
 * 
 * <p>This entity captures all transaction details including:
 * <ul>
 *   <li>Transaction type (deposit, withdrawal, transfer)</li>
 *   <li>Monetary amount and wallet association</li>
 *   <li>Timestamp for chronological ordering</li>
 *   <li>Audit information for compliance</li>
 *   <li>Related wallet for transfer operations</li>
 * </ul>
 * 
 * <p>Transactions are immutable once created and serve as the foundation
 * for event sourcing and audit trail functionality.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public class Transaction {
    
    /**
     * Enumeration of supported transaction types.
     * 
     * <p>Each type represents a different kind of money movement:
     * <ul>
     *   <li>DEPOSIT: Money added to a wallet from external source</li>
     *   <li>WITHDRAWAL: Money removed from a wallet to external destination</li>
     *   <li>TRANSFER_OUT: Money sent from this wallet to another wallet</li>
     *   <li>TRANSFER_IN: Money received by this wallet from another wallet</li>
     * </ul>
     */
    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN
    }
    
    private final String id;
    private final WalletId walletId;
    private final Type type;
    private final Money amount;
    private final WalletId relatedWalletId;
    private final Instant timestamp;
    private final AuditInfo auditInfo;
    
    /**
     * Creates a new transaction without audit information.
     * 
     * <p>This constructor is used for simple transactions where
     * audit information is not immediately available.
     * 
     * @param walletId the wallet involved in this transaction, must not be null
     * @param type the type of transaction, must not be null
     * @param amount the transaction amount, must not be null
     * @param relatedWalletId the other wallet for transfers, null for deposits/withdrawals
     */
    public Transaction(WalletId walletId, Type type, Money amount, WalletId relatedWalletId) {
        this(walletId, type, amount, relatedWalletId, null);
    }
    
    /**
     * Creates a new transaction with complete audit information.
     * 
     * <p>This is the primary constructor that captures all transaction
     * details including audit context for regulatory compliance.
     * 
     * @param walletId the wallet involved in this transaction, must not be null
     * @param type the type of transaction, must not be null
     * @param amount the transaction amount, must not be null
     * @param relatedWalletId the other wallet for transfers, null for deposits/withdrawals
     * @param auditInfo audit context information, may be null
     * @throws NullPointerException if walletId, type, or amount is null
     */
    public Transaction(WalletId walletId, Type type, Money amount, WalletId relatedWalletId, AuditInfo auditInfo) {
        this.id = UUID.randomUUID().toString();
        this.walletId = Objects.requireNonNull(walletId, "Wallet ID cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.relatedWalletId = relatedWalletId;
        this.timestamp = Instant.now();
        this.auditInfo = auditInfo;
    }
    
    /**
     * Gets the unique transaction identifier.
     * @return the transaction ID
     */
    public String getId() { return id; }
    
    /**
     * Gets the wallet associated with this transaction.
     * @return the wallet ID
     */
    public WalletId getWalletId() { return walletId; }
    
    /**
     * Gets the type of this transaction.
     * @return the transaction type
     */
    public Type getType() { return type; }
    
    /**
     * Gets the monetary amount of this transaction.
     * @return the transaction amount
     */
    public Money getAmount() { return amount; }
    
    /**
     * Gets the related wallet for transfer transactions.
     * @return the related wallet ID, or null for deposits/withdrawals
     */
    public WalletId getRelatedWalletId() { return relatedWalletId; }
    
    /**
     * Gets the timestamp when this transaction was created.
     * @return the transaction timestamp in UTC
     */
    public Instant getTimestamp() { return timestamp; }
    
    /**
     * Gets the audit information associated with this transaction.
     * @return the audit info, or null if not available
     */
    public AuditInfo getAuditInfo() { return auditInfo; }
}