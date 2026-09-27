package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员商品响应")
@Data
public class AdminProductResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "商品编号")
    private String productNo;

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

    private List<AdminProductSpecItem> specifications = new ArrayList<>();

    @Schema(description = "主图URL")
    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "售价")
    private BigDecimal sellingPrice;

    @Schema(description = "成本价")
    private BigDecimal costPrice;

    @Schema(description = "库存数量")
    private Integer stockQuantity;

    @Schema(description = "预警库存")
    private Integer warningStock;

    @Schema(description = "销量")
    private Integer salesCount;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "收藏量")
    private Integer favoriteCount;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Schema(description = "dimensions")
    private String dimensions;

    @Schema(description = "保修期")
    private Integer warrantyPeriod;

    @Schema(description = "运费")
    private BigDecimal shippingFee;

    @Schema(description = "是否包邮")
    private Integer isFreeShipping;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "是否热销")
    private Integer isHot;

    @Schema(description = "是否新品")
    private Integer isNew;

    @Schema(description = "是否推荐")
    private Integer isRecommended;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;
}
