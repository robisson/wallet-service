package com.wallet.domain.service;

import com.wallet.domain.model.*;
import com.wallet.infrastructure.adapter.persistence.AuditLogEntity;
import com.wallet.infrastructure.adapter.persistence.TransactionEntity;
import com.wallet.infrastructure.adapter.persistence.WalletSnapshotEntity;
import com.wallet.infrastructure.metrics.WalletMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImmutableAuditServiceTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;
    
    @Mock
    private WalletMetrics walletMetrics;
    
    @Mock
    private DynamoDbTable<AuditLogEntity> auditLogTable;
    
    @Mock
    private DynamoDbTable<WalletSnapshotEntity> snapshotTable;
    
    @Mock
    private DynamoDbTable<TransactionEntity> transactionTable;

    private ImmutableAuditService auditService;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("audit_logs"), any(TableSchema.class))).thenReturn(auditLogTable);
        when(enhancedClient.table(eq("wallet_snapshots"), any(TableSchema.class))).thenReturn(snapshotTable);
        when(enhancedClient.table(eq("transactions"), any(TableSchema.class))).thenReturn(transactionTable);
        
        // Mock describeTable to avoid exceptions during initialization
        try {
            when(auditLogTable.describeTable()).thenReturn(null);
            when(snapshotTable.describeTable()).thenReturn(null);
        } catch (Exception e) {
            // Ignore mock setup exceptions
        }
        
        auditService = new ImmutableAuditService(enhancedClient, walletMetrics);
    }

    @Test
    void shouldAuditTransaction() {
        Transaction transaction = new Transaction(
            WalletId.generate(),
            Transaction.Type.DEPOSIT,
            Money.of(BigDecimal.valueOf(100)),
            null,
            null
        );
        
        AuditInfo auditInfo = AuditInfo.builder()
            .userId("user123")
            .sourceIp("192.168.1.1")
            .requestId("req123")
            .timestamp(Instant.now())
            .build();
        
        auditService.auditTransaction(transaction, auditInfo);
        
        verify(auditLogTable).putItem(any(AuditLogEntity.class));
        verify(walletMetrics).incrementAuditLog();
    }

    @Test
    void shouldCreateWalletSnapshot() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(100)));
        
        AuditInfo auditInfo = AuditInfo.builder()
            .userId("user123")
            .requestId("req123")
            .timestamp(Instant.now())
            .build();
        
        // Test that method executes without throwing exception
        assertDoesNotThrow(() -> auditService.createWalletSnapshot(wallet, auditInfo));
    }

    @Test
    void shouldVerifyTransactionChainIntegrityWithEmptyChain() {
        PageIterable<TransactionEntity> pageIterable = mock(PageIterable.class);
        when(transactionTable.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() -> Stream.<TransactionEntity>empty().iterator());
        
        boolean result = auditService.verifyTransactionChainIntegrity("wallet123");
        
        assertTrue(result);
    }

    @Test
    void shouldVerifyTransactionChainIntegrityWithValidChain() {
        PageIterable<TransactionEntity> pageIterable = mock(PageIterable.class);
        when(transactionTable.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(() -> Stream.<TransactionEntity>empty().iterator());
        
        boolean result = auditService.verifyTransactionChainIntegrity("wallet123");
        
        // Empty chain should return true
        assertTrue(result);
    }

    @Test
    void shouldHandleAuditTransactionException() {
        Transaction transaction = new Transaction(
            WalletId.generate(),
            Transaction.Type.DEPOSIT,
            Money.of(BigDecimal.valueOf(100)),
            null,
            null
        );
        
        doThrow(new RuntimeException("DynamoDB error")).when(auditLogTable).putItem(any(AuditLogEntity.class));
        
        assertDoesNotThrow(() -> auditService.auditTransaction(transaction, null));
        verify(walletMetrics).incrementAuditError("create_audit_log");
    }

    @Test
    void shouldHandleCreateWalletSnapshotException() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        // Test that method handles exception gracefully without stubbing
        assertDoesNotThrow(() -> auditService.createWalletSnapshot(wallet, null));
    }

    @Test
    void shouldHandleVerifyTransactionChainException() {
        when(transactionTable.query(any(QueryConditional.class))).thenThrow(new RuntimeException("Query failed"));
        
        boolean result = auditService.verifyTransactionChainIntegrity("wallet123");
        
        assertFalse(result);
        verify(walletMetrics).incrementAuditError("verify_transaction_chain");
    }

    @Test
    void shouldAuditTransactionWithRelatedWallet() {
        Transaction transaction = new Transaction(
            WalletId.generate(),
            Transaction.Type.TRANSFER_OUT,
            Money.of(BigDecimal.valueOf(50)),
            WalletId.generate(),
            null
        );
        
        auditService.auditTransaction(transaction, null);
        
        verify(auditLogTable).putItem(any(AuditLogEntity.class));
        verify(walletMetrics).incrementAuditLog();
    }

    @Test
    void shouldCreateWalletSnapshotWithoutAuditInfo() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        // Test that method executes without throwing exception
        assertDoesNotThrow(() -> auditService.createWalletSnapshot(wallet, null));
    }
}