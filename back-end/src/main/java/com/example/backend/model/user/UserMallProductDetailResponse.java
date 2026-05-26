package com.example.backend.model.user;

import com.example.backend.model.review.ReviewItemResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserMallProductDetailResponse {

    private String id;

    private Integer productType;

    private String productTypeText;

    private String name;

    private String categoryId;

    private String categoryName;

    private String categoryPath;

    private String brand;

    private String model;

    private String description;

    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    private List<String> galleryUrls = new ArrayList<>();

    private List<UserMallProductSpecItem> specifications = new ArrayList<>();

    private BigDecimal sellingPrice;

    private BigDecimal originalPrice;

    private Integer stockQuantity;

    private Integer warrantyPeriod;

    private Integer isHot;

    private Integer isNew;

    private Integer isRecommended;

    private Integer favoriteCount;

    private Boolean isFavorite;

    private Integer reviewCount;

    private BigDecimal reviewRating;

    private List<ReviewItemResponse> reviews = new ArrayList<>();
}
