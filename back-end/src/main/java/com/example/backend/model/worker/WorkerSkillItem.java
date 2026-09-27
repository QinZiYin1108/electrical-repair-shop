package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "师傅技能列表项")
@Data
public class WorkerSkillItem {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务Mode")
    private Integer serviceMode;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类路径")
    private String categoryPath;

    @Schema(description = "技能级别")
    private Integer skillLevel;

    @Schema(description = "技能级别Text")
    private String skillLevelText;

    @Schema(description = "is是否启用")
    private Integer isActive;
}
