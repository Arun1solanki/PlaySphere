package com.playSphere.review.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;

    private String propertyId;

    private ReviewType propertyType;

    private String userId;

    private String userName;

    private String comment;

    private Integer rating;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
