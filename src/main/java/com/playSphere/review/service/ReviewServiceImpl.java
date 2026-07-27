package com.playSphere.review.service;



import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.playSphere.review.dto.ReviewRequest;
import com.playSphere.review.entity.Review;
import com.playSphere.review.entity.ReviewType;
import com.playSphere.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository repository;

    @Override
    public Review addReview(ReviewRequest request) {

        Review review = repository
                .findByUserIdAndPropertyIdAndPropertyType(
                        request.getUserId(),
                        request.getPropertyId(),
                        request.getPropertyType()
                )
                .orElse(null);

        // If review already exists, update it
        if (review != null) {
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setUpdatedAt(LocalDateTime.now());

            return repository.save(review);
        }

        // Create new review
        Review newReview = Review.builder()
                .propertyId(request.getPropertyId())
                .propertyType(request.getPropertyType())
                .userId(request.getUserId())
                .userName(request.getUserName())
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repository.save(newReview);
    }

    @Override
    public List<Review> getReviews(String propertyId, ReviewType type) {

        return repository.findByPropertyIdAndPropertyType(propertyId, type);
    }

    @Override
    public List<Review> getUserReviews(String userId) {

        return repository.findByUserId(userId);
    }

    @Override
    public Review updateReview(String reviewId, ReviewRequest request) {

        Review review = repository.findById(reviewId)
                .orElseThrow(() ->
                        new RuntimeException("Review not found with id : " + reviewId));

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        return repository.save(review);
    }

    @Override
    public void deleteReview(String reviewId) {

        Review review = repository.findById(reviewId)
                .orElseThrow(() ->
                        new RuntimeException("Review not found with id : " + reviewId));

        repository.delete(review);
    }

    @Override
    public Double getAverageRating(String propertyId, ReviewType type) {

        List<Review> reviews =
                repository.findByPropertyIdAndPropertyType(propertyId, type);

        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
}
