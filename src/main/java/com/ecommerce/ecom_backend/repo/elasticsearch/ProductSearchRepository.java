package com.ecommerce.ecom_backend.repo.elasticsearch;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecom_backend.model.ProductDocument;

/**
 * Spring Data repository for {@link ProductDocument} documents in Elasticsearch.
 * Provides advanced search capabilities for products.
 */
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    
    /**
     * Find products by name using fuzzy matching.
     * This allows for typo tolerance in search queries.
     * 
     * @param name the product name to search for
     * @return list of products matching the name
     */
    @Query("{\"fuzzy\": {\"name\": {\"value\": \"?0\", \"fuzziness\": \"AUTO\"}}}")
    List<ProductDocument> findByNameFuzzy(String name);
    
    /**
     * Find products by category.
     * 
     * @param category the category to search for
     * @return list of products in the given category
     */
    List<ProductDocument> findByCategory(String category);
    
    /**
     * Find products with price between the given range.
     * 
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of products within the price range
     */
    List<ProductDocument> findByPriceBetween(double minPrice, double maxPrice);
    
    /**
     * Find products by name containing the given keyword.
     * 
     * @param keyword the search keyword
     * @return list of products matching the search
     */
    List<ProductDocument> findByNameContaining(String keyword);
    
    /**
     * Find products by description containing the given keyword.
     * 
     * @param keyword the search keyword
     * @return list of products matching the search
     */
    List<ProductDocument> findByDescriptionContaining(String keyword);
    
    /**
     * Full-text search across name and description fields.
     * 
     * @param text the search text
     * @return list of products matching the search
     * name^3 boosts the name field to prioritize matches in the name over description by 3 times.
     * description search is performed with lower priority without any weight or boost.
     */
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^3\", \"description\"], \"type\": \"best_fields\"}}")
    List<ProductDocument> fullTextSearch(String text);
    
    /**
     * Find products with stock quantity greater than or equal to the given value.
     * 
     * @param minStock the minimum stock quantity
     * @return list of products with sufficient stock
     */
    List<ProductDocument> findByStockQuantityGreaterThanEqual(Integer minStock);
    
    /**
     * Find products with stock quantity less than or equal to the given value.
     * 
     * @param maxStock the maximum stock quantity
     * @return list of products with low stock
     */
    List<ProductDocument> findByStockQuantityLessThanEqual(Integer maxStock);
    
    /**
     * Find products by category and price range.
     * 
     * @param category the category
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of products matching the criteria
     */
    List<ProductDocument> findByCategoryAndPriceBetween(String category, double minPrice, double maxPrice);

    /**
     * Find top products by total revenue.
     * Uses a custom aggregation query for Kibana visualization.
     */
    @Query("{\"size\":0,\"aggs\":{\"top_revenue\":{\"terms\":{\"field\":\"id\",\"size\":?0,\"order\":{\"total_revenue\":\"desc\"}},\"aggs\":{\"total_revenue\":{\"sum\":{\"field\":\"totalRevenue\"}}}}}}")
    String findTopProductsByRevenue(int limit);

    /**
     * Find top products by conversion rate.
     * Uses a custom aggregation query for Kibana visualization.
     */
    @Query("{\"size\":0,\"aggs\":{\"top_conversion\":{\"terms\":{\"field\":\"id\",\"size\":?0,\"order\":{\"conversion_rate\":\"desc\"}},\"aggs\":{\"conversion_rate\":{\"avg\":{\"field\":\"conversionRate\"}}}}}}")
    String findTopProductsByConversionRate(int limit);

    /**
     * Find products by view count ranges.
     * Uses a custom aggregation query for Kibana visualization.
     */
    @Query("{\"size\":0,\"aggs\":{\"view_ranges\":{\"range\":{\"field\":\"totalViews\",\"ranges\":[{\"to\":100},{\"from\":100,\"to\":1000},{\"from\":1000,\"to\":10000},{\"from\":10000}]}}}}")
    String findProductsByViewRanges();

    /**
     * Find purchase trends over time.
     * Uses a custom aggregation query for Kibana visualization.
     */
    @Query("{\"size\":0,\"aggs\":{\"purchase_over_time\":{\"date_histogram\":{\"field\":\"lastPurchaseDate\",\"calendar_interval\":\"1d\"},\"aggs\":{\"total_purchases\":{\"sum\":{\"field\":\"totalPurchases\"}}}}}}")
    String findPurchaseTrends(LocalDateTime startDate, LocalDateTime endDate);
}