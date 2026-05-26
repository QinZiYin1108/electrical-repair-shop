package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Coupons;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserCoupons;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminCouponModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.CouponsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserCouponsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/products/coupons")
public class AdminCouponController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int USER_STATUS_NORMAL = 1;
    private static final int USER_STATUS_FROZEN = 2;

    private final CouponsService couponsService;
    private final UserCouponsService userCouponsService;
    private final UserAccountsService userAccountsService;

    public AdminCouponController(
        CouponsService couponsService,
        UserCouponsService userCouponsService,
        UserAccountsService userAccountsService
    ) {
        this.couponsService = couponsService;
        this.userCouponsService = userCouponsService;
        this.userAccountsService = userAccountsService;
    }

    @GetMapping
    public Result<Page<AdminCouponModel.ListItemResponse>> listCoupons(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "applicableType", required = false) Integer applicableType
    ) {
        requireAdmin();
        LambdaQueryWrapper<Coupons> wrapper = new LambdaQueryWrapper<Coupons>()
            .like(StringUtils.hasText(keyword), Coupons::getName, keyword == null ? null : keyword.trim())
            .eq(status != null, Coupons::getStatus, status)
            .eq(applicableType != null, Coupons::getApplicableType, applicableType)
            .orderByDesc(Coupons::getUpdatedTime)
            .orderByDesc(Coupons::getCreatedTime);
        Page<Coupons> page = couponsService.page(new Page<>(Math.max(pageNum, 1L), Math.max(pageSize, 1L)), wrapper);
        Page<AdminCouponModel.ListItemResponse> response = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        response.setRecords(buildCouponItems(page.getRecords()));
        return Result.success(response);
    }

    @PostMapping
    public Result<Void> createCoupon(@Valid @RequestBody AdminCouponModel.SaveRequest request) {
        requireAdmin();
        long now = System.currentTimeMillis();
        Coupons coupon = buildCouponEntity(new Coupons(), request, now);
        coupon.setId(SnowflakeIdUtil.nextCouponId());
        coupon.setUsedCount(0);
        coupon.setCreatedTime(now);
        coupon.setVersion(0);
        coupon.setIsDelete(0);
        if (!couponsService.save(coupon)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增优惠券失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/update")
    public Result<Void> updateCoupon(@PathVariable("id") String id, @Valid @RequestBody AdminCouponModel.SaveRequest request) {
        requireAdmin();
        Coupons coupon = requireCoupon(id);
        int receiveCount = getReceiveCount(Collections.singletonList(coupon.getId())).getOrDefault(coupon.getId(), 0);
        if (request.getTotalCount() != null && request.getTotalCount() < receiveCount) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "发放总量不能小于已领取数量");
        }
        buildCouponEntity(coupon, request, System.currentTimeMillis());
        if (!couponsService.updateById(coupon)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新优惠券失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") String id, @Valid @RequestBody AdminCouponModel.StatusUpdateRequest request) {
        requireAdmin();
        Coupons coupon = requireCoupon(id);
        coupon.setStatus(request.getStatus());
        coupon.setUpdatedTime(System.currentTimeMillis());
        if (!couponsService.updateById(coupon)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新优惠券状态失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/grant")
    public Result<AdminCouponModel.GrantResponse> grantCoupon(
        @PathVariable("id") String id,
        @RequestBody(required = false) AdminCouponModel.GrantRequest request
    ) {
        requireAdmin();
        Coupons coupon = requireCoupon(id);
        validateGrantableCoupon(coupon);
        List<String> userIds = normalizeIdList(request == null ? null : request.getUserIds());
        if (userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要发放的用户");
        }
        List<UserAccounts> users = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .in(UserAccounts::getId, userIds)
                .eq(UserAccounts::getIsDelete, 0)
        );
        Set<String> matchedUserIds = users.stream()
            .map(UserAccounts::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> validUserIds = users.stream()
            .filter(item -> Objects.equals(item.getStatus(), USER_STATUS_NORMAL))
            .map(UserAccounts::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> frozenUserIds = users.stream()
            .filter(item -> Objects.equals(item.getStatus(), USER_STATUS_FROZEN))
            .map(UserAccounts::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingUserIds = userIds.stream()
            .filter(item -> !matchedUserIds.contains(item))
            .collect(Collectors.toCollection(ArrayList::new));
        if (matchedUserIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未找到用户");
        }
        if (validUserIds.isEmpty()) {
            if (!frozenUserIds.isEmpty()) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    frozenUserIds.size() == 1 ? "该用户已冻结，无法发放优惠券" : "所选用户已冻结，无法发放优惠券"
                );
            }
            if (!missingUserIds.isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未找到用户");
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未找到可发放的有效用户");
        }
        List<UserCoupons> existingCoupons = userCouponsService.list(
            new LambdaQueryWrapper<UserCoupons>()
                .eq(UserCoupons::getCouponId, coupon.getId())
                .in(UserCoupons::getUserId, validUserIds)
        );
        Set<String> existingUserIds = existingCoupons.stream()
            .map(UserCoupons::getUserId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        int receiveCount = getReceiveCount(Collections.singletonList(coupon.getId())).getOrDefault(coupon.getId(), 0);
        int remaining = Math.max((coupon.getTotalCount() == null ? 0 : coupon.getTotalCount()) - receiveCount, 0);
        if (remaining <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券库存不足，无法继续发放");
        }
        List<UserCoupons> saveList = new ArrayList<>();
        List<String> grantedUserIds = new ArrayList<>();
        List<String> skippedUserIds = new ArrayList<>();
        skippedUserIds.addAll(frozenUserIds);
        skippedUserIds.addAll(missingUserIds);
        long now = System.currentTimeMillis();
        for (String userId : validUserIds) {
            if (existingUserIds.contains(userId)) {
                skippedUserIds.add(userId);
                continue;
            }
            if (remaining <= 0) {
                skippedUserIds.add(userId);
                continue;
            }
            UserCoupons userCoupon = new UserCoupons();
            userCoupon.setId(SnowflakeIdUtil.nextUserCouponId());
            userCoupon.setUserId(userId);
            userCoupon.setCouponId(coupon.getId());
            userCoupon.setReceiveTime(now);
            userCoupon.setExpireTime(coupon.getEndTime());
            userCoupon.setStatus(1);
            userCoupon.setCreatedTime(now);
            userCoupon.setUpdatedTime(now);
            userCoupon.setVersion(0);
            userCoupon.setIsDelete(0);
            saveList.add(userCoupon);
            grantedUserIds.add(userId);
            remaining--;
        }
        if (!saveList.isEmpty() && !userCouponsService.saveBatch(saveList)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发放优惠券失败");
        }
        AdminCouponModel.GrantResponse response = new AdminCouponModel.GrantResponse();
        response.setGrantCount(grantedUserIds.size());
        response.setSkipCount(Math.max(userIds.size() - grantedUserIds.size(), 0));
        response.setGrantedUserIds(grantedUserIds);
        response.setSkippedUserIds(skippedUserIds.stream().distinct().collect(Collectors.toCollection(ArrayList::new)));
        return Result.success(response);
    }


    private List<AdminCouponModel.ListItemResponse> buildCouponItems(List<Coupons> coupons) {
        if (coupons == null || coupons.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> couponIds = coupons.stream().map(Coupons::getId).collect(Collectors.toList());
        Map<String, Integer> receiveCountMap = getReceiveCount(couponIds);
        List<AdminCouponModel.ListItemResponse> result = new ArrayList<>();
        for (Coupons coupon : coupons) {
            AdminCouponModel.ListItemResponse item = new AdminCouponModel.ListItemResponse();
            int receiveCount = receiveCountMap.getOrDefault(coupon.getId(), 0);
            item.setId(coupon.getId());
            item.setName(coupon.getName());
            item.setType(coupon.getType());
            item.setTypeText(getTypeText(coupon.getType()));
            item.setDiscountType(coupon.getDiscountType());
            item.setDiscountTypeText(Objects.equals(coupon.getDiscountType(), 2) ? "折扣" : "固定减免");
            item.setDiscountValue(coupon.getDiscountValue());
            item.setMinAmount(normalizeMoney(coupon.getMinAmount()));
            item.setMaxDiscount(coupon.getMaxDiscount() == null ? null : normalizeMoney(coupon.getMaxDiscount()));
            item.setTotalCount(coupon.getTotalCount());
            item.setReceiveCount(receiveCount);
            item.setUsedCount(coupon.getUsedCount());
            item.setRemainingCount(Math.max((coupon.getTotalCount() == null ? 0 : coupon.getTotalCount()) - receiveCount, 0));
            item.setPerUserLimit(coupon.getPerUserLimit());
            item.setApplicableType(coupon.getApplicableType());
            item.setApplicableTypeText(getApplicableTypeText(coupon.getApplicableType()));
            item.setApplicableIds(parseApplicableIds(coupon.getApplicableIds()));
            item.setStatus(coupon.getStatus());
            item.setStatusText(Objects.equals(coupon.getStatus(), 1) ? "启用" : "停用");
            item.setStartTime(coupon.getStartTime());
            item.setEndTime(coupon.getEndTime());
            item.setCreatedTime(coupon.getCreatedTime());
            item.setUpdatedTime(coupon.getUpdatedTime());
            result.add(item);
        }
        return result;
    }

    private Map<String, Integer> getReceiveCount(List<String> couponIds) {
        if (couponIds == null || couponIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        List<UserCoupons> userCoupons = userCouponsService.list(
            new LambdaQueryWrapper<UserCoupons>()
                .in(UserCoupons::getCouponId, couponIds)
        );
        for (UserCoupons userCoupon : userCoupons) {
            result.merge(userCoupon.getCouponId(), 1, Integer::sum);
        }
        return result;
    }

    private Coupons buildCouponEntity(Coupons coupon, AdminCouponModel.SaveRequest request, long now) {
        validateCouponRequest(request);
        coupon.setName(request.getName().trim());
        coupon.setType(request.getType());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(normalizeMoney(request.getDiscountValue()));
        coupon.setMinAmount(normalizeMoney(request.getMinAmount()));
        coupon.setMaxDiscount(request.getMaxDiscount() == null ? null : normalizeMoney(request.getMaxDiscount()));
        coupon.setTotalCount(request.getTotalCount());
        coupon.setPerUserLimit(1);
        coupon.setApplicableType(request.getApplicableType());
        coupon.setApplicableIds(writeApplicableIds(request.getApplicableIds()));
        coupon.setStatus(request.getStatus());
        coupon.setStartTime(request.getStartTime());
        coupon.setEndTime(request.getEndTime());
        coupon.setUpdatedTime(now);
        return coupon;
    }

    private void validateCouponRequest(AdminCouponModel.SaveRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null && request.getStartTime() > request.getEndTime()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
        if (request.getApplicableType() == null || (request.getApplicableType() != 1 && request.getApplicableType() != 2 && request.getApplicableType() != 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "适用范围参数不合法");
        }
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券状态参数不合法");
        }
    }

    private void validateGrantableCoupon(Coupons coupon) {
        long now = System.currentTimeMillis();
        if (!Objects.equals(coupon.getStatus(), 1)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前优惠券未启用");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime() < now) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前优惠券已过期");
        }
    }

    private Coupons requireCoupon(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "优惠券ID不能为空");
        }
        Coupons coupon = couponsService.getById(id);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        return coupon;
    }

    private List<String> parseApplicableIds(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return new ArrayList<>();
        }
        try {
            return ((List<?>) OBJECT_MAPPER.readValue(rawValue, List.class)).stream().map(String::valueOf).collect(Collectors.toList());
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String writeApplicableIds(List<String> values) {
        List<String> normalized = normalizeIdList(values);
        try {
            return normalized.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存适用范围失败");
        }
    }

    private List<String> normalizeIdList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().collect(Collectors.toList());
    }

    private String getTypeText(Integer type) {
        if (Objects.equals(type, 1)) {
            return "满减券";
        }
        if (Objects.equals(type, 2)) {
            return "折扣券";
        }
        if (Objects.equals(type, 3)) {
            return "运费券";
        }
        return "未知类型";
    }

    private String getApplicableTypeText(Integer type) {
        if (Objects.equals(type, 1)) {
            return "全部商品";
        }
        if (Objects.equals(type, 3)) {
            return "指定商品";
        }
        if (Objects.equals(type, 2)) {
            return "指定分类";
        }
        return "未知范围";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可操作");
        }
        return user;
    }
}
