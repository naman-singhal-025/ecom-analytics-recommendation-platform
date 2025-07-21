package com.ecommerce.ecom_backend.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.ecom_backend.repo.elasticsearch.UserEventSearchRepository;

/**
 * Service for providing analytics data from Elasticsearch.
 */
@Service
public class AnalyticsService {

    @Autowired
    private UserEventSearchRepository userEventSearchRepository;

    /**
     * Gets the total count of all events.
     * @return The total number of events.
     */
    public long getTotalEvents() {
        return userEventSearchRepository.count();
    }

    /**
     * Gets a summary of event counts by type.
     * @return A map with event types as keys and their counts as values.
     */
    public Map<String, Long> getEventSummary() {
        return Map.of(
            "totalEvents", getTotalEvents(),
            "viewEvents", userEventSearchRepository.countByEventType("VIEW"),
            "clickEvents", userEventSearchRepository.countByEventType("CLICK"),
            "cartEvents", userEventSearchRepository.countByEventType("ADD_TO_CART"),
            "purchaseEvents", userEventSearchRepository.countByEventType("PURCHASE")
        );
    }
}