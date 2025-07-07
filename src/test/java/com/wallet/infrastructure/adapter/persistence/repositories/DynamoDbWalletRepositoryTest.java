package com.wallet.infrastructure.adapter.persistence.repositories;

import com.wallet.domain.model.*;
import com.wallet.infrastructure.adapter.persistence.WalletEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbWalletRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    @Mock
    private WalletMetrics walletMetrics;
    
    @Mock
    private DynamoDbTable<WalletEntity> table;
    
    @Mock
    private DynamoDbIndex<WalletEntity> index;
    
    @Mock
    private Timer.Sample sample;

    private DynamoDbWalletRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("wallets"), any(TableSchema.class))).thenReturn(table);
        when(walletMetrics.startDatabaseTimer()).thenReturn(sample);
        repository = new DynamoDbWalletRepository(enhancedClient, dynamoDbClient, walletMetrics);
    }

    @Test
    void shouldSaveWallet() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        Wallet result = repository.save(wallet);
        
        assertEquals(wallet, result);
        verify(table).putItem(any(WalletEntity.class));
        verify(walletMetrics).recordDatabaseDuration(sample, "save_wallet");
    }

    @Test
    void shouldHandleSaveWalletException() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        doThrow(new RuntimeException("DynamoDB error")).when(table).putItem(any(WalletEntity.class));
        
        assertThrows(RuntimeException.class, () -> repository.save(wallet));
        verify(walletMetrics).incrementDatabaseError("save_wallet");
        verify(walletMetrics).recordDatabaseDuration(sample, "save_wallet");
    }

    @Test
    void shouldFindWalletById() {
        WalletId walletId = WalletId.generate();
        WalletEntity entity = new WalletEntity();
        entity.setWalletId(walletId.value());
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.ZERO);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        when(table.getItem(any(Key.class))).thenReturn(entity);
        
        Optional<Wallet> result = repository.findById(walletId);
        
        assertTrue(result.isPresent());
        assertEquals(walletId.value(), result.get().getId().value());
        verify(walletMetrics).recordDatabaseDuration(sample, "find_wallet");
    }

    @Test
    void shouldReturnEmptyWhenWalletNotFound() {
        WalletId walletId = WalletId.generate();
        when(table.getItem(any(Key.class))).thenReturn(null);
        
        Optional<Wallet> result = repository.findById(walletId);
        
        assertFalse(result.isPresent());
        verify(walletMetrics).recordDatabaseDuration(sample, "find_wallet");
    }

    @Test
    void shouldSaveWalletWithTransaction() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        Transaction transaction = new Transaction(
            wallet.getId(), 
            Transaction.Type.DEPOSIT, 
            Money.of(BigDecimal.valueOf(100)), 
            null, 
            null
        );
        
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(TransactWriteItemsResponse.builder().build());
        
        assertDoesNotThrow(() -> repository.saveWalletWithTransaction(wallet, transaction));
        verify(dynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
        verify(walletMetrics).recordDatabaseDuration(sample, "wallet_transaction");
    }

    @Test
    void shouldHandleSaveWalletWithTransactionException() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        Transaction transaction = new Transaction(
            wallet.getId(), 
            Transaction.Type.DEPOSIT, 
            Money.of(BigDecimal.valueOf(100)), 
            null, 
            null
        );
        
        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenThrow(new RuntimeException("Transaction failed"));
        
        assertThrows(RuntimeException.class, () -> repository.saveWalletWithTransaction(wallet, transaction));
        verify(walletMetrics).incrementDatabaseError("wallet_transaction");
        verify(walletMetrics).recordDatabaseDuration(sample, "wallet_transaction");
    }

    @Test
    void shouldSaveWithTransaction() {
        Wallet fromWallet = new Wallet(WalletId.generate(), "user1");
        Wallet toWallet = new Wallet(WalletId.generate(), "user2");
        Transaction outTransaction = new Transaction(fromWallet.getId(), Transaction.Type.TRANSFER_OUT, Money.of(BigDecimal.valueOf(10.00)), toWallet.getId());
        Transaction inTransaction = new Transaction(toWallet.getId(), Transaction.Type.TRANSFER_IN, Money.of(BigDecimal.valueOf(10.00)), fromWallet.getId());

        when(dynamoDbClient.transactWriteItems(any(TransactWriteItemsRequest.class)))
            .thenReturn(TransactWriteItemsResponse.builder().build());

        assertDoesNotThrow(() -> repository.saveWithTransaction(fromWallet, toWallet, outTransaction, inTransaction));
        verify(dynamoDbClient).transactWriteItems(any(TransactWriteItemsRequest.class));
        verify(walletMetrics).recordDatabaseDuration(sample, "transaction");
    }
}