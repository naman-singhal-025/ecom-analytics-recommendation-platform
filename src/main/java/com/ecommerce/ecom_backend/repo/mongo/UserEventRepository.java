package com.ecommerce.ecom_backend.repo.mongo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecom_backend.model.UserEvent;

/**
 * Repository interface for UserEvent operations.
 * Spring Data MongoDB will automatically implement these methods.
 */
@Repository
public interface UserEventRepository extends MongoRepository<UserEvent, String> {
    
    /**
     * Find all events for a specific user
     */
    List<UserEvent> findByUserId(String userId);
    
    /**
     * Find all events for a specific session
     */
    List<UserEvent> findBySessionId(String sessionId);
    
    /**
     * Find events by type (e.g., all VIEW events)
     */
    List<UserEvent> findByEventType(String eventType);
    
    /**
     * Find events for a specific product
     */
    List<UserEvent> findByProductId(String productId);
    
    /**
     * Find events in a date range
     */
    List<UserEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find user events by type and product (useful for analytics)
     */
    List<UserEvent> findByEventTypeAndProductId(String eventType, String productId);
    
    /**
     * Custom query: Find recent events for a user
     * @Query annotation allows us to write custom MongoDB queries
     * Returns events by a user after a specific time.
     * gte -> greater than or equal to
     */
    @Query("{'userId': ?0, 'timestamp': {'$gte': ?1}}")
    List<UserEvent> findRecentUserEvents(String userId, LocalDateTime since);
    
    /**
     * Count events by type
     */
    long countByEventType(String eventType);
    
    /**
     * Find events by category
     */
    List<UserEvent> findByCategory(String category);

    /**
     * Count events by user and type
     */
    long countByUserIdAndEventType(String userId, String eventType);
}