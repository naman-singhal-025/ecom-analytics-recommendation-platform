package com.ecommerce.ecom_backend.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuration for Kafka consumers. Sets up Kafka consumer-side settings (i.e., how to receive and process messages from Kafka topics)
 */
@Configuration
@EnableKafka //Enables @KafkaListener support — allows Spring to detect and use @KafkaListener annotations anywhere in the app.
public class KafkaConsumerConfig {

    // Kafka bootstrap servers and consumer group ID are injected from application properties
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id:ecom-analytics-group}")
    private String groupId;
    
    /**
     * Creates a consumer factory for JSON messages.
     * 
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Use String deserializer for both keys and values
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // ErrorHandlingDeserializer is a Spring Kafka utility that wraps the actual deserializer (e.g., StringDeserializer)
        // It allows the consumer to gracefully handle deserialization errors (e.g., malformed JSON, type mismatches),
        // so that one bad message doesn't crash the entire consumer thread.
        // Failed messages can be logged, skipped, or sent to a dead-letter topic depending on your error handler setup.
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        
        // Auto-offset reset to earliest to ensure no messages are missed on restart
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    /**
     * Creates a Kafka listener container factory.
     * 
     * @return the Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Configure concurrency (number of threads) : Will run 3 threads for parallel consumption
        factory.setConcurrency(3);

        // Enable batch listening for more efficient processing. Listeners receive List<String> instead of one message at a time.
        factory.setBatchListener(true);

        // Allows the application to start even if the topic doesn't exist yet.
        factory.setMissingTopicsFatal(false); 

        // Add retry logic: retries up to 3 times with a fixed 2-second delay between attempts.
        // This handles failures during message processing (e.g., DB down, network issues).
        // After retries are exhausted, the message is skipped or handled by the error handler.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(2000L, 3));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}