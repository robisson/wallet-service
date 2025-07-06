package com.wallet.domain.service;

import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.Wallet;
import com.wallet.infrastructure.adapter.persistence.AuditLogEntity;
import com.wallet.infrastructure.adapter.persistence.TransactionEntity;
import com.wallet.infrastructure.adapter.persistence.WalletSnapshotEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the AuditService that provides immutable audit logs
 * and wallet state snapshots for regulatory compliance.
 * 
 * <p>This service creates tamper-evident audit records using hash chains
 * to ensure the integrity of the audit trail. Key features include:
 * <ul>
 *   <li>Immutable transaction audit logs with hash chains</li>
 *   <li>Wallet state snapshots for reconciliation</li>
 *   <li>Transaction chain integrity verification</li>
 *   <li>Asynchronous processing to avoid performance impact</li>
 *   <li>Comprehensive error handling and metrics</li>
 * </ul>
 * 
 * <p>The service uses DynamoDB for persistence and implements cryptographic
 * hashing to create an immutable audit trail that can detect unauthorized
 * modifications to transaction history.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Service
public class ImmutableAuditService implements AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImmutableAuditService.class);
    private final DynamoDbTable<AuditLogEntity> auditLogTable;
    private final DynamoDbTable<WalletSnapshotEntity> snapshotTable;
    private final DynamoDbTable<TransactionEntity> transactionTable;
    private final WalletMetrics walletMetrics;
    
    // Cache of last transaction hash per wallet to optimize chain verification
    private final Map<String, String> lastTransactionHashCache = new ConcurrentHashMap<>();
    
    @Value("${audit.snapshot.enabled:true}")
    private boolean snapshotEnabled;
    
    /**
     * Constructs a new ImmutableAuditService with required dependencies.
     * 
     * <p>Initializes DynamoDB tables for audit logs and snapshots,
     * and sets up the hash chain cache for performance optimization.
     * 
     * @param enhancedClient the DynamoDB Enhanced client for operations
     * @param walletMetrics the metrics service for monitoring
     */
    public ImmutableAuditService(DynamoDbEnhancedClient enhancedClient, WalletMetrics walletMetrics) {
        this.auditLogTable = enhancedClient.table("audit_logs", TableSchema.fromBean(AuditLogEntity.class));
        this.snapshotTable = enhancedClient.table("wallet_snapshots", TableSchema.fromBean(WalletSnapshotEntity.class));
        this.transactionTable = enhancedClient.table("transactions", TableSchema.fromBean(TransactionEntity.class));
        this.walletMetrics = walletMetrics;
        
        // Ensure tables exist or create them
        try {
            auditLogTable.describeTable();
            snapshotTable.describeTable();
            logger.info("Audit tables verified successfully");
        } catch (Exception e) {
            logger.warn("Audit tables may not exist: {}", e.getMessage());
        }
    }
    
    @Override
    @Async("auditExecutor")
    public void auditTransaction(Transaction transaction, AuditInfo auditInfo) {
        try {
            // Create audit log entry
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setWalletId(transaction.getWalletId().value());
            auditLog.setTransactionId(transaction.getId());
            auditLog.setTimestamp(transaction.getTimestamp());
            auditLog.setType(transaction.getType().name());
            auditLog.setAmount(transaction.getAmount().amount());
            
            if (transaction.getRelatedWalletId() != null) {
                auditLog.setRelatedWalletId(transaction.getRelatedWalletId().value());
            }
            
            // Add audit information
            if (auditInfo != null) {
                auditLog.setUserId(auditInfo.getUserId());
                auditLog.setSourceIp(auditInfo.getSourceIp());
                auditLog.setUserAgent(auditInfo.getUserAgent());
                auditLog.setRequestId(auditInfo.getRequestId());
                auditLog.setAuditTimestamp(auditInfo.getTimestamp());
                auditLog.setAdditionalContext(auditInfo.getAdditionalContext());
            }
            
            // Get previous transaction hash for this wallet
            String previousHash = getLastTransactionHash(transaction.getWalletId().value());
            auditLog.setPreviousTransactionHash(previousHash);
            
            // Generate hash for this transaction
            String currentHash = generateSecureHash(auditLog);
            auditLog.setTransactionHash(currentHash);
            
            // Update cache
            lastTransactionHashCache.put(transaction.getWalletId().value(), currentHash);
            
            // Save to DynamoDB
            auditLogTable.putItem(auditLog);
            
            logger.debug("Audit log created for transaction: {} of type: {} for wallet: {}", 
                transaction.getId(), transaction.getType(), transaction.getWalletId().value());
            
            walletMetrics.incrementAuditLog();
        } catch (Exception e) {
            logger.error("Failed to create audit log for transaction: {}", transaction.getId(), e);
            walletMetrics.incrementAuditError("create_audit_log");
        }
    }
    
    @Override
    @Async("auditExecutor")
    public void createWalletSnapshot(Wallet wallet, AuditInfo auditInfo) {
        if (!snapshotEnabled) {
            logger.debug("Wallet snapshots are disabled");
            return;
        }
        
        try {
            WalletSnapshotEntity snapshot = new WalletSnapshotEntity();
            snapshot.setWalletId(wallet.getId().value());
            snapshot.setSnapshotId(generateSnapshotId());
            snapshot.setBalance(wallet.getBalance().amount());
            snapshot.setTimestamp(Instant.now());
            snapshot.setUserId(wallet.getUserId());
            
            // Add audit information
            if (auditInfo != null) {
                snapshot.setRequestId(auditInfo.getRequestId());
                snapshot.setSourceIp(auditInfo.getSourceIp());
                snapshot.setUserAgent(auditInfo.getUserAgent());
                snapshot.setOperatorId(auditInfo.getUserId());
                snapshot.setAdditionalContext(auditInfo.getAdditionalContext());
            }
            
            // Generate hash for this snapshot
            String snapshotHash = generateSecureHash(snapshot);
            snapshot.setSnapshotHash(snapshotHash);
            
            // Save to DynamoDB
            snapshotTable.putItem(snapshot);
            
            logger.debug("Wallet snapshot created for wallet: {} with balance: {}", 
                wallet.getId().value(), wallet.getBalance().amount());
            
            walletMetrics.incrementWalletSnapshot();
        } catch (Exception e) {
            logger.error("Failed to create wallet snapshot for wallet: {}", wallet.getId().value(), e);
            walletMetrics.incrementAuditError("create_wallet_snapshot");
        }
    }
    
    @Override
    public boolean verifyTransactionChainIntegrity(String walletId) {
        try {
            // Query all transactions for this wallet
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(walletId).build());
            
            List<TransactionEntity> transactions = transactionTable.query(queryConditional)
                .items()
                .stream()
                .collect(Collectors.toList());
            
            if (transactions.isEmpty()) {
                return true; // No transactions to verify
            }
            
            // Sort transactions by timestamp
            transactions.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            
            String previousHash = null;
            
            // Verify the chain
            for (TransactionEntity transaction : transactions) {
                // Skip if no hash (legacy transactions)
                if (transaction.getTransactionHash() == null) {
                    continue;
                }
                
                // Verify previous hash matches
                if (previousHash != null && !previousHash.equals(transaction.getPreviousTransactionHash())) {
                    logger.error("Transaction chain integrity broken for wallet: {} at transaction: {}", 
                        walletId, transaction.getTransactionId());
                    walletMetrics.incrementAuditError("transaction_chain_integrity_broken");
                    return false;
                }
                
                // Verify current hash is correct
                String expectedHash = generateTransactionHash(transaction);
                if (!expectedHash.equals(transaction.getTransactionHash())) {
                    logger.error("Transaction hash mismatch for wallet: {} at transaction: {}", 
                        walletId, transaction.getTransactionId());
                    walletMetrics.incrementAuditError("transaction_hash_mismatch");
                    return false;
                }
                
                previousHash = transaction.getTransactionHash();
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to verify transaction chain integrity for wallet: {}", walletId, e);
            walletMetrics.incrementAuditError("verify_transaction_chain");
            return false;
        }
    }
    
    /**
     * Gets the last transaction hash for a wallet to maintain hash chain continuity.
     * 
     * <p>This method first checks an in-memory cache for performance,
     * then queries DynamoDB if not found. The hash is used to link
     * new transactions to the existing chain.
     * 
     * @param walletId the wallet ID to get the last hash for
     * @return the last transaction hash, or null if no previous transactions
     */
    private String getLastTransactionHash(String walletId) {
        // Check cache first
        if (lastTransactionHashCache.containsKey(walletId)) {
            return lastTransactionHashCache.get(walletId);
        }
        
        try {
            // Query the most recent transaction
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(walletId).build());
            
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false) // Descending order (newest first)
                .limit(1) // Only get the most recent
                .build();
            
            List<TransactionEntity> transactions = transactionTable.query(request)
                .items()
                .stream()
                .collect(Collectors.toList());
            
            if (!transactions.isEmpty() && transactions.get(0).getTransactionHash() != null) {
                String hash = transactions.get(0).getTransactionHash();
                lastTransactionHashCache.put(walletId, hash);
                return hash;
            }
            
            return null; // No previous transaction or hash
        } catch (Exception e) {
            logger.error("Failed to get last transaction hash for wallet: {}", walletId, e);
            return null;
        }
    }
    
    /**
     * Generates a secure hash for a transaction entity.
     * 
     * <p>In production, this would use a cryptographically secure
     * hashing algorithm like SHA-256 with proper salting.
     * 
     * @param entity the transaction entity to hash
     * @return the generated hash string
     */
    private String generateTransactionHash(TransactionEntity entity) {
        // In production, use a secure hashing algorithm with proper salting
        String dataToHash = entity.getWalletId() + 
                           entity.getTransactionId() + 
                           entity.getType() + 
                           entity.getAmount() + 
                           entity.getTimestamp() + 
                           (entity.getRelatedWalletId() != null ? entity.getRelatedWalletId() : "");
        
        return generateSecureHash(dataToHash);
    }
    
    /**
     * Generates a unique snapshot identifier.
     * 
     * @return a unique snapshot ID based on timestamp
     */
    private String generateSnapshotId() {
        return "snapshot-" + Instant.now().toEpochMilli();
    }
    
    /**
     * Generates a secure hash for any object.
     * 
     * <p>Uses SHA-256 hashing algorithm for cryptographic security.
     * Falls back to simple hash code if SHA-256 is not available.
     * 
     * @param obj the object to hash
     * @return the generated hash string
     */
    private String generateSecureHash(Object obj) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(obj.toString().getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate secure hash", e);
            return Integer.toHexString(obj.toString().hashCode());
        }
    }
}