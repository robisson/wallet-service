package com.wallet.infrastructure.adapter.web;

import com.wallet.application.dto.*;
import com.wallet.application.usecase.WalletUseCase;
import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.service.AuditService;
import com.wallet.infrastructure.config.LoggingInterceptor;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for wallet-related API endpoints.
 * 
 * <p>This controller provides the HTTP interface for wallet operations,
 * implementing the primary adapter in hexagonal architecture.
 * It handles request/response mapping, validation, and error handling.
 * 
 * <p>Supported operations:
 * <ul>
 *   <li>POST /api/v1/wallets - Create new wallet</li>
 *   <li>GET /api/v1/wallets/{id} - Get wallet details</li>
 *   <li>GET /api/v1/wallets/{id}/balance/historical - Get historical balance</li>
 *   <li>POST /api/v1/wallets/{id}/deposit - Deposit money</li>
 *   <li>POST /api/v1/wallets/{id}/withdraw - Withdraw money</li>
 *   <li>POST /api/v1/wallets/transfer - Transfer between wallets</li>
 * </ul>
 * 
 * <p>All operations include comprehensive logging, metrics collection,
 * and audit trail generation for regulatory compliance.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
    private final WalletUseCase walletUseCase;
    private final WalletMetrics walletMetrics;
    private final AuditService auditService;
    
    /**
     * Constructs a new WalletController with required dependencies.
     * 
     * @param walletUseCase the application service for wallet operations
     * @param walletMetrics the metrics service for monitoring
     * @param auditService the audit service for compliance logging
     */
    public WalletController(WalletUseCase walletUseCase, WalletMetrics walletMetrics, AuditService auditService) {
        this.walletUseCase = walletUseCase;
        this.walletMetrics = walletMetrics;
        this.auditService = auditService;
    }

    /**
     * Extracts audit information from the HTTP request.
     * 
     * <p>This method retrieves audit context that was populated by
     * the LoggingInterceptor, including request ID, IP address,
     * user agent, and other contextual information.
     * 
     * @param httpRequest the HTTP request containing audit context
     * @return audit information or null if not available
     */
    private AuditInfo getAuditInfo(HttpServletRequest httpRequest) {
        AuditInfo auditInfo = (AuditInfo) httpRequest.getAttribute(LoggingInterceptor.AUDIT_INFO_ATTRIBUTE);

        if (auditInfo != null) {
            return AuditInfo.builder()
                .userId(MDC.get("userId"))
                .requestId(auditInfo.getRequestId())
                .sourceIp(auditInfo.getSourceIp())
                .userAgent(auditInfo.getUserAgent())
                .timestamp(auditInfo.getTimestamp())
                .additionalContext(auditInfo.getAdditionalContext())
                .build();
        }

        return null;
    }
    
    /**
     * Creates a new wallet for a user.
     * 
     * <p>Each user can have only one wallet. If a wallet already exists
     * for the user, a 400 Bad Request response is returned.
     * 
     * @param request the wallet creation request with user ID
     * @return 201 Created with wallet details, or 400 if user already has wallet
     */
    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        MDC.put("userId", request.userId());
        logger.info("Creating wallet for user: {}", request.userId());
        
        try {
            WalletResponse response = walletUseCase.createWallet(request);

            MDC.put("walletId", response.walletId());
            logger.info("Wallet created successfully: {}", response.walletId());
            walletMetrics.incrementWalletCreated();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Failed to create wallet for user: {}", request.userId(), e);
            walletMetrics.incrementBusinessError("wallet_creation_failed");
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Retrieves wallet information by ID.
     * 
     * @param walletId the unique wallet identifier
     * @return 200 OK with wallet details, or 400 if wallet not found
     */
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable String walletId) {
        MDC.put("walletId", walletId);
        logger.debug("Retrieving wallet: {}", walletId);
        
        try {
            WalletResponse response = walletUseCase.getWallet(walletId);
            logger.debug("Wallet retrieved successfully: {}", walletId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve wallet: {}", walletId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Retrieves the historical balance of a wallet at a specific timestamp.
     * 
     * <p>This endpoint calculates the wallet balance by replaying all
     * transactions that occurred before the specified timestamp.
     * 
     * @param walletId the unique wallet identifier
     * @param timestamp the ISO-8601 timestamp for balance calculation
     * @return 200 OK with historical balance, or 400 if wallet not found or invalid timestamp
     */
    @GetMapping("/{walletId}/balance/historical")
    public ResponseEntity<WalletResponse> getHistoricalBalance(
            @PathVariable String walletId,
            @RequestParam String timestamp) {
        MDC.put("walletId", walletId);
        MDC.put("timestamp", timestamp);
        logger.debug("Retrieving historical balance for wallet: {} at timestamp: {}", walletId, timestamp);
        
        try {
            Instant instant = Instant.parse(timestamp);
            WalletResponse response = walletUseCase.getHistoricalBalance(walletId, instant);
            logger.debug("Historical balance retrieved successfully for wallet: {}", walletId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve historical balance for wallet: {} at timestamp: {}", walletId, timestamp, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Deposits money into a wallet.
     * 
     * <p>This operation atomically updates the wallet balance and creates
     * a transaction record. A wallet snapshot is also created for audit purposes.
     * 
     * @param walletId the unique wallet identifier
     * @param request the deposit request with amount
     * @param httpRequest the HTTP request for audit context
     * @return 200 OK with updated wallet details, or 400 if wallet not found or invalid amount
     */
    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<WalletResponse> deposit(@PathVariable String walletId, 
                                                 @Valid @RequestBody AmountRequest request,
                                                 HttpServletRequest httpRequest) {
        Timer.Sample sample = walletMetrics.startDepositTimer();
        MDC.put("walletId", walletId);
        MDC.put("amount", request.amount().toString());
        MDC.put("operation", "deposit");
        logger.info("Processing deposit for wallet: {} amount: {}", walletId, request.amount());
        
        try {
            AuditInfo auditInfo = getAuditInfo(httpRequest);
            
            TransactionRequest transactionRequest = new TransactionRequest(walletId, request.amount());
            WalletResponse response = walletUseCase.deposit(transactionRequest);
            
            if (response != null) {
                auditService.createWalletSnapshot(response.toDomain(), auditInfo);
            }
            
            logger.info("Deposit completed successfully for wallet: {} amount: {}", walletId, request.amount());
            walletMetrics.incrementDeposit();
            walletMetrics.recordDepositDuration(sample);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Deposit failed for wallet: {} amount: {}", walletId, request.amount(), e);

            walletMetrics.incrementBusinessError("deposit_failed");
            walletMetrics.recordDepositDuration(sample);

            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Withdraws money from a wallet.
     * 
     * <p>This operation validates sufficient funds before proceeding.
     * It atomically updates the wallet balance and creates a transaction record.
     * A wallet snapshot is also created for audit purposes.
     * 
     * @param walletId the unique wallet identifier
     * @param request the withdrawal request with amount
     * @param httpRequest the HTTP request for audit context
     * @return 200 OK with updated wallet details, or 400 if wallet not found, invalid amount, or insufficient funds
     */
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<WalletResponse> withdraw(@PathVariable String walletId,
                                                  @Valid @RequestBody AmountRequest request,
                                                  HttpServletRequest httpRequest) {
        Timer.Sample sample = walletMetrics.startWithdrawalTimer();

        MDC.put("walletId", walletId);
        MDC.put("amount", request.amount().toString());
        MDC.put("operation", "withdraw");
        logger.info("Processing withdrawal for wallet: {} amount: {}", walletId, request.amount());
        
        try {
            AuditInfo auditInfo = getAuditInfo(httpRequest);
            
            TransactionRequest transactionRequest = new TransactionRequest(walletId, request.amount());
            WalletResponse response = walletUseCase.withdraw(transactionRequest);
            
            if (response != null) {
                auditService.createWalletSnapshot(response.toDomain(), auditInfo);
            }
            
            logger.info("Withdrawal completed successfully for wallet: {} amount: {}", walletId, request.amount());
            walletMetrics.incrementWithdrawal();
            walletMetrics.recordWithdrawalDuration(sample);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Withdrawal failed for wallet: {} amount: {}", walletId, request.amount(), e);
            walletMetrics.incrementBusinessError("withdrawal_failed");
            walletMetrics.recordWithdrawalDuration(sample);

            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Transfers money between two wallets.
     * 
     * <p>This operation is atomic across both wallets. Either both wallets
     * are updated successfully, or the entire operation fails.
     * Transaction records are created for both wallets.
     * 
     * @param request the transfer request with source, destination, and amount
     * @param httpRequest the HTTP request for audit context
     * @return 200 OK if successful, or 400 if wallets not found, invalid amount, or insufficient funds
     */
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request,
                                        HttpServletRequest httpRequest) {
        Timer.Sample sample = walletMetrics.startTransferTimer();

        MDC.put("fromWalletId", request.fromWalletId());
        MDC.put("toWalletId", request.toWalletId());
        MDC.put("amount", request.amount().toString());
        MDC.put("operation", "transfer");

        logger.info("Processing transfer from wallet: {} to wallet: {} amount: {}", 
                   request.fromWalletId(), request.toWalletId(), request.amount());
        
        try {
            walletUseCase.transfer(request);
            
            logger.info("Transfer completed successfully from wallet: {} to wallet: {} amount: {}", 
                       request.fromWalletId(), request.toWalletId(), request.amount());

            walletMetrics.incrementTransfer();
            walletMetrics.recordTransferDuration(sample);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Transfer failed from wallet: {} to wallet: {} amount: {}", 
                        request.fromWalletId(), request.toWalletId(), request.amount(), e);

            walletMetrics.incrementBusinessError("transfer_failed");
            walletMetrics.recordTransferDuration(sample);

            throw e;
        } finally {
            MDC.clear();
        }
    }
}
