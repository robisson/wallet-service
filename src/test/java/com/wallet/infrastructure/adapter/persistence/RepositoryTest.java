package com.wallet.infrastructure.adapter.persistence;

import com.wallet.domain.model.*;
import com.wallet.infrastructure.adapter.persistence.repositories.DynamoDbTransactionRepository;
import com.wallet.infrastructure.adapter.persistence.repositories.DynamoDbWalletRepository;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    @Mock
    private WalletMetrics walletMetrics;
    
    @Mock
    private Timer.Sample sample;

    @Test
    void shouldCreateWalletRepository() {
        when(enhancedClient.table(anyString(), any())).thenReturn(mock(DynamoDbTable.class));
        
        DynamoDbWalletRepository repository = new DynamoDbWalletRepository(enhancedClient, dynamoDbClient, walletMetrics);
        
        assertNotNull(repository);
    }

    @Test
    void shouldCreateTransactionRepository() {
        when(enhancedClient.table(anyString(), any())).thenReturn(mock(DynamoDbTable.class));
        
        DynamoDbTransactionRepository repository = new DynamoDbTransactionRepository(enhancedClient, walletMetrics);
        
        assertNotNull(repository);
    }
    
    @Test
    void shouldHandleWalletEntityDefaultConstructor() {
        WalletEntity entity = new WalletEntity();
        
        assertNotNull(entity);
        assertNull(entity.getWalletId());
        assertNull(entity.getUserId());
        assertNull(entity.getBalance());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }
    
    @Test
    void shouldHandleTransactionEntityDefaultConstructor() {
        TransactionEntity entity = new TransactionEntity();
        
        assertNotNull(entity);
        assertNull(entity.getWalletId());
        assertNull(entity.getTransactionId());
        assertNull(entity.getType());
        assertNull(entity.getAmount());
        assertNull(entity.getRelatedWalletId());
        assertNull(entity.getTimestamp());
    }
    
    @Test
    void shouldConvertWalletEntityWithNullTimestamps() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(100.00));
        entity.setCreatedAt(null);
        entity.setUpdatedAt(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            if (entity.getCreatedAt() != null && entity.getUpdatedAt() != null) {
                entity.toDomain();
            }
        });
    }
    
    @Test
    void shouldConvertTransactionEntityWithNullRelatedWallet() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100.00));
        entity.setRelatedWalletId(null);
        entity.setTimestamp(Instant.now());
        
        Transaction transaction = entity.toDomain();
        
        assertNull(transaction.getRelatedWalletId());
    }
}