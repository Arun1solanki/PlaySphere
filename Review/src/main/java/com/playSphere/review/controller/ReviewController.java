package com.playSphere.review.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.playSphere.review.dto.ReviewRequest;
import com.playSphere.review.entity.ReviewType;
import com.playSphere.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @PostMapping
    public ResponseEntity<?> addReview(
            @RequestBody @Valid ReviewRequest request){

        return ResponseEntity.ok(service.addReview(request));
    }

    @GetMapping("/{type}/{propertyId}")

    public ResponseEntity<?> getReviews(

            @PathVariable ReviewType type,

            @PathVariable String propertyId){

        return ResponseEntity.ok(
                service.getReviews(propertyId,type));
    }

    @GetMapping("/user/{userId}")

    public ResponseEntity<?> getUserReviews(

            @PathVariable String userId){

        return ResponseEntity.ok(
                service.getUserReviews(userId));
    }

    @PutMapping("/{reviewId}")

    public ResponseEntity<?> update(

            @PathVariable String reviewId,

            @RequestBody ReviewRequest request){

        return ResponseEntity.ok(
                service.updateReview(reviewId,request));
    }

    @DeleteMapping("/{reviewId}")

    public void delete(

            @PathVariable String reviewId){

        service.deleteReview(reviewId);

    }

    @GetMapping("/{type}/{propertyId}/average")

    public Double average(

            @PathVariable ReviewType type,

            @PathVariable String propertyId){

        return service.getAverageRating(propertyId,type);

    }

}