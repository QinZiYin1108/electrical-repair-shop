package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;

@TableName(value = "order_items")
@Data
public class OrderItems {

    @TableId private String id;

    private String orderId;

    private String productId;

    private String productName;

    private String productImage;

    private BigDecimal productPrice;

    private Integer quantity;

    private BigDecimal totalPrice;

    private Long createdTime;

    @Version private Integer version;

    @TableLogic private Integer isDelete;
}
