package com.ecommerce.ecom_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.services.AnalyticsConsumerService;

/**
 * Controller for serving real-time analytics data from Kafka consumer.
 */
@RestController
@RequestMapping("/api/real-time-analytics")
public class RealTimeAnalyticsController {

    @Autowired
    private AnalyticsConsumerService analyticsConsumerService;
    
    /**
     * Get real-time event type counts.
     * 
     * @return map of event types to counts
     */
    @GetMapping("/event-counts")
    public ResponseEntity<Map<String, Long>> getEventTypeCounts() {
        return ResponseEntity.ok(analyticsConsumerService.getEventTypeCounts());
    }
    
    /**
     * Get real-time top viewed products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return map of product IDs to view counts
     */
    @GetMapping("/top-viewed-products")
    public ResponseEntity<Map<String, Long>> getTopViewedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsConsumerService.getTopViewedProducts(limit));
    }
    
    /**
     * Get real-time top purchased products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return map of product IDs to purchase counts
     */
    @GetMapping("/top-purchased-products")
    public ResponseEntity<Map<String, Long>> getTopPurchasedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsConsumerService.getTopPurchasedProducts(limit));
    }
    
    /**
     * Get real-time top categories.
     * 
     * @param limit the maximum number of categories to return (default: 10)
     * @return map of categories to counts
     */
    @GetMapping("/top-categories")
    public ResponseEntity<Map<String, Long>> getTopCategories(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsConsumerService.getTopCategories(limit));
    }
    
    /**
     * Get real-time conversion rate for a product.
     * 
     * @param productId the product ID
     * @return the conversion rate
     */
    @GetMapping("/conversion-rate/{productId}")
    public ResponseEntity<Double> getProductConversionRate(@PathVariable String productId) {
        return ResponseEntity.ok(analyticsConsumerService.getProductConversionRate(productId));
    }
    
    /**
     * Get real-time conversion rates for top viewed products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return map of product IDs to conversion rates
     */
    @GetMapping("/top-conversion-rates")
    public ResponseEntity<Map<String, Double>> getTopProductConversionRates(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsConsumerService.getTopProductConversionRates(limit));
    }
}