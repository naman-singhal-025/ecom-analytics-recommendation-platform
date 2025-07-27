package com.ecommerce.ecom_backend.services;

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
import com.ecommerce.ecom_backend.repo.elasticsearch.ProductSearchRepository;

/**
 * Service for product search operations using Elasticsearch.
 */
@Service
public class ProductSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ProductSearchService.class);
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductSearchRepository productSearchRepository;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    /**
     * Index a single product in Elasticsearch.
     * 
     * @param product the product to index
     * @return the indexed product document
     */
    public ProductDocument indexProduct(Product product) {
        logger.info("Indexing product in Elasticsearch: {}", product.getId());
        ProductDocument document = ProductDocument.fromProduct(product);
        return productSearchRepository.save(document);
    }
    
    /**
     * Index all products from the database in Elasticsearch.
     * This is useful for initial setup or reindexing.
     * 
     * @return the number of products indexed
     */
    public long indexAllProducts() {
        logger.info("Indexing all products in Elasticsearch");

        // Fetch all products using ProductService to utilize cached data instead of directly accessing the repository.
        List<Product> products = productService.getAllProducts();

        // Convert each Product entity into a ProductDocument for Elasticsearch indexing.
        // This uses a method reference to call the static fromProduct(Product) method.
        // Equivalent to: .map(product -> ProductDocument.fromProduct(product))
        // It transforms the stream of Product into a stream of ProductDocument.
        List<ProductDocument> documents = products.stream()
                .map(ProductDocument::fromProduct)
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
        indexProduct(product);
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
                productService.getProductById(productId).ifPresent(products::add);
            } catch (NumberFormatException e) {
                logger.error("Invalid product ID in Elasticsearch: {}", document.getId());
            }
        }
        
        return products;
    }
}