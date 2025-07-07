package com.wallet.domain.service;

import com.wallet.domain.model.*;
import com.wallet.domain.repositories.TransactionRepository;
import com.wallet.domain.repositories.WalletRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Domain service for complex wallet operations that span multiple aggregates.
 * 
 * <p>
 * This service implements business logic that doesn't naturally fit within
 * a single aggregate root. It coordinates operations between multiple wallets
 * and ensures business rules are enforced across aggregate boundaries.
 * 
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Money transfers between wallets</li>
 * <li>Historical balance calculations</li>
 * <li>Cross-wallet business rule enforcement</li>
 * <li>Audit trail coordination</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    /**
     * Constructs a new WalletDomainService with required dependencies.
     * 
     * @param walletRepository      repository for wallet persistence operations
     * @param transactionRepository repository for transaction persistence
     *                              operations
     * @param auditService          service for audit logging and compliance
     */
    public WalletService(WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            AuditService auditService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    /**
     * Transfers money from one wallet to another.
     * 
     * <p>
     * This operation is atomic and includes:
     * <ul>
     * <li>Source wallet balance validation</li>
     * <li>Both wallet balance updates</li>
     * <li>Transaction records for both wallets</li>
     * <li>Audit logs for compliance</li>
     * </ul>
     * 
     * <p>
     * The entire operation succeeds or fails as a unit.
     * 
     * @param fromWalletId the source wallet ID, must not be null
     * @param toWalletId   the destination wallet ID, must not be null
     * @param amount       the amount to transfer, must be positive
     * @throws IllegalArgumentException if wallets not found, amount invalid, or
     *                                  insufficient funds
     */
    public void transfer(WalletId fromWalletId, WalletId toWalletId, Money amount) {
        logger.debug("Executing transfer from wallet: {} to wallet: {} amount: {}",
                fromWalletId.value(), toWalletId.value(), amount.amount());

        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> {
                    logger.error("Transfer failed - source wallet not found: {}", fromWalletId.value());
                    return new IllegalArgumentException("Source wallet not found");
                });

        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> {
                    logger.error("Transfer failed - destination wallet not found: {}", toWalletId.value());
                    return new IllegalArgumentException("Destination wallet not found");
                });

        Money fromPreviousBalance = fromWallet.getBalance();
        Money toPreviousBalance = toWallet.getBalance();

        try {
            fromWallet.withdraw(amount);
            toWallet.deposit(amount);
        } catch (IllegalArgumentException e) {
            logger.warn("Transfer failed - insufficient balance in source wallet: {} amount: {} balance: {}",
                    fromWalletId.value(), amount.amount(), fromPreviousBalance.amount());
            throw e;
        }

        // Get audit information from current context (if available)
        String transferId = "transfer-" + fromWalletId.value() + "-" + toWalletId.value() + "-"
                + System.currentTimeMillis();
        AuditInfo auditInfo = AuditInfo.builder()
                .requestId(transferId)
                .timestamp(Instant.now())
                .build();

        Transaction outTransaction = new Transaction(fromWalletId, Transaction.Type.TRANSFER_OUT, amount, toWalletId,
                auditInfo);
        Transaction inTransaction = new Transaction(toWalletId, Transaction.Type.TRANSFER_IN, amount, fromWalletId,
                auditInfo);

        walletRepository.saveWithTransaction(fromWallet, toWallet, outTransaction, inTransaction);

        // Create audit logs for the transactions
        auditService.auditTransaction(outTransaction, auditInfo);
        auditService.auditTransaction(inTransaction, auditInfo);

        logger.info(
                "Transfer executed successfully from wallet: {} to wallet: {} amount: {} balances: {} -> {}, {} -> {}",
                fromWalletId.value(), toWalletId.value(), amount.amount(),
                fromPreviousBalance.amount(), fromWallet.getBalance().amount(),
                toPreviousBalance.amount(), toWallet.getBalance().amount());
    }

    /**
     * Calculates the historical balance of a wallet at a specific point in time.
     * 
     * <p>
     * This method reconstructs the wallet balance by replaying all transactions
     * that occurred before the specified timestamp. It's used for:
     * <ul>
     * <li>Audit and compliance reporting</li>
     * <li>Historical balance queries</li>
     * <li>Reconciliation processes</li>
     * </ul>
     * 
     * @param walletId  the wallet to calculate balance for, must not be null
     * @param timestamp the point in time for calculation, must not be null
     * @return the calculated historical balance
     */
    public Money calculateHistoricalBalance(WalletId walletId, Instant timestamp) {
        logger.debug("Calculating historical balance for wallet: {} at timestamp: {}",
                walletId.value(), timestamp);

        List<Transaction> transactions = transactionRepository.findByWalletIdBeforeTimestamp(walletId, timestamp);

        logger.debug("Found {} transactions for historical balance calculation for wallet: {}",
                transactions.size(), walletId.value());

        Money balance = Money.zero();
        for (Transaction transaction : transactions) {
            switch (transaction.getType()) {
                case DEPOSIT, TRANSFER_IN -> balance = balance.add(transaction.getAmount());
                case WITHDRAWAL, TRANSFER_OUT -> balance = balance.subtract(transaction.getAmount());
            }
        }

        logger.debug("Historical balance calculated for wallet: {} at timestamp: {} balance: {}",
                walletId.value(), timestamp, balance.amount());

        return balance;
    }
}