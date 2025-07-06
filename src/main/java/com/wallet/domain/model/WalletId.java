package com.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique wallet identifier.
 * 
 * <p>This immutable record encapsulates wallet identification with the following characteristics:
 * <ul>
 *   <li>Guaranteed uniqueness through UUID generation</li>
 *   <li>Type safety - prevents mixing with other string IDs</li>
 *   <li>Validation - ensures non-null and non-empty values</li>
 *   <li>Immutable - thread-safe and prevents accidental modification</li>
 * </ul>
 * 
 * <p>Using a dedicated value object for wallet IDs provides compile-time safety
 * and makes the domain model more expressive and less error-prone.
 * 
 * @param value the wallet identifier string, must not be null or empty
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record WalletId(String value) {
    
    public WalletId {
        Objects.requireNonNull(value, "Wallet ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Wallet ID cannot be empty");
        }
    }
    
    /**
     * Generates a new unique wallet identifier.
     * 
     * <p>Uses UUID.randomUUID() to ensure global uniqueness
     * across all wallet instances and application restarts.
     * 
     * @return a new WalletId with a unique UUID string
     */
    public static WalletId generate() {
        return new WalletId(UUID.randomUUID().toString());
    }
    
    /**
     * Creates a WalletId from an existing string value.
     * 
     * <p>Used when reconstructing wallet IDs from persistence
     * or external sources. The value is validated to ensure
     * it's not null or empty.
     * 
     * @param value the wallet identifier string, must not be null or empty
     * @return a new WalletId instance
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is empty or blank
     */
    public static WalletId of(String value) {
        return new WalletId(value);
    }
}