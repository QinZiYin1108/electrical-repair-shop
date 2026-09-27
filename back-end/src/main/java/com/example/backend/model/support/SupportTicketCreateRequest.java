package com.example.backend.model.support;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "客服工单创建请求")
@Data
public class SupportTicketCreateRequest {

    @Schema(description = "工单分类")
    private String category;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "关联业务类型：REPAIR_ORDER/PRODUCT_ORDER/PAYMENT/REFUND/AFTER_SALES/ACCOUNT")
    private String bizType;

    @Schema(description = "关联业务ID")
    private String bizId;

    @Schema(description = "关联用户账号ID")
    private String relatedUserAccountId;

    @Schema(description = "关联师傅账号ID")
    private String relatedTechnicianAccountId;

    @Schema(description = "优先级：1-高，2-中，3-低")
    private Integer priority;

    @Schema(description = "来源：1-后台创建，2-用户提交，3-系统")
    private Integer source;

    @Schema(description = "关联金额（用于阈值审批）")
    private BigDecimal amount;

    @Schema(description = "处理时限（时间戳，可空）")
    private Long dueTime;
}
