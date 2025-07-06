package com.wallet.domain.repositories;

import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for wallet persistence operations.
 * 
 * <p>This interface defines the contract for storing and retrieving
 * wallet entities. It follows the Repository pattern from DDD,
 * providing a collection-like interface for wallet aggregates.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Basic CRUD operations for wallets</li>
 *   <li>Atomic operations combining wallet and transaction updates</li>
 *   <li>Query operations by ID and user ID</li>
 *   <li>Bulk operations for administrative purposes</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public interface WalletRepository {
    
    /**
     * Persists a wallet entity.
     * 
     * @param wallet the wallet to save, must not be null
     * @return the saved wallet
     * @throws IllegalArgumentException if wallet is invalid
     */
    Wallet save(Wallet wallet);
    
    /**
     * Finds a wallet by its unique identifier.
     * 
     * @param walletId the wallet ID to search for, must not be null
     * @return an Optional containing the wallet if found, empty otherwise
     */
    Optional<Wallet> findById(WalletId walletId);
    
    /**
     * Finds a wallet by the user who owns it.
     * 
     * <p>Since each user can have only one wallet, this method
     * returns at most one wallet.
     * 
     * @param userId the user ID to search for, must not be null
     * @return an Optional containing the wallet if found, empty otherwise
     */
    Optional<Wallet> findByUserId(String userId);
    
    /**
     * Atomically saves two wallets in a single transaction.
     * 
     * <p>This method is used for transfer operations where both
     * source and destination wallets must be updated atomically.
     * Either both wallets are saved successfully, or neither is saved.
     * 
     * @param fromWallet the source wallet, must not be null
     * @param toWallet the destination wallet, must not be null
     * @throws IllegalArgumentException if either wallet is invalid
     */
    void saveWithTransaction(Wallet fromWallet, Wallet toWallet);
    
    /**
     * Atomically saves a wallet and its associated transaction.
     * 
     * <p>This method ensures that wallet balance updates and transaction
     * records are persisted together. Either both are saved successfully,
     * or neither is saved.
     * 
     * @param wallet the wallet to save, must not be null
     * @param transaction the transaction to save, must not be null
     * @throws IllegalArgumentException if wallet or transaction is invalid
     */
    void saveWalletWithTransaction(Wallet wallet, Transaction transaction);
    
    /**
     * Finds all wallets with pagination support.
     * 
     * <p>This method is primarily used for administrative operations
     * such as periodic snapshots and reconciliation processes.
     * 
     * @param limit maximum number of wallets to return, must be positive
     * @return list of wallets, never null but may be empty
     * @throws IllegalArgumentException if limit is not positive
     */
    List<Wallet> findAll(int limit);
}