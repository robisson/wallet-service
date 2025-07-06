package com.wallet.infrastructure.audit;

import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.model.Money;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.service.ImmutableAuditService;
import com.wallet.infrastructure.adapter.persistence.AuditLogEntity;
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

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private DynamoDbTable<Object> transactionTable;
    
    private ImmutableAuditService auditService;
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // Configurar mocks com type erasure
        when(enhancedClient.table(eq("audit_logs"), any(TableSchema.class))).thenReturn((DynamoDbTable) auditLogTable);
        when(enhancedClient.table(eq("wallet_snapshots"), any(TableSchema.class))).thenReturn((DynamoDbTable) snapshotTable);
        when(enhancedClient.table(eq("transactions"), any(TableSchema.class))).thenReturn((DynamoDbTable) transactionTable);
        
        // Mock para describeTable
        when(auditLogTable.describeTable()).thenReturn(null);
        when(snapshotTable.describeTable()).thenReturn(null);
        
        auditService = new ImmutableAuditService(enhancedClient, walletMetrics);
        
        // Definir campo snapshotEnabled como true usando reflection
        try {
            java.lang.reflect.Field field = ImmutableAuditService.class.getDeclaredField("snapshotEnabled");
            field.setAccessible(true);
            field.set(auditService, true);
        } catch (Exception e) {
            // Ignorar erro
        }
    }
    
    @Test
    void shouldAuditTransaction() {
        // Arrange
        WalletId walletId = WalletId.of("wallet-123");
        Transaction transaction = new Transaction(
            walletId,
            Transaction.Type.DEPOSIT,
            Money.of(BigDecimal.valueOf(100.00)),
            null
        );
        
        AuditInfo auditInfo = AuditInfo.builder()
            .userId("user-123")
            .requestId("req-123")
            .sourceIp("127.0.0.1")
            .userAgent("test-agent")
            .timestamp(Instant.now())
            .build();
        
        // Configurar o mock para aceitar qualquer AuditLogEntity
        doNothing().when(auditLogTable).putItem(any(AuditLogEntity.class));
        
        // Act
        auditService.auditTransaction(transaction, auditInfo);
        
        // Assert - usar timeout para lidar com execução assíncrona
        verify(auditLogTable, timeout(1000)).putItem(any(AuditLogEntity.class));
        verify(walletMetrics, timeout(1000)).incrementAuditLog();
    }
    
    @Test
    void shouldCreateWalletSnapshot() {
        // Arrange
        WalletId walletId = WalletId.of("wallet-123");
        Wallet wallet = new Wallet(walletId, "user-123");
        wallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        
        AuditInfo auditInfo = AuditInfo.builder()
            .userId("user-123")
            .requestId("req-123")
            .sourceIp("127.0.0.1")
            .userAgent("test-agent")
            .timestamp(Instant.now())
            .build();
        
        // Configurar o mock para aceitar qualquer WalletSnapshotEntity
        doNothing().when(snapshotTable).putItem(any(WalletSnapshotEntity.class));
        
        // Act
        auditService.createWalletSnapshot(wallet, auditInfo);
        
        // Assert
        verify(snapshotTable, timeout(1000)).putItem(any(WalletSnapshotEntity.class));
        verify(walletMetrics, timeout(1000)).incrementWalletSnapshot();
    }
}