package com.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@TestPropertySource(properties = {
    "aws.dynamodb.endpoint=http://localhost:8000",
    "aws.region=us-east-1"
})
class WalletServiceApplicationTest {

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }
    
    @Test
    void shouldStartApplication() {
        // Test main method doesn't throw exception
        assertDoesNotThrow(() -> {
            WalletServiceApplication.main(new String[]{"--spring.main.web-environment=false"});
        });
    }
}