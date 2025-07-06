package com.wallet.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Configuration class for DynamoDB client setup.
 * 
 * <p>This configuration provides DynamoDB clients for both regular
 * and enhanced operations. It supports both local development
 * (with DynamoDB Local) and production AWS environments.
 * 
 * <p>Configuration properties:
 * <ul>
 *   <li>aws.dynamodb.endpoint - Custom endpoint for local development</li>
 *   <li>aws.region - AWS region for DynamoDB operations</li>
 * </ul>
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Configuration
public class DynamoDbConfig {
    
    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    /**
     * Creates a DynamoDB client bean.
     * 
     * <p>The client is configured with the specified AWS region and
     * credentials provider. For local development, a custom endpoint
     * can be specified to connect to DynamoDB Local.
     * 
     * @return configured DynamoDB client
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create());
        
        if (!dynamoDbEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(dynamoDbEndpoint));
        }
        
        return builder.build();
    }
    
    /**
     * Creates a DynamoDB Enhanced client bean.
     * 
     * <p>The enhanced client provides higher-level operations with
     * automatic object mapping and simplified API for common operations.
     * 
     * @param dynamoDbClient the underlying DynamoDB client
     * @return configured DynamoDB Enhanced client
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
}