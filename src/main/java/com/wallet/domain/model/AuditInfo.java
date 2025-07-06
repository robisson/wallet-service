package com.wallet.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing audit information for transactions.
 * This is immutable and captures who, when, and where information.
 */
public class AuditInfo {
    private final String userId;
    private final String sourceIp;
    private final String userAgent;
    private final String requestId;
    private final Instant timestamp;
    private final Map<String, String> additionalContext;

    private AuditInfo(Builder builder) {
        this.userId = builder.userId;
        this.sourceIp = builder.sourceIp;
        this.userAgent = builder.userAgent;
        this.requestId = Objects.requireNonNull(builder.requestId, "requestId is required");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp is required");
        this.additionalContext = builder.additionalContext;
    }

    public String getUserId() {
        return userId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getAdditionalContext() {
        return additionalContext;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String sourceIp;
        private String userAgent;
        private String requestId;
        private Instant timestamp = Instant.now();
        private Map<String, String> additionalContext;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder additionalContext(Map<String, String> additionalContext) {
            this.additionalContext = additionalContext;
            return this;
        }

        public AuditInfo build() {
            return new AuditInfo(this);
        }
    }
}