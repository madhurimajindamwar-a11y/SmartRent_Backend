package com.smartrent.controller;

import com.smartrent.dto.ReviewRequest;
import com.smartrent.service.PropertyReviewService;
import com.smartrent.service.PropertyReviewService.MyReview;
import com.smartrent.service.PropertyReviewService.ReviewSummary;
import com.smartrent.service.PropertyReviewService.ReviewView;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PropertyReviewController {

    private final PropertyReviewService reviewService;

    public PropertyReviewController(PropertyReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/properties/{propertyId}/reviews")
    public ReviewSummary getReviews(
            @PathVariable("propertyId") Long propertyId) {

        return reviewService.getReviews(propertyId);
    }

    @GetMapping("/reviews/properties/{propertyId}/mine")
    public MyReview getMyReview(
            @PathVariable("propertyId") Long propertyId) {

        return reviewService.getMyReview(propertyId);
    }

    @PutMapping("/reviews/properties/{propertyId}")
    public ReviewView saveReview(
            @PathVariable("propertyId") Long propertyId,
            @Valid @RequestBody ReviewRequest request) {

        return reviewService.saveReview(propertyId, request);
    }
}