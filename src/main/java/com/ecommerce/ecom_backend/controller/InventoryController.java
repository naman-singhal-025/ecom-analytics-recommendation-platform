package com.ecommerce.ecom_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.services.InventoryService;

/**
 * REST controller for inventory management.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;
    
    /**
     * Check if products are in stock with the required quantities.
     * 
     * @param productQuantities map of product IDs to required quantities
     * @return true if all products are in stock, false otherwise
     */
    @PostMapping("/check-stock")
    public ResponseEntity<Boolean> checkStock(@RequestBody Map<Long, Integer> productQuantities) {
        return ResponseEntity.ok(inventoryService.checkStock(productQuantities));
    }
    
    /**
     * Reserve stock for products.
     * 
     * @param productQuantities map of product IDs to quantities
     * @return map of product IDs to success/failure status
     */
    @PostMapping("/reserve")
    public ResponseEntity<Map<Long, Boolean>> reserveStock(@RequestBody Map<Long, Integer> productQuantities) {
        return ResponseEntity.ok(inventoryService.reserveStock(productQuantities));
    }
    
    /**
     * Release reserved stock for products.
     * 
     * @param productQuantities map of product IDs to quantities
     * @return success message
     */
    @PostMapping("/release")
    public ResponseEntity<String> releaseReservedStock(@RequestBody Map<Long, Integer> productQuantities) {
        inventoryService.releaseReservedStock(productQuantities);
        return ResponseEntity.ok("Reserved stock released successfully");
    }
    
    /**
     * Get products that need to be restocked.
     * 
     * @param threshold the stock threshold (default: 10)
     * @return list of products that need to be restocked
     */
    @GetMapping("/to-restock")
    public ResponseEntity<List<Product>> getProductsToRestock(@RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(inventoryService.getProductsToRestock(threshold));
    }
    
    /**
     * Calculate the total value of inventory.
     * 
     * @return the total value of all products in inventory
     */
    @GetMapping("/value")
    public ResponseEntity<Double> calculateInventoryValue() {
        return ResponseEntity.ok(inventoryService.calculateInventoryValue());
    }
    
    /**
     * Get inventory statistics.
     * 
     * @return map of inventory statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getInventoryStats() {
        return ResponseEntity.ok(inventoryService.getInventoryStats());
    }
    
    /**
     * Bulk update product stock.
     * 
     * @param stockUpdates map of product IDs to new stock quantities
     * @return map of product IDs to success/failure status
     */
    @PostMapping("/bulk-update")
    public ResponseEntity<Map<Long, Boolean>> bulkUpdateStock(@RequestBody Map<Long, Integer> stockUpdates) {
        return ResponseEntity.ok(inventoryService.bulkUpdateStock(stockUpdates));
    }
}