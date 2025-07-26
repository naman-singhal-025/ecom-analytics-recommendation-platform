package com.ecommerce.ecom_backend.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.ecom_backend.model.OrderItem;
import com.ecommerce.ecom_backend.model.OrderStatus;
import com.ecommerce.ecom_backend.model.Orders;
import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.repo.jpa.OrderItemRepository;
import com.ecommerce.ecom_backend.repo.jpa.OrderRepository;
import com.ecommerce.ecom_backend.repo.jpa.ProductRepository;

/**
 * Service for managing orders.
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Create a new order.
     * 
     * @param order the order to create
     * @return the created order
     */
    @Transactional
    public Orders createOrder(Orders order) {
        logger.info("Creating new order for user: {}", order.getUserId());
        
        // Set default values if not provided
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING);
        }
        
        // Save the order first to get an ID
        Orders savedOrder = orderRepository.save(order);
        logger.info("Created order with ID: {}", savedOrder.getId());
        
        return savedOrder;
    }
    
    /**
     * Add an item to an order.
     * 
     * @param orderId the order ID
     * @param productId the product ID
     * @param quantity the quantity
     * @return the updated order
     */
    @Transactional
    public Orders addItemToOrder(Long orderId, Long productId, Integer quantity) {
        logger.info("Adding product {} to order {} with quantity {}", productId, orderId, quantity);
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        
        // Check if product is in stock
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available for product: " + product.getName());
        }
        
        // Create new order item
        OrderItem orderItem = new OrderItem(order, product, quantity, product.getPrice());
        
        // Add item to order
        order.addItem(orderItem);
        
        // Update product stock
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        
        // Save the updated order
        return orderRepository.save(order);
    }
    
    /**
     * Get an order by ID.
     * 
     * @param orderId the order ID
     * @return the order, if found
     */
    public Optional<Orders> getOrderById(Long orderId) {
        logger.info("Fetching order with ID: {}", orderId);
        return orderRepository.findById(orderId);
    }
    
    /**
     * Get orders by user ID.
     * 
     * @param userId the user ID
     * @return list of orders for the user
     */
    public List<Orders> getOrdersByUserId(String userId) {
        logger.info("Fetching orders for user: {}", userId);
        return orderRepository.findByUserId(userId);
    }
    
    /**
     * Get orders by status.
     * 
     * @param status the order status
     * @return list of orders with the given status
     */
    public List<Orders> getOrdersByStatus(OrderStatus status) {
        logger.info("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }
    
    /**
     * Update order status.
     * 
     * @param orderId the order ID
     * @param status the new status
     * @return the updated order
     */
    @Transactional
    public Orders updateOrderStatus(Long orderId, OrderStatus status) {
        logger.info("Updating order {} status to {}", orderId, status);
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        order.setStatus(status);
        
        // Update timestamps based on status
        switch (status) {
            case SHIPPED:
                order.setShippedAt(LocalDateTime.now());
                break;
            case DELIVERED:
                order.setDeliveredAt(LocalDateTime.now());
                break;
            default:
                break;
        }
        
        return orderRepository.save(order);
    }
    
    /**
     * Cancel an order.
     * 
     * @param orderId the order ID
     * @return the cancelled order
     */
    @Transactional
    public Orders cancelOrder(Long orderId) {
        logger.info("Cancelling order with ID: {}", orderId);
        
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // Only allow cancellation of pending or processing orders
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Return items to inventory
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
    
    /**
     * Get recent orders.
     * 
     * @param days number of days to look back
     * @return list of recent orders
     */
    public List<Orders> getRecentOrders(int days) {
        logger.info("Fetching orders from the last {} days", days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return orderRepository.findByOrderDateBetween(since, LocalDateTime.now());
    }
    
    /**
     * Get orders containing a specific product.
     * 
     * @param productId the product ID
     * @return list of orders containing the product
     */
    public List<Orders> getOrdersContainingProduct(Long productId) {
        logger.info("Fetching orders containing product: {}", productId);
        return orderRepository.findOrdersContainingProduct(productId);
    }
    
    /**
     * Get orders that need to be shipped.
     * 
     * @return list of orders to ship
     */
    public List<Orders> getOrdersToShip() {
        logger.info("Fetching orders that need to be shipped");
        return orderRepository.findOrdersToShip();
    }
    
    /**
     * Get orders that are in transit.
     * 
     * @return list of orders in transit
     */
    public List<Orders> getOrdersInTransit() {
        logger.info("Fetching orders in transit");
        return orderRepository.findOrdersInTransit();
    }
}