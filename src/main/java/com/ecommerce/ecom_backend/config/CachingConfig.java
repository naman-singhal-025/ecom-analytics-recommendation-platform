package com.ecommerce.ecom_backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/*
    Why separate caching configuration?
    - Flexibility: Allows you to customize caching settings without cluttering the main application class.
    - Scalability: As your application grows, you can easily add custom cache managers, serializers, or multiple caches.
    - Clarity: Keeps caching concerns separate from other configurations, making the codebase cleaner and easier to maintain.
    - Future-proofing: If you decide to add more complex caching logic later, having a dedicated config class makes it easier to implement without refactoring the main application class.
    - Best Practices: Following the principle of separation of concerns, which is a core tenet of software design.
    - Evolution: Caching needs often evolve. Starting with a separate config class prepares you for that evolution without needing to refactor your main application class later.
    - @EnableCaching is a marker annotation that enables Spring's annotation-driven cache management capability.
      It allows you to use annotations like @Cacheable, @CachePut, and @CacheEvict in your service methods.
      This is the first step in setting up caching in a Spring application.
    - @EnableCaching is typically placed on a configuration class to enable caching across the application.
    - This separation allows you to easily manage caching configurations independently of other application settings.
    - If you need to customize caching behavior (like setting TTL, using different cache managers, etc.), you can do so in this class.
    - If you only use basic caching with @Cacheable annotations, you can keep it simple and just use @EnableCaching on your main application class.
 */
/**
 * Configuration to enable caching.
 */
@Configuration
@EnableCaching
public class CachingConfig {
}