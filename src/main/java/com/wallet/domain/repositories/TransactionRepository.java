package com.wallet.domain.repositories;

import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.WalletId;
import java.time.Instant;
import java.util.List;

/**
 * Repository interface for transaction persistence operations.
 * 
 * <p>This interface defines the contract for storing and retrieving
 * transaction records. It follows the Repository pattern from DDD,
 * providing a collection-like interface for transaction entities.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Persisting individual and batch transactions</li>
 *   <li>Querying transactions by wallet and time criteria</li>
 *   <li>Supporting historical balance calculations</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public interface TransactionRepository {
    
    /**
     * Persists a single transaction.
     * 
     * @param transaction the transaction to save, must not be null
     * @throws IllegalArgumentException if transaction is invalid
     */
    void save(Transaction transaction);
    
    /**
     * Persists multiple transactions in a batch operation.
     * 
     * <p>This method is optimized for bulk operations and should
     * be used when saving multiple related transactions.
     * 
     * @param transactions the list of transactions to save, must not be null
     * @throws IllegalArgumentException if any transaction is invalid
     */
    void saveAll(List<Transaction> transactions);
    
    /**
     * Finds all transactions for a wallet that occurred before a specific timestamp.
     * 
     * <p>This method is used for historical balance calculations and audit queries.
     * Transactions are returned in chronological order.
     * 
     * @param walletId the wallet to query transactions for, must not be null
     * @param timestamp the cutoff timestamp, must not be null
     * @return list of transactions ordered by timestamp, never null
     */
    List<Transaction> findByWalletIdBeforeTimestamp(WalletId walletId, Instant timestamp);
}