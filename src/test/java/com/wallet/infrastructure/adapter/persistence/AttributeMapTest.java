package com.wallet.infrastructure.adapter.persistence;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class AttributeMapTest {

    @Test
    void shouldCreateAttributeValueMapWithAllFields() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.valueOf(150.50));
        entity.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2024-01-01T11:00:00Z"));
        
        var map = entity.toAttributeValueMap();
        
        assertNotNull(map);
        assertEquals(5, map.size());
        assertEquals("wallet123", map.get("walletId").s());
        assertEquals("user123", map.get("userId").s());
        assertEquals("150.5", map.get("balance").n());
        assertEquals("2024-01-01T10:00:00Z", map.get("createdAt").s());
        assertEquals("2024-01-01T11:00:00Z", map.get("updatedAt").s());
    }
    
    @Test
    void shouldHandleZeroBalance() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(BigDecimal.ZERO);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        var map = entity.toAttributeValueMap();
        
        assertEquals("0", map.get("balance").n());
    }
    
    @Test
    void shouldHandleLargeBalance() {
        WalletEntity entity = new WalletEntity();
        entity.setWalletId("wallet123");
        entity.setUserId("user123");
        entity.setBalance(new BigDecimal("999999.99"));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        var map = entity.toAttributeValueMap();
        
        assertEquals("999999.99", map.get("balance").n());
    }
}