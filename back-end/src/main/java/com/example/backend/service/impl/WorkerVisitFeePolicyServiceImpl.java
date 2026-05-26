package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.TechnicianVisitFeePolicies;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerVisitFeePoliciesUpdateRequest;
import com.example.backend.model.worker.WorkerVisitFeePolicyItem;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianVisitFeePoliciesService;
import com.example.backend.service.WorkerVisitFeePolicyService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkerVisitFeePolicyServiceImpl implements WorkerVisitFeePolicyService {

    private final TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService;

    public WorkerVisitFeePolicyServiceImpl(TechnicianVisitFeePoliciesService technicianVisitFeePoliciesService) {
        this.technicianVisitFeePoliciesService = technicianVisitFeePoliciesService;
    }

    @Override
    public List<WorkerVisitFeePolicyItem> getCurrentWorkerPolicies() {
        String accountId = requireWorker().getAccountId();
        List<TechnicianVisitFeePolicies> allPolicyList = technicianVisitFeePoliciesService.list(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, accountId)
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByAsc(TechnicianVisitFeePolicies::getServiceKind)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
        );

        Map<Integer, TechnicianVisitFeePolicies> latestPolicyMap = new LinkedHashMap<>();
        for (TechnicianVisitFeePolicies policy : allPolicyList) {
            Integer serviceKind = policy.getServiceKind();
            if (serviceKind == null || (serviceKind != 1 && serviceKind != 2)) {
                continue;
            }
            if (!latestPolicyMap.containsKey(serviceKind)) {
                latestPolicyMap.put(serviceKind, policy);
            }
        }

        List<WorkerVisitFeePolicyItem> responseList = new ArrayList<>();
        responseList.add(toResponseItem(latestPolicyMap.get(1), 1));
        responseList.add(toResponseItem(latestPolicyMap.get(2), 2));
        return responseList;
    }

    @Override
    public void updateCurrentWorkerPolicies(WorkerVisitFeePoliciesUpdateRequest request) {
        String accountId = requireWorker().getAccountId();
        if (request == null || request.getPolicies() == null || request.getPolicies().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "计费策略不能为空");
        }

        Set<Integer> serviceKindSet = new HashSet<>();
        for (WorkerVisitFeePolicyItem item : request.getPolicies()) {
            validatePolicyItem(item);
            if (!serviceKindSet.add(item.getServiceKind())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "同一服务类型不能重复提交");
            }
        }

        long now = System.currentTimeMillis();
        for (WorkerVisitFeePolicyItem item : request.getPolicies()) {
            TechnicianVisitFeePolicies policy = findLatestPolicy(accountId, item.getServiceKind());
            boolean isNew = policy == null;
            if (isNew) {
                policy = new TechnicianVisitFeePolicies();
                policy.setId(SnowflakeIdUtil.nextTechnicianVisitFeePolicyId());
                policy.setTechnicianAccountId(accountId);
                policy.setServiceKind(item.getServiceKind());
                policy.setEffectiveTime(now);
                policy.setCreatedTime(now);
                policy.setIsDelete(0);
            }

            policy.setMinVisitFee(item.getMinVisitFee());
            policy.setBaseRadiusKm(item.getBaseRadiusKm());
            policy.setExtraFeePerKm(item.getExtraFeePerKm());
            policy.setDistanceCalcType(item.getDistanceCalcType());
            policy.setRoundingRule(item.getRoundingRule());
            policy.setMaxVisitFee(item.getMaxVisitFee());
            policy.setIsActive(item.getIsActive() != null && item.getIsActive() == 0 ? 0 : 1);
            policy.setUpdatedTime(now);
            if (policy.getEffectiveTime() == null) {
                policy.setEffectiveTime(now);
            }

            if (isNew) {
                technicianVisitFeePoliciesService.save(policy);
            } else {
                technicianVisitFeePoliciesService.updateById(policy);
            }
        }
    }

    private TechnicianVisitFeePolicies findLatestPolicy(String accountId, Integer serviceKind) {
        return technicianVisitFeePoliciesService.getOne(
            new LambdaQueryWrapper<TechnicianVisitFeePolicies>()
                .eq(TechnicianVisitFeePolicies::getTechnicianAccountId, accountId)
                .eq(TechnicianVisitFeePolicies::getServiceKind, serviceKind)
                .eq(TechnicianVisitFeePolicies::getIsDelete, 0)
                .orderByDesc(TechnicianVisitFeePolicies::getEffectiveTime)
                .orderByDesc(TechnicianVisitFeePolicies::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private WorkerVisitFeePolicyItem toResponseItem(TechnicianVisitFeePolicies policy, Integer defaultServiceKind) {
        WorkerVisitFeePolicyItem item = new WorkerVisitFeePolicyItem();
        if (policy == null) {
            item.setServiceKind(defaultServiceKind);
            item.setMinVisitFee(BigDecimal.ZERO);
            item.setBaseRadiusKm(BigDecimal.ZERO);
            item.setExtraFeePerKm(BigDecimal.ZERO);
            item.setDistanceCalcType(1);
            item.setRoundingRule(1);
            item.setMaxVisitFee(null);
            item.setIsActive(1);
            return item;
        }
        item.setId(policy.getId());
        item.setServiceKind(policy.getServiceKind());
        item.setMinVisitFee(policy.getMinVisitFee() == null ? BigDecimal.ZERO : policy.getMinVisitFee());
        item.setBaseRadiusKm(policy.getBaseRadiusKm() == null ? BigDecimal.ZERO : policy.getBaseRadiusKm());
        item.setExtraFeePerKm(policy.getExtraFeePerKm() == null ? BigDecimal.ZERO : policy.getExtraFeePerKm());
        item.setDistanceCalcType(policy.getDistanceCalcType());
        item.setRoundingRule(policy.getRoundingRule());
        item.setMaxVisitFee(policy.getMaxVisitFee());
        item.setIsActive(policy.getIsActive());
        return item;
    }

    private void validatePolicyItem(WorkerVisitFeePolicyItem item) {
        if (item == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "计费策略不能为空");
        }
        Integer serviceKind = item.getServiceKind();
        if (serviceKind == null || (serviceKind != 1 && serviceKind != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务类型仅支持上门维修和上门安装");
        }
        if (item.getMinVisitFee() == null || item.getMinVisitFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "最低上门费不能小于0");
        }
        if (item.getBaseRadiusKm() == null || item.getBaseRadiusKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "基础服务半径不能小于0");
        }
        if (item.getExtraFeePerKm() == null || item.getExtraFeePerKm().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超区每公里费用不能小于0");
        }
        if (item.getDistanceCalcType() == null || (item.getDistanceCalcType() != 1 && item.getDistanceCalcType() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "距离计算方式不合法");
        }
        if (item.getRoundingRule() == null || (item.getRoundingRule() != 1 && item.getRoundingRule() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公里取整规则不合法");
        }
        if (item.getMaxVisitFee() != null && item.getMaxVisitFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "封顶公里数不能小于0");
        }
        if (item.getIsActive() != null && item.getIsActive() != 0 && item.getIsActive() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "启用状态不合法");
        }
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        return user;
    }
}
