package com.wallet.domain.service;

import com.wallet.domain.model.*;
import com.wallet.domain.repositories.TransactionRepository;
import com.wallet.domain.repositories.WalletRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletDomainServiceTest {

    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AuditService auditService;
    
    private WalletService walletDomainService;
    
    @BeforeEach
    void setUp() {
        walletDomainService = new WalletService(walletRepository, transactionRepository, auditService);
    }
    
    @Test
    void shouldTransferMoneyBetweenWallets() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(50.00));

        Wallet fromWallet = new Wallet(fromWalletId, "user1");
        fromWallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        Wallet toWallet = new Wallet(toWalletId, "user2");

        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));

        walletDomainService.transfer(fromWalletId, toWalletId, amount);

        assertEquals(Money.of(BigDecimal.valueOf(50.00)), fromWallet.getBalance());
        assertEquals(Money.of(BigDecimal.valueOf(50.00)), toWallet.getBalance());
        verify(walletRepository).saveWithTransaction(eq(fromWallet), eq(toWallet), any(Transaction.class), any(Transaction.class));
        // Não é mais necessário verificar transactionRepository.saveAll
    }
    
    @Test
    void shouldThrowExceptionWhenSourceWalletNotFound() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(50.00));
        
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> walletDomainService.transfer(fromWalletId, toWalletId, amount));
    }
    
    @Test
    void shouldThrowExceptionWhenDestinationWalletNotFound() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(50.00));
        
        Wallet fromWallet = new Wallet(fromWalletId, "user1");
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> walletDomainService.transfer(fromWalletId, toWalletId, amount));
    }
    
    @Test
    void shouldCalculateHistoricalBalance() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        
        List<Transaction> transactions = Arrays.asList(
            new Transaction(walletId, Transaction.Type.DEPOSIT, Money.of(BigDecimal.valueOf(100.00)), null, null),
            new Transaction(walletId, Transaction.Type.WITHDRAWAL, Money.of(BigDecimal.valueOf(30.00)), null, null),
            new Transaction(walletId, Transaction.Type.TRANSFER_IN, Money.of(BigDecimal.valueOf(20.00)), WalletId.generate(), null)
        );
        
        when(transactionRepository.findByWalletIdBeforeTimestamp(walletId, timestamp))
            .thenReturn(transactions);
        
        Money balance = walletDomainService.calculateHistoricalBalance(walletId, timestamp);
        
        assertEquals(Money.of(BigDecimal.valueOf(90.00)), balance);
    }
    
    @Test
    void shouldCalculateHistoricalBalanceWithTransferOut() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        
        List<Transaction> transactions = Arrays.asList(
            new Transaction(walletId, Transaction.Type.DEPOSIT, Money.of(BigDecimal.valueOf(100.00)), null, null),
            new Transaction(walletId, Transaction.Type.TRANSFER_OUT, Money.of(BigDecimal.valueOf(25.00)), WalletId.generate(), null)
        );
        
        when(transactionRepository.findByWalletIdBeforeTimestamp(walletId, timestamp))
            .thenReturn(transactions);
        
        Money balance = walletDomainService.calculateHistoricalBalance(walletId, timestamp);
        
        assertEquals(Money.of(BigDecimal.valueOf(75.00)), balance);
    }
    
    @Test
    void shouldThrowExceptionWhenInsufficientFundsForTransfer() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        Money amount = Money.of(BigDecimal.valueOf(150.00));
        
        Wallet fromWallet = new Wallet(fromWalletId, "user1");
        fromWallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        Wallet toWallet = new Wallet(toWalletId, "user2");
        
        when(walletRepository.findById(fromWalletId)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(toWalletId)).thenReturn(Optional.of(toWallet));
        
        assertThrows(IllegalArgumentException.class, 
            () -> walletDomainService.transfer(fromWalletId, toWalletId, amount));
    }
}