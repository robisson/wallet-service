package com.wallet.infrastructure.adapter.persistence;

import com.wallet.domain.model.AuditInfo;
import com.wallet.domain.model.Money;
import com.wallet.domain.model.Transaction;
import com.wallet.domain.model.WalletId;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB entity representing a transaction in persistent storage.
 * 
 * <p>This entity maps transaction domain objects to DynamoDB table structure.
 * It includes audit information and immutability features through hash chains
 * for regulatory compliance and tamper detection.
 * 
 * <p>Table structure:
 * <ul>
 *   <li>Partition Key: walletId</li>
 *   <li>Sort Key: transactionId</li>
 *   <li>Attributes: type, amount, timestamp, audit fields, hash fields</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@DynamoDbBean
public class TransactionEntity {
    
    private String walletId;
    private String transactionId;
    private String type;
    private BigDecimal amount;
    private String relatedWalletId;
    private Instant timestamp;
    
    // Audit fields
    private String userId;
    private String sourceIp;
    private String userAgent;
    private String requestId;
    private Instant auditTimestamp;
    private Map<String, String> additionalContext;
    
    // Immutability fields
    private String transactionHash;
    private String previousTransactionHash;
    
    public TransactionEntity() {}
    
    @DynamoDbPartitionKey
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    @DynamoDbSortKey
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getRelatedWalletId() { return relatedWalletId; }
    public void setRelatedWalletId(String relatedWalletId) { this.relatedWalletId = relatedWalletId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    // Audit getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public Instant getAuditTimestamp() { return auditTimestamp; }
    public void setAuditTimestamp(Instant auditTimestamp) { this.auditTimestamp = auditTimestamp; }
    
    public Map<String, String> getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(Map<String, String> additionalContext) { this.additionalContext = additionalContext; }
    
    // Immutability fields getters and setters
    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    
    public String getPreviousTransactionHash() { return previousTransactionHash; }
    public void setPreviousTransactionHash(String previousTransactionHash) { this.previousTransactionHash = previousTransactionHash; }
    
    /**
     * Creates a TransactionEntity from a domain Transaction object.
     * 
     * <p>Includes audit information mapping and hash generation
     * for immutability verification.
     * 
     * @param transaction the domain transaction object
     * @return the corresponding entity with audit and hash information
     */
    public static TransactionEntity fromDomain(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setWalletId(transaction.getWalletId().value());
        entity.setTransactionId(transaction.getId());
        entity.setType(transaction.getType().name());
        entity.setAmount(transaction.getAmount().amount());
        entity.setRelatedWalletId(transaction.getRelatedWalletId() != null ? 
            transaction.getRelatedWalletId().value() : null);
        entity.setTimestamp(transaction.getTimestamp());
        
        // Set audit information if available
        if (transaction.getAuditInfo() != null) {
            AuditInfo auditInfo = transaction.getAuditInfo();
            entity.setUserId(auditInfo.getUserId());
            entity.setSourceIp(auditInfo.getSourceIp());
            entity.setUserAgent(auditInfo.getUserAgent());
            entity.setRequestId(auditInfo.getRequestId());
            entity.setAuditTimestamp(auditInfo.getTimestamp());
            entity.setAdditionalContext(auditInfo.getAdditionalContext());
        }
        
        // Generate hash for immutability
        entity.setTransactionHash(generateTransactionHash(entity));
        
        return entity;
    }
    
    /**
     * Converts this entity to a domain Transaction object.
     * 
     * <p>Reconstructs audit information if available and creates
     * the appropriate domain object.
     * 
     * @return the corresponding domain object
     */
    public Transaction toDomain() {
        // Create AuditInfo if audit fields are present
        AuditInfo auditInfo = null;
        if (requestId != null) {
            auditInfo = AuditInfo.builder()
                .userId(userId)
                .sourceIp(sourceIp)
                .userAgent(userAgent)
                .requestId(requestId)
                .timestamp(auditTimestamp != null ? auditTimestamp : timestamp)
                .additionalContext(additionalContext)
                .build();
        }
        
        return new Transaction(
            WalletId.of(walletId),
            Transaction.Type.valueOf(type),
            Money.of(amount),
            relatedWalletId != null ? WalletId.of(relatedWalletId) : null,
            auditInfo
        );
    }
    
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
        map.put("transactionId", AttributeValue.builder().s(transactionId).build());
        map.put("type", AttributeValue.builder().s(type).build());
        map.put("amount", AttributeValue.builder().n(amount.toString()).build());
        map.put("timestamp", AttributeValue.builder().s(timestamp.toString()).build());
        
        if (relatedWalletId != null) {
            map.put("relatedWalletId", AttributeValue.builder().s(relatedWalletId).build());
        }
        if (userId != null) {
            map.put("userId", AttributeValue.builder().s(userId).build());
        }
        if (sourceIp != null) {
            map.put("sourceIp", AttributeValue.builder().s(sourceIp).build());
        }
        if (userAgent != null) {
            map.put("userAgent", AttributeValue.builder().s(userAgent).build());
        }
        if (requestId != null) {
            map.put("requestId", AttributeValue.builder().s(requestId).build());
        }
        if (auditTimestamp != null) {
            map.put("auditTimestamp", AttributeValue.builder().s(auditTimestamp.toString()).build());
        }
        if (transactionHash != null) {
            map.put("transactionHash", AttributeValue.builder().s(transactionHash).build());
        }
        if (previousTransactionHash != null) {
            map.put("previousTransactionHash", AttributeValue.builder().s(previousTransactionHash).build());
        }
        
        return map;
    }
    
    /**
     * Generates a hash for the transaction to ensure immutability.
     * 
     * <p>In a production environment, this would use a secure hashing algorithm
     * like SHA-256 with proper salting and potentially include digital signatures
     * for enhanced security and non-repudiation.
     * 
     * @param entity the transaction entity to hash
     * @return the generated hash string
     */
    private static String generateTransactionHash(TransactionEntity entity) {
        // Simple hash generation for demonstration
        // In production, use a secure hashing algorithm like SHA-256 with proper salting
        String dataToHash = entity.getWalletId() + 
                           entity.getTransactionId() + 
                           entity.getType() + 
                           entity.getAmount() + 
                           entity.getTimestamp() + 
                           (entity.getRelatedWalletId() != null ? entity.getRelatedWalletId() : "");
        
        return Integer.toHexString(dataToHash.hashCode());
    }
}