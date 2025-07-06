package com.wallet.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalletIdTest {

    @Test
    void shouldGenerateUniqueWalletIds() {
        WalletId walletId1 = WalletId.generate();
        WalletId walletId2 = WalletId.generate();
        
        assertNotNull(walletId1.value());
        assertNotNull(walletId2.value());
        assertNotEquals(walletId1.value(), walletId2.value());
    }

    @Test
    void shouldCreateWalletIdFromString() {
        String value = "test-wallet-id";
        WalletId walletId = WalletId.of(value);
        
        assertEquals(value, walletId.value());
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        String value = "same-wallet-id";
        WalletId walletId1 = WalletId.of(value);
        WalletId walletId2 = WalletId.of(value);
        
        assertEquals(walletId1, walletId2);
        assertEquals(walletId1.hashCode(), walletId2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentValue() {
        WalletId walletId1 = WalletId.of("wallet1");
        WalletId walletId2 = WalletId.of("wallet2");
        
        assertNotEquals(walletId1, walletId2);
    }
    
    @Test
    void shouldThrowExceptionForNullValue() {
        assertThrows(NullPointerException.class, () -> WalletId.of(null));
    }
    
    @Test
    void shouldThrowExceptionForEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> WalletId.of(""));
        assertThrows(IllegalArgumentException.class, () -> WalletId.of("   "));
    }
    
    @Test
    void shouldHaveToStringMethod() {
        WalletId walletId = WalletId.of("test-id");
        String toString = walletId.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("test-id"));
    }
}