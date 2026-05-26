package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Coupons;
import com.example.backend.entity.UserCoupons;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserCouponModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.CouponsService;
import com.example.backend.service.UserCouponsService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/coupons")
public class UserCouponController {

    private static final int STATUS_UNUSED = 1;
    private static final int STATUS_USED = 2;
    private static final int STATUS_EXPIRED = 3;

    private final UserCouponsService userCouponsService;
    private final CouponsService couponsService;

    public UserCouponController(UserCouponsService userCouponsService, CouponsService couponsService) {
        this.userCouponsService = userCouponsService;
        this.couponsService = couponsService;
    }

    @GetMapping("/list")
    public Result<UserCouponModel.ListResponse> listCoupons(@RequestParam(value = "status", required = false) String status) {
        LoginUserInfo user = requireCurrentUser();
        long now = System.currentTimeMillis();
        refreshExpiredCoupons(user.getAccountId(), now);
        List<UserCoupons> userCoupons = userCouponsService.list(
            buildListQuery(user.getAccountId(), normalizeStatus(status))
        );
        UserCouponModel.ListResponse response = new UserCouponModel.ListResponse();
        if (userCoupons.isEmpty()) {
            return Result.success(response);
        }
        Map<String, Coupons> couponMap = loadCouponMap(userCoupons);
        for (UserCoupons userCoupon : userCoupons) {
            response.getItems().add(toListItem(userCoupon, couponMap.get(userCoupon.getCouponId()), now));
        }
        return Result.success(response);
    }

    @GetMapping("/detail")
    public Result<UserCouponModel.DetailResponse> getDetail(@RequestParam("id") String id) {
        LoginUserInfo user = requireCurrentUser();
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户优惠券ID不能为空");
        }
        long now = System.currentTimeMillis();
        refreshExpiredCoupons(user.getAccountId(), now);
        UserCoupons userCoupon = userCouponsService.getOne(
            new LambdaQueryWrapper<UserCoupons>()
                .eq(UserCoupons::getId, id)
                .eq(UserCoupons::getUserId, user.getAccountId())
                .last("limit 1"),
            false
        );
        if (userCoupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        Coupons coupon = couponsService.getById(userCoupon.getCouponId());
        return Result.success(toDetail(userCoupon, coupon, now));
    }

    private LambdaQueryWrapper<UserCoupons> buildListQuery(String userId, String status) {
        LambdaQueryWrapper<UserCoupons> wrapper = new LambdaQueryWrapper<UserCoupons>()
            .eq(UserCoupons::getUserId, userId)
            .orderByDesc(UserCoupons::getUpdatedTime)
            .orderByDesc(UserCoupons::getCreatedTime);
        if ("unused".equals(status)) {
            wrapper.eq(UserCoupons::getStatus, STATUS_UNUSED);
        } else if ("used".equals(status)) {
            wrapper.eq(UserCoupons::getStatus, STATUS_USED);
        } else if ("expired".equals(status)) {
            wrapper.eq(UserCoupons::getStatus, STATUS_EXPIRED);
        }
        return wrapper;
    }

    private Map<String, Coupons> loadCouponMap(List<UserCoupons> userCoupons) {
        Set<String> couponIds = userCoupons.stream()
            .map(UserCoupons::getCouponId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (couponIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return couponsService.list(
            new LambdaQueryWrapper<Coupons>()
                .in(Coupons::getId, couponIds)
        ).stream().collect(Collectors.toMap(Coupons::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private UserCouponModel.ListItemResponse toListItem(UserCoupons userCoupon, Coupons coupon, long now) {
        UserCouponModel.DetailResponse detail = toDetail(userCoupon, coupon, now);
        UserCouponModel.ListItemResponse item = new UserCouponModel.ListItemResponse();
        item.setId(detail.getId());
        item.setCouponId(detail.getCouponId());
        item.setName(detail.getName());
        item.setType(detail.getType());
        item.setTypeText(detail.getTypeText());
        item.setDiscountType(detail.getDiscountType());
        item.setDiscountValue(detail.getDiscountValue());
        item.setDiscountText(detail.getDiscountText());
        item.setMinAmount(detail.getMinAmount());
        item.setMaxDiscount(detail.getMaxDiscount());
        item.setApplicableType(detail.getApplicableType());
        item.setApplicableTypeText(detail.getApplicableTypeText());
        item.setStatus(detail.getStatus());
        item.setStatusText(detail.getStatusText());
        item.setDisabledReason(detail.getDisabledReason());
        item.setStartTime(detail.getStartTime());
        item.setEndTime(detail.getEndTime());
        item.setReceiveTime(detail.getReceiveTime());
        item.setUseTime(detail.getUseTime());
        item.setExpireTime(detail.getExpireTime());
        item.setOrderId(detail.getOrderId());
        return item;
    }

    private UserCouponModel.DetailResponse toDetail(UserCoupons userCoupon, Coupons coupon, long now) {
        UserCouponModel.DetailResponse response = new UserCouponModel.DetailResponse();
        response.setId(userCoupon.getId());
        response.setCouponId(userCoupon.getCouponId());
        response.setReceiveTime(userCoupon.getReceiveTime());
        response.setUseTime(userCoupon.getUseTime());
        response.setExpireTime(userCoupon.getExpireTime());
        response.setOrderId(userCoupon.getOrderId());
        response.setStatus(defaultStatus(userCoupon.getStatus(), userCoupon.getExpireTime(), now));
        response.setStatusText(getStatusText(response.getStatus()));
        if (coupon == null) {
            response.setName("优惠券已失效");
            response.setTypeText("未知类型");
            response.setDiscountText("暂无优惠");
            response.setApplicableTypeText("适用范围未知");
            response.setDisabledReason("优惠券信息不存在或已失效");
            return response;
        }
        response.setName(coupon.getName());
        response.setType(coupon.getType());
        response.setTypeText(getTypeText(coupon.getType()));
        response.setDiscountType(coupon.getDiscountType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setDiscountText(buildDiscountText(coupon));
        response.setMinAmount(normalizeMoney(coupon.getMinAmount()));
        response.setMaxDiscount(coupon.getMaxDiscount() == null ? null : normalizeMoney(coupon.getMaxDiscount()));
        response.setApplicableType(coupon.getApplicableType());
        response.setApplicableTypeText(getApplicableTypeText(coupon.getApplicableType()));
        response.setStartTime(coupon.getStartTime());
        response.setEndTime(coupon.getEndTime());
        if (!Objects.equals(coupon.getStatus(), 1) && Objects.equals(response.getStatus(), STATUS_UNUSED)) {
            response.setDisabledReason("当前优惠券已停用");
        } else if (coupon.getStartTime() != null && coupon.getStartTime() > now && Objects.equals(response.getStatus(), STATUS_UNUSED)) {
            response.setDisabledReason("当前优惠券尚未生效");
        }
        return response;
    }

    private void refreshExpiredCoupons(String userId, long now) {
        userCouponsService.update(
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserCoupons>()
                .eq(UserCoupons::getUserId, userId)
                .eq(UserCoupons::getStatus, STATUS_UNUSED)
                .lt(UserCoupons::getExpireTime, now)
                .set(UserCoupons::getStatus, STATUS_EXPIRED)
                .set(UserCoupons::getUpdatedTime, now)
        );
    }

    private Integer defaultStatus(Integer status, Long expireTime, long now) {
        if (Objects.equals(status, STATUS_UNUSED) && expireTime != null && expireTime < now) {
            return STATUS_EXPIRED;
        }
        return status == null ? STATUS_UNUSED : status;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "all";
        }
        String value = status.trim().toLowerCase();
        if ("unused".equals(value) || "used".equals(value) || "expired".equals(value)) {
            return value;
        }
        return "all";
    }

    private String getStatusText(Integer status) {
        if (Objects.equals(status, STATUS_USED)) {
            return "已使用";
        }
        if (Objects.equals(status, STATUS_EXPIRED)) {
            return "已过期";
        }
        return "未使用";
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

    private String buildDiscountText(Coupons coupon) {
        if (coupon == null) {
            return "暂无优惠";
        }
        if (Objects.equals(coupon.getType(), 3)) {
            return "运费减免";
        }
        if (Objects.equals(coupon.getDiscountType(), 2)) {
            return (coupon.getDiscountValue() == null ? BigDecimal.TEN : coupon.getDiscountValue()).stripTrailingZeros().toPlainString() + "折";
        }
        return "￥" + normalizeMoney(coupon.getDiscountValue()).toPlainString() + "优惠";
    }

    private String getApplicableTypeText(Integer applicableType) {
        if (Objects.equals(applicableType, 3)) {
            return "指定商品";
        }
        if (Objects.equals(applicableType, 2)) {
            return "指定分类";
        }
        return "全部商品";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private LoginUserInfo requireCurrentUser() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅用户可操作");
        }
        return user;
    }
}
