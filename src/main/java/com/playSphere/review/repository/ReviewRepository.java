package com.playSphere.review.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.playSphere.review.entity.Review;
import com.playSphere.review.entity.ReviewType;
import java.util.*;

@Repository
public interface ReviewRepository extends MongoRepository<Review,String>{

    List<Review> findByPropertyIdAndPropertyType(
            String propertyId,
            ReviewType propertyType
    );

    List<Review> findByUserId(String userId);

    Optional<Review> findByUserIdAndPropertyIdAndPropertyType(
            String userId,
            String propertyId,
            ReviewType propertyType
    );

}
