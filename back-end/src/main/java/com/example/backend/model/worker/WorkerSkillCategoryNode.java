package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "师傅技能分类节点")
@Data
public class WorkerSkillCategoryNode {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "父级ID")
    private String parentId;

    private List<WorkerSkillCategoryNode> children = new ArrayList<>();
}
