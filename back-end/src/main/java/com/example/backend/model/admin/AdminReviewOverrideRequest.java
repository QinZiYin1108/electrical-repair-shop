package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 超管改判请求 */
@Schema(description = "管理员评价Override请求")
public class AdminReviewOverrideRequest {

    /** 改判为：2-通过，3-拒绝 */
    @NotNull(message = "改判结果不能为空")
    @Schema(description = "新状态")
    private Integer newStatus;

    /** 改判备注 */
    @Schema(description = "备注")
    private String remark;

    // ==================== getter / setter ====================

    public Integer getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Integer newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
