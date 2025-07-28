package com.ecommerce.ecom_backend.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.dto.ProductAnalytics;
import com.ecommerce.ecom_backend.dto.ProductConversionAnalytics;
import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.repo.elasticsearch.UserEventSearchRepository;
import com.ecommerce.ecom_backend.util.ElasticsearchAggregationParser;

/**
 * Service for providing analytics data from Elasticsearch.
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private UserEventSearchRepository userEventSearchRepository;
    
    @Autowired
    private ProductService productService;

    /**
     * Gets the total count of all events.
     * @return The total number of events.
     */
    public long getTotalEvents() {
        return userEventSearchRepository.count();
    }

    /**
     * Gets a summary of event counts by type.
     * @return A map with event types as keys and their counts as values.
     */
    @Cacheable("eventSummary")
    public Map<String, Long> getEventSummary() {
        return Map.of(
            "totalEvents", getTotalEvents(),
            "viewEvents", userEventSearchRepository.countByEventType("VIEW"),
            "clickEvents", userEventSearchRepository.countByEventType("CLICK"),
            "cartEvents", userEventSearchRepository.countByEventType("ADD_TO_CART"),
            "purchaseEvents", userEventSearchRepository.countByEventType("PURCHASE")
        );
    }

    /**
     * Gets a summary of event counts by type (non-cached).
     * This is a development mode endpoint.
     * @return A map with event types as keys and their counts as values.
     */
    public Map<String, Long> getEventSummaryDevMode() {
        return Map.of(
            "totalEvents", getTotalEvents(),
            "viewEvents", userEventSearchRepository.countByEventType("VIEW"),
            "clickEvents", userEventSearchRepository.countByEventType("CLICK"),
            "cartEvents", userEventSearchRepository.countByEventType("ADD_TO_CART"),
            "purchaseEvents", userEventSearchRepository.countByEventType("PURCHASE")
        );
    }
    
    /**
     * Gets the top viewed products.
     * 
     * @param limit the maximum number of products to return
     * @return list of top viewed products with their view counts
     */
    @Cacheable(value = "topViewedProducts", key = "#limit")
    public List<ProductAnalytics> getTopViewedProducts(int limit) {
        logger.info("Getting top {} viewed products", limit);
        String aggregationResult = userEventSearchRepository.findTopViewedProducts(limit);
        Map<String, Long> productCounts = ElasticsearchAggregationParser.parseTopProducts(aggregationResult);
        return ElasticsearchAggregationParser.toProductAnalyticsList(productCounts);
    }
    
    /**
     * Gets the top purchased products.
     * 
     * @param limit the maximum number of products to return
     * @return list of top purchased products with their purchase counts
     */
    @Cacheable(value = "topPurchasedProducts", key = "#limit")
    public List<ProductAnalytics> getTopPurchasedProducts(int limit) {
        logger.info("Getting top {} purchased products", limit);
        String aggregationResult = userEventSearchRepository.findTopPurchasedProducts(limit);
        Map<String, Long> productCounts = ElasticsearchAggregationParser.parseTopProducts(aggregationResult);
        return ElasticsearchAggregationParser.toProductAnalyticsList(productCounts);
    }
    
    /**
     * Gets the top categories.
     * 
     * @param limit the maximum number of categories to return
     * @return list of top categories with their event counts
     */
    @Cacheable(value = "topCategories", key = "#limit")
    public Map<String, Long> getTopCategories(int limit) {
        logger.info("Getting top {} categories", limit);
        String aggregationResult = userEventSearchRepository.findTopCategories(limit);
        return ElasticsearchAggregationParser.parseTopCategories(aggregationResult);
    }
    
    /**
     * Gets the trending products based on recent events.
     * 
     * @param hours the number of hours to look back
     * @param limit the maximum number of products to return
     * @return list of trending products with their event counts
     */
    @Cacheable(value = "trendingProducts", key = "#hours + '-' + #limit")
    public List<ProductAnalytics> getTrendingProducts(int hours, int limit) {
        logger.info("Getting trending products in the last {} hours (limit: {})", hours, limit);
        String aggregationResult = userEventSearchRepository.findTrendingProducts(hours, limit);
        Map<String, Long> productCounts = ElasticsearchAggregationParser.parseTrendingProducts(aggregationResult);
        return ElasticsearchAggregationParser.toProductAnalyticsList(productCounts);
    }
    
    /**
     * Gets the trending categories based on recent events.
     * 
     * @param hours the number of hours to look back
     * @param limit the maximum number of categories to return
     * @return list of trending categories with their event counts
     */
    @Cacheable(value = "trendingCategories", key = "#hours + '-' + #limit")
    public Map<String, Long> getTrendingCategories(int hours, int limit) {
        logger.info("Getting trending categories in the last {} hours (limit: {})", hours, limit);
        String aggregationResult = userEventSearchRepository.findTrendingCategories(hours, limit);
        return ElasticsearchAggregationParser.parseTrendingCategories(aggregationResult);
    }
    
    /**
     * Gets the product conversion rates (view to purchase).
     * 
     * @param limit the maximum number of products to return
     * @return list of products with their conversion rates
     */
    @Cacheable(value = "productConversionRates", key = "#limit")
    public List<ProductConversionAnalytics> getProductConversionRates(int limit) {
        logger.info("Getting conversion rates for top {} products", limit);
        String aggregationResult = userEventSearchRepository.findProductConversionRates(limit);
        Map<String, Double> conversionRates = ElasticsearchAggregationParser.parseProductConversionRates(aggregationResult);
        return ElasticsearchAggregationParser.toProductConversionAnalyticsList(conversionRates);
    }
    
    /**
     * Gets the trending products with full product details.
     * 
     * @param hours the number of hours to look back
     * @param limit the maximum number of products to return
     * @return list of trending products with their details
     */
    public List<Product> getTrendingProductsWithDetails(int hours, int limit) {
        List<ProductAnalytics> trendingProducts = getTrendingProducts(hours, limit);
        
        return trendingProducts.stream()
                .map(pa -> {
                    try {
                        Long productId = Long.valueOf(pa.getProductId());
                        return productService.getProductById(productId).orElse(null);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid product ID: {}", pa.getProductId());
                        return null;
                    }
                })
                .filter(product -> product != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the top purchased products with full product details.
     * 
     * @param limit the maximum number of products to return
     * @return list of top purchased products with their details
     */
    public List<Product> getTopPurchasedProductsWithDetails(int limit) {
        List<ProductAnalytics> topProducts = getTopPurchasedProducts(limit);
        
        return topProducts.stream()
                .map(pa -> {
                    try {
                        Long productId = Long.valueOf(pa.getProductId());
                        return productService.getProductById(productId).orElse(null);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid product ID: {}", pa.getProductId());
                        return null;
                    }
                })
                .filter(product -> product != null)
                .collect(Collectors.toList());
    }
}