package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.ContentCheckLogs;
import com.example.backend.entity.ImageReviewQueue;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.*;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ContentCheckLogsService;
import com.example.backend.service.ImageReviewQueueService;
import com.example.backend.service.ImagesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** 内容审核管理 — 仅超级管理员可用。 提供审核日志查看、存疑内容复审、图片/视频审核队列管理和改判。 */
@Tag(name = "管理员端/内容审核", description = "审核日志查看、存疑内容复审、图片/视频审核队列、改判")
@RestController
@RequestMapping("/admin/content-check")
public class AdminContentCheckController {

    private final ContentCheckLogsService logsService;
    private final ImageReviewQueueService imageReviewQueueService;
    private final ImagesService imagesService;

    public AdminContentCheckController(
            ContentCheckLogsService logsService,
            ImageReviewQueueService imageReviewQueueService,
            ImagesService imagesService) {
        this.logsService = logsService;
        this.imageReviewQueueService = imageReviewQueueService;
        this.imagesService = imagesService;
    }

    private void requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作内容审核");
        }
    }

    // ==================== 审核日志 ====================

    /** 分页查看所有检测日志。 */
    @Operation(summary = "查询日志列表")
    @GetMapping("/logs")
    public Result<Page<AdminContentCheckLogItemResponse>> listLogs(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1")
                    Integer page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20")
                    Integer size,
            @Parameter(description = "提交人账号ID（可选）") @RequestParam(required = false)
                    String accountId,
            @Parameter(description = "审核结果：1-通过，2-拦截，4-存疑（可选）") @RequestParam(required = false)
                    Integer checkResult,
            @Parameter(description = "开始时间戳（可选）") @RequestParam(required = false) Long startTime,
            @Parameter(description = "结束时间戳（可选）") @RequestParam(required = false) Long endTime) {
        requireSuperAdmin();
        Page<ContentCheckLogs> logPage =
                logsService.pageLogs(page, size, accountId, checkResult, startTime, endTime);
        Page<AdminContentCheckLogItemResponse> result = new Page<>(page, size, logPage.getTotal());
        result.setRecords(logPage.getRecords().stream().map(this::toLogResponse).toList());
        return Result.success(result);
    }

    // ==================== 存疑复审（文字） ====================

    /** 分页查看存疑复审列表（watch 的文字内容）。 */
    @Operation(summary = "查询复审列表")
    @GetMapping("/reviews")
    public Result<Page<AdminWatchReviewItemResponse>> listReviews(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1")
                    Integer page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20")
                    Integer size,
            @Parameter(description = "内容类型：1-文字，2-图片（可选）") @RequestParam(required = false)
                    Integer contentType) {
        requireSuperAdmin();
        Page<ContentCheckLogs> logPage = logsService.pageWatchLogs(page, size, contentType);

        // 同时查询图片审核队列中 status=4（存疑）的记录
        Page<ImageReviewQueue> imageWatchPage =
                imageReviewQueueService.pageQueue(
                        1, 1000, ImageReviewQueueService.STATUS_WATCH, null);

        // 合并文字和图片存疑记录
        List<AdminWatchReviewItemResponse> items = new ArrayList<>();
        for (ContentCheckLogs log : logPage.getRecords()) {
            items.add(toWatchResponseFromLog(log));
        }
        if (imageWatchPage.getRecords() != null) {
            for (ImageReviewQueue queue : imageWatchPage.getRecords()) {
                items.add(toWatchResponseFromQueue(queue));
            }
        }

        Page<AdminWatchReviewItemResponse> result =
                new Page<>(page, size, logPage.getTotal() + imageWatchPage.getTotal());
        result.setRecords(items);
        return Result.success(result);
    }

    /** 超管改判文字审核结果。 */
    @Operation(summary = "提交改判TextReview")
    @PostMapping("/reviews/{id}/override")
    public Result<Void> overrideTextReview(
            @Parameter(description = "审核日志ID") @PathVariable String id,
            @Valid @RequestBody AdminReviewOverrideRequest request) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();

        ContentCheckLogs log = logsService.getById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审核记录不存在");
        }

        // newStatus: 1-通过，2-拦截（将 watch 改判为通过或拦截）
        int newCheckResult = request.getNewStatus();
        if (newCheckResult != ContentCheckLogsService.RESULT_PASS
                && newCheckResult != ContentCheckLogsService.RESULT_BLOCK) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "改判结果只能为 1(通过) 或 2(拦截)");
        }

        log.setCheckResult(newCheckResult);
        log.setHitKeyword(request.getRemark() != null ? request.getRemark() : "管理员改判");
        logsService.updateById(log);

        return Result.success();
    }

    // ==================== 图片/视频审核队列 ====================

    /** 分页查看图片/视频审核队列。 */
    @Operation(summary = "查询图片队列")
    @GetMapping("/images")
    public Result<Page<AdminImageReviewItemResponse>> listImages(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1")
                    Integer page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20")
                    Integer size,
            @Parameter(description = "审核状态：1-待审核，2-通过，3-拒绝，4-存疑（可选）")
                    @RequestParam(required = false)
                    Integer status,
            @Parameter(description = "业务类型：AVATAR/STORE/GOODS/CASE/VIDEO/FAULT（可选）")
                    @RequestParam(required = false)
                    String businessType) {
        requireSuperAdmin();
        Page<ImageReviewQueue> queuePage =
                imageReviewQueueService.pageQueue(page, size, status, businessType);
        Page<AdminImageReviewItemResponse> result = new Page<>(page, size, queuePage.getTotal());
        result.setRecords(
                queuePage.getRecords().stream().map(this::toImageReviewResponse).toList());
        return Result.success(result);
    }

    /** 超管改判图片/视频审核结果。 */
    @Operation(summary = "提交改判ImageReview")
    @PostMapping("/images/{id}/override")
    public Result<Void> overrideImageReview(
            @Parameter(description = "图片审核队列ID") @PathVariable String id,
            @Valid @RequestBody AdminReviewOverrideRequest request) {
        requireSuperAdmin();
        LoginUserInfo user = AuthUserContext.get();

        int newStatus = request.getNewStatus();
        if (newStatus != ImageReviewQueueService.STATUS_PASS
                && newStatus != ImageReviewQueueService.STATUS_BLOCK) {
            throw new BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "改判结果只能为 "
                            + ImageReviewQueueService.STATUS_PASS
                            + "(通过) 或 "
                            + ImageReviewQueueService.STATUS_BLOCK
                            + "(拒绝)");
        }

        imageReviewQueueService.overrideReview(
                id, newStatus, user.getAccountId(), request.getRemark());
        return Result.success();
    }

    // ==================== Helper ====================

    private AdminContentCheckLogItemResponse toLogResponse(ContentCheckLogs log) {
        AdminContentCheckLogItemResponse resp = new AdminContentCheckLogItemResponse();
        resp.setId(log.getId());
        resp.setAccountId(log.getAccountId());
        resp.setAccountType(log.getAccountType());
        resp.setContentType(log.getContentType());
        resp.setContent(log.getContent());
        resp.setCheckResult(log.getCheckResult());
        resp.setHitRuleId(log.getHitRuleId()); // 阿里云 suggestion
        resp.setHitKeyword(log.getHitKeyword()); // 阿里云 label
        resp.setSource(log.getSource());
        resp.setCreatedTime(log.getCreatedTime());
        return resp;
    }

    private AdminWatchReviewItemResponse toWatchResponseFromLog(ContentCheckLogs log) {
        AdminWatchReviewItemResponse resp = new AdminWatchReviewItemResponse();
        resp.setId(log.getId());
        resp.setReviewType(log.getContentType() == 1 ? "text" : "image");
        resp.setAccountId(log.getAccountId());
        resp.setAccountType(log.getAccountType());
        resp.setContent(log.getContent());
        resp.setSuggestion(log.getHitRuleId());
        resp.setLabel(log.getHitKeyword());
        resp.setLabelDesc(log.getContent());
        resp.setCreatedTime(log.getCreatedTime());
        return resp;
    }

    private AdminWatchReviewItemResponse toWatchResponseFromQueue(ImageReviewQueue queue) {
        AdminWatchReviewItemResponse resp = new AdminWatchReviewItemResponse();
        resp.setId(queue.getId());
        resp.setReviewType("FILE".equals(queue.getBusinessType()) ? "file" : "image");
        resp.setAccountId("");
        resp.setAccountType(0);
        resp.setContent(queue.getRejectReason());
        resp.setSuggestion("watch");
        resp.setLabel("");
        resp.setLabelDesc(queue.getRejectReason());
        resp.setCreatedTime(queue.getCreatedTime());

        if (queue.getImageId() != null) {
            Images img = imagesService.getById(queue.getImageId());
            if (img != null) {
                resp.setImageUrl(img.getFileUrl());
            }
        }
        return resp;
    }

    private AdminImageReviewItemResponse toImageReviewResponse(ImageReviewQueue queue) {
        AdminImageReviewItemResponse resp = new AdminImageReviewItemResponse();
        resp.setId(queue.getId());
        resp.setImageId(queue.getImageId());
        resp.setBusinessType(queue.getBusinessType());
        resp.setBusinessId(queue.getBusinessId());
        resp.setStatus(queue.getStatus());
        resp.setRejectReason(queue.getRejectReason());
        resp.setReviewTime(queue.getReviewTime());
        resp.setCreatedTime(queue.getCreatedTime());

        if (queue.getImageId() != null) {
            Images img = imagesService.getById(queue.getImageId());
            if (img != null) {
                resp.setImageUrl(img.getFileUrl());
            }
        }
        return resp;
    }
}
