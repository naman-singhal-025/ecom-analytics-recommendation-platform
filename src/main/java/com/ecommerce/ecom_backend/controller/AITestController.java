package com.ecommerce.ecom_backend.controller;

import com.ecommerce.ecom_backend.services.OllamaService;
import com.ecommerce.ecom_backend.services.VectorSearchService;
import com.ecommerce.ecom_backend.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test controller for AI services
 * Use this to test Ollama integration before building the full RAG system
 */
@RestController
@RequestMapping("/api/ai/test")
public class AITestController {
    
    @Autowired
    private OllamaService ollamaService;
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    /**
     * Test endpoint to check if Ollama is working
     * GET /api/ai/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> serviceInfo = ollamaService.getServiceInfo();
            return ResponseEntity.ok(serviceInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Health check failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test embedding generation
     * POST /api/ai/test/embedding
     * Body: {"text": "your text here"}
     */
    @PostMapping("/embedding")
    public ResponseEntity<Map<String, Object>> testEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Text is required"
                ));
            }
            
            long startTime = System.currentTimeMillis();
            float[] embedding = ollamaService.generateEmbedding(text);
            long duration = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok(Map.of(
                "text", text,
                "embeddingDimensions", embedding.length,
                "embeddingPreview", Arrays.toString(Arrays.copyOf(embedding, 5)) + "...",
                "processingTimeMs", duration
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Embedding generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test text generation
     * POST /api/ai/test/generate
     * Body: {"prompt": "your prompt here"}
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> testTextGeneration(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Prompt is required"
                ));
            }
            
            long startTime = System.currentTimeMillis();
            String response = ollamaService.generateText(prompt);
            long duration = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok(Map.of(
                "prompt", prompt,
                "response", response,
                "processingTimeMs", duration
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Text generation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test similarity calculation
     * POST /api/ai/test/similarity
     * Body: {"text1": "first text", "text2": "second text"}
     */
    @PostMapping("/similarity")
    public ResponseEntity<Map<String, Object>> testSimilarity(@RequestBody Map<String, String> request) {
        try {
            String text1 = request.get("text1");
            String text2 = request.get("text2");
            
            if (text1 == null || text2 == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Both text1 and text2 are required"
                ));
            }
            
            // Generate embeddings for both texts
            float[] embedding1 = ollamaService.generateEmbedding(text1);
            float[] embedding2 = ollamaService.generateEmbedding(text2);
            
            // Calculate similarity
            double similarity = ollamaService.calculateCosineSimilarity(embedding1, embedding2);
            
            return ResponseEntity.ok(Map.of(
                "text1", text1,
                "text2", text2,
                "similarity", similarity,
                "interpretation", similarity > 0.8 ? "Very similar" : 
                               similarity > 0.6 ? "Similar" : 
                               similarity > 0.4 ? "Somewhat similar" : "Not similar"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Similarity calculation failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Initialize vector database table
     * POST /api/ai/test/vector/init
     */
    @PostMapping("/vector/init")
    public ResponseEntity<Map<String, Object>> initVectorDatabase() {
        try {
            vectorSearchService.initializeVectorTable();
            Map<String, Object> serviceInfo = vectorSearchService.getServiceInfo();
            
            return ResponseEntity.ok(Map.of(
                "message", "Vector database initialized successfully",
                "serviceInfo", serviceInfo
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Vector database initialization failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test storing a sample product embedding
     * POST /api/ai/test/vector/store
     * Body: {"name": "Product Name", "category": "Category", "price": 99.99, "description": "Description"}
     */
    @PostMapping("/vector/store")
    public ResponseEntity<Map<String, Object>> testStoreProduct(@RequestBody Map<String, Object> request) {
        try {
            // Create a test product from request
            Product testProduct = new Product();
            testProduct.setId(System.currentTimeMillis()); // Use timestamp as ID for testing
            testProduct.setName((String) request.get("name"));
            testProduct.setCategory((String) request.get("category"));
            testProduct.setDescription((String) request.get("description"));
            
            // Handle price conversion
            Object priceObj = request.get("price");
            if (priceObj != null) {
                if (priceObj instanceof Number) {
                    testProduct.setPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                } else {
                    testProduct.setPrice(new BigDecimal(priceObj.toString()));
                }
            }
            
            testProduct.setStockQuantity(10); // Default stock
            
            long startTime = System.currentTimeMillis();
            boolean success = vectorSearchService.storeProductEmbedding(testProduct);
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "Product embedding stored successfully",
                    "productId", testProduct.getId(),
                    "processingTimeMs", duration,
                    "embeddingCount", vectorSearchService.getEmbeddingCount()
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to store product embedding"
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Product storage failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Test vector similarity search
     * POST /api/ai/test/vector/search
     * Body: {"query": "search text", "limit": 5}
     */
    @PostMapping("/vector/search")
    public ResponseEntity<Map<String, Object>> testVectorSearch(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            Integer limit = (Integer) request.getOrDefault("limit", 5);
            
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Query is required"
                ));
            }
            
            long startTime = System.currentTimeMillis();
            List<VectorSearchService.SearchResult> results = vectorSearchService.searchSimilarProducts(
                query, limit, null);
            long duration = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "results", results,
                "resultCount", results.size(),
                "processingTimeMs", duration,
                "totalEmbeddings", vectorSearchService.getEmbeddingCount()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Vector search failed",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get vector database status
     * GET /api/ai/test/vector/status
     */
    @GetMapping("/vector/status")
    public ResponseEntity<Map<String, Object>> getVectorStatus() {
        try {
            Map<String, Object> serviceInfo = vectorSearchService.getServiceInfo();
            return ResponseEntity.ok(serviceInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get vector status",
                "message", e.getMessage()
            ));
        }
    }
}