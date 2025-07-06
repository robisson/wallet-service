package com.wallet.application.dto;

import com.wallet.domain.model.Money;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object representing a wallet in API responses.
 * 
 * <p>This record contains all the essential information about a wallet
 * that is returned to API clients. It includes the current balance,
 * timestamps, and user association.
 * 
 * <p>The balance is represented as a BigDecimal with 2 decimal places
 * precision using HALF_UP rounding mode for financial accuracy.
 * 
 * <p>Timestamps are in UTC and represent:
 * <ul>
 *   <li>createdAt: When the wallet was first created</li>
 *   <li>updatedAt: When the wallet was last modified (balance change)</li>
 * </ul>
 * 
 * @param walletId the unique identifier of the wallet
 * @param userId the unique identifier of the wallet owner
 * @param balance the current wallet balance with 2 decimal places
 * @param createdAt when the wallet was created (UTC)
 * @param updatedAt when the wallet was last updated (UTC)
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
public record WalletResponse(
    String walletId,
    String userId,
    BigDecimal balance,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Converts this response DTO to a domain model object.
     * 
     * <p>This method is used internally for audit and snapshot purposes,
     * allowing the response to be converted back to a domain object
     * when needed for business operations.
     * 
     * @return a Wallet domain object with the same data
     */
    public Wallet toDomain() {
        return new Wallet(
            WalletId.of(walletId),
            userId,
            Money.of(balance),
            createdAt,
            updatedAt
        );
    }
}