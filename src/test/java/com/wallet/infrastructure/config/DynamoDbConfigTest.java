package com.wallet.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDbConfigTest {

    @Test
    void shouldCreateDynamoDbClient() {
        DynamoDbConfig config = new DynamoDbConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "");
        
        DynamoDbClient client = config.dynamoDbClient();
        
        assertNotNull(client);
    }
    
    @Test
    void shouldCreateDynamoDbClientWithEndpoint() {
        DynamoDbConfig config = new DynamoDbConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "http://localhost:8000");
        
        DynamoDbClient client = config.dynamoDbClient();
        
        assertNotNull(client);
    }
    
    @Test
    void shouldCreateEnhancedClient() {
        DynamoDbConfig config = new DynamoDbConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "dynamoDbEndpoint", "");
        
        DynamoDbClient client = config.dynamoDbClient();
        DynamoDbEnhancedClient enhancedClient = config.dynamoDbEnhancedClient(client);
        
        assertNotNull(enhancedClient);
    }
}