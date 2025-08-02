package com.ecommerce.ecom_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.services.ProductSearchService;

/**
 * Controller for serving product analytics data.
 */
@RestController
@RequestMapping("/api/product-analytics")
public class ProductAnalyticsController {
    
    @Autowired
    private ProductSearchService productSearchService;

    // @GetMapping("/top-selling-products")
    // public ResponseEntity<List<ProductDocument>> getTopSellingProducts(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByPurchases(limit));
    // }

    // @GetMapping("/top-revenue-products")
    // public ResponseEntity<List<ProductDocument>> getTopRevenueProducts(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByRevenue(limit));
    // }

    // @GetMapping("/top-converting-products")
    // public ResponseEntity<List<ProductDocument>> getTopConvertingProducts(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByConversionRate(limit));
    // }

    // @GetMapping("/low-stock-products")
    // public ResponseEntity<List<ProductDocument>> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
    //     return ResponseEntity.ok(productSearchService.getProductsBelowStock(threshold));
    // }

    // @GetMapping("/top-viewed-products")
    // public ResponseEntity<List<ProductDocument>> getTopViewedProducts(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByViews(limit));
    // }

    // @GetMapping("/top-purchased-products")
    // public ResponseEntity<List<ProductDocument>> getTopPurchasedProducts(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByPurchases(limit));
    // }

    // @GetMapping("/top-categories")
    // public ResponseEntity<List<String>> getTopCategories(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopCategories(limit));
    // }

    // @GetMapping("/conversion-rate/{productId}")
    // public ResponseEntity<Double> getConversionRate(@PathVariable String productId) {
    //     return ResponseEntity.ok(productSearchService.getConversionRateForProduct(productId));
    // }

    // @GetMapping("/top-conversion-rates")
    // public ResponseEntity<List<ProductDocument>> getTopConversionRates(@RequestParam(defaultValue = "10") int limit) {
    //     return ResponseEntity.ok(productSearchService.getTopProductsByConversionRate(limit));
    // }
}