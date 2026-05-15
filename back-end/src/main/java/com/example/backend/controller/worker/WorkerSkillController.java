package com.example.backend.controller.worker;

import com.example.backend.common.Result;
import com.example.backend.model.worker.WorkerSkillBatchCreateRequest;
import com.example.backend.model.worker.WorkerSkillCategoryNode;
import com.example.backend.model.worker.WorkerSkillCreateRequest;
import com.example.backend.model.worker.WorkerSkillDeleteRequest;
import com.example.backend.model.worker.WorkerSkillItem;
import com.example.backend.model.worker.WorkerSkillServiceTypeOption;
import com.example.backend.service.WorkerSkillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
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

    @GetMapping("/available-category-tree")
    public Result<List<WorkerSkillCategoryNode>> listAvailableCategoryTree(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "serviceMode", required = false) Integer serviceMode
    ) {
        return Result.success(workerSkillService.listAvailableCategoryTree(keyword, serviceMode));
    }

    @GetMapping("/available-service-types")
    public Result<List<WorkerSkillServiceTypeOption>> listAvailableServiceTypes(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
        @RequestParam(value = "categoryId", required = false) String categoryId
    ) {
        return Result.success(workerSkillService.listAvailableServiceTypes(keyword, serviceMode, categoryId));
    }

    @PostMapping("/add")
    public Result<Void> addCurrentWorkerSkill(@RequestBody WorkerSkillCreateRequest request) {
        workerSkillService.addCurrentWorkerSkill(request);
        return Result.success();
    }

    @PostMapping("/batch-add")
    public Result<Void> batchAddCurrentWorkerSkills(@RequestBody WorkerSkillBatchCreateRequest request) {
        workerSkillService.batchAddCurrentWorkerSkills(request);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> deleteCurrentWorkerSkill(@RequestBody WorkerSkillDeleteRequest request) {
        workerSkillService.deleteCurrentWorkerSkill(request);
        return Result.success();
    }
}
