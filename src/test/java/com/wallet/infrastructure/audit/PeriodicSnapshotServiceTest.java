package com.wallet.infrastructure.audit;

import com.wallet.domain.model.Money;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.repositories.WalletRepository;
import com.wallet.domain.service.AuditService;
import com.wallet.domain.service.PeriodicSnapshotService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodicSnapshotServiceTest {

    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private AuditService auditService;
    
    private PeriodicSnapshotService snapshotService;
    
    @BeforeEach
    void setUp() {
        snapshotService = new PeriodicSnapshotService(walletRepository, auditService);
        ReflectionTestUtils.setField(snapshotService, "snapshotEnabled", true);
        ReflectionTestUtils.setField(snapshotService, "batchSize", 100);
    }
    
    @Test
    void shouldCreatePeriodicSnapshots() {
        // Arrange
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user-1");
        wallet.deposit(Money.of(BigDecimal.valueOf(100.00)));
        
        List<Wallet> wallets = Collections.singletonList(wallet);
        
        when(walletRepository.findAll(anyInt())).thenReturn(wallets);
        
        // Act
        snapshotService.createPeriodicSnapshots();
        
        // Assert
        verify(auditService).createWalletSnapshot(any(Wallet.class), any());
    }
    
    @Test
    void shouldNotCreateSnapshotsWhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(snapshotService, "snapshotEnabled", false);
        
        // Act
        snapshotService.createPeriodicSnapshots();
        
        // Assert
        verify(walletRepository, never()).findAll(anyInt());
        verify(auditService, never()).createWalletSnapshot(any(Wallet.class), any());
    }
    
    @Test
    void shouldVerifyTransactionChainIntegrity() {
        // Arrange
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, "user-1");
        
        List<Wallet> wallets = Collections.singletonList(wallet);
        
        when(walletRepository.findAll(anyInt())).thenReturn(wallets);
        when(auditService.verifyTransactionChainIntegrity(anyString())).thenReturn(true);
        
        // Act
        snapshotService.verifyTransactionChainIntegrity();
        
        // Assert
        verify(auditService).verifyTransactionChainIntegrity(anyString());
    }
    
    @Test
    void shouldNotVerifyIntegrityWhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(snapshotService, "snapshotEnabled", false);
        
        // Act
        snapshotService.verifyTransactionChainIntegrity();
        
        // Assert
        verify(walletRepository, never()).findAll(anyInt());
        verify(auditService, never()).verifyTransactionChainIntegrity(anyString());
    }
}