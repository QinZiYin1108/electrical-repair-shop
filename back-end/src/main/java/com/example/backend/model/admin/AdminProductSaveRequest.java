package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理员商品Save请求")
@Data
public class AdminProductSaveRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200个字符")
    @Schema(description = "名称")
    private String name;

    @NotBlank(message = "商品分类不能为空")
    @Schema(description = "分类ID")
    private String categoryId;

    @NotBlank(message = "商品品牌不能为空")
    @Size(max = 100, message = "商品品牌长度不能超过100个字符")
    @Schema(description = "品牌")
    private String brand;

    @NotBlank(message = "商品型号不能为空")
    @Size(max = 100, message = "商品型号长度不能超过100个字符")
    @Schema(description = "型号")
    private String model;

    @Size(max = 5000, message = "商品描述长度不能超过5000个字符")
    @Schema(description = "description")
    private String description;

    @Valid private List<AdminProductSpecItem> specifications = new ArrayList<>();

    @NotBlank(message = "主图地址不能为空")
    @Size(max = 500, message = "主图地址长度不能超过500个字符")
    @Schema(description = "主图URL")
    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    @NotNull(message = "原价不能为空")
    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @NotNull(message = "售价不能为空")
    @Schema(description = "售价")
    private BigDecimal sellingPrice;

    @NotNull(message = "成本价不能为空")
    @Schema(description = "成本价")
    private BigDecimal costPrice;

    @NotNull(message = "库存数量不能为空")
    @Schema(description = "库存数量")
    private Integer stockQuantity;

    @Schema(description = "预警库存")
    private Integer warningStock;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Size(max = 100, message = "尺寸长度不能超过100个字符")
    @Schema(description = "dimensions")
    private String dimensions;

    @Schema(description = "保修期")
    private Integer warrantyPeriod;

    @Schema(description = "运费")
    private BigDecimal shippingFee;

    @Schema(description = "是否包邮")
    private Integer isFreeShipping;

    @NotNull(message = "商品状态不能为空")
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否热销")
    private Integer isHot;

    @Schema(description = "是否新品")
    private Integer isNew;

    @Schema(description = "是否推荐")
    private Integer isRecommended;

    @Schema(description = "排序")
    private Integer sortOrder;
}
