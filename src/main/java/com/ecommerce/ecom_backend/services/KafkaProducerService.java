/*
 * Why Kafka here?
 * Asynchronous Processing: When a user clicks a product, we don't want to slow down their experience by processing analytics immediately
 * Reliability: If our analytics service is down, events are still stored in Kafka
 * Scalability: Multiple services can consume the same events
 * Partitioning: Using userId as the key ensures events from the same user go to the same partition (maintaining order)
 */


package com.ecommerce.ecom_backend.services;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.model.UserEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for sending user events to Kafka topics.
 * This allows other services to process events asynchronously.
 */
@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Topic name from configuration
    @Value("${kafka.topic.user-events}")
    private String userEventsTopic;
    
    /**
     * Send a user event to Kafka topic
     * @param userEvent The event to send
     */
    public void sendUserEvent(UserEvent userEvent) {
        try {
            // Convert the event object to JSON string
            String eventJson = objectMapper.writeValueAsString(userEvent);
            
            // Send to Kafka topic
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(userEventsTopic, userEvent.getUserId(), eventJson);
            
            // Add callback for success/failure handling
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Sent user event=[{}] with offset=[{}]", 
                               userEvent.getId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Unable to send user event=[{}] due to : {}", 
                                userEvent.getId(), exception.getMessage());
                }
            });
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing user event: {}", e.getMessage());
        }
    }
    
    /**
     * Send event and wait for acknowledgment (synchronous)
     * Use this when you need to ensure the message was sent successfully
     * When guaranteed delivery is critical (e.g., payment, audit logs).
     */
    public boolean sendUserEventSync(UserEvent userEvent) {
        try {
            String eventJson = objectMapper.writeValueAsString(userEvent);
            
            SendResult<String, String> result = kafkaTemplate
                .send(userEventsTopic, userEvent.getUserId(), eventJson)
                .get(); // This blocks until message is sent
            
            logger.info("Sent user event synchronously=[{}] with offset=[{}]", 
                       userEvent.getId(), result.getRecordMetadata().offset());
            return true;
            
        } catch (Exception e) {
            logger.error("Error sending user event synchronously: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Send a custom event message
     * @param topic The Kafka topic
     * @param key The message key (for partitioning)
     * @param message The message content
     */
    public void sendCustomMessage(String topic, String key, String message) {
        CompletableFuture<SendResult<String, String>> future = 
            kafkaTemplate.send(topic, key, message);
            
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                logger.info("Sent message=[{}] to topic=[{}] with offset=[{}]", 
                           message, topic, result.getRecordMetadata().offset());
            } else {
                logger.error("Unable to send message=[{}] to topic=[{}] due to : {}", 
                            message, topic, exception.getMessage());
            }
        });
    }
}
