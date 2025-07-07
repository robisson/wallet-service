package com.wallet.infrastructure.adapter.persistence.repositories;

import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.repositories.WalletRepository;
import com.wallet.infrastructure.adapter.persistence.TransactionEntity;
import com.wallet.infrastructure.adapter.persistence.WalletEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB implementation of the WalletRepository interface.
 * 
 * <p>
 * This repository provides persistence operations for wallet entities
 * using Amazon DynamoDB as the underlying storage. It implements the
 * secondary adapter pattern in hexagonal architecture.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>CRUD operations for wallet entities</li>
 * <li>Atomic transactions for multi-wallet operations</li>
 * <li>Query operations by wallet ID and user ID</li>
 * <li>Comprehensive error handling and logging</li>
 * <li>Metrics collection for monitoring</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Repository
public class DynamoDbWalletRepository implements WalletRepository {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbWalletRepository.class);
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<WalletEntity> table;
    private final DynamoDbClient dynamoDbClient;
    private final WalletMetrics walletMetrics;

    /**
     * Constructs a new DynamoDbWalletRepository with required dependencies.
     * 
     * @param enhancedClient the DynamoDB Enhanced client for high-level operations
     * @param dynamoDbClient the low-level DynamoDB client for transactions
     * @param walletMetrics  the metrics service for monitoring
     */
    public DynamoDbWalletRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient,
            WalletMetrics walletMetrics) {
        this.enhancedClient = enhancedClient;
        this.dynamoDbClient = dynamoDbClient;
        this.walletMetrics = walletMetrics;
        this.table = enhancedClient.table("wallets", TableSchema.fromBean(WalletEntity.class));
    }

    @Override
    public Wallet save(Wallet wallet) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Saving wallet to DynamoDB: {}", wallet.getId().value());

        try {
            WalletEntity entity = WalletEntity.fromDomain(wallet);

            table.putItem(entity);

            logger.debug("Wallet saved successfully to DynamoDB: {}", wallet.getId().value());
            walletMetrics.recordDatabaseDuration(sample, "save_wallet");

            return wallet;
        } catch (Exception e) {
            logger.error("Failed to save wallet to DynamoDB: {}", wallet.getId().value(), e);

            walletMetrics.incrementDatabaseError("save_wallet");
            walletMetrics.recordDatabaseDuration(sample, "save_wallet");
            throw e;
        }
    }

    @Override
    public Optional<Wallet> findById(WalletId walletId) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Finding wallet by ID in DynamoDB: {}", walletId.value());

        try {
            Key key = Key.builder().partitionValue(walletId.value()).build();
            WalletEntity entity = table.getItem(key);
            Optional<Wallet> result = Optional.ofNullable(entity).map(WalletEntity::toDomain);

            if (result.isPresent()) {
                logger.debug("Wallet found in DynamoDB: {}", walletId.value());
            } else {
                logger.debug("Wallet not found in DynamoDB: {}", walletId.value());
            }

            walletMetrics.recordDatabaseDuration(sample, "find_wallet");
            return result;
        } catch (Exception e) {
            logger.error("Failed to find wallet by ID in DynamoDB: {}", walletId.value(), e);
            walletMetrics.incrementDatabaseError("find_wallet");
            walletMetrics.recordDatabaseDuration(sample, "find_wallet");
            throw e;
        }
    }

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(userId).build());

        var results = table.index("UserIdIndex")
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(queryConditional)
                        .build());

        return results.stream()
                .flatMap(page -> page.items().stream())
                .findFirst()
                .map(WalletEntity::toDomain);
    }

    @Override
    public void saveWithTransaction(Wallet fromWallet, Wallet toWallet, Transaction outTransaction,
            Transaction inTransaction) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Executing DynamoDB transaction for wallets: {} and {}",
                fromWallet.getId().value(), toWallet.getId().value());

        try {
            WalletEntity fromEntity = WalletEntity.fromDomain(fromWallet);
            WalletEntity toEntity = WalletEntity.fromDomain(toWallet);

            TransactWriteItem fromItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("wallets").item(fromEntity.toAttributeValueMap()))
                    .build();

            TransactWriteItem toItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("wallets").item(toEntity.toAttributeValueMap()))
                    .build();

            // add outTransaction and inTransaction to the transaction
            TransactionEntity outTransactionEntity = TransactionEntity.fromDomain(outTransaction);
            TransactionEntity inTransactionEntity = TransactionEntity.fromDomain(inTransaction);
            
            TransactWriteItem outTransactionItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("transactions").item(outTransactionEntity.toAttributeValueMap()))
                    .build();
            TransactWriteItem inTransactionItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("transactions").item(inTransactionEntity.toAttributeValueMap()))
                    .build();

            TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                    .transactItems(Arrays.asList(fromItem, toItem, outTransactionItem, inTransactionItem))
                    .build();

            dynamoDbClient.transactWriteItems(request);

            logger.info("DynamoDB transaction completed successfully for wallets: {} and {}",
                    fromWallet.getId().value(), toWallet.getId().value());

            walletMetrics.recordDatabaseDuration(sample, "transaction");
        } catch (Exception e) {
            logger.error("DynamoDB transaction failed for wallets: {} and {}",
                    fromWallet.getId().value(), toWallet.getId().value(), e);
            walletMetrics.incrementDatabaseError("transaction");
            walletMetrics.recordDatabaseDuration(sample, "transaction");
            throw e;
        }
    }

    @Override
    public List<Wallet> findAll(int limit) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Finding all wallets with limit: {}", limit);

        try {
            List<Wallet> wallets = new ArrayList<>();

            // Scan the table with the specified limit
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .limit(limit)
                    .build();

            PageIterable<WalletEntity> pages = table.scan(scanRequest);

            // Convert entities to domain objects
            pages.stream()
                    .flatMap(page -> page.items().stream())
                    .map(WalletEntity::toDomain)
                    .forEach(wallets::add);

            logger.debug("Found {} wallets", wallets.size());
            walletMetrics.recordDatabaseDuration(sample, "find_all_wallets");

            return wallets;
        } catch (Exception e) {
            logger.error("Failed to find all wallets", e);
            walletMetrics.incrementDatabaseError("find_all_wallets");
            walletMetrics.recordDatabaseDuration(sample, "find_all_wallets");
            throw e;
        }
    }

    @Override
    public void saveWalletWithTransaction(Wallet wallet, Transaction transaction) {
        Timer.Sample sample = walletMetrics.startDatabaseTimer();
        logger.debug("Executing DynamoDB transaction for wallet: {} with transaction: {}",
                wallet.getId().value(), transaction.getType());

        try {
            WalletEntity walletEntity = WalletEntity.fromDomain(wallet);
            TransactionEntity transactionEntity = TransactionEntity.fromDomain(transaction);

            TransactWriteItem walletItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("wallets").item(walletEntity.toAttributeValueMap()))
                    .build();

            TransactWriteItem transactionItem = TransactWriteItem.builder()
                    .put(r -> r.tableName("transactions").item(transactionEntity.toAttributeValueMap()))
                    .build();

            TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                    .transactItems(Arrays.asList(walletItem, transactionItem))
                    .build();

            dynamoDbClient.transactWriteItems(request);

            logger.info("DynamoDB transaction completed successfully for wallet: {} with transaction: {}",
                    wallet.getId().value(), transaction.getType());

            walletMetrics.recordDatabaseDuration(sample, "wallet_transaction");
        } catch (Exception e) {
            logger.error("DynamoDB transaction failed for wallet: {} with transaction: {}",
                    wallet.getId().value(), transaction.getType(), e);

            walletMetrics.incrementDatabaseError("wallet_transaction");
            walletMetrics.recordDatabaseDuration(sample, "wallet_transaction");

            throw e;
        }
    }
}