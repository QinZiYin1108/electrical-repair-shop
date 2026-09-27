package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "用户商城商品列表Item响应")
@Data
public class UserMallProductListItemResponse {

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

    @Schema(description = "主图URL")
    private String mainImageUrl;

    @Schema(description = "售价")
    private BigDecimal sellingPrice;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存数量")
    private Integer stockQuantity;

    @Schema(description = "销量")
    private Integer salesCount;

    @Schema(description = "是否包邮")
    private Integer isFreeShipping;

    @Schema(description = "是否热销")
    private Integer isHot;

    @Schema(description = "是否新品")
    private Integer isNew;

    @Schema(description = "是否推荐")
    private Integer isRecommended;

    @Schema(description = "所属门店ID")
    private String storeId;
}
