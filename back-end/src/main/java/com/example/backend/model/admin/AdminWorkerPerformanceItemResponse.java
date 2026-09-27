package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理员师傅绩效Item响应")
@Data
public class AdminWorkerPerformanceItemResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "username")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "账号状态")
    private Integer accountStatus;

    @Schema(description = "工作状态")
    private Integer workStatus;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "评价数量")
    private Integer reviewCount;

    @Schema(description = "总数Orders")
    private Long totalOrders;

    @Schema(description = "waitingOrders")
    private Long waitingOrders;

    @Schema(description = "ongoingOrders")
    private Long ongoingOrders;

    @Schema(description = "waitingPayOrders")
    private Long waitingPayOrders;

    @Schema(description = "pendingOrders")
    private Long pendingOrders;

    @Schema(description = "completedOrders")
    private Long completedOrders;

    @Schema(description = "canceledOrders")
    private Long canceledOrders;

    @Schema(description = "refundedOrders")
    private Long refundedOrders;

    @Schema(description = "完成率")
    private BigDecimal completionRate;

    @Schema(description = "grossIncome")
    private BigDecimal grossIncome;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "netIncome")
    private BigDecimal netIncome;

    @Schema(description = "average排序金额")
    private BigDecimal averageOrderAmount;

    @Schema(description = "服务Hours")
    private BigDecimal serviceHours;

    @Schema(description = "latestCompleted时间")
    private Long latestCompletedTime;

    @Schema(description = "创建时间")
    private Long createdTime;
}
