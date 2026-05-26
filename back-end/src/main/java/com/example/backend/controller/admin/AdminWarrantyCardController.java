package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Products;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.WarrantyCardUsageRecords;
import com.example.backend.entity.WarrantyCards;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminWarrantyCardModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ProductsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.WarrantyCardUsageRecordsService;
import com.example.backend.service.WarrantyCardsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/products/warranty")
public class AdminWarrantyCardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int STATUS_ACTIVE = 1;
    private static final int USAGE_STATUS_PENDING = 1;
    private static final int USAGE_STATUS_COMPLETED = 2;
    private static final int USAGE_STATUS_REJECTED = 3;

    private final WarrantyCardsService warrantyCardsService;
    private final UserAccountsService userAccountsService;
    private final ProductsService productsService;
    private final WarrantyCardUsageRecordsService usageRecordsService;

    public AdminWarrantyCardController(
        WarrantyCardsService warrantyCardsService,
        UserAccountsService userAccountsService,
        ProductsService productsService,
        WarrantyCardUsageRecordsService usageRecordsService
    ) {
        this.warrantyCardsService = warrantyCardsService;
        this.userAccountsService = userAccountsService;
        this.productsService = productsService;
        this.usageRecordsService = usageRecordsService;
    }

    @GetMapping
    public Result<Page<AdminWarrantyCardModel.ListItemResponse>> listCards(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "warrantyStatus", required = false) Integer warrantyStatus
    ) {
        requireAdmin();
        refreshExpiredCards();
        LambdaQueryWrapper<WarrantyCards> wrapper = new LambdaQueryWrapper<WarrantyCards>()
            .like(StringUtils.hasText(keyword), WarrantyCards::getCardNo, keyword == null ? null : keyword.trim())
            .eq(warrantyStatus != null, WarrantyCards::getWarrantyStatus, warrantyStatus)
            .orderByDesc(WarrantyCards::getUpdatedTime)
            .orderByDesc(WarrantyCards::getCreatedTime);
        Page<WarrantyCards> page = warrantyCardsService.page(new Page<>(Math.max(pageNum, 1L), Math.max(pageSize, 1L)), wrapper);
        Page<AdminWarrantyCardModel.ListItemResponse> response = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        response.setRecords(buildListItems(page.getRecords()));
        return Result.success(response);
    }

    @GetMapping("/{id}")
    public Result<AdminWarrantyCardModel.DetailResponse> getDetail(@PathVariable("id") String id) {
        requireAdmin();
        refreshExpiredCards();
        WarrantyCards card = requireCard(id);
        return Result.success(toDetail(card));
    }

    @GetMapping("/{id}/usage-records")
    public Result<AdminWarrantyCardModel.UsageRecordListResponse> listUsageRecords(@PathVariable("id") String id) {
        requireAdmin();
        WarrantyCards card = requireCard(id);
        AdminWarrantyCardModel.UsageRecordListResponse response = new AdminWarrantyCardModel.UsageRecordListResponse();
        response.setItems(buildUsageRecords(card.getId()));
        return Result.success(response);
    }

    @PostMapping
    public Result<Void> createCard(@Valid @RequestBody AdminWarrantyCardModel.CreateRequest request) {
        requireAdmin();
        UserAccounts user = requireUser(request.getUserId());
        Products product = requireProduct(request.getProductId());
        long now = System.currentTimeMillis();
        LocalDate purchaseDate = parseDate(request.getPurchaseDate(), LocalDate.now());
        LocalDate startDate = parseDate(request.getWarrantyStartDate(), purchaseDate);
        int warrantyPeriod = request.getWarrantyPeriod() == null ? (product.getWarrantyPeriod() == null ? 0 : product.getWarrantyPeriod()) : request.getWarrantyPeriod();
        if (warrantyPeriod <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保修期必须大于0");
        }
        WarrantyCards card = new WarrantyCards();
        String cardId = SnowflakeIdUtil.nextWarrantyCardId();
        card.setId(cardId);
        card.setCardNo(buildCardNo(cardId));
        card.setUserId(user.getId());
        card.setProductId(product.getId());
        card.setProductName(product.getName());
        card.setProductModel(product.getModel());
        card.setPurchaseDate(toDate(purchaseDate));
        card.setWarrantyStartDate(toDate(startDate));
        card.setWarrantyEndDate(toDate(startDate.plusMonths(warrantyPeriod)));
        card.setWarrantyPeriod(warrantyPeriod);
        card.setWarrantyType(request.getWarrantyType());
        card.setWarrantyStatus(1);
        card.setRepairCount(0);
        card.setCreatedTime(now);
        card.setUpdatedTime(now);
        card.setVersion(0);
        card.setIsDelete(0);
        if (!warrantyCardsService.save(card)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增保修卡失败");
        }
        return Result.success();
    }

    @PostMapping("/usage-records/{recordId}/process")
    public Result<Void> processUsageRecord(
        @PathVariable("recordId") String recordId,
        @Valid @RequestBody AdminWarrantyCardModel.ProcessUsageRequest request
    ) {
        requireAdmin();
        WarrantyCardUsageRecords record = requireUsageRecord(recordId);
        if (!Objects.equals(record.getStatus(), USAGE_STATUS_PENDING)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前申请已处理，请勿重复操作");
        }
        if (!Objects.equals(request.getStatus(), USAGE_STATUS_COMPLETED) && !Objects.equals(request.getStatus(), USAGE_STATUS_REJECTED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理结果不合法");
        }

        long now = System.currentTimeMillis();
        record.setStatus(request.getStatus());
        record.setProcessRemark(trimToNull(request.getProcessRemark()));
        record.setProcessTime(now);
        record.setUpdatedTime(now);
        if (!usageRecordsService.updateById(record)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "处理保修使用申请失败");
        }

        if (Objects.equals(request.getStatus(), USAGE_STATUS_COMPLETED)) {
            WarrantyCards card = requireCard(record.getWarrantyCardId());
            card.setRepairCount(defaultIfNull(card.getRepairCount(), 0) + 1);
            card.setLastRepairDate(new Date());
            card.setUpdatedTime(now);
            if (!warrantyCardsService.updateById(card)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新保修卡使用信息失败");
            }
        }
        return Result.success();
    }

    private List<AdminWarrantyCardModel.ListItemResponse> buildListItems(List<WarrantyCards> cards) {
        if (cards == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = cards.stream().map(WarrantyCards::getUserId).collect(Collectors.toSet());
        Map<String, UserAccounts> userMap = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>().in(!userIds.isEmpty(), UserAccounts::getId, userIds)
        ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        return cards.stream().map(card -> {
            AdminWarrantyCardModel.DetailResponse detail = toDetail(card);
            AdminWarrantyCardModel.ListItemResponse item = new AdminWarrantyCardModel.ListItemResponse();
            UserAccounts user = userMap.get(card.getUserId());
            item.setId(detail.getId());
            item.setCardNo(detail.getCardNo());
            item.setUserId(detail.getUserId());
            item.setUserName(user == null ? "" : user.getUsername());
            item.setUserPhone(user == null ? "" : user.getPhone());
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
            item.setCreatedTime(detail.getCreatedTime());
            return item;
        }).collect(Collectors.toList());
    }

    private AdminWarrantyCardModel.DetailResponse toDetail(WarrantyCards card) {
        UserAccounts user = userAccountsService.getById(card.getUserId());
        AdminWarrantyCardModel.DetailResponse response = new AdminWarrantyCardModel.DetailResponse();
        response.setId(card.getId());
        response.setCardNo(card.getCardNo());
        response.setUserId(card.getUserId());
        response.setUserName(user == null ? "" : user.getUsername());
        response.setUserPhone(user == null ? "" : user.getPhone());
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
        response.setCreatedTime(card.getCreatedTime());
        response.setUpdatedTime(card.getUpdatedTime());
        return response;
    }

    private List<AdminWarrantyCardModel.UsageRecordResponse> buildUsageRecords(String warrantyCardId) {
        List<WarrantyCardUsageRecords> records = usageRecordsService.list(
            new LambdaQueryWrapper<WarrantyCardUsageRecords>()
                .eq(WarrantyCardUsageRecords::getWarrantyCardId, warrantyCardId)
                .orderByDesc(WarrantyCardUsageRecords::getCreatedTime)
        );
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = records.stream()
            .map(WarrantyCardUsageRecords::getUserId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, UserAccounts> userMap = userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>().in(!userIds.isEmpty(), UserAccounts::getId, userIds)
        ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        return records.stream().map(record -> {
            AdminWarrantyCardModel.UsageRecordResponse item = new AdminWarrantyCardModel.UsageRecordResponse();
            UserAccounts user = userMap.get(record.getUserId());
            item.setId(record.getId());
            item.setWarrantyCardId(record.getWarrantyCardId());
            item.setCardNo(record.getCardNo());
            item.setUserId(record.getUserId());
            item.setUserName(user == null ? "" : user.getUsername());
            item.setUserPhone(user == null ? "" : user.getPhone());
            item.setProductId(record.getProductId());
            item.setProductName(record.getProductName());
            item.setProductModel(record.getProductModel());
            item.setIssueDescription(record.getIssueDescription());
            item.setContactName(record.getContactName());
            item.setContactPhone(record.getContactPhone());
            item.setStatus(record.getStatus());
            item.setStatusText(getUsageStatusText(record.getStatus()));
            item.setProcessRemark(record.getProcessRemark());
            item.setApplyTime(record.getApplyTime());
            item.setProcessTime(record.getProcessTime());
            return item;
        }).collect(Collectors.toList());
    }

    private void refreshExpiredCards() {
        Date today = toDate(LocalDate.now());
        warrantyCardsService.update(
            new LambdaUpdateWrapper<WarrantyCards>()
                .eq(WarrantyCards::getWarrantyStatus, STATUS_ACTIVE)
                .lt(WarrantyCards::getWarrantyEndDate, today)
                .set(WarrantyCards::getWarrantyStatus, 2)
                .set(WarrantyCards::getUpdatedTime, System.currentTimeMillis())
        );
    }


    private WarrantyCards requireCard(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "保修卡ID不能为空");
        }
        WarrantyCards card = warrantyCardsService.getById(id);
        if (card == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "保修卡不存在");
        }
        return card;
    }

    private WarrantyCardUsageRecords requireUsageRecord(String recordId) {
        if (!StringUtils.hasText(recordId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "申请记录ID不能为空");
        }
        WarrantyCardUsageRecords record = usageRecordsService.getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "保修使用申请不存在");
        }
        return record;
    }

    private UserAccounts requireUser(String userId) {
        UserAccounts user = userAccountsService.getById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在或已禁用");
        }
        return user;
    }

    private Products requireProduct(String productId) {
        Products product = productsService.getById(productId);
        if (product == null || !Objects.equals(product.getIsDelete(), 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在");
        }
        return product;
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日期格式错误，应为 yyyy-MM-dd");
        }
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER);
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
        if (Objects.equals(status, 2)) {
            return "已过期";
        }
        if (Objects.equals(status, 3)) {
            return "已使用";
        }
        return "未知状态";
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

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String buildCardNo(String id) {
        return "BW" + id.substring(2);
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
