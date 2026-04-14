package com.ecommerce.ecom_backend.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Represents a product in the e-commerce system.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    private String category;

    @PositiveOrZero(message = "Stock quantity must be zero or positive")
    private Integer stockQuantity;

    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // AI/ML: Vector embedding for semantic search
    // Stores 768-dimensional vector as JSON string for similarity search
    // Generated from product name + description using AI embedding models
    @Column(columnDefinition = "TEXT")
    private String embeddingJson;

    // Constructors
    public Product() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    // ============================================================================
    // AI/ML EMBEDDING HELPER METHODS
    // ============================================================================
    
    /**
     * Set embedding vector by converting float array to JSON string
     * This is the method you'll use in your business logic
     * 
     * @param embedding 768-dimensional float array from AI model
     */
    public void setEmbedding(float[] embedding) {
        if (embedding == null) {
            this.embeddingJson = null;
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.embeddingJson = mapper.writeValueAsString(embedding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert embedding to JSON", e);
        }
    }
    
    /**
     * Get embedding vector by converting JSON string back to float array
     * This is the method you'll use in your business logic
     * 
     * @return 768-dimensional float array for similarity calculations
     */
    public float[] getEmbedding() {
        if (embeddingJson == null || embeddingJson.trim().isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(embeddingJson, float[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse embedding JSON: " + embeddingJson, e);
        }
    }
    
    /**
     * Check if this product has an embedding vector
     * Useful for filtering products that are ready for AI search
     * 
     * @return true if embedding exists and is not empty
     */
    public boolean hasEmbedding() {
        return embeddingJson != null && !embeddingJson.trim().isEmpty() && !embeddingJson.equals("null");
    }
    
    /**
     * Get text representation for generating embeddings
     * Combines name, category, and description for AI processing
     * 
     * @return formatted text string for embedding generation
     */
    public String getTextForEmbedding() {
        StringBuilder text = new StringBuilder();
        
        if (name != null) {
            text.append(name);
        }
        
        if (category != null) {
            text.append(" Category: ").append(category);
        }
        
        if (description != null && !description.trim().isEmpty()) {
            text.append(" Description: ").append(description);
        }
        
        return text.toString().trim();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                '}';
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}