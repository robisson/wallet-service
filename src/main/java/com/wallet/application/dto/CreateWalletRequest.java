package com.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for wallet creation requests.
 * 
 * <p>This record contains the minimal information required to create a new wallet.
 * Each user can have only one wallet, so the userId must be unique across the system.
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>User ID cannot be null or blank</li>
 *   <li>User ID must contain at least one non-whitespace character</li>
 * </ul>
 * 
 * @param userId the unique identifier for the user, cannot be blank
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record CreateWalletRequest(
    @NotBlank(message = "User ID is required and cannot be blank") 
    String userId
) {
}