package com.wallet.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class WalletTest {
    
    @Test
    void shouldCreateWalletWithZeroBalance() {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user123");
        
        assertEquals(walletId, wallet.getId());
        assertEquals("user123", wallet.getUserId());
        assertEquals(Money.zero(), wallet.getBalance());
    }
    
    @Test
    void shouldDepositMoney() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        Money depositAmount = Money.of(BigDecimal.valueOf(100.50));
        
        wallet.deposit(depositAmount);
        
        assertEquals(depositAmount, wallet.getBalance());
    }
    
    @Test
    void shouldWithdrawMoney() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        
        wallet.withdraw(Money.of(BigDecimal.valueOf(50.00)));
        
        assertEquals(Money.of(BigDecimal.valueOf(50.00)), wallet.getBalance());
    }
    
    @Test
    void shouldThrowExceptionWhenWithdrawingMoreThanBalance() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(50.00)));
        
        assertThrows(IllegalArgumentException.class, () -> 
            wallet.withdraw(Money.of(BigDecimal.valueOf(100.00))));
    }
    
    @Test
    void shouldThrowExceptionForNegativeDeposit() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        assertThrows(IllegalArgumentException.class, () -> 
            wallet.deposit(Money.of(BigDecimal.valueOf(-10.00))));
    }
    
    @Test
    void shouldThrowExceptionForZeroDeposit() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        assertThrows(IllegalArgumentException.class, () -> 
            wallet.deposit(Money.zero()));
    }
    
    @Test
    void shouldThrowExceptionForZeroWithdrawal() {
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        assertThrows(IllegalArgumentException.class, () -> 
            wallet.withdraw(Money.zero()));
    }
    
    @Test
    void shouldCreateWalletWithConstructorParameters() {
        WalletId walletId = WalletId.generate();
        Money balance = Money.of(BigDecimal.valueOf(100.00));
        Instant now = Instant.now();
        
        Wallet wallet = new Wallet(walletId, "user123", balance, now, now);
        
        assertEquals(walletId, wallet.getId());
        assertEquals("user123", wallet.getUserId());
        assertEquals(balance, wallet.getBalance());
        assertEquals(now, wallet.getCreatedAt());
        assertEquals(now, wallet.getUpdatedAt());
    }
}