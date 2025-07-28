package com.ecommerce.ecom_backend.repo.elasticsearch;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.ecom_backend.model.UserEvent;

/**
 * Spring Data repository for {@link UserEvent} documents in Elasticsearch.
 */
@Repository
public interface UserEventSearchRepository extends ElasticsearchRepository<UserEvent, String> {
    
    /**
     * Count events by type.
     * 
     * @param eventType the event type
     * @return count of events with the given type
     */
    long countByEventType(String eventType);
    
    /**
     * Find events by type.
     * 
     * @param eventType the event type
     * @return list of events with the given type
     */
    List<UserEvent> findByEventType(String eventType);
    
    /**
     * Find events by product ID.
     * 
     * @param productId the product ID
     * @return list of events for the given product
     */
    List<UserEvent> findByProductId(String productId);
    
    /**
     * Find events by category.
     * 
     * @param category the category
     * @return list of events for the given category
     */
    List<UserEvent> findByCategory(String category);
    
    /**
     * Find events by user ID.
     * 
     * @param userId the user ID
     * @return list of events for the given user
     */
    List<UserEvent> findByUserId(String userId);
    
    /**
     * Find events by type and product ID.
     * 
     * @param eventType the event type
     * @param productId the product ID
     * @return list of events with the given type for the given product
     */
    List<UserEvent> findByEventTypeAndProductId(String eventType, String productId);
    
    /**
     * Find events by type and category.
     * 
     * @param eventType the event type
     * @param category the category
     * @return list of events with the given type for the given category
     */
    List<UserEvent> findByEventTypeAndCategory(String eventType, String category);
    
    /**
     * Find events by timestamp range.
     * 
     * @param start the start timestamp
     * @param end the end timestamp
     * @return list of events within the given timestamp range
     */
    List<UserEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find events by type and timestamp range.
     * 
     * @param eventType the event type
     * @param start the start timestamp
     * @param end the end timestamp
     * @return list of events with the given type within the given timestamp range
     */
    List<UserEvent> findByEventTypeAndTimestampBetween(String eventType, LocalDateTime start, LocalDateTime end);
    
    /**
     * Find top products by view count.
     * 
     * @param limit the maximum number of results to return
     * @return list of product IDs and their view counts as raw JSON string not parsed as objects
     * 
     * This query filters events by eventType = "VIEW", then groups them by productId.
     * For each productId, it counts the number of documents (i.e., view events)
     * using value_count on the _id (internal document ID) field. The top N products by view count are returned
     * via aggregation, not as full documents. Default sorting is done in descending order.
     */
    @Query("{\"size\":0,\"query\":{\"term\":{\"eventType\":\"VIEW\"}},\"aggs\":{\"top_products\":{\"terms\":{\"field\":\"productId\",\"size\":?0},\"aggs\":{\"event_count\":{\"value_count\":{\"field\":\"_id\"}}}}}}")
    String findTopViewedProducts(int limit);
    
    /**
     * Find top products by purchase count.
     * 
     * @param limit the maximum number of results to return
     * @return list of product IDs and their purchase counts
     * 
     * Json query looks like this:
     * {
     *   "size": 0, // set size to 0 to only return aggregation results not documents
     *   "query": {
     *     "term": {
     *       "eventType": "PURCHASE"
     *     }
     *   },
     *   "aggs": {
     *     "top_products": { // top_products is the name of the aggregation
     *       "terms": {
     *         "field": "productId",
     *         "size": ?0 // ?0 is a placeholder for the limit parameter
     *       },
     *       "aggs": {
     *         "event_count": {
     *           "value_count": {
     *             "field": "_id"
     *           }
     *        }
     *      }
     *    }
     *   }
     * 
     * Json respose looks like this:
     * {
     *   "aggregations": {
     *     "top_products": { // top_products is the name of the aggregation
     *       "buckets": [
     *         {
     *           "key": "product123",
     *           "doc_count": 100,  // doc_count is automatically added by ElasticSearch for each bucket
     *           "event_count": {
     *             "value": 100
     *           }
     *         },
     *         {
     *           "key": "product456",
     *           "doc_count": 80,
     *           "event_count": {
     *             "value": 80
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     */
    @Query("{\"size\":0,\"query\":{\"term\":{\"eventType\":\"PURCHASE\"}},\"aggs\":{\"top_products\":{\"terms\":{\"field\":\"productId\",\"size\":?0},\"aggs\":{\"event_count\":{\"value_count\":{\"field\":\"_id\"}}}}}}")
    String findTopPurchasedProducts(int limit);
    
    /**
     * Find top categories by event count.
     * 
     * @param limit the maximum number of results to return
     * @return list of categories and their event counts
     * 
     * Json query looks like this:
     * {
     *   "size": 0, // set size to 0 to only return aggregation results not documents
     *   "aggs": {
     *     "top_categories": { // top_categories is the name of the aggregation
     *       "terms": {
     *         "field": "category",
     *        "size": ?0
     *      },
     *      "aggs": {
     *        "event_count": {
     *         "value_count": {
     *          "field": "_id"
     *         }
     *        }
     *       }
     *     }
     *    }
     * }
     */
    @Query("{\"size\":0,\"aggs\":{\"top_categories\":{\"terms\":{\"field\":\"category\",\"size\":?0},\"aggs\":{\"event_count\":{\"value_count\":{\"field\":\"_id\"}}}}}}")
    String findTopCategories(int limit);
    
    /**
     * Find trending products based on recent events.
     * 
     * @param hours the number of hours to look back
     * @param limit the maximum number of results to return
     * @return list of product IDs and their event counts
     */
    @Query("{\"size\":0,\"query\":{\"range\":{\"timestamp\":{\"gte\":\"now-?0h\"}}},\"aggs\":{\"trending_products\":{\"terms\":{\"field\":\"productId\",\"size\":?1},\"aggs\":{\"event_count\":{\"value_count\":{\"field\":\"_id\"}}}}}}")
    String findTrendingProducts(int hours, int limit);
    
    /**
     * Find trending categories based on recent events.
     * 
     * @param hours the number of hours to look back
     * @param limit the maximum number of results to return
     * @return list of categories and their event counts
     */
    @Query("{\"size\":0,\"query\":{\"range\":{\"timestamp\":{\"gte\":\"now-?0h\"}}},\"aggs\":{\"trending_categories\":{\"terms\":{\"field\":\"category\",\"size\":?1},\"aggs\":{\"event_count\":{\"value_count\":{\"field\":\"_id\"}}}}}}")
    String findTrendingCategories(int hours, int limit);
    
    /**
     * Find conversion rate (view to purchase) for products.
     * 
     * @param limit the maximum number of results to return
     * @return list of product IDs and their conversion rates
     * 
     * Groups all events by productId
     * Within each product bucket, filters "VIEW" and "PURCHASE" events separately
     * Counts each using value_count on the _id field
     * Returns aggregated counts for both event types, so the conversion rate can be calculated in Java as purchases/views
     * 
     * Json query looks like this:
     * {
     *   "size": 0, // set size to 0 to only return aggregation results not documents
     *   "aggs": {
     *     "products": { // products is the name of the aggregation
     *       "terms": {
     *         "field": "productId",
     *         "size": ?0 // ?0 is a placeholder for the limit parameter
     *       },
     *       "aggs": {
     *         "views": {
     *           "filter": {
     *             "term": {
     *               "eventType": "VIEW"
     *             }
     *           },
     *           "aggs": {
     *             "count": {
     *               "value_count": {
     *                 "field": "_id"
     *               }
     *             }
     *           }
     *         },
     *         "purchases": {
     *           "filter": {
     *             "term": {
     *               "eventType": "PURCHASE"
     *             }
     *           },
     *           "aggs": {
     *             "count": {
     *               "value_count": {
     *                 "field": "_id"
     *               }
     *             }
     *           }
     *         }
     *       }
     *     }
     *   }
     * }
     * 
     * Json response looks like this:
     * {
     *   "aggregations": {
     *     "products": { // products is the name of the aggregation
     *       "buckets": [
     *         {
     *           "key": "product123",
     *           "doc_count": 100, // total number of events for this product
     *           "views": {
     *             "doc_count": 80, // number of view events
     *             "count": {
     *               "value": 80 // count of view events
     *             }
     *           },
     *           "purchases": {
     *             "doc_count": 20, // number of purchase events
     *             "count": {
     *               "value": 20 // count of purchase events
     *             }
     *           }
     *         },
     *         {
     *           "key": "product456",
     *           "doc_count": 50,
     *           "views": {
     *             "doc_count": 30,
     *             "count": {
     *               "value": 30
     *             }
     *           },
     *           "purchases": {
     *             "doc_count": 10,
     *             "count": {
     *               "value": 10
     *             }
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     * 
     * In above respose we see that for product456, there were 30 views and 10 purchases, which doesn't add up to 50
     * because the doc_count is the total number of events for that product, which includes all event types (VIEW, CLICK, PURCHASE, etc.)
     * while view doc_count and purchase doc_count are specific to those event types.
     */
    @Query("{\"size\":0,\"aggs\":{\"products\":{\"terms\":{\"field\":\"productId\",\"size\":?0},\"aggs\":{\"views\":{\"filter\":{\"term\":{\"eventType\":\"VIEW\"}},\"aggs\":{\"count\":{\"value_count\":{\"field\":\"_id\"}}}},\"purchases\":{\"filter\":{\"term\":{\"eventType\":\"PURCHASE\"}},\"aggs\":{\"count\":{\"value_count\":{\"field\":\"_id\"}}}}}}}}}")
    String findProductConversionRates(int limit);
}