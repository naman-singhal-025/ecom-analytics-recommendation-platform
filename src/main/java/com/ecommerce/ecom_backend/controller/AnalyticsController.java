package com.ecommerce.ecom_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.services.AnalyticsService;

/**
 * Controller for serving analytics data.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Gets the total number of events.
     * @return A response entity with the total event count.
     */
    @GetMapping("/total-events")
    public ResponseEntity<Long> getTotalEvents() {
        return ResponseEntity.ok(analyticsService.getTotalEvents());
    }

    /**
     * Gets a cached summary of event counts by type from elastic search repository.
     * @return A response entity with the event summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getEventSummary() {
        return ResponseEntity.ok(analyticsService.getEventSummary());
    }

    /**
     * Gets a summary of event counts by type from elastic search repository (non-cached). This is a development mode endpoint.
     * @return A response entity with the event summary.
     */
    @GetMapping("/summary-dev")
    public ResponseEntity<Map<String, Long>> getEventSummaryDevMode() {
        return ResponseEntity.ok(analyticsService.getEventSummaryDevMode());
    }
}