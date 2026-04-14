package com.ecommerce.ecom_backend.dto;

import java.util.List;

import com.ecommerce.ecom_backend.services.VectorSearchService.SearchResult;

/**
 * Response DTO for semantic search operations
 */
public class SemanticSearchResponse {
    
    private String query;
    private int totalResults;
    private long processingTimeMs;
    private List<SearchResult> results;
    private SearchMetadata metadata;
    
    // Nested class for search metadata
    public static class SearchMetadata {
        private long totalEmbeddings;
        private String searchType;
        private boolean filtersApplied;
        private double avgSimilarity;
        private double maxSimilarity;
        private double minSimilarity;
        
        // Constructors
        public SearchMetadata() {}
        
        // Getters and Setters
        public long getTotalEmbeddings() {
            return totalEmbeddings;
        }
        
        public void setTotalEmbeddings(long totalEmbeddings) {
            this.totalEmbeddings = totalEmbeddings;
        }
        
        public String getSearchType() {
            return searchType;
        }
        
        public void setSearchType(String searchType) {
            this.searchType = searchType;
        }
        
        public boolean isFiltersApplied() {
            return filtersApplied;
        }
        
        public void setFiltersApplied(boolean filtersApplied) {
            this.filtersApplied = filtersApplied;
        }
        
        public double getAvgSimilarity() {
            return avgSimilarity;
        }
        
        public void setAvgSimilarity(double avgSimilarity) {
            this.avgSimilarity = avgSimilarity;
        }
        
        public double getMaxSimilarity() {
            return maxSimilarity;
        }
        
        public void setMaxSimilarity(double maxSimilarity) {
            this.maxSimilarity = maxSimilarity;
        }
        
        public double getMinSimilarity() {
            return minSimilarity;
        }
        
        public void setMinSimilarity(double minSimilarity) {
            this.minSimilarity = minSimilarity;
        }
    }
    
    // Constructors
    public SemanticSearchResponse() {}
    
    public SemanticSearchResponse(String query, List<SearchResult> results, long processingTimeMs) {
        this.query = query;
        this.results = results;
        this.totalResults = results.size();
        this.processingTimeMs = processingTimeMs;
        this.metadata = new SearchMetadata();
        
        // Calculate similarity statistics
        if (!results.isEmpty()) {
            double sum = results.stream().mapToDouble(SearchResult::getSimilarity).sum();
            this.metadata.setAvgSimilarity(sum / results.size());
            this.metadata.setMaxSimilarity(results.stream().mapToDouble(SearchResult::getSimilarity).max().orElse(0.0));
            this.metadata.setMinSimilarity(results.stream().mapToDouble(SearchResult::getSimilarity).min().orElse(0.0));
        }
        
        this.metadata.setSearchType("semantic");
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    public SearchMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(SearchMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return String.format("SemanticSearchResponse{query='%s', totalResults=%d, processingTimeMs=%d}", 
                           query, totalResults, processingTimeMs);
    }
}