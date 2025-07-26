package com.ecommerce.ecom_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerce.ecom_backend.model.OrderStatus;
import com.ecommerce.ecom_backend.model.Orders;
import com.ecommerce.ecom_backend.services.OrderService;

import jakarta.validation.Valid;

/**
 * REST controller for managing orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Create a new order.
     * 
     * @param order the order to create
     * @return the created order
     */
    @PostMapping
    public ResponseEntity<Orders> createOrder(@Valid @RequestBody Orders order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }
    
    /**
     * Get an order by ID.
     * 
     * @param orderId the order ID
     * @return the order, if found
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderById(@PathVariable Long orderId) {
        // .map is used to transform the Optional<Orders> returned by orderService.getOrderById into a ResponseEntity.
        // If the order is not found, it throws a ResponseStatusException with a 404 NOT FOUND status.
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok) // Shorthand for ResponseEntity.ok(order). If order is found, return it with 200 OK status. 
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found with ID: " + orderId));
    }

    /**
     * Get orders by user ID.
     * 
     * @param userId the user ID
     * @return list of orders for the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Orders>> getOrdersByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
    
    /**
     * Get orders by status.
     * 
     * @param status the order status
     * @return list of orders with the given status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Orders>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }
    
    /**
     * Add an item to an order.
     * 
     * @param orderId the order ID
     * @param requestBody the request body containing productId and quantity
     * @return the updated order
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<Orders> addItemToOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> requestBody) {
        
        Long productId = Long.valueOf(requestBody.get("productId").toString());
        Integer quantity = Integer.valueOf(requestBody.get("quantity").toString());
        
        try {
            Orders updatedOrder = orderService.addItemToOrder(orderId, productId, quantity);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * Update order status.
     * 
     * @param orderId the order ID
     * @param status the new status
     * @return the updated order
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<Orders> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> requestBody) {
        
        try {
            OrderStatus status = OrderStatus.valueOf(requestBody.get("status"));
            Orders updatedOrder = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    
    /**
     * Cancel an order.
     * 
     * @param orderId the order ID
     * @return the cancelled order
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Orders> cancelOrder(@PathVariable Long orderId) {
        try {
            Orders cancelledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * Get recent orders.
     * 
     * @param days number of days to look back (default: 7)
     * @return list of recent orders
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Orders>> getRecentOrders(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(orderService.getRecentOrders(days));
    }
    
    /**
     * Get orders containing a specific product.
     * 
     * @param productId the product ID
     * @return list of orders containing the product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Orders>> getOrdersContainingProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(orderService.getOrdersContainingProduct(productId));
    }
    
    /**
     * Get orders that need to be shipped.
     * 
     * @return list of orders to ship
     */
    @GetMapping("/to-ship")
    public ResponseEntity<List<Orders>> getOrdersToShip() {
        return ResponseEntity.ok(orderService.getOrdersToShip());
    }
    
    /**
     * Get orders that are in transit.
     * 
     * @return list of orders in transit
     */
    @GetMapping("/in-transit")
    public ResponseEntity<List<Orders>> getOrdersInTransit() {
        return ResponseEntity.ok(orderService.getOrdersInTransit());
    }
}