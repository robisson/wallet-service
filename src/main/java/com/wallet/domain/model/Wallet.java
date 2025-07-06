package com.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing a user's wallet in the financial system.
 * 
 * <p>This entity encapsulates all wallet-related business logic and maintains
 * consistency boundaries. Key characteristics:
 * <ul>
 *   <li>Unique identification through WalletId</li>
 *   <li>Association with a single user</li>
 *   <li>Current balance tracking</li>
 *   <li>Creation and modification timestamps</li>
 *   <li>Business rule enforcement for deposits/withdrawals</li>
 * </ul>
 * 
 * <p>The Wallet enforces business invariants such as:
 * <ul>
 *   <li>Balance cannot go negative</li>
 *   <li>Only positive amounts for deposits/withdrawals</li>
 *   <li>Automatic timestamp updates on balance changes</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public class Wallet {
    
    private final WalletId id;
    private final String userId;
    private Money balance;
    private final Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Creates a new wallet with zero balance.
     * 
     * <p>This constructor is used when creating a brand new wallet
     * for a user. The wallet starts with zero balance and current
     * timestamps for both creation and last update.
     * 
     * @param id the unique wallet identifier, must not be null
     * @param userId the user who owns this wallet, must not be null
     * @throws NullPointerException if id or userId is null
     */
    public Wallet(WalletId id, String userId) {
        this.id = Objects.requireNonNull(id, "Wallet ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.balance = Money.zero();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Creates a wallet with all specified attributes.
     * 
     * <p>This constructor is typically used when reconstructing
     * a wallet from persistent storage or for testing purposes.
     * 
     * @param id the unique wallet identifier, must not be null
     * @param userId the user who owns this wallet, must not be null
     * @param balance the current wallet balance, must not be null
     * @param createdAt when the wallet was created, must not be null
     * @param updatedAt when the wallet was last updated, must not be null
     * @throws NullPointerException if any parameter is null
     */
    public Wallet(WalletId id, String userId, Money balance, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Wallet ID cannot be null");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.balance = Objects.requireNonNull(balance, "Balance cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated timestamp cannot be null");
    }
    
    /**
     * Deposits money into this wallet.
     * 
     * <p>Adds the specified amount to the current balance and updates
     * the modification timestamp. The amount must be positive.
     * 
     * @param amount the amount to deposit, must be positive
     * @throws IllegalArgumentException if amount is not positive
     */
    public void deposit(Money amount) {
        if (amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Withdraws money from this wallet.
     * 
     * <p>Subtracts the specified amount from the current balance and updates
     * the modification timestamp. The amount must be positive and the wallet
     * must have sufficient funds.
     * 
     * @param amount the amount to withdraw, must be positive
     * @throws IllegalArgumentException if amount is not positive or insufficient funds
     */
    public void withdraw(Money amount) {
        if (amount.amount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }
    
    /**
     * Gets the unique wallet identifier.
     * @return the wallet ID
     */
    public WalletId getId() { return id; }
    
    /**
     * Gets the user who owns this wallet.
     * @return the user ID
     */
    public String getUserId() { return userId; }
    
    /**
     * Gets the current wallet balance.
     * @return the current balance
     */
    public Money getBalance() { return balance; }
    
    /**
     * Gets when this wallet was created.
     * @return the creation timestamp in UTC
     */
    public Instant getCreatedAt() { return createdAt; }
    
    /**
     * Gets when this wallet was last updated.
     * @return the last update timestamp in UTC
     */
    public Instant getUpdatedAt() { return updatedAt; }
}