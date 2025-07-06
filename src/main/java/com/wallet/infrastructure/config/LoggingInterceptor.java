package com.wallet.infrastructure.config;

import com.wallet.domain.model.AuditInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP request interceptor for logging and audit context creation.
 * 
 * <p>This interceptor captures request details and creates audit context
 * for all API requests. It provides:
 * <ul>
 *   <li>Request correlation IDs for tracing</li>
 *   <li>Client IP address extraction</li>
 *   <li>User agent capture</li>
 *   <li>Request/response timing</li>
 *   <li>Structured logging with MDC</li>
 * </ul>
 * 
 * <p>The audit information is stored in request attributes and can be
 * accessed by controllers for compliance logging.
 * 
 * @author Wallet Service Team
 * @since 1.0.0
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    public static final String AUDIT_INFO_ATTRIBUTE = "auditInfo";
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    /**
     * Pre-processes incoming HTTP requests.
     * 
     * <p>Creates audit context, generates correlation IDs, and sets up
     * structured logging context for the request.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the request handler
     * @return true to continue processing
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String remoteAddr = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Create audit context
        Map<String, String> additionalContext = new HashMap<>();
        additionalContext.put("method", method);
        additionalContext.put("uri", uri);
        
        // Create AuditInfo object and store in request for later use
        AuditInfo auditInfo = AuditInfo.builder()
            .requestId(requestId)
            .sourceIp(remoteAddr)
            .userAgent(userAgent)
            .timestamp(Instant.now())
            .additionalContext(additionalContext)
            .build();
        
        request.setAttribute(AUDIT_INFO_ATTRIBUTE, auditInfo);
        
        // Standard logging
        MDC.put("requestId", requestId);
        MDC.put("method", method);
        MDC.put("uri", uri);
        MDC.put("clientIp", remoteAddr);
        
        logger.info("Incoming request: {} {} from {}", method, uri, remoteAddr);
        
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }
    
    /**
     * Post-processes HTTP requests after completion.
     * 
     * <p>Logs request completion details including status code,
     * duration, and any exceptions that occurred.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the request handler
     * @param ex any exception that occurred during processing
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            long startTime = (Long) request.getAttribute("startTime");
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            MDC.put("status", String.valueOf(status));
            MDC.put("duration", String.valueOf(duration));
            
            if (ex != null) {
                logger.error("Request completed with error: {} {} - status: {} duration: {}ms", 
                           request.getMethod(), request.getRequestURI(), status, duration, ex);
            } else if (status >= 400) {
                logger.warn("Request completed with error status: {} {} - status: {} duration: {}ms", 
                          request.getMethod(), request.getRequestURI(), status, duration);
            } else {
                logger.info("Request completed successfully: {} {} - status: {} duration: {}ms", 
                          request.getMethod(), request.getRequestURI(), status, duration);
            }
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Extracts the real client IP address from the HTTP request.
     * 
     * <p>Handles various proxy headers to determine the original
     * client IP address, including X-Forwarded-For and X-Real-IP.
     * 
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}