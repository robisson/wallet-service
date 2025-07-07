package com.wallet.application.usecase;

import com.wallet.application.dto.CreateWalletRequest;
import com.wallet.application.dto.TransactionRequest;
import com.wallet.application.dto.TransferRequest;
import com.wallet.application.dto.WalletResponse;
import com.wallet.domain.model.Money;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.repositories.TransactionRepository;
import com.wallet.domain.repositories.WalletRepository;
import com.wallet.domain.service.AuditService;
import com.wallet.domain.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletUseCaseTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private WalletService walletDomainService;
    
    @Mock
    private AuditService auditService;
    
    private WalletUseCase walletUseCase;
    
    @BeforeEach
    void setUp() {
        walletUseCase = new WalletUseCase(walletRepository, transactionRepository, walletDomainService, auditService);
    }
    
    @Test
    void shouldCreateWallet() {
        CreateWalletRequest request = new CreateWalletRequest("user123");
        Wallet wallet = new Wallet(WalletId.generate(), "user123");
        
        when(walletRepository.findByUserId("user123")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        
        WalletResponse response = walletUseCase.createWallet(request);
        
        assertEquals("user123", response.userId());
        assertEquals(BigDecimal.ZERO.setScale(2), response.balance());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void shouldThrowExceptionWhenUserAlreadyHasWallet() {
        CreateWalletRequest request = new CreateWalletRequest("user123");
        Wallet existingWallet = new Wallet(WalletId.generate(), "user123");
        
        when(walletRepository.findByUserId("user123")).thenReturn(Optional.of(existingWallet));
        
        assertThrows(IllegalArgumentException.class, () -> walletUseCase.createWallet(request));
    }
    
    @Test
    void shouldDepositMoney() {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user123");
        TransactionRequest request = new TransactionRequest(walletId.value(), BigDecimal.valueOf(100.00));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        
        WalletResponse response = walletUseCase.deposit(request);
        
        assertEquals(new BigDecimal("100.00"), response.balance());
        verify(walletRepository).saveWalletWithTransaction(any(), any());
    }
    
    @Test
    void shouldWithdrawMoney() {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(200.00)));
        TransactionRequest request = new TransactionRequest(walletId.value(), BigDecimal.valueOf(50.00));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        
        WalletResponse response = walletUseCase.withdraw(request);
        
        assertEquals(new BigDecimal("150.00"), response.balance());
        verify(walletRepository).saveWalletWithTransaction(any(), any());
    }
    
    @Test
    void shouldGetWallet() {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user123");
        wallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        
        WalletResponse response = walletUseCase.getWallet(walletId.value());
        
        assertEquals(walletId.value(), response.walletId());
        assertEquals("user123", response.userId());
        assertEquals(new BigDecimal("100.00"), response.balance());
    }
    
    @Test
    void shouldThrowExceptionWhenWalletNotFoundForDeposit() {
        WalletId walletId = WalletId.generate();
        TransactionRequest request = new TransactionRequest(walletId.value(), BigDecimal.valueOf(100.00));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> walletUseCase.deposit(request));
    }
    
    @Test
    void shouldTransferMoney() {
        WalletId fromWalletId = WalletId.generate();
        WalletId toWalletId = WalletId.generate();
        TransferRequest request = new TransferRequest(fromWalletId.value(), toWalletId.value(), BigDecimal.valueOf(50.00));
        
        walletUseCase.transfer(request);
        
        verify(walletDomainService).transfer(fromWalletId, toWalletId, Money.of(BigDecimal.valueOf(50.00)));
    }
    
    @Test
    void shouldGetHistoricalBalance() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        Money historicalBalance = Money.of(BigDecimal.valueOf(75.00));
        Wallet wallet = new Wallet(walletId, "user123");
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletDomainService.calculateHistoricalBalance(walletId, timestamp))
            .thenReturn(historicalBalance);
        
        WalletResponse response = walletUseCase.getHistoricalBalance(walletId.value(), timestamp);
        
        assertEquals(walletId.value(), response.walletId());
        assertEquals(new BigDecimal("75.00"), response.balance());
    }
    
    @Test
    void shouldThrowExceptionWhenWalletNotFoundForWithdraw() {
        WalletId walletId = WalletId.generate();
        TransactionRequest request = new TransactionRequest(walletId.value(), BigDecimal.valueOf(50.00));
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> walletUseCase.withdraw(request));
    }
    
    @Test
    void shouldThrowExceptionWhenWalletNotFoundForGet() {
        WalletId walletId = WalletId.generate();
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> walletUseCase.getWallet(walletId.value()));
    }
    
    @Test
    void shouldThrowExceptionWhenWalletNotFoundForHistoricalBalance() {
        WalletId walletId = WalletId.generate();
        Instant timestamp = Instant.now();
        
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> walletUseCase.getHistoricalBalance(walletId.value(), timestamp));
    }
}