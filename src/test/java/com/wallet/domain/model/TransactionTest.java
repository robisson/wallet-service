package com.wallet.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreateDepositTransaction() {
        WalletId walletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(100.00));
        
        Transaction transaction = new Transaction(walletId, Transaction.Type.DEPOSIT, amount, null);
        
        assertNotNull(transaction.getId());
        assertEquals(walletId, transaction.getWalletId());
        assertEquals(Transaction.Type.DEPOSIT, transaction.getType());
        assertEquals(amount, transaction.getAmount());
        assertNull(transaction.getRelatedWalletId());
        assertNotNull(transaction.getTimestamp());
    }

    @Test
    void shouldCreateTransferTransaction() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(50.00));
        
        Transaction transaction = new Transaction(fromWalletId, Transaction.Type.TRANSFER_OUT, amount, toWalletId);
        
        assertEquals(fromWalletId, transaction.getWalletId());
        assertEquals(Transaction.Type.TRANSFER_OUT, transaction.getType());
        assertEquals(toWalletId, transaction.getRelatedWalletId());
    }

    @Test
    void shouldThrowExceptionForNullWalletId() {
        Money amount = Money.of(BigDecimal.valueOf(100.00));
        
        assertThrows(NullPointerException.class, () -> 
            new Transaction(null, Transaction.Type.DEPOSIT, amount, null));
    }

    @Test
    void shouldThrowExceptionForNullType() {
        WalletId walletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(100.00));
        
        assertThrows(NullPointerException.class, () -> 
            new Transaction(walletId, null, amount, null));
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        WalletId walletId = WalletId.generate();
        
        assertThrows(NullPointerException.class, () -> 
            new Transaction(walletId, Transaction.Type.DEPOSIT, null, null));
    }
}