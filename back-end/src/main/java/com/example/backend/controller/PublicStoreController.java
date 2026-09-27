package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.Images;
import com.example.backend.entity.StoreBusinessHours;
import com.example.backend.entity.Stores;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserStoreDetailResponse;
import com.example.backend.service.ImagesService;
import com.example.backend.service.StoreBusinessHoursService;
import com.example.backend.service.StoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "公开/门店展示")
@RequestMapping("/pass/stores")
public class PublicStoreController {

    private static final int AUDIT_STATUS_PASSED = 2;
    private static final int RATING_MIN_COUNT = 3;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] DAY_LABELS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final StoresService storesService;
    private final StoreBusinessHoursService storeBusinessHoursService;
    private final ImagesService imagesService;

    public PublicStoreController(
            StoresService storesService,
            StoreBusinessHoursService storeBusinessHoursService,
            ImagesService imagesService) {
        this.storesService = storesService;
        this.storeBusinessHoursService = storeBusinessHoursService;
        this.imagesService = imagesService;
    }

    @Operation(summary = "查询门店公开详情")
    @GetMapping("/{id}")
    public Result<UserStoreDetailResponse> getStoreDetail(@PathVariable("id") String id) {
        String storeId = id == null ? null : id.trim();
        if (!StringUtils.hasText(storeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门店ID不能为空");
        }

        Stores store =
                storesService.getOne(
                        new LambdaQueryWrapper<Stores>()
                                .eq(Stores::getId, storeId)
                                .eq(Stores::getAuditStatus, AUDIT_STATUS_PASSED)
                                .eq(Stores::getIsDelete, 0)
                                .last("limit 1"),
                        false);
        if (store == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门店不存在");
        }

        List<StoreBusinessHours> hoursList =
                storeBusinessHoursService.list(
                        new LambdaQueryWrapper<StoreBusinessHours>()
                                .eq(StoreBusinessHours::getStoreId, storeId)
                                .eq(StoreBusinessHours::getIsDelete, 0)
                                .orderByAsc(StoreBusinessHours::getDayOfWeek));

        String logoUrl = resolveLogoUrl(store.getLogoImageId());
        int ratingCount = store.getRatingCount() == null ? 0 : store.getRatingCount();
        BigDecimal rating = ratingCount >= RATING_MIN_COUNT ? store.getRating() : null;

        UserStoreDetailResponse resp = new UserStoreDetailResponse();
        resp.setId(store.getId());
        resp.setName(store.getName() == null ? "" : store.getName());
        resp.setLogoUrl(logoUrl);
        resp.setRating(rating);
        resp.setRatingCount(ratingCount);
        resp.setIsOnline(store.getIsOnline() == null ? 0 : store.getIsOnline());
        resp.setDescription(store.getDescription() == null ? "" : store.getDescription());
        resp.setContactPhone(store.getContactPhone() == null ? "" : store.getContactPhone());
        resp.setAddress(store.getAddress() == null ? "" : store.getAddress());
        resp.setBusinessHours(buildHoursList(hoursList));
        return Result.success(resp);
    }

    private String resolveLogoUrl(String logoImageId) {
        if (!StringUtils.hasText(logoImageId)) {
            return null;
        }
        Images image = imagesService.getById(logoImageId);
        return image != null ? image.getFileUrl() : null;
    }

    private List<UserStoreDetailResponse.StoreHoursItem> buildHoursList(
            List<StoreBusinessHours> hoursList) {
        List<UserStoreDetailResponse.StoreHoursItem> result = new ArrayList<>();
        for (StoreBusinessHours hours : hoursList) {
            UserStoreDetailResponse.StoreHoursItem item =
                    new UserStoreDetailResponse.StoreHoursItem();
            int day = hours.getDayOfWeek() == null ? 0 : hours.getDayOfWeek();
            item.setDayOfWeek(day);
            item.setDayLabel(day >= 1 && day <= 7 ? DAY_LABELS[day] : "");
            item.setStartTime(
                    hours.getStartTime() != null ? hours.getStartTime().format(TIME_FMT) : "");
            item.setEndTime(hours.getEndTime() != null ? hours.getEndTime().format(TIME_FMT) : "");
            item.setIsAvailable(hours.getIsAvailable() == null ? 0 : hours.getIsAvailable());
            result.add(item);
        }
        return result;
    }
}
