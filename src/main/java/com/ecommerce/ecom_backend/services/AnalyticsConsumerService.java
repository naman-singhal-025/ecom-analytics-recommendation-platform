package com.ecommerce.ecom_backend.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${analytics.trending.timewindow.hours:24}")
    private int trendingTimeWindowHours;
    
    // In-memory counters for real-time analytics
    private final Map<String, AtomicLong> eventTypeCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> productViewCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> productPurchaseCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> categoryCounters = new ConcurrentHashMap<>();
    
    // Timestamp of the last reset of counters
    private LocalDateTime lastCounterReset = LocalDateTime.now();
    
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
        
        // Check if we need to reset counters (every 24 hours)
        if (LocalDateTime.now().minusHours(trendingTimeWindowHours).isAfter(lastCounterReset)) {
            resetCounters();
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
        UserEvent event = objectMapper.readValue(eventJson, UserEvent.class);
        logger.debug("Processing user event: {}", event.getId());
        
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
        
        // Update event type counters (keeping in-memory counters as a backup)
        eventTypeCounters.computeIfAbsent(event.getEventType(), k -> new AtomicLong(0)).incrementAndGet();
        
        // Update product counters based on event type
        if ("VIEW".equals(event.getEventType())) {
            productViewCounters.computeIfAbsent(event.getProductId(), k -> new AtomicLong(0)).incrementAndGet();
        } else if ("PURCHASE".equals(event.getEventType())) {
            productPurchaseCounters.computeIfAbsent(event.getProductId(), k -> new AtomicLong(0)).incrementAndGet();
        }
        
        // Update category counters
        if (event.getCategory() != null) {
            categoryCounters.computeIfAbsent(event.getCategory(), k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    /**
     * Reset all counters.
     */
    private void resetCounters() {
        logger.info("Resetting real-time analytics counters");
        eventTypeCounters.clear();
        productViewCounters.clear();
        productPurchaseCounters.clear();
        categoryCounters.clear();
        lastCounterReset = LocalDateTime.now();
    }
    
    /**
     * Get the current event type counts.
     * 
     * @return map of event types to counts
     */
    public Map<String, Long> getEventTypeCounts() {
        Map<String, Long> result = new HashMap<>();
        eventTypeCounters.entrySet().stream()
        .forEach(entry -> result.put(entry.getKey(), entry.getValue().get()));
        return result;
    }
    
    /**
     * Get the current top viewed products.
     * 
     * @param limit the maximum number of products to return
     * @return map of product IDs to view counts
     */
    public Map<String, Long> getTopViewedProducts(int limit) {
        return getTopEntries(productViewCounters, limit);
    }
    
    /**
     * Get the current top purchased products.
     * 
     * @param limit the maximum number of products to return
     * @return map of product IDs to purchase counts
     */
    public Map<String, Long> getTopPurchasedProducts(int limit) {
        return getTopEntries(productPurchaseCounters, limit);
    }
    
    /**
     * Get the current top categories.
     * 
     * @param limit the maximum number of categories to return
     * @return map of categories to counts
     */
    public Map<String, Long> getTopCategories(int limit) {
        return getTopEntries(categoryCounters, limit);
    }
    
    /**
     * Get the top entries from a counter map.
     * 
     * @param counterMap the counter map
     * @param limit the maximum number of entries to return
     * @return map of keys to counts
     */
    private Map<String, Long> getTopEntries(Map<String, AtomicLong> counterMap, int limit) {
        return counterMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(limit)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().get()), HashMap::putAll);
    }
    
    /**
     * Get the conversion rate for a product (purchases / views).
     * 
     * @param productId the product ID
     * @return the conversion rate, or 0 if there are no views
     */
    public double getProductConversionRate(String productId) {
        long views = productViewCounters.getOrDefault(productId, new AtomicLong(0)).get();
        long purchases = productPurchaseCounters.getOrDefault(productId, new AtomicLong(0)).get();
        
        return views > 0 ? (double) purchases / views : 0;
    }
    
    /**
     * Get the conversion rates for the top viewed products.
     * 
     * @param limit the maximum number of products to return
     * @return map of product IDs to conversion rates
     */
    public Map<String, Double> getTopProductConversionRates(int limit) {
        Map<String, Double> result = new HashMap<>();
        
        getTopViewedProducts(limit).keySet().forEach(productId -> {
            result.put(productId, getProductConversionRate(productId));
        });
        
        return result;
    }
}