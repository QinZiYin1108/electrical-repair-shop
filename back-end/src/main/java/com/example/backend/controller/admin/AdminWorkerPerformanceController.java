package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Images;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.Reviews;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminWorkerPerformanceItemResponse;
import com.example.backend.model.admin.AdminWorkerPerformancePageResponse;
import com.example.backend.model.admin.AdminWorkerPerformanceSummaryResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ImagesService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ReviewsService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianProfilesService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/workers/stats")
public class AdminWorkerPerformanceController {

    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianProfilesService technicianProfilesService;
    private final ImagesService imagesService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final ReviewsService reviewsService;

    public AdminWorkerPerformanceController(
        TechnicianAccountsService technicianAccountsService,
        TechnicianProfilesService technicianProfilesService,
        ImagesService imagesService,
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        ReviewsService reviewsService
    ) {
        this.technicianAccountsService = technicianAccountsService;
        this.technicianProfilesService = technicianProfilesService;
        this.imagesService = imagesService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.reviewsService = reviewsService;
    }

    @GetMapping("/performance")
    public Result<AdminWorkerPerformancePageResponse> pagePerformance(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        LoginUserInfo admin = requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        List<TechnicianAccounts> workers = technicianAccountsService.list(buildWorkerQuery(keyword, admin));
        AdminWorkerPerformancePageResponse response = new AdminWorkerPerformancePageResponse();
        response.setPageNum(currentPage);
        response.setPageSize(currentSize);
        response.setSummary(createEmptySummary());

        if (workers == null || workers.isEmpty()) {
            response.setTotal(0L);
            response.setList(new ArrayList<>());
            return Result.success(response);
        }

        Set<String> workerIds = new LinkedHashSet<>();
        for (TechnicianAccounts worker : workers) {
            if (worker != null && StringUtils.hasText(worker.getId())) {
                workerIds.add(worker.getId());
            }
        }

        Map<String, TechnicianProfiles> profileMap = queryProfileMap(workerIds);
        Map<String, String> avatarMap = queryAvatarMap(workerIds);

        Map<String, AdminWorkerPerformanceItemResponse> itemMap = new LinkedHashMap<>();
        Map<String, Long> serviceMinutesMap = new HashMap<>();
        Map<String, BigDecimal> reviewTotalMap = new HashMap<>();
        Map<String, Integer> reviewCountMap = new HashMap<>();
        for (TechnicianAccounts worker : workers) {
            AdminWorkerPerformanceItemResponse item = new AdminWorkerPerformanceItemResponse();
            item.setId(worker.getId());
            item.setUsername(worker.getUsername());
            item.setPhone(worker.getPhone());
            item.setEmail(worker.getEmail());
            item.setAccountStatus(worker.getAccountStatus());
            item.setWorkStatus(worker.getWorkStatus());
            item.setCreatedTime(worker.getCreatedTime());
            item.setAvatarUrl(avatarMap.get(worker.getId()));
            item.setRating(defaultDecimal(worker.getRating(), 2));
            item.setReviewCount(0);
            item.setTotalOrders(0L);
            item.setWaitingOrders(0L);
            item.setOngoingOrders(0L);
            item.setWaitingPayOrders(0L);
            item.setPendingOrders(0L);
            item.setCompletedOrders(0L);
            item.setCanceledOrders(0L);
            item.setRefundedOrders(0L);
            item.setCompletionRate(defaultPercent(worker.getCompletionRate()));
            item.setGrossIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setNetIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setAverageOrderAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setServiceHours(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            TechnicianProfiles profile = profileMap.get(worker.getId());
            if (profile != null) {
                item.setRealName(profile.getRealName());
            }
            itemMap.put(worker.getId(), item);
            serviceMinutesMap.put(worker.getId(), 0L);
        }

        List<RepairOrders> orders = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .in(RepairOrders::getTechnicianAccountId, workerIds)
                .eq(RepairOrders::getIsDelete, 0)
        );
        Set<String> orderIds = new LinkedHashSet<>();
        for (RepairOrders order : orders) {
            if (order != null && StringUtils.hasText(order.getId())) {
                orderIds.add(order.getId());
            }
        }
        Map<String, RepairOrderPayments> paymentMap = queryPaymentMap(orderIds);

        for (RepairOrders order : orders) {
            if (order == null || !StringUtils.hasText(order.getTechnicianAccountId())) {
                continue;
            }
            AdminWorkerPerformanceItemResponse item = itemMap.get(order.getTechnicianAccountId());
            if (item == null) {
                continue;
            }
            item.setTotalOrders(item.getTotalOrders() + 1);

            int status = safeInt(order.getStatus());
            if (status == 1) {
                item.setWaitingOrders(item.getWaitingOrders() + 1);
            } else if (status == 2 || status == 3 || status == 4) {
                item.setOngoingOrders(item.getOngoingOrders() + 1);
            } else if (status == 5) {
                item.setWaitingPayOrders(item.getWaitingPayOrders() + 1);
            } else if (status == 6) {
                item.setCompletedOrders(item.getCompletedOrders() + 1);
                long completedTime = firstPositive(order.getCompletionTime(), order.getEndTime(), order.getUpdatedTime());
                if (completedTime > firstPositive(item.getLatestCompletedTime())) {
                    item.setLatestCompletedTime(completedTime);
                }
                long serviceMinutes = calculateServiceMinutes(order);
                serviceMinutesMap.put(item.getId(), serviceMinutesMap.getOrDefault(item.getId(), 0L) + serviceMinutes);
            } else if (status == 7) {
                item.setCanceledOrders(item.getCanceledOrders() + 1);
            } else if (status == 8) {
                item.setRefundedOrders(item.getRefundedOrders() + 1);
            }

            RepairOrderPayments payment = paymentMap.get(order.getId());
            item.setGrossIncome(toMoney(item.getGrossIncome().add(safeMoney(payment == null ? null : payment.getActualAmount()))));
            item.setRefundAmount(toMoney(item.getRefundAmount().add(safeMoney(order.getRefundAmount()))));
        }

        List<Reviews> reviews = reviewsService.list(
            new LambdaQueryWrapper<Reviews>()
                .in(Reviews::getTargetId, workerIds)
                .eq(Reviews::getTargetType, 1)
                .eq(Reviews::getStatus, 1)
                .eq(Reviews::getIsDelete, 0)
        );
        for (Reviews review : reviews) {
            if (review == null || !StringUtils.hasText(review.getTargetId())) {
                continue;
            }
            String workerId = review.getTargetId();
            reviewTotalMap.put(workerId, reviewTotalMap.getOrDefault(workerId, BigDecimal.ZERO)
                .add(BigDecimal.valueOf(safeInt(review.getRating()))));
            reviewCountMap.put(workerId, reviewCountMap.getOrDefault(workerId, 0) + 1);
        }

        List<AdminWorkerPerformanceItemResponse> items = new ArrayList<>(itemMap.values());
        BigDecimal ratingTotal = BigDecimal.ZERO;
        AdminWorkerPerformanceSummaryResponse summary = createEmptySummary();
        for (AdminWorkerPerformanceItemResponse item : items) {
            long pendingOrders = item.getWaitingOrders() + item.getOngoingOrders() + item.getWaitingPayOrders();
            item.setPendingOrders(pendingOrders);
            item.setNetIncome(toMoney(item.getGrossIncome().subtract(item.getRefundAmount())));
            item.setAverageOrderAmount(item.getCompletedOrders() > 0
                ? toMoney(item.getGrossIncome().divide(BigDecimal.valueOf(item.getCompletedOrders()), 2, RoundingMode.HALF_UP))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            item.setServiceHours(toHours(serviceMinutesMap.getOrDefault(item.getId(), 0L)));

            Integer reviewCount = reviewCountMap.get(item.getId());
            if (reviewCount != null && reviewCount > 0) {
                item.setReviewCount(reviewCount);
                item.setRating(reviewTotalMap.get(item.getId()).divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP));
            }
            if (item.getTotalOrders() > 0) {
                item.setCompletionRate(BigDecimal.valueOf(item.getCompletedOrders() * 100.0 / item.getTotalOrders())
                    .setScale(2, RoundingMode.HALF_UP));
            }

            summary.setTotalWorkers(summary.getTotalWorkers() + 1);
            if (isActiveWorker(item.getAccountStatus(), item.getWorkStatus())) {
                summary.setActiveWorkers(summary.getActiveWorkers() + 1);
            }
            summary.setTotalOrders(summary.getTotalOrders() + item.getTotalOrders());
            summary.setPendingOrders(summary.getPendingOrders() + item.getPendingOrders());
            summary.setCompletedOrders(summary.getCompletedOrders() + item.getCompletedOrders());
            summary.setGrossIncome(toMoney(summary.getGrossIncome().add(item.getGrossIncome())));
            summary.setRefundAmount(toMoney(summary.getRefundAmount().add(item.getRefundAmount())));
            summary.setNetIncome(toMoney(summary.getNetIncome().add(item.getNetIncome())));
            ratingTotal = ratingTotal.add(defaultDecimal(item.getRating(), 2));
        }
        if (!items.isEmpty()) {
            summary.setAverageRating(ratingTotal.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP));
        }

        items.sort(
            Comparator.comparing(AdminWorkerPerformanceItemResponse::getCompletedOrders, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdminWorkerPerformanceItemResponse::getNetIncome, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdminWorkerPerformanceItemResponse::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdminWorkerPerformanceItemResponse::getTotalOrders, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AdminWorkerPerformanceItemResponse::getCreatedTime, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        int fromIndex = (int) ((currentPage - 1) * currentSize);
        int toIndex = (int) Math.min(fromIndex + currentSize, items.size());
        response.setSummary(summary);
        response.setTotal((long) items.size());
        if (fromIndex >= items.size()) {
            response.setList(Collections.emptyList());
        } else {
            response.setList(new ArrayList<>(items.subList(fromIndex, toIndex)));
        }
        return Result.success(response);
    }

    private LambdaQueryWrapper<TechnicianAccounts> buildWorkerQuery(String keyword, LoginUserInfo admin) {
        LambdaQueryWrapper<TechnicianAccounts> wrapper = new LambdaQueryWrapper<TechnicianAccounts>()
            .eq(TechnicianAccounts::getIsDelete, 0);
        // 门店管理员：仅查看本门店师傅
        if (admin != null && admin.isStoreAdmin() && StringUtils.hasText(admin.getStoreId())) {
            wrapper.eq(TechnicianAccounts::getStoreId, admin.getStoreId());
        }
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(query -> query.like(TechnicianAccounts::getUsername, trimmedKeyword)
                .or().like(TechnicianAccounts::getPhone, trimmedKeyword)
                .or().like(TechnicianAccounts::getEmail, trimmedKeyword));
        }
        wrapper.orderByDesc(TechnicianAccounts::getOrderCount)
            .orderByDesc(TechnicianAccounts::getCompletionRate)
            .orderByDesc(TechnicianAccounts::getRating)
            .orderByAsc(TechnicianAccounts::getCreatedTime);
        return wrapper;
    }

    private Map<String, TechnicianProfiles> queryProfileMap(Set<String> workerIds) {
        Map<String, TechnicianProfiles> result = new HashMap<>();
        if (workerIds == null || workerIds.isEmpty()) {
            return result;
        }
        List<TechnicianProfiles> profiles = technicianProfilesService.list(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .in(TechnicianProfiles::getTechnicianAccountId, workerIds)
                .eq(TechnicianProfiles::getIsDelete, 0)
        );
        for (TechnicianProfiles profile : profiles) {
            if (profile != null && StringUtils.hasText(profile.getTechnicianAccountId())) {
                result.put(profile.getTechnicianAccountId(), profile);
            }
        }
        return result;
    }

    private Map<String, String> queryAvatarMap(Set<String> workerIds) {
        Map<String, String> result = new HashMap<>();
        if (workerIds == null || workerIds.isEmpty()) {
            return result;
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .in(Images::getBusinessId, workerIds)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
        );
        for (Images image : images) {
            if (image == null || !StringUtils.hasText(image.getBusinessId()) || !StringUtils.hasText(image.getFileUrl())) {
                continue;
            }
            result.putIfAbsent(image.getBusinessId(), image.getFileUrl());
        }
        return result;
    }

    private Map<String, RepairOrderPayments> queryPaymentMap(Set<String> orderIds) {
        Map<String, RepairOrderPayments> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) {
            return result;
        }
        List<RepairOrderPayments> payments = repairOrderPaymentsService.list(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .in(RepairOrderPayments::getRepairOrderId, orderIds)
                .eq(RepairOrderPayments::getIsDelete, 0)
        );
        for (RepairOrderPayments payment : payments) {
            if (payment != null && StringUtils.hasText(payment.getRepairOrderId())) {
                result.put(payment.getRepairOrderId(), payment);
            }
        }
        return result;
    }

    private long calculateServiceMinutes(RepairOrders order) {
        long startTime = firstPositive(order == null ? null : order.getStartTime());
        long endTime = firstPositive(
            order == null ? null : order.getEndTime(),
            order == null ? null : order.getCompletionTime()
        );
        if (startTime <= 0L || endTime <= startTime) {
            return 0L;
        }
        return (endTime - startTime) / 60000;
    }

    private long firstPositive(Long... values) {
        if (values == null) {
            return 0L;
        }
        for (Long value : values) {
            if (value != null && value > 0L) {
                return value;
            }
        }
        return 0L;
    }

    private BigDecimal toMoney(BigDecimal amount) {
        return safeMoney(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal safeMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    private BigDecimal defaultDecimal(BigDecimal value, int scale) {
        return (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultPercent(BigDecimal value) {
        return defaultDecimal(value, 2);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isActiveWorker(Integer accountStatus, Integer workStatus) {
        return safeInt(accountStatus) == 1 && (safeInt(workStatus) == 1 || safeInt(workStatus) == 2 || safeInt(workStatus) == 3);
    }

    private AdminWorkerPerformanceSummaryResponse createEmptySummary() {
        AdminWorkerPerformanceSummaryResponse summary = new AdminWorkerPerformanceSummaryResponse();
        summary.setTotalWorkers(0L);
        summary.setActiveWorkers(0L);
        summary.setTotalOrders(0L);
        summary.setPendingOrders(0L);
        summary.setCompletedOrders(0L);
        summary.setGrossIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.setNetIncome(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summary.setAverageRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return summary;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问绩效统计");
        }
        return user;
    }
}
