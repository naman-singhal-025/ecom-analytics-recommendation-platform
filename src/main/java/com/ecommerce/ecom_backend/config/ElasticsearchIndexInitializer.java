package com.ecommerce.ecom_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import com.ecommerce.ecom_backend.model.ProductDocument;
import com.ecommerce.ecom_backend.model.UserEvent;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class responsible for initializing the Elasticsearch index for ProductDocument.
 * 
 * This class ensures that the "products" index exists in Elasticsearch when the application starts.
 * If the index doesn't exist, it will be created along with the necessary mapping.
 * 
 * Why @PostConstruct?
 * --------------------
 * The @PostConstruct annotation tells Spring to run this method immediately after the bean is fully initialized.
 * It is a good place to perform one-time setup logic like index creation, which must happen
 * before any repository or service attempts to access the index to avoid NoSuchIndexException.
 */
@Configuration
public class ElasticsearchIndexInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void createProductIndexIfNotExists() {
        IndexCoordinates index = IndexCoordinates.of("products");

        if (!elasticsearchOperations.indexOps(index).exists()) {
            // Create the index for the ProductDocument if it doesn't exist
            elasticsearchOperations.indexOps(ProductDocument.class).create();

            // Apply the mapping derived from the ProductDocument class
            elasticsearchOperations.indexOps(ProductDocument.class)
                .putMapping(elasticsearchOperations.indexOps(ProductDocument.class).createMapping());

            logger.info("Created 'products' index and mapping during application startup.");
        } else {
            logger.info("'products' index already exists.");
        }
    }

    /**
     * Ensures that the "user_events" index exists in Elasticsearch when the application starts.
     * This prevents runtime errors during save operations that expect the index to be available.
     */
    @PostConstruct
    public void createUserEventIndexIfNotExists() {
        IndexCoordinates index = IndexCoordinates.of("user_events");

        if (!elasticsearchOperations.indexOps(index).exists()) {
            // Create the index for the UserEventDocument if it doesn't exist
            elasticsearchOperations.indexOps(UserEvent.class).create();

            // Apply the mapping derived from the UserEventDocument class
            elasticsearchOperations.indexOps(UserEvent.class)
                .putMapping(elasticsearchOperations.indexOps(UserEvent.class).createMapping());

            logger.info("Created 'user_events' index and mapping during application startup.");
        } else {
            logger.info("'user_events' index already exists.");
        }
    }
}
