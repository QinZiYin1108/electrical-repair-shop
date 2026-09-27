package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 图片审核队列 @TableName image_review_queue */
@TableName(value = "image_review_queue")
@Data
public class ImageReviewQueue {

    /** 主键，IRQ+雪花ID */
    @TableId private String id;

    /** 关联 images.id */
    private String imageId;

    /** 业务类型：AVATAR/STORE/GOODS/CASE */
    private String businessType;

    /** 业务ID */
    private String businessId;

    /** 审核状态：1-待审核，2-审核通过，3-审核拒绝，4-存疑放行（待复审） */
    private Integer status;

    /** 审核人ID */
    private String reviewerId;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审核时间戳 */
    private Long reviewTime;

    /** 创建时间戳 */
    private Long createdTime;

    /** 更新时间戳 */
    private Long updatedTime;

    /** 乐观锁版本号 */
    @Version private Integer version;

    /** 逻辑删除 */
    @TableLogic private Integer isDelete;
}
