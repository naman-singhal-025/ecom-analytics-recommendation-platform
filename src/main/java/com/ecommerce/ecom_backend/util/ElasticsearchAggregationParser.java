package com.ecommerce.ecom_backend.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.ecom_backend.dto.ProductAnalytics;
import com.ecommerce.ecom_backend.dto.ProductConversionAnalytics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for parsing Elasticsearch aggregation results.
 * 
 * Follows the Utility Class Pattern:
 * - All methods are static and stateless.
 * - No instance variables or object state are used.
 * - Provides reusable, self-contained parsing logic for Elasticsearch responses.
 * 
 * Benefits:
 * - Enables direct access without instantiation.
 * - Clear separation of concerns.
 * - Thread-safe due to lack of shared mutable state.
 */
/**
 * Purpose of the Class:
 * 
 * ElasticsearchAggregationParser is a utility class that:
 * - Parses raw Elasticsearch JSON aggregation responses.
 * - Extracts structured data such as:
 *   - Top products
 *   - Trending categories
 *   - Conversion rates
 * - Converts raw parsed data into custom DTOs): (@link https://docs.google.com/document/d/1jg4k7_DQ6Inb8NfwwPDFjPud6TIjuXWHNC8V4WAgFAs/edit?tab=t.e4qx3xmqjqcq)
 *   - ProductAnalytics (@see ProductAnalytics)
 *   - ProductConversionAnalytics (@see ProductConversionAnalytics)
 * 
 * This helps separate parsing logic from service/business logic,
 * promoting cleaner and more maintainable code.
 */

public class ElasticsearchAggregationParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchAggregationParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Parse top products aggregation result.
     * 
     * @param aggregationJson the JSON string from Elasticsearch
     * @return map of product IDs to counts
     * Example JSON structure:
     * {
     *   "aggregations": {
     *     "top_products": { // top_products is the name of the aggregation
     *       "buckets": [
     *         {
     *           "key": "product123",
     *           "doc_count": 100,  // doc_count is automatically added by ElasticSearch for each bucket
     *           "event_count": {
     *             "value": 100
     *           }
     *         },
     *         {
     *           "key": "product456",
     *           "doc_count": 80,
     *           "event_count": {
     *             "value": 80
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     */
    public static Map<String, Long> parseTopProducts(String aggregationJson) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(aggregationJson);
            JsonNode bucketsNode = rootNode.path("aggregations").path("top_products").path("buckets");
            
            if (bucketsNode.isArray()) {
                for (JsonNode bucket : bucketsNode) {
                    String productId = bucket.path("key").asText();
                    long count = bucket.path("doc_count").asLong();  // or use bucket.path("event_count").path("value").asLong() if needed
                    result.put(productId, count);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing top products aggregation: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse trending products aggregation result.
     * 
     * @param aggregationJson the JSON string from Elasticsearch
     * @return map of product IDs to counts
     */
    public static Map<String, Long> parseTrendingProducts(String aggregationJson) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(aggregationJson);
            JsonNode bucketsNode = rootNode.path("aggregations").path("trending_products").path("buckets");
            
            if (bucketsNode.isArray()) {
                for (JsonNode bucket : bucketsNode) {
                    String productId = bucket.path("key").asText();
                    long count = bucket.path("doc_count").asLong();
                    result.put(productId, count);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing trending products aggregation: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse top categories aggregation result.
     * 
     * @param aggregationJson the JSON string from Elasticsearch
     * @return map of categories to counts
     */
    public static Map<String, Long> parseTopCategories(String aggregationJson) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(aggregationJson);
            JsonNode bucketsNode = rootNode.path("aggregations").path("top_categories").path("buckets");
            
            if (bucketsNode.isArray()) {
                for (JsonNode bucket : bucketsNode) {
                    String category = bucket.path("key").asText();
                    long count = bucket.path("doc_count").asLong();
                    result.put(category, count);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing top categories aggregation: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse trending categories aggregation result.
     * 
     * @param aggregationJson the JSON string from Elasticsearch
     * @return map of categories to counts
     */
    public static Map<String, Long> parseTrendingCategories(String aggregationJson) {
        Map<String, Long> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(aggregationJson);
            JsonNode bucketsNode = rootNode.path("aggregations").path("trending_categories").path("buckets");
            
            if (bucketsNode.isArray()) {
                for (JsonNode bucket : bucketsNode) {
                    String category = bucket.path("key").asText();
                    long count = bucket.path("doc_count").asLong();
                    result.put(category, count);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing trending categories aggregation: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse product conversion rates aggregation result.
     * 
     * @param aggregationJson the JSON string from Elasticsearch
     * @return map of product IDs to conversion rates
     */
    public static Map<String, Double> parseProductConversionRates(String aggregationJson) {
        Map<String, Double> result = new HashMap<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(aggregationJson);
            JsonNode bucketsNode = rootNode.path("aggregations").path("products").path("buckets");
            
            if (bucketsNode.isArray()) {
                for (JsonNode bucket : bucketsNode) {
                    String productId = bucket.path("key").asText();
                    long views = bucket.path("views").path("doc_count").asLong();
                    long purchases = bucket.path("purchases").path("doc_count").asLong();
                    
                    double conversionRate = views > 0 ? (double) purchases / views : 0;
                    result.put(productId, conversionRate);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error parsing product conversion rates aggregation: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Convert a map of product IDs to counts to a list of product analytics objects.
     * 
     * @param productCounts map of product IDs to counts
     * @return list of product analytics objects
     */
    public static List<ProductAnalytics> toProductAnalyticsList(Map<String, Long> productCounts) {
        List<ProductAnalytics> result = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : productCounts.entrySet()) {
            result.add(new ProductAnalytics(entry.getKey(), entry.getValue()));
        }
        
        return result;
    }
    
    /**
     * Convert a map of product IDs to conversion rates to a list of product conversion analytics objects.
     * 
     * @param conversionRates map of product IDs to conversion rates
     * @return list of product conversion analytics objects
     */
    public static List<ProductConversionAnalytics> toProductConversionAnalyticsList(Map<String, Double> conversionRates) {
        List<ProductConversionAnalytics> result = new ArrayList<>();
        
        for (Map.Entry<String, Double> entry : conversionRates.entrySet()) {
            result.add(new ProductConversionAnalytics(entry.getKey(), entry.getValue()));
        }
        
        return result;
    }
}