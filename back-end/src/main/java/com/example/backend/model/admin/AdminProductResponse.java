package com.example.backend.model.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminProductResponse {

    private String id;

    private String productNo;

    private Integer productType;

    private String productTypeText;

    private String name;

    private String categoryId;

    private String categoryName;

    private String categoryPath;

    private String brand;

    private String model;

    private String description;

    private List<AdminProductSpecItem> specifications = new ArrayList<>();

    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    private BigDecimal originalPrice;

    private BigDecimal sellingPrice;

    private BigDecimal costPrice;

    private Integer stockQuantity;

    private Integer warningStock;

    private Integer salesCount;

    private Integer viewCount;

    private Integer favoriteCount;

    private BigDecimal weight;

    private String dimensions;

    private Integer warrantyPeriod;

    private BigDecimal shippingFee;

    private Integer isFreeShipping;

    private Integer status;

    private String statusText;

    private Integer isHot;

    private Integer isNew;

    private Integer isRecommended;

    private Integer sortOrder;

    private Long createdTime;

    private Long updatedTime;
}
