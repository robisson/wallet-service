package com.wallet.domain.service;

import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.model.Money;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import com.wallet.domain.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodicSnapshotServiceTest {

    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private AuditService auditService;

    private PeriodicSnapshotService periodicSnapshotService;

    @BeforeEach
    void setUp() {
        periodicSnapshotService = new PeriodicSnapshotService(walletRepository, auditService);
        ReflectionTestUtils.setField(periodicSnapshotService, "snapshotEnabled", true);
        ReflectionTestUtils.setField(periodicSnapshotService, "batchSize", 100);
    }

    @Test
    void shouldCreatePeriodicSnapshots() {
        Wallet wallet1 = new Wallet(WalletId.generate(), "user1");
        wallet1.deposit(Money.of(BigDecimal.valueOf(100)));
        
        Wallet wallet2 = new Wallet(WalletId.generate(), "user2");
        wallet2.deposit(Money.of(BigDecimal.valueOf(200)));
        
        when(walletRepository.findAll(100)).thenReturn(List.of(wallet1, wallet2));
        
        periodicSnapshotService.createPeriodicSnapshots();
        
        verify(walletRepository).findAll(100);
        verify(auditService, times(2)).createWalletSnapshot(any(Wallet.class), any(AuditInfo.class));
    }

    @Test
    void shouldSkipPeriodicSnapshotsWhenDisabled() {
        ReflectionTestUtils.setField(periodicSnapshotService, "snapshotEnabled", false);
        
        periodicSnapshotService.createPeriodicSnapshots();
        
        verify(walletRepository, never()).findAll(anyInt());
        verify(auditService, never()).createWalletSnapshot(any(Wallet.class), any(AuditInfo.class));
    }

    @Test
    void shouldHandleExceptionDuringPeriodicSnapshots() {
        when(walletRepository.findAll(100)).thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> periodicSnapshotService.createPeriodicSnapshots());
        
        verify(walletRepository).findAll(100);
        verify(auditService, never()).createWalletSnapshot(any(Wallet.class), any(AuditInfo.class));
    }

    @Test
    void shouldVerifyTransactionChainIntegrity() {
        Wallet wallet1 = new Wallet(WalletId.generate(), "user1");
        Wallet wallet2 = new Wallet(WalletId.generate(), "user2");
        
        when(walletRepository.findAll(100)).thenReturn(List.of(wallet1, wallet2));
        when(auditService.verifyTransactionChainIntegrity(wallet1.getId().value())).thenReturn(true);
        when(auditService.verifyTransactionChainIntegrity(wallet2.getId().value())).thenReturn(true);
        
        periodicSnapshotService.verifyTransactionChainIntegrity();
        
        verify(walletRepository).findAll(100);
        verify(auditService).verifyTransactionChainIntegrity(wallet1.getId().value());
        verify(auditService).verifyTransactionChainIntegrity(wallet2.getId().value());
    }

    @Test
    void shouldSkipIntegrityVerificationWhenDisabled() {
        ReflectionTestUtils.setField(periodicSnapshotService, "snapshotEnabled", false);
        
        periodicSnapshotService.verifyTransactionChainIntegrity();
        
        verify(walletRepository, never()).findAll(anyInt());
        verify(auditService, never()).verifyTransactionChainIntegrity(anyString());
    }

    @Test
    void shouldHandleIntegrityVerificationFailure() {
        Wallet wallet = new Wallet(WalletId.generate(), "user1");
        
        when(walletRepository.findAll(100)).thenReturn(List.of(wallet));
        when(auditService.verifyTransactionChainIntegrity(wallet.getId().value())).thenReturn(false);
        
        assertDoesNotThrow(() -> periodicSnapshotService.verifyTransactionChainIntegrity());
        
        verify(auditService).verifyTransactionChainIntegrity(wallet.getId().value());
    }

    @Test
    void shouldHandleExceptionDuringIntegrityVerification() {
        when(walletRepository.findAll(100)).thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> periodicSnapshotService.verifyTransactionChainIntegrity());
        
        verify(walletRepository).findAll(100);
        verify(auditService, never()).verifyTransactionChainIntegrity(anyString());
    }

    @Test
    void shouldHandleExceptionForIndividualWalletSnapshot() {
        Wallet wallet1 = new Wallet(WalletId.generate(), "user1");
        Wallet wallet2 = new Wallet(WalletId.generate(), "user2");
        
        when(walletRepository.findAll(100)).thenReturn(List.of(wallet1, wallet2));
        doThrow(new RuntimeException("Snapshot error")).when(auditService).createWalletSnapshot(eq(wallet1), any(AuditInfo.class));
        
        assertDoesNotThrow(() -> periodicSnapshotService.createPeriodicSnapshots());
        
        verify(auditService).createWalletSnapshot(eq(wallet1), any(AuditInfo.class));
        verify(auditService).createWalletSnapshot(eq(wallet2), any(AuditInfo.class));
    }

    @Test
    void shouldHandleExceptionForIndividualWalletIntegrityCheck() {
        Wallet wallet1 = new Wallet(WalletId.generate(), "user1");
        Wallet wallet2 = new Wallet(WalletId.generate(), "user2");
        
        when(walletRepository.findAll(100)).thenReturn(List.of(wallet1, wallet2));
        when(auditService.verifyTransactionChainIntegrity(wallet1.getId().value()))
            .thenThrow(new RuntimeException("Integrity check error"));
        when(auditService.verifyTransactionChainIntegrity(wallet2.getId().value())).thenReturn(true);
        
        assertDoesNotThrow(() -> periodicSnapshotService.verifyTransactionChainIntegrity());
        
        verify(auditService).verifyTransactionChainIntegrity(wallet1.getId().value());
        verify(auditService).verifyTransactionChainIntegrity(wallet2.getId().value());
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception, but got: " + e.getMessage(), e);
        }
    }
}