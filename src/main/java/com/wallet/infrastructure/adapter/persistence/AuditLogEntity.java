package com.wallet.infrastructure.adapter.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing an immutable audit log entry in DynamoDB.
 */
@DynamoDbBean
public class AuditLogEntity {
    
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
    
    public AuditLogEntity() {}
    
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
    
    @Override
    public String toString() {
        return "AuditLogEntity{" +
                "walletId='" + walletId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", relatedWalletId='" + relatedWalletId + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}