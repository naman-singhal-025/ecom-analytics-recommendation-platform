package com.ecommerce.ecom_backend.services;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.model.UserEvent;
import com.ecommerce.ecom_backend.repo.elasticsearch.UserEventSearchRepository;
import com.ecommerce.ecom_backend.repo.mongo.UserEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for consuming user events from Kafka and processing them for analytics.
 */
@Service
public class AnalyticsConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsConsumerService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserEventSearchRepository userEventSearchRepository;
    
    @Autowired
    private UserEventRepository userEventRepository;
    
    @Autowired
    private ProductSearchService productSearchService;
    
    /**
     * Listen to user events from Kafka and process them for analytics.
     * group-id is consumer groupId. Essential for:
     * - Load balancing message consumption
     * - Tracking consumption state (offsets)
     * - Enabling parallelism and failover
     * - Ensuring that each message is processed once per group (not per consumer).
     * @param messages the list of messages from Kafka
     */
    /*
     * Here due to @KafkaListener, this method will be invoked automatically
     * whenever new messages are available in the specified Kafka topic.
     * The messages are received in batches for efficiency as specified by the
     * `factory.setBatchListener(true)` configuration in {@link KafkaConsumerConfig}.
     */
    @KafkaListener(topics = "${kafka.topic.user-events}", groupId = "${spring.kafka.consumer.group-id:ecom-analytics-group}")
    public void consumeUserEvents(List<String> messages) {
        logger.info("Received batch of {} messages from Kafka", messages.size());
        
        for (String message : messages) {
            try {
                processUserEvent(message);
            } catch (Exception e) {
                logger.error("Error processing user event: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Process a single user event.
     * 
     * @param eventJson the JSON string representing the user event
     * @throws IOException if there's an error parsing the JSON
     */
    private void processUserEvent(String eventJson) throws IOException {
        // Deserialize the JSON string to a UserEvent object using objectMapper method from Jackson library
        logger.debug("Processing user event JSON: {}", eventJson);
        String parsedJson = objectMapper.readValue(eventJson, String.class);
        UserEvent event = objectMapper.readValue(parsedJson, UserEvent.class);
        
        // Save event to Elasticsearch for real-time analytics
        try {
            userEventSearchRepository.save(event);
            logger.debug("Saved user event to Elasticsearch: {}", event.getId());
        } catch (Exception e) {
            logger.error("Error saving user event to Elasticsearch: {}", e.getMessage());
        }
        
        // Save event to MongoDB
        try {
            userEventRepository.save(event);
            logger.debug("Saved user event to MongoDB: {}", event.getId());
        } catch (Exception e) {
            logger.error("Error saving user event to MongoDB: {}", e.getMessage());
        }

        logger.debug("Processing user event: {}", event.getId());
        
        productSearchService.updateProductAnalytics(event);
    }
}