package com.ecommerce.ecom_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.model.Product;
import com.ecommerce.ecom_backend.model.ProductDocument;
import com.ecommerce.ecom_backend.services.ProductSearchService;

/**
 * REST controller for product search operations.
 */
@RestController
@RequestMapping("/api/product-search")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    /**
     * Index single product in Elasticsearch.
     * 
     * @return the number of products indexed
     */
    @PostMapping("/index/{id}")
    public ResponseEntity<ProductDocument> indexProductById(@PathVariable Long id) {
        ProductDocument indexedProduct = productSearchService.indexProductById(id);
        if (indexedProduct == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(indexedProduct);
    }
    
    /**
     * Index all products in Elasticsearch.
     * 
     * @return the number of products indexed
     */
    @PostMapping("/index-all")
    public ResponseEntity<Long> indexAllProducts() {
        return ResponseEntity.ok(productSearchService.indexAllProducts());
    }
    
    /**
     * Search for products by name using fuzzy matching.
     * 
     * @param name the product name to search for
     * @return list of matching products
     */
    @GetMapping("/fuzzy")
    public ResponseEntity<List<Product>> searchByNameFuzzy(@RequestParam String name) {
        return ResponseEntity.ok(productSearchService.searchByNameFuzzy(name));
    }
    
    /**
     * Full-text search across product name and description.
     * 
     * @param query the search query
     * @return list of matching products
     */
    @GetMapping("/full-text")
    public ResponseEntity<List<Product>> fullTextSearch(@RequestParam String query) {
        return ResponseEntity.ok(productSearchService.fullTextSearch(query));
    }
    
    /**
     * Search for products by category and price range.
     * 
     * @param category the category
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of matching products
     */
    @GetMapping("/category-price")
    public ResponseEntity<List<Product>> searchByCategoryAndPriceRange(
            @RequestParam String category,
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(productSearchService.searchByCategoryAndPriceRange(
                category, minPrice, maxPrice));
    }
    
    /**
     * Advanced search with multiple criteria.
     * 
     * @param searchCriteria map of field names to search values
     * @return list of matching products
     */
    @PostMapping("/advanced")
    public ResponseEntity<List<Product>> advancedSearch(@RequestBody Map<String, Object> searchCriteria) {
        return ResponseEntity.ok(productSearchService.advancedSearch(searchCriteria));
    }
    
    /**
     * Get product suggestions based on a partial query.
     * 
     * @param query the partial query
     * @param limit the maximum number of suggestions to return (default: 5)
     * @return list of product suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(productSearchService.getSuggestions(query, limit));
    }
}