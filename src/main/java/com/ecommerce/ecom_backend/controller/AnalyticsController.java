package com.ecommerce.ecom_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.dto.ProductAnalytics;
import com.ecommerce.ecom_backend.dto.ProductConversionAnalytics;
import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.services.AnalyticsService;

/**
 * Controller for serving analytics data.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Gets the total number of events.
     * @return A response entity with the total event count.
     */
    @GetMapping("/total-events")
    public ResponseEntity<Long> getTotalEvents() {
        return ResponseEntity.ok(analyticsService.getTotalEvents());
    }

    /**
     * Gets a cached summary of event counts by type from elastic search repository.
     * @return A response entity with the event summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getEventSummary() {
        return ResponseEntity.ok(analyticsService.getEventSummary());
    }

    /**
     * Gets a summary of event counts by type from elastic search repository (non-cached). This is a development mode endpoint.
     * @return A response entity with the event summary.
     */
    @GetMapping("/summary-dev")
    public ResponseEntity<Map<String, Long>> getEventSummaryDevMode() {
        return ResponseEntity.ok(analyticsService.getEventSummaryDevMode());
    }
    
    /**
     * Gets the top viewed products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the top viewed products.
     */
    @GetMapping("/top-viewed-products")
    public ResponseEntity<List<ProductAnalytics>> getTopViewedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopViewedProducts(limit));
    }
    
    /**
     * Gets the top purchased products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the top purchased products.
     */
    @GetMapping("/top-purchased-products")
    public ResponseEntity<List<ProductAnalytics>> getTopPurchasedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopPurchasedProducts(limit));
    }
    
    /**
     * Gets the top purchased products with full product details.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the top purchased products with details.
     */
    @GetMapping("/top-purchased-products/details")
    public ResponseEntity<List<Product>> getTopPurchasedProductsWithDetails(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopPurchasedProductsWithDetails(limit));
    }
    
    /**
     * Gets the top categories.
     * 
     * @param limit the maximum number of categories to return (default: 10)
     * @return A response entity with the top categories.
     */
    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Long>> getTopCategories(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopCategories(limit));
    }
    
    /**
     * Gets the trending products based on recent events.
     * 
     * @param hours the number of hours to look back (default: 24)
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the trending products.
     */
    @GetMapping("/trending-products")
    public ResponseEntity<List<ProductAnalytics>> getTrendingProducts(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTrendingProducts(hours, limit));
    }
    
    /**
     * Gets the trending products with full product details.
     * 
     * @param hours the number of hours to look back (default: 24)
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the trending products with details.
     */
    @GetMapping("/trending-products/details")
    public ResponseEntity<List<Product>> getTrendingProductsWithDetails(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTrendingProductsWithDetails(hours, limit));
    }
    
    /**
     * Gets the trending categories based on recent events.
     * 
     * @param hours the number of hours to look back (default: 24)
     * @param limit the maximum number of categories to return (default: 10)
     * @return A response entity with the trending categories.
     */
    @GetMapping("/trending-categories")
    public ResponseEntity<Map<String, Long>> getTrendingCategories(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTrendingCategories(hours, limit));
    }
    
    /**
     * Gets the product conversion rates (view to purchase).
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return A response entity with the product conversion rates.
     */
    @GetMapping("/product-conversion-rates")
    public ResponseEntity<List<ProductConversionAnalytics>> getProductConversionRates(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getProductConversionRates(limit));
    }
}