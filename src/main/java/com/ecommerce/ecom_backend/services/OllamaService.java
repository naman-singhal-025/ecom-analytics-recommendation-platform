package com.ecommerce.ecom_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with Ollama AI models
 * 
 * This service handles:
 * - Generating embeddings for semantic search
 * - Generating text responses for chat/recommendations
 * - Error handling and retries
 * - Caching (future enhancement)
 * 
 * Ollama API Documentation: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
@Service
public class OllamaService {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    // Configuration from application.properties
    @Value("${ollama.api.url}")
    private String ollamaUrl;
    
    @Value("${ollama.embedding.model}")
    private String embeddingModel;
    
    @Value("${ollama.chat.model}")
    private String chatModel;
    
    @Value("${ollama.timeout.seconds:30}")
    private int timeoutSeconds;
    
    @Value("${ollama.max-retries:3}")
    private int maxRetries;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OllamaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate embedding vector from text using Ollama
     * 
     * @param text Input text to convert to vector
     * @return 768-dimensional float array representing the text
     * @throws RuntimeException if embedding generation fails
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        logger.debug("Generating embedding for text: {}", text.substring(0, Math.min(text.length(), 100)));
        
        // Prepare request payload for Ollama embeddings API
        Map<String, Object> requestBody = Map.of(
            "model", embeddingModel,
            "prompt", text.trim()
        );
        
        try {
            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Create HTTP entity with request body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Call Ollama embeddings API
            ResponseEntity<String> response = restTemplate.exchange(
                ollamaUrl + "/embeddings",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // Parse response and extract embedding array
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = jsonResponse.get("embedding");
            
            if (embeddingNode == null || !embeddingNode.isArray()) {
                throw new RuntimeException("Invalid response format: missing or invalid embedding array");
            }
            
            // Convert JSON array to float array
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }
            
            logger.debug("Generated embedding with {} dimensions", embedding.length);
            return embedding;
            
        } catch (RestClientException e) {
            logger.error("Ollama API error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate text response using Ollama chat model
     * 
     * @param prompt Input prompt for text generation
     * @return Generated text response
     * @throws RuntimeException if text generation fails
     */
    public String generateText(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        
        logger.debug("Generating text for prompt: {}", prompt.substring(0, Math.min(prompt.length(), 100)));
        
        // Prepare request payload for Ollama generate API
        Map<String, Object> requestBody = Map.of(
            "model", chatModel,
            "prompt", prompt.trim(),
            "stream", false  // Get complete response, not streaming
        );
        
        try {
            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            // Create HTTP entity with request body
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Call Ollama generate API
            ResponseEntity<String> response = restTemplate.exchange(
                ollamaUrl + "/generate",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // Parse response and extract generated text
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode responseNode = jsonResponse.get("response");
            
            if (responseNode == null) {
                throw new RuntimeException("Invalid response format: missing response field");
            }
            
            String generatedText = responseNode.asText();
            logger.debug("Generated text with {} characters", generatedText.length());
            
            return generatedText;
            
        } catch (RestClientException e) {
            logger.error("Ollama API error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate text: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error generating text", e);
            throw new RuntimeException("Failed to generate text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate product recommendation text using retrieved product context
     * 
     * @param userQuery Original user search query
     * @param productContext List of relevant products found by vector search
     * @return AI-generated recommendation text
     */
    public String generateProductRecommendation(String userQuery, List<String> productContext) {
        if (productContext == null || productContext.isEmpty()) {
            return "I couldn't find any relevant products for your search. Please try different keywords.";
        }
        
        // Build context from retrieved products
        String context = String.join("\n\n", productContext);
        
        // Craft prompt for product recommendations
        String prompt = String.format("""
            You are a helpful e-commerce product advisor. A customer is searching for: "%s"
            
            Based on the following products from our catalog, provide helpful recommendations:
            
            %s
            
            Instructions:
            1. Recommend 2-3 most relevant products from the list above
            2. Explain WHY each product matches their search
            3. Include specific details like price and key features
            4. Be conversational and helpful
            5. If no products match well, suggest alternative search terms
            6. Don't make up information not provided in the product data
            
            Response:
            """, userQuery, context);
        
        return generateText(prompt);
    }
    
    /**
     * Calculate cosine similarity between two vectors
     * Used for finding similar products in vector search
     * 
     * @param vector1 First embedding vector
     * @param vector2 Second embedding vector
     * @return Similarity score between 0.0 and 1.0 (1.0 = identical)
     */
    public double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimensions");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        
        // Avoid division by zero
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Health check - verify Ollama is running and models are available
     * 
     * @return true if Ollama is healthy and ready
     */
    public boolean isHealthy() {
        try {
            // Check if Ollama is running by calling the tags endpoint
            ResponseEntity<String> response = restTemplate.getForEntity(
                ollamaUrl.replace("/api", "") + "/api/tags",
                String.class
            );
            
            // Parse response to check if our models are available
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode modelsNode = jsonResponse.get("models");
            
            if (modelsNode != null && modelsNode.isArray()) {
                boolean hasEmbeddingModel = false;
                boolean hasChatModel = false;
                
                for (JsonNode model : modelsNode) {
                    String modelName = model.get("name").asText();
                    if (modelName.contains(embeddingModel)) {
                        hasEmbeddingModel = true;
                    }
                    if (modelName.contains(chatModel)) {
                        hasChatModel = true;
                    }
                }
                
                return hasEmbeddingModel && hasChatModel;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get service configuration info for debugging
     */
    public Map<String, Object> getServiceInfo() {
        return Map.of(
            "ollamaUrl", ollamaUrl,
            "embeddingModel", embeddingModel,
            "chatModel", chatModel,
            "timeoutSeconds", timeoutSeconds,
            "maxRetries", maxRetries,
            "healthy", isHealthy()
        );
    }
}