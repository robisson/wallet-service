package com.wallet.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class EdgeCaseTest {

    @Test
    void shouldHandleMoneyEquality() {
        Money money1 = Money.of(BigDecimal.valueOf(100.00));
        Money money2 = Money.of(BigDecimal.valueOf(100.00));
        Money money3 = Money.of(BigDecimal.valueOf(50.00));
        
        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
        assertEquals(money1.hashCode(), money2.hashCode());
    }
    
    @Test
    void shouldHandleWalletIdEquality() {
        WalletId id1 = WalletId.of("same-id");
        WalletId id2 = WalletId.of("same-id");
        WalletId id3 = WalletId.of("different-id");
        
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
    
    @Test
    void shouldHandleTransactionGetters() {
        WalletId walletId = WalletId.generate();
        WalletId relatedWalletId = WalletId.generate();
        Transaction transaction = new Transaction(walletId, Transaction.Type.TRANSFER_OUT, 
            Money.of(BigDecimal.valueOf(25.00)), relatedWalletId);
        
        assertNotNull(transaction.getId());
        assertEquals(walletId, transaction.getWalletId());
        assertEquals(Transaction.Type.TRANSFER_OUT, transaction.getType());
        assertEquals(Money.of(BigDecimal.valueOf(25.00)), transaction.getAmount());
        assertEquals(relatedWalletId, transaction.getRelatedWalletId());
        assertNotNull(transaction.getTimestamp());
    }
    
    @Test
    void shouldHandleMoneyComparisons() {
        Money small = Money.of(BigDecimal.valueOf(10.00));
        Money large = Money.of(BigDecimal.valueOf(100.00));
        Money equal = Money.of(BigDecimal.valueOf(100.00));
        
        assertTrue(large.isGreaterThan(small));
        assertFalse(small.isGreaterThan(large));
        assertTrue(large.isGreaterThanOrEqual(small));
        assertTrue(large.isGreaterThanOrEqual(equal));
        assertFalse(small.isGreaterThanOrEqual(large));
    }
}