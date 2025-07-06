package com.wallet.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void shouldAddInterceptors() {
        LoggingInterceptor interceptor = new LoggingInterceptor();
        WebConfig config = new WebConfig(interceptor);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        
        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        
        config.addInterceptors(registry);
        
        verify(registry).addInterceptor(interceptor);
        verify(registration).addPathPatterns("/api/**");
    }
}