package com.ecommerce.ecom_backend.model;

/**
 * Enum representing the possible states of an order in the system.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet processed.
     */
    PENDING,
    
    /**
     * Payment has been received and order is being processed.
     */
    PROCESSING,
    
    /**
     * Order has been shipped to the customer.
     */
    SHIPPED,
    
    /**
     * Order has been delivered to the customer.
     */
    DELIVERED,
    
    /**
     * Order has been cancelled.
     */
    CANCELLED,
    
    /**
     * Order has been returned by the customer.
     */
    RETURNED,
    
    /**
     * Order is on hold (e.g., due to payment issues, inventory problems).
     */
    ON_HOLD;
    
    /**
     * Checks if the order is in a final state (completed or cancelled).
     * 
     * @return true if the order is in a final state
     */
    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }
    
    /**
     * Checks if the order is in an active state (not cancelled or returned).
     * 
     * @return true if the order is active
     */
    public boolean isActive() {
        return this != CANCELLED && this != RETURNED;
    }
}