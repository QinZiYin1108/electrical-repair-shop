package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerSkillBatchCreateRequest;
import com.example.backend.model.worker.WorkerSkillCategoryNode;
import com.example.backend.model.worker.WorkerSkillCreateRequest;
import com.example.backend.model.worker.WorkerSkillDeleteRequest;
import com.example.backend.model.worker.WorkerSkillItem;
import com.example.backend.model.worker.WorkerSkillServiceTypeOption;
import com.example.backend.service.WorkerSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "师傅端/技能管理")
@RequestMapping("/worker/skills")
public class WorkerSkillController {

    private final WorkerSkillService workerSkillService;

    public WorkerSkillController(WorkerSkillService workerSkillService) {
        this.workerSkillService = workerSkillService;
    }

    @GetMapping
    public Result<List<WorkerSkillItem>> listCurrentWorkerSkills() {
        return Result.success(workerSkillService.listCurrentWorkerSkills());
    }

    @Operation(summary = "查询AvailableCategoryTree")
    @GetMapping("/available-category-tree")
    public Result<List<WorkerSkillCategoryNode>> listAvailableCategoryTree(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "serviceMode", required = false) Integer serviceMode) {
        return Result.success(workerSkillService.listAvailableCategoryTree(keyword, serviceMode));
    }

    @Operation(summary = "查询AvailableServiceTypes")
    @GetMapping("/available-service-types")
    public Result<List<WorkerSkillServiceTypeOption>> listAvailableServiceTypes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
            @RequestParam(value = "categoryId", required = false) String categoryId) {
        return Result.success(
                workerSkillService.listAvailableServiceTypes(keyword, serviceMode, categoryId));
    }

    @Operation(summary = "创建addCurrentWorkerSkill")
    @PostMapping("/add")
    public Result<Void> addCurrentWorkerSkill(@RequestBody WorkerSkillCreateRequest request) {
        workerSkillService.addCurrentWorkerSkill(request);
        return Result.success();
    }

    @Operation(summary = "创建批量AddCurrentWorker技能列表")
    @PostMapping("/batch-add")
    public Result<Void> batchAddCurrentWorkerSkills(
            @RequestBody WorkerSkillBatchCreateRequest request) {
        workerSkillService.batchAddCurrentWorkerSkills(request);
        return Result.success();
    }

    @Operation(summary = "删除删除CurrentWorkerSkill")
    @PostMapping("/delete")
    public Result<Void> deleteCurrentWorkerSkill(@RequestBody WorkerSkillDeleteRequest request) {
        workerSkillService.deleteCurrentWorkerSkill(request);
        return Result.success();
    }
}
