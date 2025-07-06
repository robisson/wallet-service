package com.wallet.application.usecase;

import com.wallet.application.dto.*;
import com.wallet.domain.model.*;
import com.wallet.domain.repositories.TransactionRepository;
import com.wallet.domain.repositories.WalletRepository;
import com.wallet.domain.service.AuditService;
import com.wallet.domain.service.WalletDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;

/**
 * Application service that orchestrates wallet-related business operations.
 * 
 * <p>This class implements the application layer in hexagonal architecture,
 * coordinating between the domain layer and infrastructure adapters.
 * It handles all wallet use cases including creation, transactions, and queries.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Wallet creation and validation</li>
 *   <li>Deposit and withdrawal operations</li>
 *   <li>Money transfers between wallets</li>
 *   <li>Historical balance calculations</li>
 *   <li>Audit trail coordination</li>
 * </ul>
 * 
 * <p>All operations are logged and audited for regulatory compliance.
 * Transaction operations are atomic and maintain data consistency.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Service
public class WalletUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletUseCase.class);
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletDomainService walletDomainService;
    private final AuditService auditService;
    
    /**
     * Constructs a new WalletUseCase with required dependencies.
     * 
     * @param walletRepository repository for wallet persistence operations
     * @param transactionRepository repository for transaction persistence operations
     * @param walletDomainService domain service for complex wallet operations
     * @param auditService service for audit logging and compliance
     */
    public WalletUseCase(WalletRepository walletRepository, 
                        TransactionRepository transactionRepository,
                        WalletDomainService walletDomainService,
                        AuditService auditService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.walletDomainService = walletDomainService;
        this.auditService = auditService;
    }
    
    /**
     * Creates a new wallet for the specified user.
     * 
     * <p>Each user can have only one wallet. If a wallet already exists
     * for the user, an IllegalArgumentException is thrown.
     * 
     * @param request the wallet creation request containing user ID
     * @return the created wallet response with initial zero balance
     * @throws IllegalArgumentException if user already has a wallet
     */
    public WalletResponse createWallet(CreateWalletRequest request) {
        logger.debug("Creating wallet for user: {}", request.userId());
        
        if (walletRepository.findByUserId(request.userId()).isPresent()) {
            logger.warn("Wallet creation failed - user already has wallet: {}", request.userId());
            throw new IllegalArgumentException("User already has a wallet");
        }
        
        Wallet wallet = new Wallet(WalletId.generate(), request.userId());
        Wallet savedWallet = walletRepository.save(wallet);
        
        logger.info("Wallet created successfully for user: {} with ID: {}", 
                   request.userId(), savedWallet.getId().value());
        
        return new WalletResponse(
            savedWallet.getId().value(),
            savedWallet.getUserId(),
            savedWallet.getBalance().amount(),
            savedWallet.getCreatedAt(),
            savedWallet.getUpdatedAt()
        );
    }
    
    /**
     * Retrieves wallet information by wallet ID.
     * 
     * @param walletId the unique identifier of the wallet
     * @return the wallet response with current balance and metadata
     * @throws IllegalArgumentException if wallet is not found
     */
    public WalletResponse getWallet(String walletId) {
        logger.debug("Retrieving wallet: {}", walletId);
        
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
            .orElseThrow(() -> {
                logger.warn("Wallet not found: {}", walletId);
                return new IllegalArgumentException("Wallet not found");
            });
        
        logger.debug("Wallet retrieved successfully: {} for user: {}", walletId, wallet.getUserId());
        
        return new WalletResponse(
            wallet.getId().value(),
            wallet.getUserId(),
            wallet.getBalance().amount(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
    }
    
    /**
     * Calculates the historical balance of a wallet at a specific point in time.
     * 
     * <p>This method reconstructs the wallet balance by replaying all transactions
     * that occurred before the specified timestamp. This is useful for auditing
     * and reconciliation purposes.
     * 
     * @param walletId the unique identifier of the wallet
     * @param timestamp the point in time for balance calculation
     * @return the wallet response with historical balance
     * @throws IllegalArgumentException if wallet is not found
     */
    public WalletResponse getHistoricalBalance(String walletId, Instant timestamp) {
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        
        Money historicalBalance = walletDomainService.calculateHistoricalBalance(
            WalletId.of(walletId), timestamp);
        
        return new WalletResponse(
            wallet.getId().value(),
            wallet.getUserId(),
            historicalBalance.amount(),
            wallet.getCreatedAt(),
            timestamp
        );
    }
    
    /**
     * Processes a deposit transaction to add money to a wallet.
     * 
     * <p>This operation is atomic and includes:
     * <ul>
     *   <li>Wallet balance update</li>
     *   <li>Transaction record creation</li>
     *   <li>Audit log generation</li>
     * </ul>
     * 
     * @param request the transaction request with wallet ID and amount
     * @return the updated wallet response with new balance
     * @throws IllegalArgumentException if wallet not found or amount invalid
     */
    public WalletResponse deposit(TransactionRequest request) {
        logger.info("Processing deposit for wallet: {} amount: {}", request.walletId(), request.amount());
        
        WalletId walletId = WalletId.of(request.walletId());
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> {
                logger.error("Deposit failed - wallet not found: {}", request.walletId());
                return new IllegalArgumentException("Wallet not found");
            });
        
        Money amount = Money.of(request.amount());
        Money previousBalance = wallet.getBalance();
        wallet.deposit(amount);
        
        // Get audit information from current context (if available)
        AuditInfo auditInfo = AuditInfo.builder()
            .requestId("deposit-" + walletId.value() + "-" + System.currentTimeMillis())
            .timestamp(Instant.now())
            .build();
            
        Transaction transaction = new Transaction(walletId, Transaction.Type.DEPOSIT, amount, null, auditInfo);
        
        // Save wallet and transaction atomically
        walletRepository.saveWalletWithTransaction(wallet, transaction);
        
        // Create audit log for the transaction
        auditService.auditTransaction(transaction, auditInfo);
        
        logger.info("Deposit completed for wallet: {} amount: {} balance: {} -> {}", 
                   request.walletId(), request.amount(), previousBalance.amount(), wallet.getBalance().amount());
        
        return new WalletResponse(
            wallet.getId().value(),
            wallet.getUserId(),
            wallet.getBalance().amount(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
    }
    
    /**
     * Processes a withdrawal transaction to remove money from a wallet.
     * 
     * <p>This operation validates sufficient funds before proceeding.
     * The operation is atomic and includes:
     * <ul>
     *   <li>Balance sufficiency check</li>
     *   <li>Wallet balance update</li>
     *   <li>Transaction record creation</li>
     *   <li>Audit log generation</li>
     * </ul>
     * 
     * @param request the transaction request with wallet ID and amount
     * @return the updated wallet response with new balance
     * @throws IllegalArgumentException if wallet not found, amount invalid, or insufficient funds
     */
    public WalletResponse withdraw(TransactionRequest request) {
        logger.info("Processing withdrawal for wallet: {} amount: {}", request.walletId(), request.amount());
        
        WalletId walletId = WalletId.of(request.walletId());
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> {
                logger.error("Withdrawal failed - wallet not found: {}", request.walletId());
                return new IllegalArgumentException("Wallet not found");
            });
        
        Money amount = Money.of(request.amount());
        Money previousBalance = wallet.getBalance();
        
        try {
            wallet.withdraw(amount);
        } catch (IllegalArgumentException e) {
            logger.warn("Withdrawal failed - insufficient balance for wallet: {} amount: {} balance: {}", 
                       request.walletId(), request.amount(), previousBalance.amount());
            throw e;
        }
        
        // Get audit information from current context (if available)
        AuditInfo auditInfo = AuditInfo.builder()
            .requestId("withdrawal-" + walletId.value() + "-" + System.currentTimeMillis())
            .timestamp(Instant.now())
            .build();
            
        Transaction transaction = new Transaction(walletId, Transaction.Type.WITHDRAWAL, amount, null, auditInfo);
        
        // Save wallet and transaction atomically
        walletRepository.saveWalletWithTransaction(wallet, transaction);
        
        // Create audit log for the transaction
        auditService.auditTransaction(transaction, auditInfo);
        
        logger.info("Withdrawal completed for wallet: {} amount: {} balance: {} -> {}", 
                   request.walletId(), request.amount(), previousBalance.amount(), wallet.getBalance().amount());
        
        return new WalletResponse(
            wallet.getId().value(),
            wallet.getUserId(),
            wallet.getBalance().amount(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
    }
    
    /**
     * Processes a money transfer between two wallets.
     * 
     * <p>This operation is atomic across both wallets and includes:
     * <ul>
     *   <li>Source wallet balance validation</li>
     *   <li>Both wallet balance updates</li>
     *   <li>Transaction records for both wallets</li>
     *   <li>Audit logs for both transactions</li>
     * </ul>
     * 
     * <p>The entire operation succeeds or fails as a unit.
     * 
     * @param request the transfer request with source, destination, and amount
     * @throws IllegalArgumentException if wallets not found, amount invalid, or insufficient funds
     */
    public void transfer(TransferRequest request) {
        logger.info("Processing transfer from wallet: {} to wallet: {} amount: {}", 
                   request.fromWalletId(), request.toWalletId(), request.amount());
        
        WalletId fromWalletId = WalletId.of(request.fromWalletId());
        WalletId toWalletId = WalletId.of(request.toWalletId());
        Money amount = Money.of(request.amount());
        
        try {
            walletDomainService.transfer(fromWalletId, toWalletId, amount);
            logger.info("Transfer completed successfully from wallet: {} to wallet: {} amount: {}", 
                       request.fromWalletId(), request.toWalletId(), request.amount());
        } catch (Exception e) {
            logger.error("Transfer failed from wallet: {} to wallet: {} amount: {}", 
                        request.fromWalletId(), request.toWalletId(), request.amount(), e);
            throw e;
        }
    }
}