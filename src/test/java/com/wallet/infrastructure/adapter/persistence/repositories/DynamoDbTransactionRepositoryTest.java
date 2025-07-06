package com.wallet.infrastructure.adapter.persistence.repositories;

import com.wallet.domain.model.*;
import com.wallet.infrastructure.adapter.persistence.TransactionEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamoDbTransactionRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    
    @Mock
    private WalletMetrics walletMetrics;
    
    @Mock
    private DynamoDbTable<TransactionEntity> table;
    
    @Mock
    private Timer.Sample sample;

    private DynamoDbTransactionRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("transactions"), any(TableSchema.class))).thenReturn(table);
        repository = new DynamoDbTransactionRepository(enhancedClient, walletMetrics);
    }

    @Test
    void shouldSaveTransaction() {
        when(walletMetrics.startDatabaseTimer()).thenReturn(sample);
        Transaction transaction = new Transaction(
            WalletId.generate(), 
            Transaction.Type.DEPOSIT, 
            Money.of(BigDecimal.valueOf(100)), 
            null, 
            null
        );
        
        repository.save(transaction);
        verify(table).putItem(any(TransactionEntity.class));
        verify(walletMetrics).recordDatabaseDuration(sample, "save_transaction");
    }

    @Test
    void shouldHandleSaveTransactionException() {
        when(walletMetrics.startDatabaseTimer()).thenReturn(sample);
        Transaction transaction = new Transaction(
            WalletId.generate(), 
            Transaction.Type.DEPOSIT, 
            Money.of(BigDecimal.valueOf(100)), 
            null, 
            null
        );
        
        doThrow(new RuntimeException("DynamoDB error")).when(table).putItem(any(TransactionEntity.class));
        
        assertThrows(RuntimeException.class, () -> repository.save(transaction));
        verify(walletMetrics).incrementDatabaseError("save_transaction");
        verify(walletMetrics).recordDatabaseDuration(sample, "save_transaction");
    }

    @Test
    void shouldSaveAllTransactions() {
        Transaction transaction1 = new Transaction(
            WalletId.generate(), 
            Transaction.Type.DEPOSIT, 
            Money.of(BigDecimal.valueOf(100)), 
            null, 
            null
        );
        Transaction transaction2 = new Transaction(
            WalletId.generate(), 
            Transaction.Type.WITHDRAWAL, 
            Money.of(BigDecimal.valueOf(50)), 
            null, 
            null
        );
        
        List<Transaction> transactions = List.of(transaction1, transaction2);
        
        repository.saveAll(transactions);
        verify(table, times(2)).putItem(any(TransactionEntity.class));
    }

    @Test
    void shouldFindTransactionsByWalletIdBeforeTimestamp() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId(walletId.value());
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100));
        entity.setTimestamp(timestamp.minusSeconds(3600));
        
        PageIterable<TransactionEntity> pageIterable = mock(PageIterable.class);
        when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() -> Stream.of(entity).iterator());
        
        List<Transaction> result = repository.findByWalletIdBeforeTimestamp(walletId, timestamp);
        
        assertEquals(1, result.size());
        assertEquals(walletId.value(), result.get(0).getWalletId().value());
    }

    @Test
    void shouldHandleFindTransactionsException() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        
        when(table.query(any(QueryEnhancedRequest.class))).thenThrow(new RuntimeException("Query failed"));
        
        assertThrows(RuntimeException.class, () -> repository.findByWalletIdBeforeTimestamp(walletId, timestamp));
    }
}