/*
 * Key concepts here:
 * Service Layer: This is where business logic lives (between controllers and repositories)
 * Dual Write: We save to MongoDB (for storage) AND send to Kafka (for real-time processing)
 * Request Context: We extract IP and user agent for analytics
 * Logging: Essential for debugging and monitoring
 */
package com.ecommerce.ecom_backend.services;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.model.UserEvent;
import com.ecommerce.ecom_backend.repo.mongo.UserEventRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service layer for handling user behavior tracking.
 * This is where our main business logic lives.
 */
@Service
public class UserBehaviorService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);
    
    @Autowired
    private UserEventRepository userEventRepository;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    /**
     * Track a user event and process it
     * @param userId The user ID (can be null for anonymous users)
     * @param sessionId The session ID
     * @param eventType Type of event (VIEW, CLICK, ADD_TO_CART, etc.)
     * @param productId The product involved
     * @param category Product category
     * @param metadata Additional event data
     * @param request HTTP request for extracting IP and user agent
     * @return The created UserEvent
     */
    public UserEvent trackUserEvent(String userId, String sessionId, String eventType, 
                                   String productId, String category, 
                                   Map<String, Object> metadata, HttpServletRequest request) {
        
        logger.info("Tracking user event: userId={}, eventType={}, productId={}", 
                   userId, eventType, productId);
        
        // Create the event
        UserEvent event = new UserEvent();
        event.setUserId(userId);
        event.setSessionId(sessionId);
        event.setEventType(eventType);
        event.setProductId(productId);
        event.setCategory(category);
        event.setMetadata(metadata);
        event.setTimestamp(Instant.now());
        
        // Extract request info
        if (request != null) {
            event.setIpAddress(getClientIpAddress(request));
            event.setUserAgent(request.getHeader("User-Agent"));
        }
        
        // Send to Kafka for real-time processing and downstream persistence
        // this event will be consumed by AnalyticsConsumerService and saved to Elasticsearch as well as MongoDB
        kafkaProducerService.sendUserEvent(event);

        return event;
    }
    
    /**
     * Below all methods are not currently exposed via REST endpoints in UserEventController
     * but can be used internally or in future endpoints.
     * They provide various ways to query user events from the MongoDB repository.
     */
    
    /**
     * Get user events for a specific user
     */
    public List<UserEvent> getUserEvents(String userId) {
        logger.info("Fetching events for user: {}", userId);
        return userEventRepository.findByUserId(userId);
    }
    
    /**
     * Get session events
     */
    public List<UserEvent> getSessionEvents(String sessionId) {
        logger.info("Fetching events for session: {}", sessionId);
        return userEventRepository.findBySessionId(sessionId);
    }
    
    /**
     * Get recent events for a user
     */
    public List<UserEvent> getRecentUserEvents(String userId, int hoursBack) {
        Instant since = Instant.now().minus(Duration.ofHours(hoursBack));
        logger.info("Fetching recent events for user: {} since: {}", userId, since);
        return userEventRepository.findRecentUserEvents(userId, since);
    }
    
    /**
     * Get events by type
     */
    public List<UserEvent> getEventsByType(String eventType) {
        logger.info("Fetching events by type: {}", eventType);
        return userEventRepository.findByEventType(eventType);
    }
    
    /**
     * Get product interaction events
     */
    public List<UserEvent> getProductEvents(String productId) {
        logger.info("Fetching events for product: {}", productId);
        return userEventRepository.findByProductId(productId);
    }
    
    /**
     * Generate a session ID if not provided
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Extract client IP address from request
     *          Header ----> Purpose
     * X-Forwarded-For ----> Most common header set by proxies. Can contain a comma-separated list of IPs. The first IP is usually the real client IP.
     * X-Real-IP ----> Some proxies set this explicitly as the client’s IP (less common than X-Forwarded-For).
     * request.getRemoteAddr() ----> Default method — gives the IP of the direct connection, usually a load balancer or reverse proxy, not the actual client.
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
    
    /**
     * Get event statistics
     */
    public Map<String, Object> getEventStats() {
        long totalEvents = userEventRepository.count();
        long viewEvents = userEventRepository.countByEventType("VIEW");
        long clickEvents = userEventRepository.countByEventType("CLICK");
        long cartEvents = userEventRepository.countByEventType("ADD_TO_CART");
        long purchaseEvents = userEventRepository.countByEventType("PURCHASE");
        
        return Map.of(
            "totalEvents", totalEvents,
            "viewEvents", viewEvents,
            "clickEvents", clickEvents,
            "cartEvents", cartEvents,
            "purchaseEvents", purchaseEvents
        );
    }
}
