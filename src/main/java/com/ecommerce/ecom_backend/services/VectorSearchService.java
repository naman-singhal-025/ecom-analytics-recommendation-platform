package com.ecommerce.ecom_backend.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.model.Product;

/**
 * Service for vector-based similarity search using PostgreSQL with pgvector extension
 * 
 * This service handles:
 * - Storing product embeddings in PostgreSQL
 * - Performing cosine similarity search
 * - Batch operations for efficiency
 * - Metadata filtering (category, price range, etc.)
 * 
 * Vector Storage Strategy:
 * - Uses PostgreSQL with pgvector extension for persistent storage
 * - Stores vectors as VECTOR type for efficient similarity operations
 * - Includes product metadata for filtering
 * - Supports both exact and approximate similarity search
 */
@Service
public class VectorSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private OllamaService ollamaService;
    
    // Configuration from application.properties
    @Value("${vector.postgresql.table:product_embeddings}")
    private String vectorTable;
    
    @Value("${vector.postgresql.dimensions:768}")
    private int vectorDimensions;
    
    /**
     * Search Result DTO for returning similar products
     */
    public static class SearchResult {
        private Long productId;
        private String name;
        private String category;
        private Double price;
        private String description;
        private double similarity;
        
        // Constructors
        public SearchResult() {}
        
        public SearchResult(Long productId, String name, String category, Double price, 
                          String description, double similarity) {
            this.productId = productId;
            this.name = name;
            this.category = category;
            this.price = price;
            this.description = description;
            this.similarity = similarity;
        }
        
        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
        
        @Override
        public String toString() {
            return String.format("SearchResult{id=%d, name='%s', similarity=%.3f}", 
                               productId, name, similarity);
        }
    }
    
    /**
     * Search Filters for constraining similarity search
     */
    public static class SearchFilters {
        private String category;
        private Double minPrice;
        private Double maxPrice;
        private Integer minStock;
        private List<String> excludeIds;
        
        // Constructors
        public SearchFilters() {}
        
        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
        
        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
        
        public Integer getMinStock() { return minStock; }
        public void setMinStock(Integer minStock) { this.minStock = minStock; }
        
        public List<String> getExcludeIds() { return excludeIds; }
        public void setExcludeIds(List<String> excludeIds) { this.excludeIds = excludeIds; }
    }
    
    /**
     * Initialize vector table if it doesn't exist
     * Creates the table with proper pgvector column and indexes
     */
    public void initializeVectorTable() {
        try {
            // Create table if not exists
            String createTableSql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGSERIAL PRIMARY KEY,
                    product_id BIGINT NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    category VARCHAR(100),
                    price DECIMAL(10,2),
                    description TEXT,
                    stock_quantity INTEGER,
                    embedding VECTOR(%d) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """, vectorTable, vectorDimensions);
            
            jdbcTemplate.execute(createTableSql);
            
            // Create indexes for efficient similarity search
            String createIndexSql = String.format("""
                CREATE INDEX IF NOT EXISTS %s_embedding_cosine_idx 
                ON %s USING ivfflat (embedding vector_cosine_ops) 
                WITH (lists = 100)
                """, vectorTable, vectorTable);
            
            jdbcTemplate.execute(createIndexSql);
            
            // Create indexes for filtering
            jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_product_id_idx ON %s (product_id)", 
                vectorTable, vectorTable));
            
            jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_category_idx ON %s (category)", 
                vectorTable, vectorTable));
            
            logger.info("Vector table '{}' initialized successfully", vectorTable);
            
        } catch (Exception e) {
            logger.error("Failed to initialize vector table: {}", e.getMessage());
            throw new RuntimeException("Vector table initialization failed", e);
        }
    }
    
    /**
     * Store or update product embedding in vector database
     * 
     * @param product Product to store embedding for
     * @return true if successful, false otherwise
     */
    public boolean storeProductEmbedding(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product and product ID cannot be null");
        }
        
        try {
            // Generate embedding if not already present
            float[] embedding = product.getEmbedding();
            if (embedding == null) {
                String textForEmbedding = product.getTextForEmbedding();
                embedding = ollamaService.generateEmbedding(textForEmbedding);
                product.setEmbedding(embedding); // Store back in product
            }
            
            // Convert float array to PostgreSQL vector format
            String vectorString = arrayToVectorString(embedding);
            
            // Upsert (insert or update) the embedding
            String upsertSql = String.format("""
                INSERT INTO %s (product_id, name, category, price, description, stock_quantity, embedding, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector, CURRENT_TIMESTAMP)
                ON CONFLICT (product_id) 
                DO UPDATE SET 
                    name = EXCLUDED.name,
                    category = EXCLUDED.category,
                    price = EXCLUDED.price,
                    description = EXCLUDED.description,
                    stock_quantity = EXCLUDED.stock_quantity,
                    embedding = EXCLUDED.embedding,
                    updated_at = CURRENT_TIMESTAMP
                """, vectorTable);
            
            int rowsAffected = jdbcTemplate.update(upsertSql,
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getStockQuantity(),
                vectorString
            );
            
            logger.debug("Stored embedding for product ID: {} (rows affected: {})", 
                        product.getId(), rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (Exception e) {
            logger.error("Failed to store embedding for product ID: {}", product.getId(), e);
            return false;
        }
    }
    
    /**
     * Perform similarity search to find products similar to query text
     * 
     * @param queryText Text to search for similar products
     * @param limit Maximum number of results to return
     * @param filters Optional filters to apply
     * @return List of similar products with similarity scores
     */
    public List<SearchResult> searchSimilarProducts(String queryText, int limit, SearchFilters filters) {
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new IllegalArgumentException("Query text cannot be null or empty");
        }
        
        try {
            // Generate embedding for query text
            float[] queryEmbedding = ollamaService.generateEmbedding(queryText.trim());
            String queryVectorString = arrayToVectorString(queryEmbedding);
            
            // Build SQL query with optional filters
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(String.format("""
                SELECT product_id, name, category, price, description,
                       1 - (embedding <=> ?::vector) as similarity
                FROM %s
                WHERE 1=1
                """, vectorTable));
            
            List<Object> params = new ArrayList<>();
            params.add(queryVectorString);
            
            // Add filters
            if (filters != null) {
                if (filters.getCategory() != null) {
                    sqlBuilder.append(" AND category = ?");
                    params.add(filters.getCategory());
                }
                
                if (filters.getMinPrice() != null) {
                    sqlBuilder.append(" AND price >= ?");
                    params.add(filters.getMinPrice());
                }
                
                if (filters.getMaxPrice() != null) {
                    sqlBuilder.append(" AND price <= ?");
                    params.add(filters.getMaxPrice());
                }
                
                if (filters.getMinStock() != null) {
                    sqlBuilder.append(" AND stock_quantity >= ?");
                    params.add(filters.getMinStock());
                }
                
                if (filters.getExcludeIds() != null && !filters.getExcludeIds().isEmpty()) {
                    String placeholders = String.join(",", 
                        filters.getExcludeIds().stream().map(id -> "?").toArray(String[]::new));
                    sqlBuilder.append(" AND product_id NOT IN (").append(placeholders).append(")");
                    filters.getExcludeIds().forEach(id -> params.add(Long.parseLong(id)));
                }
            }
            
            // Order by similarity and limit results
            sqlBuilder.append(" ORDER BY similarity DESC LIMIT ?");
            params.add(limit);
            
            String sql = sqlBuilder.toString();
            logger.debug("Executing similarity search: {}", sql);
            
            // Execute query and map results
            List<SearchResult> results = jdbcTemplate.query(sql, params.toArray(), new SearchResultRowMapper());
            
            logger.info("Found {} similar products for query: '{}'", results.size(), queryText);
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to search similar products for query: '{}'", queryText, e);
            throw new RuntimeException("Similarity search failed", e);
        }
    }
    
    /**
     * Find products similar to a given product
     * 
     * @param productId ID of the product to find similar items for
     * @param limit Maximum number of results to return
     * @param filters Optional filters to apply
     * @return List of similar products with similarity scores
     */
    public List<SearchResult> findSimilarProducts(Long productId, int limit, SearchFilters filters) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        try {
            // Get embedding for the given product
            String getEmbeddingSql = String.format(
                "SELECT embedding FROM %s WHERE product_id = ?", vectorTable);
            
            String embeddingVector = jdbcTemplate.queryForObject(getEmbeddingSql, String.class, productId);
            
            if (embeddingVector == null) {
                logger.warn("No embedding found for product ID: {}", productId);
                return new ArrayList<>();
            }
            
            // Build similarity search query (exclude the source product)
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(String.format("""
                SELECT product_id, name, category, price, description,
                       1 - (embedding <=> ?::vector) as similarity
                FROM %s
                WHERE product_id != ?
                """, vectorTable));
            
            List<Object> params = new ArrayList<>();
            params.add(embeddingVector);
            params.add(productId);
            
            // Add filters (same logic as searchSimilarProducts)
            if (filters != null) {
                if (filters.getCategory() != null) {
                    sqlBuilder.append(" AND category = ?");
                    params.add(filters.getCategory());
                }
                
                if (filters.getMinPrice() != null) {
                    sqlBuilder.append(" AND price >= ?");
                    params.add(filters.getMinPrice());
                }
                
                if (filters.getMaxPrice() != null) {
                    sqlBuilder.append(" AND price <= ?");
                    params.add(filters.getMaxPrice());
                }
                
                if (filters.getMinStock() != null) {
                    sqlBuilder.append(" AND stock_quantity >= ?");
                    params.add(filters.getMinStock());
                }
            }
            
            sqlBuilder.append(" ORDER BY similarity DESC LIMIT ?");
            params.add(limit);
            
            List<SearchResult> results = jdbcTemplate.query(sqlBuilder.toString(), 
                                                          params.toArray(), 
                                                          new SearchResultRowMapper());
            
            logger.info("Found {} similar products for product ID: {}", results.size(), productId);
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to find similar products for product ID: {}", productId, e);
            throw new RuntimeException("Similar products search failed", e);
        }
    }
    
    /**
     * Get total count of stored embeddings
     */
    public long getEmbeddingCount() {
        try {
            String countSql = String.format("SELECT COUNT(*) FROM %s", vectorTable);
            return jdbcTemplate.queryForObject(countSql, Long.class);
        } catch (Exception e) {
            logger.error("Failed to get embedding count", e);
            return 0;
        }
    }
    
    /**
     * Delete embedding for a product
     */
    public boolean deleteProductEmbedding(Long productId) {
        if (productId == null) {
            return false;
        }
        
        try {
            String deleteSql = String.format("DELETE FROM %s WHERE product_id = ?", vectorTable);
            int rowsAffected = jdbcTemplate.update(deleteSql, productId);
            
            logger.debug("Deleted embedding for product ID: {} (rows affected: {})", 
                        productId, rowsAffected);
            
            return rowsAffected > 0;
        } catch (Exception e) {
            logger.error("Failed to delete embedding for product ID: {}", productId, e);
            return false;
        }
    }
    
    /**
     * Convert float array to PostgreSQL vector string format
     * Example: [0.1, 0.2, 0.3] -> "[0.1,0.2,0.3]"
     */
    private String arrayToVectorString(float[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Row mapper for converting SQL results to SearchResult objects
     */
    private static class SearchResultRowMapper implements RowMapper<SearchResult> {
        @Override
        public SearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SearchResult(
                rs.getLong("product_id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getString("description"),
                rs.getDouble("similarity")
            );
        }
    }
    
    /**
     * Get service status and statistics
     */
    public Map<String, Object> getServiceInfo() {
        return Map.of(
            "vectorTable", vectorTable,
            "vectorDimensions", vectorDimensions,
            "embeddingCount", getEmbeddingCount(),
            "status", "healthy"
        );
    }
}