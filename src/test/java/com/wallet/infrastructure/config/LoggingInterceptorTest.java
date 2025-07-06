package com.wallet.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private LoggingInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
    }
    
    @Test
    void shouldHandlePreRequest() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/wallets");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertTrue(result);
        verify(request).setAttribute(eq("startTime"), any(Long.class));
    }
    
    @Test
    void shouldHandleAfterCompletion() {
        when(request.getAttribute("startTime")).thenReturn(System.currentTimeMillis() - 100);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/wallets/123");
        when(response.getStatus()).thenReturn(200);
        
        assertDoesNotThrow(() -> 
            interceptor.afterCompletion(request, response, null, null));
    }
    
    @Test
    void shouldGetClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        
        interceptor.preHandle(request, response, null);
        
        verify(request, never()).getRemoteAddr();
    }
    
    @Test
    void shouldGetClientIpFromXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        
        interceptor.preHandle(request, response, null);
        
        verify(request, never()).getRemoteAddr();
    }
}