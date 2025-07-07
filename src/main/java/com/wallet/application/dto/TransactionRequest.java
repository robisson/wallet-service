package com.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Data Transfer Object for single-wallet transaction requests.
 * 
 * <p>This record is used internally for deposit and withdrawal operations
 * where a specific wallet and amount are required. It combines wallet
 * identification with monetary amount validation.
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>Wallet ID cannot be null or blank</li>
 *   <li>Amount cannot be null</li>
 *   <li>Amount must be at least 0.01</li>
 *   <li>Amount is automatically rounded to 2 decimal places</li>
 * </ul>
 * 
 * @param walletId the unique identifier of the wallet, cannot be blank
 * @param amount the transaction amount, must be positive and at least 0.01
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record TransactionRequest(
    @NotBlank(message = "Wallet ID is required and cannot be blank") 
    String walletId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") 
    BigDecimal amount
) {
}