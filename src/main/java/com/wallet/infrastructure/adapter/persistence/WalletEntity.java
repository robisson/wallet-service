package com.wallet.infrastructure.adapter.persistence;

import com.wallet.domain.model.Money;
import com.wallet.domain.model.Wallet;
import com.wallet.domain.model.WalletId;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB entity representing a wallet in persistent storage.
 * 
 * <p>This entity maps wallet domain objects to DynamoDB table structure.
 * It includes all necessary annotations for DynamoDB Enhanced Client
 * and provides conversion methods between domain and persistence layers.
 * 
 * <p>Table structure:
 * <ul>
 *   <li>Partition Key: walletId</li>
 *   <li>GSI: UserIdIndex on userId</li>
 *   <li>Attributes: balance, createdAt, updatedAt</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@DynamoDbBean
public class WalletEntity {
    
    private String walletId;
    private String userId;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;
    
    public WalletEntity() {}
    
    @DynamoDbPartitionKey
    public String getWalletId() { return walletId; }
    
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "UserIdIndex")
    public String getUserId() { return userId; }

    public void setUserId(String userId) { this.userId = userId; }
    
    public BigDecimal getBalance() { return balance; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Instant getCreatedAt() { return createdAt; }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Converts this entity to a DynamoDB attribute value map.
     * 
     * <p>Used for low-level DynamoDB operations such as transactions
     * where the Enhanced Client API is not sufficient.
     * 
     * @return map of attribute names to values
     */
    public Map<String, AttributeValue> toAttributeValueMap() {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("walletId", AttributeValue.builder().s(walletId).build());
        map.put("userId", AttributeValue.builder().s(userId).build());
        map.put("balance", AttributeValue.builder().n(balance.toString()).build());
        map.put("createdAt", AttributeValue.builder().s(createdAt.toString()).build());
        map.put("updatedAt", AttributeValue.builder().s(updatedAt.toString()).build());

        return map;
    }
    
    /**
     * Creates a WalletEntity from a domain Wallet object.
     * 
     * @param wallet the domain wallet object
     * @return the corresponding entity
     */
    public static WalletEntity fromDomain(Wallet wallet) {
        WalletEntity entity = new WalletEntity();

        entity.setWalletId(wallet.getId().value());
        entity.setUserId(wallet.getUserId());
        entity.setBalance(wallet.getBalance().amount());
        entity.setCreatedAt(wallet.getCreatedAt());
        entity.setUpdatedAt(wallet.getUpdatedAt());

        return entity;
    }
    
    /**
     * Converts this entity to a domain Wallet object.
     * 
     * @return the corresponding domain object
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