package com.wallet.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Data Transfer Object for money transfer requests between wallets.
 * 
 * <p>This record represents a transfer operation from one wallet to another.
 * The transfer is atomic - either both wallets are updated successfully,
 * or the entire operation fails without any changes.
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>Source wallet ID cannot be null or blank</li>
 *   <li>Destination wallet ID cannot be null or blank</li>
 *   <li>Amount cannot be null</li>
 *   <li>Amount must be at least 0.01</li>
 *   <li>Source and destination wallets must be different</li>
 * </ul>
 * 
 * @param fromWalletId the source wallet ID, cannot be blank
 * @param toWalletId the destination wallet ID, cannot be blank
 * @param amount the transfer amount, must be positive and at least 0.01
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record TransferRequest(
    @NotBlank(message = "Source wallet ID is required and cannot be blank") 
    String fromWalletId,

    @NotBlank(message = "Destination wallet ID is required and cannot be blank") 
    String toWalletId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") 
    BigDecimal amount
) {
}