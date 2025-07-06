package com.wallet.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditConfigTest {

    @Test
    void testAuditExecutor() {
        // Arrange
        AuditConfig config = new AuditConfig();
        
        // Act
        Executor executor = config.auditExecutor();
        
        // Assert
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
    }
}