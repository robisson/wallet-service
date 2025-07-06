package com.wallet.infrastructure.adapter.persistence;

import com.wallet.domain.model.*;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EntityMappingTest {

    @Test
    void shouldConvertWalletEntityToAttributeValueMap() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(100.50));
        entity.setCreatedAt(Instant.parse("2023-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2023-01-01T01:00:00Z"));
        
        Map<String, AttributeValue> map = entity.toAttributeValueMap();
        
        assertEquals("wallet123", map.get("walletId").s());
        assertEquals("user123", map.get("userId").s());
        assertEquals("100.5", map.get("balance").n());
        assertEquals("2023-01-01T00:00:00Z", map.get("createdAt").s());
        assertEquals("2023-01-01T01:00:00Z", map.get("updatedAt").s());
    }

    @Test
    void shouldConvertTransactionEntityToAttributeValueMap() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100.00));
        entity.setTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        entity.setUserId("user123");
        entity.setSourceIp("192.168.1.1");
        entity.setUserAgent("TestAgent");
        entity.setRequestId("req123");
        entity.setAuditTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        entity.setTransactionHash("hash123");
        entity.setPreviousTransactionHash("prevhash123");
        
        Map<String, AttributeValue> map = entity.toAttributeValueMap();
        
        assertEquals("wallet123", map.get("walletId").s());
        assertEquals("tx123", map.get("transactionId").s());
        assertEquals("DEPOSIT", map.get("type").s());
        assertEquals("100.0", map.get("amount").n());
        assertEquals("2023-01-01T00:00:00Z", map.get("timestamp").s());
        assertEquals("user123", map.get("userId").s());
        assertEquals("192.168.1.1", map.get("sourceIp").s());
        assertEquals("TestAgent", map.get("userAgent").s());
        assertEquals("req123", map.get("requestId").s());
        assertEquals("2023-01-01T00:00:00Z", map.get("auditTimestamp").s());
        assertEquals("hash123", map.get("transactionHash").s());
        assertEquals("prevhash123", map.get("previousTransactionHash").s());
    }

    @Test
    void shouldConvertTransactionEntityToAttributeValueMapWithNulls() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100.00));
        entity.setTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        
        Map<String, AttributeValue> map = entity.toAttributeValueMap();
        
        assertEquals("wallet123", map.get("walletId").s());
        assertEquals("tx123", map.get("transactionId").s());
        assertEquals("DEPOSIT", map.get("type").s());
        assertEquals("100.0", map.get("amount").n());
        assertEquals("2023-01-01T00:00:00Z", map.get("timestamp").s());
        assertFalse(map.containsKey("relatedWalletId"));
        assertFalse(map.containsKey("userId"));
        assertFalse(map.containsKey("sourceIp"));
    }

    @Test
    void shouldConvertWalletFromDomainToEntity() {
        Wallet wallet = new Wallet(WalletId.of("wallet123"), "user123");
        
        WalletEntity entity = WalletEntity.fromDomain(wallet);
        
        assertEquals("wallet123", entity.getWalletId());
        assertEquals("user123", entity.getUserId());
        assertEquals(BigDecimal.ZERO.setScale(2), entity.getBalance());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void shouldConvertWalletFromEntityToDomain() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(100.50));
        entity.setCreatedAt(Instant.parse("2023-01-01T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2023-01-01T01:00:00Z"));
        
        Wallet wallet = entity.toDomain();
        
        assertEquals("wallet123", wallet.getId().value());
        assertEquals("user123", wallet.getUserId());
        assertEquals(new BigDecimal("100.50"), wallet.getBalance().amount());
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), wallet.getCreatedAt());
        assertEquals(Instant.parse("2023-01-01T01:00:00Z"), wallet.getUpdatedAt());
    }

    @Test
    void shouldConvertTransactionFromDomainToEntity() {
        AuditInfo auditInfo = AuditInfo.builder()
            .userId("user123")
            .sourceIp("192.168.1.1")
            .userAgent("TestAgent")
            .requestId("req123")
            .timestamp(Instant.parse("2023-01-01T00:00:00Z"))
            .build();
            
        Transaction transaction = new Transaction(
            WalletId.of("wallet123"),
            Transaction.Type.DEPOSIT,
            Money.of(BigDecimal.valueOf(100.00)),
            WalletId.of("wallet456"),
            auditInfo
        );
        
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        
        assertEquals("wallet123", entity.getWalletId());
        assertEquals("DEPOSIT", entity.getType());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("wallet456", entity.getRelatedWalletId());
        assertEquals("user123", entity.getUserId());
        assertEquals("192.168.1.1", entity.getSourceIp());
        assertEquals("TestAgent", entity.getUserAgent());
        assertEquals("req123", entity.getRequestId());
        assertNotNull(entity.getTransactionHash());
    }

    @Test
    void shouldConvertTransactionFromEntityToDomain() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("WITHDRAWAL");
        entity.setAmount(BigDecimal.valueOf(50.00));
        entity.setRelatedWalletId("wallet456");
        entity.setTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        entity.setUserId("user123");
        entity.setSourceIp("192.168.1.1");
        entity.setUserAgent("TestAgent");
        entity.setRequestId("req123");
        entity.setAuditTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        
        Transaction transaction = entity.toDomain();
        
        assertEquals("wallet123", transaction.getWalletId().value());
        assertEquals(Transaction.Type.WITHDRAWAL, transaction.getType());
        assertEquals(new BigDecimal("50.00"), transaction.getAmount().amount());
        assertEquals("wallet456", transaction.getRelatedWalletId().value());
        assertNotNull(transaction.getAuditInfo());
        assertEquals("user123", transaction.getAuditInfo().getUserId());
        assertEquals("192.168.1.1", transaction.getAuditInfo().getSourceIp());
    }

    @Test
    void shouldConvertTransactionFromEntityToDomainWithoutAuditInfo() {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId("wallet123");
        entity.setTransactionId("tx123");
        entity.setType("DEPOSIT");
        entity.setAmount(BigDecimal.valueOf(100.00));
        entity.setTimestamp(Instant.parse("2023-01-01T00:00:00Z"));
        
        Transaction transaction = entity.toDomain();
        
        assertEquals("wallet123", transaction.getWalletId().value());
        assertEquals(Transaction.Type.DEPOSIT, transaction.getType());
        assertEquals(new BigDecimal("100.00"), transaction.getAmount().amount());
        assertNull(transaction.getRelatedWalletId());
        assertNull(transaction.getAuditInfo());
    }
}