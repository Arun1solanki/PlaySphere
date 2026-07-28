package com.playSphere.review.service;

import com.playSphere.review.dto.ReviewRequest;
import com.playSphere.review.entity.Review;
import com.playSphere.review.entity.ReviewType;

import java.util.*;
public interface ReviewService {

    Review addReview(ReviewRequest request);

    List<Review> getReviews(String propertyId, ReviewType type);

    List<Review> getUserReviews(String userId);

    Review updateReview(String reviewId,ReviewRequest request);

    void deleteReview(String reviewId);

    Double getAverageRating(String propertyId,ReviewType type);

}
