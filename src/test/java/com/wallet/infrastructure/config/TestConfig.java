package com.wallet.infrastructure.config;

import com.wallet.domain.model.AuditInfo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;

/**
 * Test configuration for mocking audit-related components
 */
@TestConfiguration
public class TestConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public LoggingInterceptor testLoggingInterceptor() {
        return new TestLoggingInterceptor();
    }

    /**
     * Test implementation of LoggingInterceptor that adds a mock AuditInfo to requests
     */
    static class TestLoggingInterceptor extends LoggingInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // Create a test AuditInfo
            AuditInfo auditInfo = AuditInfo.builder()
                    .requestId("test-request-id")
                    .userId("test-user")
                    .sourceIp("127.0.0.1")
                    .userAgent("Test-Agent")
                    .timestamp(Instant.now())
                    .additionalContext(new HashMap<>())
                    .build();
            
            // Add it to the request
            request.setAttribute(AUDIT_INFO_ATTRIBUTE, auditInfo);
            
            return super.preHandle(request, response, handler);
        }
    }
}