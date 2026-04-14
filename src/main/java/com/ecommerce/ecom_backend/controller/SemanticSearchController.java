package com.ecommerce.ecom_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.dto.SemanticSearchRequest;
import com.ecommerce.ecom_backend.dto.SemanticSearchResponse;
import com.ecommerce.ecom_backend.services.VectorSearchService;
import com.ecommerce.ecom_backend.services.VectorSearchService.SearchFilters;
import com.ecommerce.ecom_backend.services.VectorSearchService.SearchResult;

import jakarta.validation.Valid;

/**
 * REST controller for semantic search operations
 * 
 * This controller provides AI-powered product search capabilities using
 * vector embeddings and similarity matching.
 */
@RestController
@RequestMapping("/api/search")
public class SemanticSearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SemanticSearchController.class);
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    /**
     * Perform semantic search for products
     * 
     * POST /api/search/semantic
     * 
     * @param request Search request with query and optional filters
     * @return Semantic search results with similarity scores
     */
    @PostMapping("/semantic")
    public ResponseEntity<SemanticSearchResponse> semanticSearch(@Valid @RequestBody SemanticSearchRequest request) {
        logger.info("Semantic search request: query='{}', limit={}", request.getQuery(), request.getLimit());
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Convert request filters to VectorSearchService filters
            SearchFilters filters = convertToSearchFilters(request);
            
            // Perform semantic search
            List<SearchResult> results = vectorSearchService.searchSimilarProducts(
                request.getQuery(), 
                request.getLimit(), 
                filters
            );
            
            // Filter by minimum similarity if specified
            if (request.getMinSimilarity() != null && request.getMinSimilarity() > 0.0) {
                results = results.stream()
                    .filter(result -> result.getSimilarity() >= request.getMinSimilarity())
                    .collect(Collectors.toList());
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Build response
            SemanticSearchResponse response = new SemanticSearchResponse(
                request.getQuery(), 
                results, 
                processingTime
            );
            
            // Set metadata
            response.getMetadata().setTotalEmbeddings(vectorSearchService.getEmbeddingCount());
            response.getMetadata().setFiltersApplied(hasFilters(request));
            
            logger.info("Semantic search completed: {} results in {}ms", 
                       results.size(), processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Semantic search failed for query: '{}'", request.getQuery(), e);
            return ResponseEntity.status(500).body(createErrorResponse(request.getQuery(), e.getMessage()));
        }
    }
    
    /**
     * Find products similar to a specific product
     * 
     * GET /api/search/similar/{productId}
     * 
     * @param productId ID of the product to find similar items for
     * @param limit Maximum number of results (default: 5)
     * @param category Optional category filter
     * @param minPrice Optional minimum price filter
     * @param maxPrice Optional maximum price filter
     * @return Similar products with similarity scores
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<SemanticSearchResponse> findSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        
        logger.info("Finding similar products for ID: {}, limit: {}", productId, limit);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Build filters
            SearchFilters filters = new SearchFilters();
            filters.setCategory(category);
            filters.setMinPrice(minPrice);
            filters.setMaxPrice(maxPrice);
            
            // Find similar products
            List<SearchResult> results = vectorSearchService.findSimilarProducts(
                productId, 
                limit, 
                filters
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Build response
            SemanticSearchResponse response = new SemanticSearchResponse(
                "Similar to product ID: " + productId, 
                results, 
                processingTime
            );
            
            response.getMetadata().setTotalEmbeddings(vectorSearchService.getEmbeddingCount());
            response.getMetadata().setFiltersApplied(category != null || minPrice != null || maxPrice != null);
            response.getMetadata().setSearchType("similar_products");
            
            logger.info("Similar products search completed: {} results in {}ms", 
                       results.size(), processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Similar products search failed for product ID: {}", productId, e);
            return ResponseEntity.status(500).body(
                createErrorResponse("Similar to product ID: " + productId, e.getMessage())
            );
        }
    }
    
    /**
     * Get search service status and statistics
     * 
     * GET /api/search/status
     * 
     * @return Service status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSearchStatus() {
        try {
            Map<String, Object> status = vectorSearchService.getServiceInfo();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get search status", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get search status",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Batch initialize embeddings for existing products
     * 
     * POST /api/search/initialize
     * 
     * This endpoint can be used to generate embeddings for products
     * that were created before the semantic search feature was implemented.
     * 
     * @return Initialization status
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeEmbeddings() {
        try {
            // Initialize vector table
            vectorSearchService.initializeVectorTable();
            
            Map<String, Object> status = Map.of(
                "message", "Vector database initialized successfully",
                "embeddingCount", vectorSearchService.getEmbeddingCount(),
                "status", "ready"
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Failed to initialize embeddings", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Initialization failed",
                "message", e.getMessage()
            ));
        }
    }
    
    // Helper methods
    
    /**
     * Convert SemanticSearchRequest filters to VectorSearchService.SearchFilters
     */
    private SearchFilters convertToSearchFilters(SemanticSearchRequest request) {
        SearchFilters filters = new SearchFilters();
        filters.setCategory(request.getCategory());
        filters.setMinPrice(request.getMinPrice());
        filters.setMaxPrice(request.getMaxPrice());
        filters.setMinStock(request.getMinStock());
        filters.setExcludeIds(request.getExcludeIds());
        return filters;
    }
    
    /**
     * Check if the request has any filters applied
     */
    private boolean hasFilters(SemanticSearchRequest request) {
        return request.getCategory() != null ||
               request.getMinPrice() != null ||
               request.getMaxPrice() != null ||
               request.getMinStock() != null ||
               (request.getExcludeIds() != null && !request.getExcludeIds().isEmpty()) ||
               (request.getMinSimilarity() != null && request.getMinSimilarity() > 0.0);
    }
    
    /**
     * Create error response for failed searches
     */
    private SemanticSearchResponse createErrorResponse(String query, String errorMessage) {
        SemanticSearchResponse response = new SemanticSearchResponse();
        response.setQuery(query);
        response.setTotalResults(0);
        response.setResults(List.of());
        response.setProcessingTimeMs(0);
        
        SemanticSearchResponse.SearchMetadata metadata = new SemanticSearchResponse.SearchMetadata();
        metadata.setSearchType("error");
        metadata.setTotalEmbeddings(0);
        response.setMetadata(metadata);
        
        return response;
    }
}