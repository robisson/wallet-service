package com.wallet.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for the wallet service.
 * 
 * <p>This configuration class sets up web-related components including
 * interceptors for logging and audit trail creation.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final LoggingInterceptor loggingInterceptor;
    
    /**
     * Constructs WebConfig with required interceptors.
     * 
     * @param loggingInterceptor the logging interceptor for audit context
     */
    public WebConfig(LoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }
    
    /**
     * Registers HTTP interceptors for API endpoints.
     * 
     * <p>The logging interceptor is applied to all API paths to ensure
     * consistent audit context creation and request logging.
     * 
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**");
    }
}