package com.playSphere.review.dto;

import com.playSphere.review.entity.ReviewType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank
    private String propertyId;

    @NotNull
    private ReviewType propertyType;

    @NotBlank
    private String userId;

    @NotBlank
    private String userName;

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;

}
