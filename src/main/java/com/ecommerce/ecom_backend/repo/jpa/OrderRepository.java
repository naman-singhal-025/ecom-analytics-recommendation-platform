package com.ecommerce.ecom_backend.repo.jpa;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.ecom_backend.model.OrderStatus;
import com.ecommerce.ecom_backend.model.Orders;

/**
 * Spring Data repository for {@link Orders} entities.
 */
public interface OrderRepository extends JpaRepository<Orders, Long> {
    
    /**
     * Find orders by user ID.
     * 
     * @param userId the user ID
     * @return list of orders for the user
     */
    List<Orders> findByUserId(String userId);
    
    /**
     * Find orders by status.
     * 
     * @param status the order status
     * @return list of orders with the given status
     */
    List<Orders> findByStatus(OrderStatus status);
    
    /**
     * Find orders created between the given dates.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of orders created in the date range
     */
    List<Orders> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find orders by user ID and status.
     * 
     * @param userId the user ID
     * @param status the order status
     * @return list of orders for the user with the given status
     */
    List<Orders> findByUserIdAndStatus(String userId, OrderStatus status);
    
    /**
     * Find recent orders for a user.
     * 
     * @param userId the user ID
     * @param since the date from which to find orders
     * @return list of recent orders for the user
     */
    List<Orders> findByUserIdAndOrderDateAfter(String userId, LocalDateTime since);
    
    /**
     * Find orders containing a specific product.
     * 
     * @param productId the product ID
     * @return list of orders containing the product
     */
    @Query("SELECT o FROM Orders o JOIN o.items i WHERE i.product.id = :productId")
    List<Orders> findOrdersContainingProduct(@Param("productId") Long productId);
    
    /**
     * Count orders by status.
     * 
     * @param status the order status
     * @return count of orders with the given status
     */
    long countByStatus(OrderStatus status);
    
    /**
     * Find orders that need to be shipped (processing status).
     * 
     * @return list of orders that need to be shipped
     */
    @Query("SELECT o FROM Orders o WHERE o.status = 'PROCESSING' ORDER BY o.orderDate ASC")
    List<Orders> findOrdersToShip();
    
    /**
     * Find orders that have been shipped but not delivered.
     * 
     * @return list of orders in transit
     */
    @Query("SELECT o FROM Orders o WHERE o.status = 'SHIPPED' ORDER BY o.shippedAt ASC")
    List<Orders> findOrdersInTransit();
}