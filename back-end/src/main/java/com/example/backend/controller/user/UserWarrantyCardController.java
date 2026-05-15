package com.example.backend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.WarrantyCardUsageRecords;
import com.example.backend.entity.WarrantyCards;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserWarrantyCardModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.WarrantyCardUsageRecordsService;
import com.example.backend.service.WarrantyCardsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/user/warranty-cards")
public class UserWarrantyCardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_EXPIRED = 2;
    private static final int USAGE_STATUS_PENDING = 1;
    private static final int USAGE_STATUS_COMPLETED = 2;
    private static final int USAGE_STATUS_REJECTED = 3;

    private final WarrantyCardsService warrantyCardsService;
    private final WarrantyCardUsageRecordsService usageRecordsService;

    public UserWarrantyCardController(
        WarrantyCardsService warrantyCardsService,
        WarrantyCardUsageRecordsService usageRecordsService
    ) {
        this.warrantyCardsService = warrantyCardsService;
        this.usageRecordsService = usageRecordsService;
    }

    @GetMapping("/list")
    public Result<UserWarrantyCardModel.ListResponse> listCards(@RequestParam(value = "status", required = false) String status) {
        LoginUserInfo user = requireCurrentUser();
        refreshExpiredCards();
        UserWarrantyCardModel.ListResponse response = new UserWarrantyCardModel.ListResponse();
        List<WarrantyCards> list = warrantyCardsService.list(buildListQuery(user.getAccountId(), normalizeStatus(status)));
        for (WarrantyCards card : list) {
            response.getItems().add(toListItem(card));
        }
        return Result.success(response);
    }

    @GetMapping("/detail")
    public Result<UserWarrantyCardModel.DetailResponse> getDetail(@RequestParam("id") String id) {
        LoginUserInfo user = requireCurrentUser();
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保修卡ID不能为空");
        }
        refreshExpiredCards();
        WarrantyCards card = warrantyCardsService.getOne(
            new LambdaQueryWrapper<WarrantyCards>()
                .eq(WarrantyCards::getId, id)
                .eq(WarrantyCards::getUserId, user.getAccountId())
                .last("limit 1"),
            false
        );
        if (card == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "保修卡不存在");
        }
        return Result.success(toDetail(card));
    }

    @PostMapping("/usage/apply")
    public Result<Void> applyUsage(@Valid @RequestBody UserWarrantyCardModel.ApplyUsageRequest request) {
        LoginUserInfo user = requireCurrentUser();
        refreshExpiredCards();
        WarrantyCards card = requireUserCard(user.getAccountId(), request.getWarrantyCardId());
        if (!Objects.equals(card.getWarrantyStatus(), STATUS_ACTIVE)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前保修卡不可申请使用");
        }
        if (countPendingUsage(card.getId()) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前保修卡已有待处理申请");
        }

        String issueDescription = trimToNull(request.getIssueDescription());
        String contactName = trimToNull(request.getContactName());
        String contactPhone = trimToNull(request.getContactPhone());
        if (!StringUtils.hasText(issueDescription)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "故障描述不能为空");
        }
        if (!StringUtils.hasText(contactName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系人不能为空");
        }
        if (!StringUtils.hasText(contactPhone)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系电话不能为空");
        }

        long now = System.currentTimeMillis();
        WarrantyCardUsageRecords record = new WarrantyCardUsageRecords();
        record.setId(SnowflakeIdUtil.nextWarrantyCardUsageRecordId());
        record.setWarrantyCardId(card.getId());
        record.setCardNo(card.getCardNo());
        record.setUserId(user.getAccountId());
        record.setProductId(card.getProductId());
        record.setProductName(card.getProductName());
        record.setProductModel(card.getProductModel());
        record.setIssueDescription(issueDescription);
        record.setContactName(contactName);
        record.setContactPhone(contactPhone);
        record.setStatus(USAGE_STATUS_PENDING);
        record.setApplyTime(now);
        record.setCreatedTime(now);
        record.setUpdatedTime(now);
        record.setVersion(0);
        record.setIsDelete(0);
        if (!usageRecordsService.save(record)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提交保修使用申请失败");
        }
        return Result.success();
    }

    private LambdaQueryWrapper<WarrantyCards> buildListQuery(String userId, String status) {
        LambdaQueryWrapper<WarrantyCards> wrapper = new LambdaQueryWrapper<WarrantyCards>()
            .eq(WarrantyCards::getUserId, userId)
            .orderByDesc(WarrantyCards::getUpdatedTime)
            .orderByDesc(WarrantyCards::getCreatedTime);
        if ("active".equals(status)) {
            wrapper.eq(WarrantyCards::getWarrantyStatus, STATUS_ACTIVE);
        } else if ("expired".equals(status)) {
            wrapper.eq(WarrantyCards::getWarrantyStatus, STATUS_EXPIRED);
        }
        return wrapper;
    }

    private void refreshExpiredCards() {
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        warrantyCardsService.update(
            new LambdaUpdateWrapper<WarrantyCards>()
                .eq(WarrantyCards::getWarrantyStatus, STATUS_ACTIVE)
                .lt(WarrantyCards::getWarrantyEndDate, today)
                .set(WarrantyCards::getWarrantyStatus, STATUS_EXPIRED)
                .set(WarrantyCards::getUpdatedTime, System.currentTimeMillis())
        );
    }

    private UserWarrantyCardModel.ListItemResponse toListItem(WarrantyCards card) {
        UserWarrantyCardModel.DetailResponse detail = toDetail(card);
        UserWarrantyCardModel.ListItemResponse item = new UserWarrantyCardModel.ListItemResponse();
        item.setId(detail.getId());
        item.setCardNo(detail.getCardNo());
        item.setProductId(detail.getProductId());
        item.setProductName(detail.getProductName());
        item.setProductModel(detail.getProductModel());
        item.setPurchaseDate(detail.getPurchaseDate());
        item.setWarrantyStartDate(detail.getWarrantyStartDate());
        item.setWarrantyEndDate(detail.getWarrantyEndDate());
        item.setWarrantyPeriod(detail.getWarrantyPeriod());
        item.setWarrantyType(detail.getWarrantyType());
        item.setWarrantyTypeText(detail.getWarrantyTypeText());
        item.setWarrantyStatus(detail.getWarrantyStatus());
        item.setWarrantyStatusText(detail.getWarrantyStatusText());
        item.setRepairCount(detail.getRepairCount());
        item.setLastRepairDate(detail.getLastRepairDate());
        item.setRemainingDays(detail.getRemainingDays());
        return item;
    }

    private UserWarrantyCardModel.DetailResponse toDetail(WarrantyCards card) {
        int pendingUsageCount = countPendingUsage(card.getId());
        UserWarrantyCardModel.DetailResponse response = new UserWarrantyCardModel.DetailResponse();
        response.setId(card.getId());
        response.setCardNo(card.getCardNo());
        response.setProductId(card.getProductId());
        response.setProductName(card.getProductName());
        response.setProductModel(card.getProductModel());
        response.setPurchaseDate(formatDate(card.getPurchaseDate()));
        response.setWarrantyStartDate(formatDate(card.getWarrantyStartDate()));
        response.setWarrantyEndDate(formatDate(card.getWarrantyEndDate()));
        response.setWarrantyPeriod(card.getWarrantyPeriod());
        response.setWarrantyType(card.getWarrantyType());
        response.setWarrantyTypeText(getTypeText(card.getWarrantyType()));
        response.setWarrantyStatus(card.getWarrantyStatus());
        response.setWarrantyStatusText(getStatusText(card.getWarrantyStatus()));
        response.setRepairCount(card.getRepairCount());
        response.setLastRepairDate(formatDate(card.getLastRepairDate()));
        response.setRemainingDays(calculateRemainingDays(card.getWarrantyEndDate()));
        response.setPendingUsageCount(pendingUsageCount);
        response.setCanApplyUsage(Objects.equals(card.getWarrantyStatus(), STATUS_ACTIVE) && pendingUsageCount == 0);
        response.setUsageRecords(buildUsageRecords(card.getId()));
        return response;
    }

    private WarrantyCards requireUserCard(String userId, String warrantyCardId) {
        if (!StringUtils.hasText(warrantyCardId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保修卡ID不能为空");
        }
        WarrantyCards card = warrantyCardsService.getOne(
            new LambdaQueryWrapper<WarrantyCards>()
                .eq(WarrantyCards::getId, warrantyCardId)
                .eq(WarrantyCards::getUserId, userId)
                .last("limit 1"),
            false
        );
        if (card == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "保修卡不存在");
        }
        return card;
    }

    private int countPendingUsage(String warrantyCardId) {
        return Math.toIntExact(usageRecordsService.count(
            new LambdaQueryWrapper<WarrantyCardUsageRecords>()
                .eq(WarrantyCardUsageRecords::getWarrantyCardId, warrantyCardId)
                .eq(WarrantyCardUsageRecords::getStatus, USAGE_STATUS_PENDING)
        ));
    }

    private List<UserWarrantyCardModel.UsageRecordResponse> buildUsageRecords(String warrantyCardId) {
        List<WarrantyCardUsageRecords> records = usageRecordsService.list(
            new LambdaQueryWrapper<WarrantyCardUsageRecords>()
                .eq(WarrantyCardUsageRecords::getWarrantyCardId, warrantyCardId)
                .orderByDesc(WarrantyCardUsageRecords::getCreatedTime)
        );
        List<UserWarrantyCardModel.UsageRecordResponse> result = new ArrayList<>();
        for (WarrantyCardUsageRecords record : records) {
            UserWarrantyCardModel.UsageRecordResponse item = new UserWarrantyCardModel.UsageRecordResponse();
            item.setId(record.getId());
            item.setIssueDescription(record.getIssueDescription());
            item.setContactName(record.getContactName());
            item.setContactPhone(record.getContactPhone());
            item.setStatus(record.getStatus());
            item.setStatusText(getUsageStatusText(record.getStatus()));
            item.setProcessRemark(defaultText(record.getProcessRemark(), ""));
            item.setApplyTime(record.getApplyTime());
            item.setProcessTime(record.getProcessTime());
            result.add(item);
        }
        return result;
    }

    private Long calculateRemainingDays(Date endDate) {
        if (endDate == null) {
            return 0L;
        }
        LocalDate current = LocalDate.now();
        LocalDate target = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(current, target);
        return Math.max(days, 0L);
    }

    private String formatDate(Date value) {
        if (value == null) {
            return "";
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER);
    }

    private String getTypeText(Integer type) {
        if (Objects.equals(type, 1)) {
            return "厂家保修";
        }
        if (Objects.equals(type, 2)) {
            return "店铺保修";
        }
        if (Objects.equals(type, 3)) {
            return "延保";
        }
        return "未知类型";
    }

    private String getStatusText(Integer status) {
        if (Objects.equals(status, STATUS_ACTIVE)) {
            return "有效";
        }
        if (Objects.equals(status, STATUS_EXPIRED)) {
            return "已过期";
        }
        if (Objects.equals(status, 3)) {
            return "已使用";
        }
        return "未知状态";
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "all";
        }
        String value = status.trim().toLowerCase();
        if ("active".equals(value) || "expired".equals(value)) {
            return value;
        }
        return "all";
    }

    private String getUsageStatusText(Integer status) {
        if (Objects.equals(status, USAGE_STATUS_COMPLETED)) {
            return "已完成";
        }
        if (Objects.equals(status, USAGE_STATUS_REJECTED)) {
            return "已驳回";
        }
        return "待处理";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
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
