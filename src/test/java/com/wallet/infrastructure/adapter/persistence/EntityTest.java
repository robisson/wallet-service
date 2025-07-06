package com.wallet.infrastructure.adapter.persistence;

import com.wallet.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void shouldConvertWalletEntityFromDomain() {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        
        WalletEntity entity = WalletEntity.fromDomain(wallet);
        
        assertEquals(walletId.value(), entity.getWalletId());
        assertEquals("user123", entity.getUserId());
        assertEquals(BigDecimal.valueOf(100.00).setScale(2), entity.getBalance());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void shouldConvertWalletEntityToDomain() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(50.00));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        Wallet wallet = entity.toDomain();
        
        assertEquals("wallet123", wallet.getId().value());
        assertEquals("user123", wallet.getUserId());
        assertEquals(Money.of(BigDecimal.valueOf(50.00)), wallet.getBalance());
    }

    @Test
    void shouldCreateAttributeValueMap() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(100.00));
        entity.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        
        var map = entity.toAttributeValueMap();
        
        assertEquals("wallet123", map.get("walletId").s());
        assertEquals("user123", map.get("userId").s());
        assertEquals("100.0", map.get("balance").n());
    }

    @Test
    void shouldConvertTransactionEntityFromDomain() {
        WalletId walletId = WalletId.generate();
        WalletId relatedWalletId = WalletId.generate();
        Transaction transaction = new Transaction(walletId, Transaction.Type.TRANSFER_OUT, 
            Money.of(BigDecimal.valueOf(25.00)), relatedWalletId);
        
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        
        assertEquals(walletId.value(), entity.getWalletId());
        assertEquals(transaction.getId(), entity.getTransactionId());
        assertEquals("TRANSFER_OUT", entity.getType());
        assertEquals(BigDecimal.valueOf(25.00).setScale(2), entity.getAmount());
        assertEquals(relatedWalletId.value(), entity.getRelatedWalletId());
    }

    @Test
    void shouldConvertTransactionEntityToDomain() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100.00));
        entity.setTimestamp(Instant.now());
        
        Transaction transaction = entity.toDomain();
        
        assertEquals("wallet123", transaction.getWalletId().value());
        assertEquals(Transaction.Type.DEPOSIT, transaction.getType());
        assertEquals(Money.of(BigDecimal.valueOf(100.00)), transaction.getAmount());
        assertNull(transaction.getRelatedWalletId());
    }
    
    @Test
    void shouldHandleAllTransactionEntityGettersSetters() {
        TransactionEntity entity = new TransactionEntity();
        Instant now = Instant.now();
        
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("WITHDRAWAL");
        entity.setAmount(BigDecimal.valueOf(50.00));
        entity.setRelatedWalletId("wallet456");
        entity.setTimestamp(now);
        
        assertEquals("wallet123", entity.getWalletId());
        assertEquals("tx123", entity.getTransactionId());
        assertEquals("WITHDRAWAL", entity.getType());
        assertEquals(BigDecimal.valueOf(50.00), entity.getAmount());
        assertEquals("wallet456", entity.getRelatedWalletId());
        assertEquals(now, entity.getTimestamp());
    }
    
    @Test
    void shouldHandleAllWalletEntityGettersSetters() {
        WalletEntity entity = new WalletEntity();
        Instant now = Instant.now();
        
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(100.00));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
        assertEquals("wallet123", entity.getWalletId());
        assertEquals("user123", entity.getUserId());
        assertEquals(BigDecimal.valueOf(100.00), entity.getBalance());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }
    
    @Test
    void shouldConvertTransactionWithRelatedWallet() {
        WalletId walletId = WalletId.generate();
        WalletId relatedWalletId = WalletId.generate();
        Transaction transaction = new Transaction(walletId, Transaction.Type.TRANSFER_IN, 
            Money.of(BigDecimal.valueOf(75.00)), relatedWalletId);
        
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        Transaction converted = entity.toDomain();
        
        assertEquals(walletId.value(), converted.getWalletId().value());
        assertEquals(Transaction.Type.TRANSFER_IN, converted.getType());
        assertEquals(relatedWalletId.value(), converted.getRelatedWalletId().value());
    }
}