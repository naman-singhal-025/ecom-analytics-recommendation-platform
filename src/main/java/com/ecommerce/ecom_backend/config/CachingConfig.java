package com.ecommerce.ecom_backend.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
 * @EnableCaching is a marker annotation that enables Spring's "annotation-driven" cache management capability.
 * @EnableCaching allows the use of caching annotations such as @Cacheable, @CachePut, and @CacheEvict.
 * @EnableCaching is typically placed on a configuration class to enable caching across the application.
 * If you need to customize caching behavior (like setting TTL, using different cache managers, etc.), you can do so in this class.
 * If you only use basic caching with @Cacheable annotations, you can keep it simple and just use @EnableCaching on your main application class.
 */

/*
 * Configuration for Redis caching.
 */
@Configuration
@EnableCaching
public class CachingConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    /**
     * Spring automatically calls this @Bean method at application startup to create and manage
     * a singleton RedisConnectionFactory instance. 
     * Spring registers the return value (RedisConnectionFactory) as a singleton bean in the Spring Application Context.
     * It will be injected wherever required.
     * Creates a Redis connection factory. Spring Data Redis uses this factory to create connections to the Redis server.
     * This configuration uses Lettuce as the Redis client.
     * Here return value is stored
     * @return the Redis connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }
    
    /**
     * Creates a Redis template for general-purpose operations.
     * bean is created at startup and injected wherever Redis operations are needed.
     * @param connectionFactory the Redis connection factory
     * @return the Redis template
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    /**
     * Creates a cache manager for Redis.
     * bean is created at startup and injected wherever Redis operations are needed.
     * @param connectionFactory the Redis connection factory
     * @return the cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL: 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
        
        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Products cache: 1 hour TTL
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Product by ID cache: 2 hours TTL
        cacheConfigurations.put("productById", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // Category products cache: 3 hours TTL
        cacheConfigurations.put("categoryProducts", defaultConfig.entryTtl(Duration.ofHours(3)));
        
        // Popular products cache: 15 minutes TTL (more frequent updates)
        cacheConfigurations.put("popularProducts", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Event summary cache: 5 minutes TTL (frequently changing)
        cacheConfigurations.put("eventSummary", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Build the cache manager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}