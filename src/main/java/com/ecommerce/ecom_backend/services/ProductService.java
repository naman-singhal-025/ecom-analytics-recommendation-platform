package com.ecommerce.ecom_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.repo.jpa.OrderItemRepository;
import com.ecommerce.ecom_backend.repo.jpa.ProductRepository;

/**
 * Service for managing products and inventory.
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;

    // ApplicationEventPublisher is used to publish events, such as product creation or updates.
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private CacheManager cacheManager;
    
    /**
     * Get all products.
     * 
     * @return list of all products
     */
    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        logger.info("Fetching all products");
        return productRepository.findAll();
    }
    
    /**
     * Get a product by ID.
     *
     * This method returns an Optional<Product> but caches only the unwrapped Product instance.
     * Rationale:
     * - Caching Optional<Product> directly can cause deserialization issues in Redis (e.g., LinkedHashMap cannot be cast to Optional)
     * - We cache only the actual Product instance to avoid these issues
     * - This avoids type mismatch issues and keeps service methods clean
     *
     * @param id the product ID
     * @return an product if found, otherwise null
     */
    @Cacheable(value = "productById", key = "#id", unless = "#result == null")
    public Product getProductById(Long id) {
        logger.info("Fetching product with ID: {}", id);
        // We cache only the unwrapped Product (not Optional) to avoid issues with Redis serialization.
        return productRepository.findById(id).orElse(null); // unwrap Optional for caching
    }
    
    /**
     * Create a new product.
     * 
     * @param product the product to create
     * @return the created product
     * @Transactional ensures that the operation is atomic and consistent.
     * @CacheEvict clears the cache for products to ensure the new product is reflected in the cache.
     * This is important to avoid stale data in the cache after creation.
     * 	After creating a new product, the "products" cache (used in getAllProducts) is invalidated so that subsequent fetches reflect the new data. 
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        logger.info("Creating new product: {}", product.getName());
        
        // Set default values if not provided
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(Instant.now());
        }
        
        if (product.getUpdatedAt() == null) {
            product.setUpdatedAt(Instant.now());
        }
        
        Product savedProduct = productRepository.save(product);
        /**
         * Publish the product creation event. This will be handled by a {@link com.ecommerce.ecom_backend.search.ProductSearchService#handleProductChange}
         * after the transaction commits as it is annotated with @TransactionalEventListener.
         */
        applicationEventPublisher.publishEvent(savedProduct);
        return savedProduct;
    }
    
    /**
     * Update an existing product.
     * 
     * @param id the product ID
     * @param productDetails the updated product details
     * @return the updated product, if found
     */
    @Transactional
    @Caching(
        evict = {
            // You cannot selectively remove one product from products list cache because it’s not cached by individual product ID, but as one bulk result.
            @CacheEvict(value = "products", allEntries = true),
            // evicts list of products in the category of the updated product. {c1 -> l1, c2 -> l2, ...} . So, list from one category is evicted.
            // here condition check happen before key generation, so if the product is not found, it will not try to evict the cache.
            @CacheEvict(value = "categoryProducts", key = "#result != null ? #result.getCategory() : ''", condition = "#result != null"),
            @CacheEvict(value = "popularProducts", allEntries = true)
        },
        put = {
            @CachePut(value = "productById", key = "#id", condition = "#result != null")
        }
    )
    public Optional<Product> updateProduct(Long id, Product productDetails) {
        logger.info("Updating product with ID: {}", id);

        String oldCategory = productRepository.findById(id)
                                              .map(Product::getCategory)
                                              .orElse(null);
        // Manually evict the product list for the old category to prevent stale data,
        // especially if the category was changed during the update.
        if (oldCategory != null) {
            // Cache name is pre-registered in cacheManager configuration
            cacheManager.getCache("categoryProducts").evict(oldCategory);
        }

        Product existingProduct = getProductById(id);
        if (existingProduct == null) return Optional.empty();

        existingProduct.setName(productDetails.getName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setStockQuantity(productDetails.getStockQuantity());
        existingProduct.setImageUrl(productDetails.getImageUrl());
        existingProduct.setUpdatedAt(Instant.now());

        Product savedProduct = productRepository.save(existingProduct);
        /*
         * Publish the product update event. This will be handled by a {@link com.ecommerce.ecom_backend.search.ProductSearchService#handleProductChange} 
         * after the transaction commits.
         */
        applicationEventPublisher.publishEvent(savedProduct);
        return Optional.of(savedProduct);
    }
    
    /**
     * Delete a product.
     * 
     * @param id the product ID
     * @return true if deleted, false if not found
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "productById", key = "#id"),
        @CacheEvict(value = "products", allEntries = true),
        //return type is boolean, not Optional<Product>, so SpEL(Spring Expression Language) (#result.get().getCategory()) can’t access the deleted product’s category after deletion.
        @CacheEvict(value = "categoryProducts", allEntries = true),
        @CacheEvict(value = "popularProducts", allEntries = true)
    })
    public boolean deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        
        Product product = getProductById(id);
        if (product == null) return false;
        productRepository.delete(product);
        /**
         * Publish the product deletion event. This will be handled by a {@link com.ecommerce.ecom_backend.search.ProductSearchService#handleProductChange}
         * after the transaction commits.
         */
        applicationEventPublisher.publishEvent(product);
        return true;
    }
    
    /**
     * Update product stock quantity.
     * 
     * @param id the product ID
     * @param quantity the new stock quantity
     * @return the updated product, if found
     */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "popularProducts", allEntries = true)
        },
        put = {
            @CachePut(value = "productById", key = "#id", condition = "#result != null")
        }
    )
    public Optional<Product> updateStock(Long id, Integer quantity) {
        logger.info("Updating stock for product ID: {} to {}", id, quantity);
        
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        
        Product product = getProductById(id);
        if (product == null) return Optional.empty();

        product.setStockQuantity(quantity);
        product.setUpdatedAt(Instant.now());
        Product savedProduct = productRepository.save(product);
        /**
         * Publish the product update event. This will be handled by a {@link com.ecommerce.ecom_backend.search.ProductSearchService#handleProductChange}
         * after the transaction commits.
         */
        applicationEventPublisher.publishEvent(savedProduct);
        return Optional.of(savedProduct);
    }
    
    /**
     * Adjust product stock (add or subtract).
     * 
     * @param id the product ID
     * @param adjustment the stock adjustment (positive to add, negative to subtract)
     * @return the updated product, if found
     */
    @Transactional
    @Caching(
        evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "popularProducts", allEntries = true)
        },
        put = {
            @CachePut(value = "productById", key = "#id", condition = "#result != null")
        }
    )
    public Optional<Product> adjustStock(Long id, Integer adjustment) {
        logger.info("Adjusting stock for product ID: {} by {}", id, adjustment);
        
        Product product = getProductById(id);
        if (product == null) return Optional.empty();

        int newStock = product.getStockQuantity() + adjustment;
        
        if (newStock < 0) {
            throw new IllegalArgumentException("Cannot reduce stock below zero");
        }
        
        product.setStockQuantity(newStock);
        product.setUpdatedAt(Instant.now());
        Product savedProduct = productRepository.save(product);
        /**
         * Publish the product update event. This will be handled by a {@link com.ecommerce.ecom_backend.search.ProductSearchService#handleProductChange}
         * after the transaction commits.
         */
        applicationEventPublisher.publishEvent(savedProduct);
        return Optional.of(savedProduct);
    }
    
    /**
     * Check if a product is in stock.
     * 
     * @param id the product ID
     * @param requiredQuantity the required quantity
     * @return true if the product is in stock with the required quantity
     */
    public boolean isInStock(Long id, Integer requiredQuantity) {
        logger.info("Checking stock for product ID: {} (required: {})", id, requiredQuantity);
        
        Product product = getProductById(id);
        return product != null && product.getStockQuantity() >= requiredQuantity;
    }
    
    /**
     * Get products with low stock.
     * 
     * @param threshold the stock threshold
     * @return list of products with stock below or equal to the threshold
     */
    public List<Product> getLowStockProducts(Integer threshold) {
        logger.info("Fetching products with stock below or equal to: {}", threshold);
        return productRepository.findByStockQuantityLessThanEqual(threshold);
    }
    
    /**
     * Get products by category.
     * 
     * @param category the category
     * @return list of products in the category
     */
    @Cacheable(value = "categoryProducts", key = "#category")
    public List<Product> getProductsByCategory(String category) {
        logger.info("Fetching products in category: {}", category);
        return productRepository.findByCategory(category);
    }
    
    /**
     * Search products by name.
     * 
     * @param keyword the search keyword
     * @return list of products matching the search
     */
    public List<Product> searchProducts(String keyword) {
        logger.info("Searching products with keyword: {}", keyword);
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }
    
    /**
     * Get products by price range.
     * 
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of products within the price range
     */
    public List<Product> getProductsByPriceRange(double minPrice, double maxPrice) {
        logger.info("Fetching products with price between {} and {}", minPrice, maxPrice);
        return productRepository.findByPriceRange(minPrice, maxPrice);
    }
    
    /**
     * Get the most popular products based on order history.
     * 
     * @param limit the maximum number of products to return
     * @return list of the most popular products
     */
    @Cacheable(value = "popularProducts", key = "#limit")
    public List<Product> getMostPopularProducts(int limit) {
        logger.info("Fetching the {} most popular products", limit);
        
        List<Object[]> results = orderItemRepository.findMostOrderedProducts(limit);
        
        return results.stream()
                .map(result -> {
                    Long productId = ((Number) result[0]).longValue();
                    return productRepository.findById(productId).orElse(null);
                })
                .filter(product -> product != null)
                .toList();
    }
}