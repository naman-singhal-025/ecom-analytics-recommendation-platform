package com.ecommerce.ecom_backend.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Represents a user behavior event in our e-commerce system.
 * This could be a page view, product click, add to cart, etc.
 * This entity is mapped to both MongoDB and Elasticsearch.
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "user_events")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "user_events")
public class UserEvent {
    
    // @Id annotated field will be mapped as _id in Elastic Search and MongoDB irrespective of the field name.
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Keyword)
    private String eventType;  // VIEW, CLICK, ADD_TO_CART, PURCHASE, etc.

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // Additional data that might vary by event type
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
    
    // IP address for analytics
    @Field(type = FieldType.Ip)
    private String ipAddress;
    
    // User agent for device/browser info
    @Field(type = FieldType.Text)
    private String userAgent;
    
    // Constructors
    public UserEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public UserEvent(String userId, String sessionId, String eventType, 
                    String productId, String category) {
        this();
        this.userId = userId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.productId = productId;
        this.category = category;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    @Override
    public String toString() {
        return "UserEvent{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", productId='" + productId + '\'' +
                ", category='" + category + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
