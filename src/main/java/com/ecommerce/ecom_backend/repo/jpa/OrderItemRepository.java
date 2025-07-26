package com.ecommerce.ecom_backend.repo.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.ecom_backend.model.OrderItem;

/**
 * Spring Data repository for {@link OrderItem} entities.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find order items by order ID.
     * 
     * @param orderId the order ID
     * @return list of items in the order
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Find order items by product ID.
     * 
     * @param productId the product ID
     * @return list of order items containing the product
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);
    
    /**
     * Count the number of times a product has been ordered.
     * 
     * @param productId the product ID
     * @return count of order items containing the product
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
    
    /**
     * Calculate the total quantity of a product that has been ordered.
     * 
     * @param productId the product ID
     * @return total quantity ordered
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId")
    Integer getTotalQuantityOrdered(@Param("productId") Long productId);
    
    /**
     * Find the most ordered products.
     * 
     * @param limit the maximum number of results to return
     * @return list of product IDs and their order counts
     */
    @Query(value = "SELECT oi.product_id, SUM(oi.quantity) as total_quantity " +
                  "FROM order_items oi " +
                  "GROUP BY oi.product_id " +
                  "ORDER BY total_quantity DESC " +
                  "LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> findMostOrderedProducts(@Param("limit") int limit);
}