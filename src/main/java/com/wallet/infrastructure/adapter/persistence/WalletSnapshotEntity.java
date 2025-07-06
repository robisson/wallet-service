package com.wallet.infrastructure.adapter.persistence;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Entity representing a wallet state snapshot in DynamoDB.
 * Used for reconciliation and audit purposes.
 */
@DynamoDbBean
public class WalletSnapshotEntity {
    
    private String walletId;
    private String snapshotId;
    private BigDecimal balance;
    private Instant timestamp;
    private String userId;
    
    // Audit fields
    private String requestId;
    private String sourceIp;
    private String userAgent;
    private String operatorId;
    private Map<String, String> additionalContext;
    
    // Immutability field
    private String snapshotHash;
    
    public WalletSnapshotEntity() {}
    
    @DynamoDbPartitionKey
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    @DynamoDbSortKey
    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    // Audit getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    
    public Map<String, String> getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(Map<String, String> additionalContext) { this.additionalContext = additionalContext; }
    
    // Immutability field getter and setter
    public String getSnapshotHash() { return snapshotHash; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }
    
    @Override
    public String toString() {
        return "WalletSnapshotEntity{" +
                "walletId='" + walletId + '\'' +
                ", snapshotId='" + snapshotId + '\'' +
                ", balance=" + balance +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                '}';
    }
}