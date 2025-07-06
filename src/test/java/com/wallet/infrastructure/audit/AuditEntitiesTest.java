package com.wallet.infrastructure.audit;

import org.junit.jupiter.api.Test;

import com.wallet.infrastructure.adapter.persistence.AuditLogEntity;
import com.wallet.infrastructure.adapter.persistence.WalletSnapshotEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditEntitiesTest {

    @Test
    void testAuditLogEntity() {
        // Arrange
        AuditLogEntity entity = new AuditLogEntity();
        String walletId = "wallet-123";
        String transactionId = "tx-123";
        String type = "DEPOSIT";
        BigDecimal amount = BigDecimal.valueOf(100.00);
        Instant timestamp = Instant.now();
        
        // Act
        entity.setWalletId(walletId);
        entity.setTransactionId(transactionId);
        entity.setType(type);
        entity.setAmount(amount);
        entity.setTimestamp(timestamp);
        
        // Assert
        assertEquals(walletId, entity.getWalletId());
        assertEquals(transactionId, entity.getTransactionId());
        assertEquals(type, entity.getType());
        assertEquals(amount, entity.getAmount());
        assertEquals(timestamp, entity.getTimestamp());
        
        // Test toString
        String toString = entity.toString();
        assertNotNull(toString);
    }
    
    @Test
    void testWalletSnapshotEntity() {
        // Arrange
        WalletSnapshotEntity entity = new WalletSnapshotEntity();
        String walletId = "wallet-123";
        String snapshotId = "snapshot-123";
        BigDecimal balance = BigDecimal.valueOf(100.00);
        Instant timestamp = Instant.now();
        String userId = "user-123";
        
        // Act
        entity.setWalletId(walletId);
        entity.setSnapshotId(snapshotId);
        entity.setBalance(balance);
        entity.setTimestamp(timestamp);
        entity.setUserId(userId);
        
        // Assert
        assertEquals(walletId, entity.getWalletId());
        assertEquals(snapshotId, entity.getSnapshotId());
        assertEquals(balance, entity.getBalance());
        assertEquals(timestamp, entity.getTimestamp());
        assertEquals(userId, entity.getUserId());
        
        // Test toString
        String toString = entity.toString();
        assertNotNull(toString);
    }
}