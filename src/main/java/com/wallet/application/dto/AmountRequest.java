package com.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Data Transfer Object representing a monetary amount in API requests.
 * 
 * <p>This record is used for deposit and withdrawal operations where only
 * an amount is required. The amount is validated to ensure it's positive
 * and has a minimum value of 0.01 (1 cent).
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>Amount cannot be null</li>
 *   <li>Amount must be at least 0.01</li>
 *   <li>Amount is automatically rounded to 2 decimal places</li>
 * </ul>
 * 
 * @param amount the monetary amount, must be positive and at least 0.01
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record AmountRequest(
    @JsonProperty("amount")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") 
    BigDecimal amount
) {
}