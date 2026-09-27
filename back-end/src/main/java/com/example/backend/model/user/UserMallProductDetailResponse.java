package com.example.backend.model.user;

import com.example.backend.model.review.ReviewItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "用户商城商品详情响应")
@Data
public class UserMallProductDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商品类型")
    private Integer productType;

    @Schema(description = "商品类型Text")
    private String productTypeText;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类路径")
    private String categoryPath;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "description")
    private String description;

    @Schema(description = "主图URL")
    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    private List<String> galleryUrls = new ArrayList<>();

    private List<UserMallProductSpecItem> specifications = new ArrayList<>();

    @Schema(description = "售价")
    private BigDecimal sellingPrice;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存数量")
    private Integer stockQuantity;

    @Schema(description = "保修期")
    private Integer warrantyPeriod;

    @Schema(description = "是否热销")
    private Integer isHot;

    @Schema(description = "是否新品")
    private Integer isNew;

    @Schema(description = "是否推荐")
    private Integer isRecommended;

    @Schema(description = "收藏量")
    private Integer favoriteCount;

    @Schema(description = "is收藏量")
    private Boolean isFavorite;

    @Schema(description = "评价数量")
    private Integer reviewCount;

    @Schema(description = "评价评分")
    private BigDecimal reviewRating;

    private List<ReviewItemResponse> reviews = new ArrayList<>();

    @Schema(description = "履约方式：1-自取，2-送货上门")
    private Integer fulfillmentType;

    @Schema(description = "所属门店ID")
    private String storeId;

    @Schema(description = "所属门店名称")
    private String storeName;
}
