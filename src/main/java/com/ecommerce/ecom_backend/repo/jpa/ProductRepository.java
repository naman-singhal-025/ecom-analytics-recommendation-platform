package com.ecommerce.ecom_backend.repo.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.ecom_backend.model.Product;

/**
 * Spring Data repository for {@link Product} entities.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Find products by category.
     * 
     * @param category the category to search for
     * @return list of products in the given category
     */
    List<Product> findByCategory(String category);
    
    /**
     * Find products with stock quantity greater than or equal to the given value.
     * 
     * @param minStock the minimum stock quantity
     * @return list of products with sufficient stock
     */
    List<Product> findByStockQuantityGreaterThanEqual(Integer minStock);
    
    /**
     * Search products by name containing the given keyword (case-insensitive).
     * 
     * @param keyword the search keyword
     * @return list of products matching the search
     */
    List<Product> findByNameContainingIgnoreCase(String keyword);
    
    /**
     * Find products with price between the given range.
     * 
     * @param minPrice the minimum price
     * @param maxPrice the maximum price
     * @return list of products within the price range
     */
    @Query("SELECT p FROM Product p WHERE p.price >= :minPrice AND p.price <= :maxPrice")
    List<Product> findByPriceRange(@Param("minPrice") double minPrice, @Param("maxPrice") double maxPrice);
    
    /**
     * Find products that are low in stock (below the specified threshold).
     * 
     * @param threshold the stock threshold
     * @return list of products with low stock
     */
    List<Product> findByStockQuantityLessThanEqual(Integer threshold);
}