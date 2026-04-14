package com.ecommerce.ecom_backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for semantic search operations
 */
public class SemanticSearchRequest {
    
    @NotBlank(message = "Query cannot be empty")
    private String query;
    
    @Positive(message = "Limit must be positive")
    private int limit = 10;
    
    // Optional filters
    private String category;
    
    @PositiveOrZero(message = "Minimum price cannot be negative")
    private Double minPrice;
    
    @PositiveOrZero(message = "Maximum price cannot be negative")
    private Double maxPrice;
    
    @PositiveOrZero(message = "Minimum stock cannot be negative")
    private Integer minStock;
    
    private List<String> excludeIds;
    
    @PositiveOrZero(message = "Minimum similarity cannot be negative")
    private Double minSimilarity = 0.0;
    
    // Constructors
    public SemanticSearchRequest() {}
    
    public SemanticSearchRequest(String query, int limit) {
        this.query = query;
        this.limit = limit;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Double getMinPrice() {
        return minPrice;
    }
    
    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }
    
    public Double getMaxPrice() {
        return maxPrice;
    }
    
    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }
    
    public Integer getMinStock() {
        return minStock;
    }
    
    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }
    
    public List<String> getExcludeIds() {
        return excludeIds;
    }
    
    public void setExcludeIds(List<String> excludeIds) {
        this.excludeIds = excludeIds;
    }
    
    public Double getMinSimilarity() {
        return minSimilarity;
    }
    
    public void setMinSimilarity(Double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }
    
    @Override
    public String toString() {
        return String.format("SemanticSearchRequest{query='%s', limit=%d, category='%s'}", 
                           query, limit, category);
    }
}