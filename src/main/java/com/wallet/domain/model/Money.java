package com.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a monetary amount in the wallet system.
 * 
 * <p>This immutable record encapsulates monetary values with the following characteristics:
 * <ul>
 *   <li>Always non-negative (cannot represent debt)</li>
 *   <li>Precision fixed to 2 decimal places</li>
 *   <li>Uses HALF_UP rounding mode for financial accuracy</li>
 *   <li>Immutable - operations return new instances</li>
 * </ul>
 * 
 * <p>The Money class ensures type safety and prevents common monetary calculation errors
 * by encapsulating BigDecimal operations and enforcing business rules.
 * 
 * @param amount the monetary amount as BigDecimal, must be non-negative
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record Money(BigDecimal amount) {
    
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Creates a Money instance from a BigDecimal amount.
     * 
     * @param amount the monetary amount, must be non-negative
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is negative
     * @throws NullPointerException if amount is null
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }
    
    /**
     * Creates a Money instance from a double amount.
     * 
     * <p>Note: Use with caution due to floating-point precision issues.
     * Prefer using BigDecimal or string-based constructors for exact amounts.
     * 
     * @param amount the monetary amount as double, must be non-negative
     * @return a new Money instance
     * @throws IllegalArgumentException if amount is negative
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }
    
    /**
     * Creates a Money instance representing zero amount.
     * 
     * @return a Money instance with zero value
     */
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
    
    /**
     * Adds another Money amount to this amount.
     * 
     * @param other the Money amount to add, must not be null
     * @return a new Money instance with the sum
     * @throws NullPointerException if other is null
     */
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
    
    /**
     * Subtracts another Money amount from this amount.
     * 
     * @param other the Money amount to subtract, must not be null
     * @return a new Money instance with the difference
     * @throws IllegalArgumentException if result would be negative
     * @throws NullPointerException if other is null
     */
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }
    
    /**
     * Checks if this amount is greater than another amount.
     * 
     * @param other the Money amount to compare with, must not be null
     * @return true if this amount is greater than other
     * @throws NullPointerException if other is null
     */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Checks if this amount is greater than or equal to another amount.
     * 
     * @param other the Money amount to compare with, must not be null
     * @return true if this amount is greater than or equal to other
     * @throws NullPointerException if other is null
     */
    public boolean isGreaterThanOrEqual(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }
}