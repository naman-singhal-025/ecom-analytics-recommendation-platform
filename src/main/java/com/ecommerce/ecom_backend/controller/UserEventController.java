package com.ecommerce.ecom_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecom_backend.model.UserEvent;
import com.ecommerce.ecom_backend.services.UserBehaviorService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/events")
public class UserEventController {

    @Autowired
    private UserBehaviorService userBehaviorService;

    @PostMapping("/track")
    public ResponseEntity<UserEvent> trackEvent(@RequestBody UserEvent event, HttpServletRequest request) {
        UserEvent trackedEvent = userBehaviorService.trackUserEvent(
            event.getUserId(),
            event.getSessionId(),
            event.getEventType(),
            event.getProductId(),
            event.getCategory(),
            event.getMetadata(),
            request
        );
        return new ResponseEntity<>(trackedEvent, HttpStatus.CREATED);
    }
}