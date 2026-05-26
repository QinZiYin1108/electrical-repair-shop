package com.example.backend.service;

import com.example.backend.model.worker.WorkerSkillBatchCreateRequest;
import com.example.backend.model.worker.WorkerSkillCategoryNode;
import com.example.backend.model.worker.WorkerSkillCreateRequest;
import com.example.backend.model.worker.WorkerSkillDeleteRequest;
import com.example.backend.model.worker.WorkerSkillItem;
import com.example.backend.model.worker.WorkerSkillServiceTypeOption;

import java.util.List;

public interface WorkerSkillService {

    List<WorkerSkillItem> listCurrentWorkerSkills();

    List<WorkerSkillCategoryNode> listAvailableCategoryTree(String keyword, Integer serviceMode);

    List<WorkerSkillServiceTypeOption> listAvailableServiceTypes(String keyword, Integer serviceMode, String categoryId);

    void addCurrentWorkerSkill(WorkerSkillCreateRequest request);

    void batchAddCurrentWorkerSkills(WorkerSkillBatchCreateRequest request);

    void deleteCurrentWorkerSkill(WorkerSkillDeleteRequest request);
}
