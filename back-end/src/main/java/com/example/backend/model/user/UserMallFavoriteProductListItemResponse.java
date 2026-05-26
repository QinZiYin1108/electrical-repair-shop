package com.example.backend.model.user;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserMallFavoriteProductListItemResponse {

    private String id;

    private Integer productType;

    private String productTypeText;

    private String name;

    private String categoryId;

    private String categoryName;

    private String categoryPath;

    private String brand;

    private String model;

    private String mainImageUrl;

    private BigDecimal sellingPrice;

    private BigDecimal originalPrice;

    private Integer stockQuantity;

    private Integer salesCount;

    private Integer isFreeShipping;

    private Integer isHot;

    private Integer isNew;

    private Integer isRecommended;

    private Long favoriteTime;
}
