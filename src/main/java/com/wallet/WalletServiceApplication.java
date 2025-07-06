package com.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Wallet Service application.
 * 
 * <p>This is a mission-critical financial service that manages user wallets with support for:
 * <ul>
 *   <li>Wallet creation and management</li>
 *   <li>Deposit and withdrawal operations</li>
 *   <li>Money transfers between wallets</li>
 *   <li>Historical balance queries</li>
 *   <li>Comprehensive audit logging</li>
 *   <li>Regulatory compliance features</li>
 * </ul>
 * 
 * <p>The application follows hexagonal architecture (ports & adapters) with Domain-Driven Design principles,
 * ensuring clean separation of concerns and maintainable code structure.
 * 
 * <p>Key features:
 * <ul>
 *   <li>ACID transactions using DynamoDB</li>
 *   <li>Immutable audit trail with hash chains</li>
 *   <li>Comprehensive monitoring and metrics</li>
 *   <li>Structured logging with correlation IDs</li>
 *   <li>Periodic wallet snapshots for reconciliation</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class WalletServiceApplication {
    
    /**
     * Main method to start the Wallet Service application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}