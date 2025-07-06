package com.wallet.infrastructure.adapter.persistence.repositories;

import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.repositories.TransactionRepository;
import com.wallet.infrastructure.adapter.persistence.TransactionEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DynamoDB implementation of the TransactionRepository interface.
 * 
 * <p>This repository provides persistence operations for transaction entities
 * using Amazon DynamoDB as the underlying storage. It supports the event
 * sourcing pattern by maintaining a complete history of all transactions.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Individual and batch transaction persistence</li>
 *   <li>Historical transaction queries by wallet and timestamp</li>
 *   <li>Optimized queries for balance calculations</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Metrics collection for monitoring</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Repository
public class DynamoDbTransactionRepository implements TransactionRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbTransactionRepository.class);
    private final DynamoDbTable<TransactionEntity> table;
    private final WalletMetrics walletMetrics;
    
    /**
     * Constructs a new DynamoDbTransactionRepository with required dependencies.
     * 
     * @param enhancedClient the DynamoDB Enhanced client for operations
     * @param walletMetrics the metrics service for monitoring
     */
    public DynamoDbTransactionRepository(DynamoDbEnhancedClient enhancedClient, WalletMetrics walletMetrics) {
        this.table = enhancedClient.table("transactions", TableSchema.fromBean(TransactionEntity.class));
        this.walletMetrics = walletMetrics;
    }
    
    @Override
    public void save(Transaction transaction) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Saving transaction to DynamoDB: {} for wallet: {}", 
                    transaction.getType(), transaction.getWalletId().value());
        
        try {
            TransactionEntity entity = TransactionEntity.fromDomain(transaction);
            table.putItem(entity);
            logger.debug("Transaction saved successfully to DynamoDB: {} for wallet: {}", 
                        transaction.getType(), transaction.getWalletId().value());
            walletMetrics.recordDatabaseDuration(sample, "save_transaction");
        } catch (Exception e) {
            logger.error("Failed to save transaction to DynamoDB: {} for wallet: {}", 
                        transaction.getType(), transaction.getWalletId().value(), e);
            walletMetrics.incrementDatabaseError("save_transaction");
            walletMetrics.recordDatabaseDuration(sample, "save_transaction");
            throw e;
        }
    }
    
    @Override
    public void saveAll(List<Transaction> transactions) {
        logger.debug("Saving {} transactions to DynamoDB", transactions.size());
        
        try {
            // Note: In production, implement proper batch writing with DynamoDB Enhanced Client
            transactions.forEach(this::save);
            
            logger.debug("Successfully saved {} transactions to DynamoDB", transactions.size());
        } catch (Exception e) {
            logger.error("Failed to save {} transactions to DynamoDB", transactions.size(), e);
            throw e;
        }
    }
    
    
    @Override
    public List<Transaction> findByWalletIdBeforeTimestamp(WalletId walletId, Instant timestamp) {
        logger.debug("Finding transactions for wallet: {} before timestamp: {}", walletId.value(), timestamp);
        
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(walletId.value()).build());
            
            List<Transaction> transactions = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .filterExpression(Expression.builder()
                        .expression("#ts < :timestamp")
                        .putExpressionName("#ts", "timestamp")
                        .putExpressionValue(":timestamp", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(timestamp.toString()).build())
                        .build())
                    .build())
                .items()
                .stream()
                .map(TransactionEntity::toDomain)
                .collect(Collectors.toList());
            
            logger.debug("Found {} transactions for wallet: {} before timestamp: {}", 
                        transactions.size(), walletId.value(), timestamp);
            
            return transactions;
        } catch (Exception e) {
            logger.error("Failed to find transactions for wallet: {} before timestamp: {}", 
                        walletId.value(), timestamp, e);
            throw e;
        }
    }
}