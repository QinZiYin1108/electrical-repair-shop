package com.example.backend.model.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminProductSaveRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200个字符")
    private String name;

    @NotBlank(message = "商品分类不能为空")
    private String categoryId;

    @NotBlank(message = "商品品牌不能为空")
    @Size(max = 100, message = "商品品牌长度不能超过100个字符")
    private String brand;

    @NotBlank(message = "商品型号不能为空")
    @Size(max = 100, message = "商品型号长度不能超过100个字符")
    private String model;

    @Size(max = 5000, message = "商品描述长度不能超过5000个字符")
    private String description;

    @Valid
    private List<AdminProductSpecItem> specifications = new ArrayList<>();

    @NotBlank(message = "主图地址不能为空")
    @Size(max = 500, message = "主图地址长度不能超过500个字符")
    private String mainImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private List<String> videoUrls = new ArrayList<>();

    @NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;

    @NotNull(message = "售价不能为空")
    private BigDecimal sellingPrice;

    @NotNull(message = "成本价不能为空")
    private BigDecimal costPrice;

    @NotNull(message = "库存数量不能为空")
    private Integer stockQuantity;

    private Integer warningStock;

    private BigDecimal weight;

    @Size(max = 100, message = "尺寸长度不能超过100个字符")
    private String dimensions;

    private Integer warrantyPeriod;

    private BigDecimal shippingFee;

    private Integer isFreeShipping;

    @NotNull(message = "商品状态不能为空")
    private Integer status;

    private Integer isHot;

    private Integer isNew;

    private Integer isRecommended;

    private Integer sortOrder;
}
