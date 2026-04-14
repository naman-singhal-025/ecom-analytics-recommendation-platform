package com.ecommerce.ecom_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.services.ProductService;
import com.ecommerce.ecom_backend.services.VectorSearchService;
import com.ecommerce.ecom_backend.services.VectorSearchService.SearchResult;

import jakarta.validation.Valid;

/**
 * REST controller for managing products.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private VectorSearchService vectorSearchService;

    /**
     * Get all products.
     * 
     * @return list of all products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    
    /**
     * Get a product by ID.
     * 
     * @param id the product ID
     * @return the product, if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id);
        }
        return ResponseEntity.ok(product);
    }
    
    /**
     * Create a new product.
     * 
     * @param product the product to create
     * @return the created product
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        return new ResponseEntity<>(productService.createProduct(product), HttpStatus.CREATED);
    }
    
    /**
     * Update an existing product.
     * 
     * @param id the product ID
     * @param productDetails the updated product details
     * @return the updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product productDetails) {
        return productService.updateProduct(id, productDetails)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
    }
    
    /**
     * Delete a product.
     * 
     * @param id the product ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id);
        }
    }
    
    /**
     * Search products by name.
     * 
     * @param keyword the search keyword
     * @return list of products matching the search
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }
    
    /**
     * Get products by category.
     * 
     * @param category the category
     * @return list of products in the category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }
    
    /**
     * Get products with low stock.
     * 
     * @param threshold the stock threshold (default: 10)
     * @return list of products with low stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(@RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }
    
    /**
     * Get products by price range.
     * 
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of products within the price range
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }
    
    /**
     * Update product stock.
     * 
     * @param id the product ID
     * @param quantity the new stock quantity
     * @return the updated product
     */
    @PutMapping("/{id}/stock")
    public ResponseEntity<Product> updateProductStock(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            return productService.updateStock(id, quantity)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * Adjust product stock (add or subtract).
     * 
     * @param id the product ID
     * @param adjustment the stock adjustment (positive to add, negative to subtract)
     * @return the updated product
     */
    @PutMapping("/{id}/stock/adjust")
    public ResponseEntity<Product> adjustProductStock(@PathVariable Long id, @RequestParam Integer adjustment) {
        try {
            return productService.adjustStock(id, adjustment)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    /**
     * Check if a product is in stock.
     * 
     * @param id the product ID
     * @param quantity the required quantity
     * @return true if the product is in stock with the required quantity
     */
    @GetMapping("/{id}/in-stock")
    public ResponseEntity<Boolean> isProductInStock(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer quantity) {
        return ResponseEntity.ok(productService.isInStock(id, quantity));
    }
    
    /**
     * Get the most popular products.
     * 
     * @param limit the maximum number of products to return (default: 10)
     * @return list of the most popular products
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Product>> getMostPopularProducts(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getMostPopularProducts(limit));
    }
    
    /**
     * Semantic search for products using AI embeddings
     * 
     * @param query Natural language search query
     * @param limit Maximum number of results (default: 10)
     * @param category Optional category filter
     * @param minPrice Optional minimum price filter
     * @param maxPrice Optional maximum price filter
     * @return List of products ranked by semantic similarity
     */
    @GetMapping("/semantic-search")
    public ResponseEntity<List<SearchResult>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        
        try {
            // Build filters
            VectorSearchService.SearchFilters filters = new VectorSearchService.SearchFilters();
            filters.setCategory(category);
            filters.setMinPrice(minPrice);
            filters.setMaxPrice(maxPrice);
            
            List<SearchResult> results = vectorSearchService.searchSimilarProducts(query, limit, filters);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Semantic search failed: " + e.getMessage());
        }
    }
    
    /**
     * Find products similar to a given product
     * 
     * @param id Product ID to find similar products for
     * @param limit Maximum number of results (default: 5)
     * @param category Optional category filter
     * @return List of similar products with similarity scores
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<SearchResult>> getSimilarProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String category) {
        
        try {
            VectorSearchService.SearchFilters filters = new VectorSearchService.SearchFilters();
            filters.setCategory(category);
            
            List<SearchResult> results = vectorSearchService.findSimilarProducts(id, limit, filters);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Similar products search failed: " + e.getMessage());
        }
    }
}