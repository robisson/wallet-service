package com.wallet.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    
    @Test
    void shouldCreateMoneyWithValidAmount() {
        Money money = Money.of(BigDecimal.valueOf(100.50));
        assertEquals(new BigDecimal("100.50"), money.amount());
    }
    
    @Test
    void shouldThrowExceptionForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> 
            Money.of(BigDecimal.valueOf(-10.00)));
    }
    
    @Test
    void shouldAddMoney() {
        Money money1 = Money.of(BigDecimal.valueOf(50.25));
        Money money2 = Money.of(BigDecimal.valueOf(25.75));
        
        Money result = money1.add(money2);
        
        assertEquals(new BigDecimal("76.00"), result.amount());
    }
    
    @Test
    void shouldSubtractMoney() {
        Money money1 = Money.of(BigDecimal.valueOf(100.00));
        Money money2 = Money.of(BigDecimal.valueOf(30.00));
        
        Money result = money1.subtract(money2);
        
        assertEquals(new BigDecimal("70.00"), result.amount());
    }
    
    @Test
    void shouldCompareMoneyAmounts() {
        Money money1 = Money.of(BigDecimal.valueOf(100.00));
        Money money2 = Money.of(BigDecimal.valueOf(50.00));
        
        assertTrue(money1.isGreaterThan(money2));
        assertTrue(money1.isGreaterThanOrEqual(money2));
        assertFalse(money2.isGreaterThan(money1));
    }
    
    @Test
    void shouldCreateMoneyFromDouble() {
        Money money = Money.of(99.99);
        assertEquals(new BigDecimal("99.99"), money.amount());
    }
    
    @Test
    void shouldCreateZeroMoney() {
        Money zero = Money.zero();
        assertEquals(BigDecimal.ZERO.setScale(2), zero.amount());
    }
    
    @Test
    void shouldThrowExceptionForNullAmount() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
    }
}