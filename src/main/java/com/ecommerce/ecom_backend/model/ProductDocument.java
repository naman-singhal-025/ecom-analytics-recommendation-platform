package com.ecommerce.ecom_backend.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * Elasticsearch document representation of a Product.
 * This is used for advanced search capabilities.
 */
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;
    
    // Analytics fields
    @Field(type = FieldType.Long)
    private Long totalViews = 0L;
    
    @Field(type = FieldType.Long)
    private Long totalPurchases = 0L;
    
    @Field(type = FieldType.Double)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Field(type = FieldType.Double)
    private Double conversionRate = 0.0;
    
    @Field(type = FieldType.Keyword)
    private String lastPurchaseDate;
    
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String name;
    
    // Description field is indexed as text for full-text search capabilities
    // analyzer is set to "standard" for basic text analysis.
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String description;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    // Category field is indexed as a keyword for exact match searches
    @MultiField(
        mainField = @Field(type = FieldType.Text),
        otherFields = { @InnerField(suffix = "keyword", type = FieldType.Keyword) }
    )
    private String category;
    
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
    
    @Field(type = FieldType.Keyword)
    private String imageUrl;
    
    @Field(type = FieldType.Date)
    private Instant createdAt;
    
    @Field(type = FieldType.Date)
    private Instant updatedAt;
    
    // Constructors
    public ProductDocument() {
    }
    
    /**
     * Create a ProductDocument from a Product entity.
     * 
     * @param product the JPA Product entity
     * @return a new ProductDocument
     */
    public static ProductDocument fromProduct(Product product) {
        ProductDocument document = new ProductDocument();
        document.setId(product.getId().toString());
        document.setName(product.getName());
        document.setDescription(product.getDescription());
        document.setPrice(product.getPrice());
        document.setCategory(product.getCategory());
        document.setStockQuantity(product.getStockQuantity());
        document.setImageUrl(product.getImageUrl());
        
        // Assign Instant values directly
        if (product.getCreatedAt() != null) {
            document.setCreatedAt(product.getCreatedAt());
        }
        if (product.getUpdatedAt() != null) {
            document.setUpdatedAt(product.getUpdatedAt());
        }
        
        return document;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    // Analytics getters and setters
    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public Long getTotalPurchases() {
        return totalPurchases;
    }

    public void setTotalPurchases(Long totalPurchases) {
        this.totalPurchases = totalPurchases;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(String lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }
    
    @Override
    public String toString() {
        return "ProductDocument{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                '}';
    }
}