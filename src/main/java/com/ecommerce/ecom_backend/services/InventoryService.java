package com.ecommerce.ecom_backend.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.repo.jpa.OrderItemRepository;
import com.ecommerce.ecom_backend.repo.jpa.ProductRepository;

/**
 * Service for managing inventory operations.
 */
@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    /**
     * Check if all products in the given map are in stock with the required quantities.
     * 
     * @param productQuantities map of product IDs to required quantities
     * @return true if all products are in stock, false otherwise
     */
    public boolean checkStock(Map<Long, Integer> productQuantities) {
        logger.info("Checking stock for {} products", productQuantities.size());
        
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requiredQuantity = entry.getValue();
            
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isEmpty() || productOpt.get().getStockQuantity() < requiredQuantity) {
                logger.warn("Product {} is not in stock (required: {})", productId, requiredQuantity);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Reserve stock for products in the given map.
     * This reduces the stock quantity but doesn't finalize the transaction.
     * 
     * @param productQuantities map of product IDs to quantities
     * @return map of product IDs to success/failure status
     */
    @Transactional
    public Map<Long, Boolean> reserveStock(Map<Long, Integer> productQuantities) {
        logger.info("Reserving stock for {} products", productQuantities.size());
        
        Map<Long, Boolean> results = new HashMap<>();
        
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                
                if (product.getStockQuantity() >= quantity) {
                    product.setStockQuantity(product.getStockQuantity() - quantity);
                    product.setUpdatedAt(LocalDateTime.now());
                    productRepository.save(product);
                    results.put(productId, true);
                    logger.info("Reserved {} units of product {}", quantity, productId);
                } else {
                    results.put(productId, false);
                    logger.warn("Failed to reserve {} units of product {} (insufficient stock)", quantity, productId);
                }
            } else {
                results.put(productId, false);
                logger.warn("Failed to reserve stock for product {} (not found)", productId);
            }
        }
        
        return results;
    }
    
    /**
     * Release reserved stock for products in the given map.
     * This is used when an order is cancelled or fails.
     * 
     * @param productQuantities map of product IDs to quantities
     */
    @Transactional
    public void releaseReservedStock(Map<Long, Integer> productQuantities) {
        logger.info("Releasing reserved stock for {} products", productQuantities.size());
        
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStockQuantity(product.getStockQuantity() + quantity);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                logger.info("Released {} units of product {}", quantity, productId);
            } else {
                logger.warn("Failed to release stock for product {} (not found)", productId);
            }
        }
    }
    
    /**
     * Get products that need to be restocked (below the specified threshold).
     * 
     * @param threshold the stock threshold
     * @return list of products that need to be restocked
     */
    public List<Product> getProductsToRestock(int threshold) {
        logger.info("Fetching products with stock below {}", threshold);
        return productRepository.findByStockQuantityLessThanEqual(threshold);
    }
    
    /**
     * Calculate the total value of inventory.
     * 
     * @return the total value of all products in inventory
     */
    public double calculateInventoryValue() {
        logger.info("Calculating total inventory value");
        
        return productRepository.findAll().stream()
                .mapToDouble(product -> 
                    product.getPrice().doubleValue() * product.getStockQuantity())
                .sum();
    }
    
    /**
     * Get inventory statistics.
     * 
     * @return map of inventory statistics
     */
    public Map<String, Object> getInventoryStats() {
        logger.info("Generating inventory statistics");
        
        List<Product> allProducts = productRepository.findAll();
        
        long totalProducts = allProducts.size();
        long outOfStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() == 0)
                .count();
        long lowStockProducts = allProducts.stream()
                .filter(p -> p.getStockQuantity() > 0 && p.getStockQuantity() <= 10)
                .count();
        double totalValue = allProducts.stream()
                .mapToDouble(p -> p.getPrice().doubleValue() * p.getStockQuantity())
                .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("outOfStockProducts", outOfStockProducts);
        stats.put("lowStockProducts", lowStockProducts);
        stats.put("totalValue", totalValue);
        
        return stats;
    }
    
    /**
     * Bulk update product stock.
     * 
     * @param stockUpdates map of product IDs to new stock quantities
     * @return map of product IDs to success/failure status
     */
    @Transactional
    public Map<Long, Boolean> bulkUpdateStock(Map<Long, Integer> stockUpdates) {
        logger.info("Performing bulk stock update for {} products", stockUpdates.size());
        
        Map<Long, Boolean> results = new HashMap<>();
        
        for (Map.Entry<Long, Integer> entry : stockUpdates.entrySet()) {
            Long productId = entry.getKey();
            Integer newQuantity = entry.getValue();
            
            if (newQuantity < 0) {
                results.put(productId, false);
                logger.warn("Failed to update stock for product {} (negative quantity)", productId);
                continue;
            }
            
            Optional<Product> productOpt = productRepository.findById(productId);
            
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStockQuantity(newQuantity);
                product.setUpdatedAt(LocalDateTime.now());
                productRepository.save(product);
                results.put(productId, true);
                logger.info("Updated stock for product {} to {}", productId, newQuantity);
            } else {
                results.put(productId, false);
                logger.warn("Failed to update stock for product {} (not found)", productId);
            }
        }
        
        return results;
    }
}