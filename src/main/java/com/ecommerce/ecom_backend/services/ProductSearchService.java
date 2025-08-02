package com.ecommerce.ecom_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.model.ProductDocument;
import com.ecommerce.ecom_backend.model.UserEvent;
import com.ecommerce.ecom_backend.repo.elasticsearch.ProductSearchRepository;
import com.ecommerce.ecom_backend.repo.jpa.ProductRepository;

/**
 * Service for product search operations using Elasticsearch.
 */
@Service
public class ProductSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ProductSearchService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    /**
     * Index a single product in Elasticsearch.
     * 
     * @param product the product to index
     * @return the indexed product document
     */
    public ProductDocument indexProductById(Long id) {
        logger.info("Indexing product in Elasticsearch: {}", id);
        Product product = productService.getProductById(id);
        if (product == null) {
            logger.warn("Product not found for indexing: {}", id);
            return null;
        }
        
        ProductDocument existingDoc = productSearchRepository.findById(id.toString())
            .orElse(new ProductDocument());

        ProductDocument newDoc = ProductDocument.fromProduct(product);
        newDoc.setTotalViews(existingDoc.getTotalViews());
        newDoc.setTotalPurchases(existingDoc.getTotalPurchases());
        newDoc.setTotalRevenue(existingDoc.getTotalRevenue());
        newDoc.setConversionRate(existingDoc.getConversionRate());
        newDoc.setLastPurchaseDate(existingDoc.getLastPurchaseDate());

        return productSearchRepository.save(newDoc);
    }
    
    /**
     * Index all products from the database in Elasticsearch.
     * This is useful for initial setup or reindexing.
     * 
     * @return the number of products indexed
     */
    public long indexAllProducts() {
        logger.info("Indexing all products in Elasticsearch");

        // Fetch directly from repository to avoid cache deserialization issues
        List<Product> products = productRepository.findAll();

        List<ProductDocument> documents = products.stream()
                    .map(product -> {
                    ProductDocument existingDoc = productSearchRepository.findById(product.getId().toString())
                    .orElse(new ProductDocument());
                    ProductDocument newDoc = ProductDocument.fromProduct(product);
                    newDoc.setTotalViews(existingDoc.getTotalViews());
                    newDoc.setTotalPurchases(existingDoc.getTotalPurchases());
                    newDoc.setTotalRevenue(existingDoc.getTotalRevenue());
                    newDoc.setConversionRate(existingDoc.getConversionRate());
                    newDoc.setLastPurchaseDate(existingDoc.getLastPurchaseDate());
                    return newDoc;
                })
                .collect(Collectors.toList());
        
        Iterable<ProductDocument> savedDocuments = productSearchRepository.saveAll(documents);
        
        // StreamSupport allows us to convert an Iterable into a Stream.
        // spliterator() creates a Spliterator from the Iterable which can be used to create a Stream.
        // Passing 'false' means use a sequential stream (process items one-by-one).
        // You could pass 'true' to use a parallel stream, which uses multiple threads,
        long count = StreamSupport.stream(savedDocuments.spliterator(), false).count();
        logger.info("Indexed {} products in Elasticsearch", count);
        return count;
    }
    
    /**
     * Delete a product from the Elasticsearch index.
     * 
     * @param productId the product ID
     */
    public void deleteProductFromIndex(Long productId) {
        logger.info("Deleting product from Elasticsearch index: {}", productId);
        productSearchRepository.deleteById(productId.toString());
    }
    
    /**
     * Synchronize a product between PostgreSQL and Elasticsearch.
     * This is called after a product is created or updated.
     * 
     * @param product the product to synchronize
     */
    @TransactionalEventListener
    public void handleProductChange(Product product) {
        logger.info("Handling product change event for product: {}", product.getId());
        
        // Get existing document if it exists to preserve analytics data
        ProductDocument existingDoc = productSearchRepository.findById(product.getId().toString())
            .orElse(new ProductDocument());
        
        // Create new document with updated product data
        ProductDocument newDoc = ProductDocument.fromProduct(product);
        
        // Preserve analytics data
        newDoc.setTotalViews(existingDoc.getTotalViews());
        newDoc.setTotalPurchases(existingDoc.getTotalPurchases());
        newDoc.setTotalRevenue(existingDoc.getTotalRevenue());
        newDoc.setConversionRate(existingDoc.getConversionRate());
        newDoc.setLastPurchaseDate(existingDoc.getLastPurchaseDate());
        
        productSearchRepository.save(newDoc);
    }

    /**
     * Update analytics data for a product.
     *
     * @param productId the product ID
     * @param views number of new views
     * @param purchases number of new purchases
     */
    public void updateProductAnalytics(String productId, long views, long purchases) {
        logger.info("Updating analytics for product: {}", productId);
        
        productSearchRepository.findById(productId).ifPresent(doc -> {
            // Update analytics fields
            doc.setTotalViews(doc.getTotalViews() + views);
            doc.setTotalPurchases(doc.getTotalPurchases() + purchases);
            doc.setUpdatedAt(Instant.now());
            
            BigDecimal price = doc.getPrice();
            if (price != null) {
                BigDecimal additionalRevenue = price.multiply(BigDecimal.valueOf(purchases));
                doc.setTotalRevenue(doc.getTotalRevenue().add(additionalRevenue));
            }
            
            // Update conversion rate
            if (doc.getTotalViews() > 0) {
                double convRate = (double) doc.getTotalPurchases() / doc.getTotalViews();
                doc.setConversionRate(convRate);
            }
            
            // Update last purchase date if there were purchases
            if (purchases > 0) {
                doc.setLastPurchaseDate(DATE_FORMATTER.format(LocalDateTime.now()));
            }
            
            productSearchRepository.save(doc);
            logger.info("Updated analytics for product: {} : {}", productId, doc);
        });
    }
    
    /**
     * Search for products by name using fuzzy matching.
     * 
     * @param name the product name to search for
     * @return list of matching products
     */
    public List<Product> searchByNameFuzzy(String name) {
        logger.info("Performing fuzzy search for products with name: {}", name);
        List<ProductDocument> documents = productSearchRepository.findByNameFuzzy(name);
        return convertToProducts(documents);
    }
    
    /**
     * Full-text search across product name and description.
     * 
     * @param text the search text
     * @return list of matching products
     */
    public List<Product> fullTextSearch(String text) {
        logger.info("Performing full-text search for: {}", text);
        List<ProductDocument> documents = productSearchRepository.fullTextSearch(text);
        return convertToProducts(documents);
    }
    
    /**
     * Search for products by category and price range.
     * 
     * @param category the category
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of matching products
     */
    public List<Product> searchByCategoryAndPriceRange(String category, double minPrice, double maxPrice) {
        logger.info("Searching for products in category {} with price between {} and {}", 
                category, minPrice, maxPrice);
        List<ProductDocument> documents = productSearchRepository.findByCategoryAndPriceBetween(
                category, minPrice, maxPrice);
        return convertToProducts(documents);
    }
    
    /**
     * Advanced search with multiple criteria.
     * 
     * @param searchCriteria map of field names to search values
     * @return list of matching products
     */
    public List<Product> advancedSearch(Map<String, Object> searchCriteria) {
        logger.info("Performing advanced search with criteria: {}", searchCriteria);
        
        Criteria criteria = new Criteria();
        
        for (Map.Entry<String, Object> entry : searchCriteria.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                if (field.equals("name") || field.equals("description")) {
                    criteria = criteria.and(field).contains(stringValue);
                } else {
                    criteria = criteria.and(field).is(value);
                }
            } else if (value instanceof Number) {
                criteria = criteria.and(field).is(value);
            } else if (value instanceof Map) {
                Map<String, Object> rangeMap = (Map<String, Object>) value;
                if (rangeMap.containsKey("min") && rangeMap.containsKey("max")) {
                    criteria = criteria.and(field).between(rangeMap.get("min"), rangeMap.get("max"));
                } else if (rangeMap.containsKey("min")) {
                    criteria = criteria.and(field).greaterThanEqual(rangeMap.get("min"));
                } else if (rangeMap.containsKey("max")) {
                    criteria = criteria.and(field).lessThanEqual(rangeMap.get("max"));
                }
            }
        }
        
        Query query = new CriteriaQuery(criteria);
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                query, ProductDocument.class, IndexCoordinates.of("products"));
        
        List<ProductDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        
        return convertToProducts(documents);
    }
    
    /**
     * Get product suggestions based on a partial query.
     * 
     * @param query the partial query
     * @param maxSuggestions the maximum number of suggestions to return
     * @return list of product suggestions
     */
    public List<String> getSuggestions(String query, int maxSuggestions) {
        logger.info("Getting product suggestions for query: {}", query);
        
        // Use a simple prefix search instead of NativeSearchQueryBuilder
        List<ProductDocument> documents = productSearchRepository.findByNameContaining(query.toLowerCase());
        
        return documents.stream()
                .map(ProductDocument::getName)
                .distinct()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }

    /**
     * Get top products by revenue for Kibana visualization.
     */
    public String getTopProductsByRevenue(int limit) {
        logger.info("Getting top {} products by revenue", limit);
        return productSearchRepository.findTopProductsByRevenue(limit);
    }

    /**
     * Get top products by conversion rate for Kibana visualization.
     */
    public String getTopProductsByConversionRate(int limit) {
        logger.info("Getting top {} products by conversion rate", limit);
        return productSearchRepository.findTopProductsByConversionRate(limit);
    }

    /**
     * Get product distribution by view count ranges for Kibana visualization.
     */
    public String getProductsByViewRanges() {
        logger.info("Getting products by view count ranges");
        return productSearchRepository.findProductsByViewRanges();
    }

    /**
     * Get purchase trends over time for Kibana visualization.
     */
    public String getPurchaseTrends(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Getting purchase trends from {} to {}", startDate, endDate);
        return productSearchRepository.findPurchaseTrends(startDate, endDate);
    }
    
    /**
     * Convert ProductDocument objects to Product entities.
     * 
     * @param documents the Elasticsearch documents
     * @return list of Product entities
     */
    private List<Product> convertToProducts(List<ProductDocument> documents) {
        List<Product> products = new ArrayList<>();
        
        for (ProductDocument document : documents) {
            try {
                Long productId = Long.valueOf(document.getId());
                Product product = productService.getProductById(productId);
                if (product != null) {
                    products.add(product);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid product ID in Elasticsearch: {}", document.getId());
            }
        }
        
        return products;
    }

    /**
     * Update product analytics based on a UserEvent.
     * Increments views for "VIEW" events, and increments purchases and revenue for "PURCHASE" events.
     *
     * @param event the user event containing productId, eventType, and price
     */
    public void updateProductAnalytics(UserEvent event) {
        String productId = event.getProductId();
        long views = 0;
        long purchases = 0;

        if ("VIEW".equalsIgnoreCase(event.getEventType())) {
            views = 1;
        } else if ("PURCHASE".equalsIgnoreCase(event.getEventType())) {
            purchases = 1;
        }

        updateProductAnalytics(productId, views, purchases);
    }
}