package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.ImageReviewQueue;
import com.example.backend.service.contentcheck.CheckResult;

/** 图片/视频审核队列 Service */
public interface ImageReviewQueueService extends IService<ImageReviewQueue> {

    /** 审核状态常量 */
    int STATUS_PENDING = 1; // 待审核

    int STATUS_PASS = 2; // 审核通过
    int STATUS_BLOCK = 3; // 审核拒绝
    int STATUS_WATCH = 4; // 存疑放行（待复审）

    /**
     * 图片上传后同步检测并入队。调阿里云审核，blocked 时抛 BusinessException（调用方应删 OSS 文件）， pass/watch 时保存队列记录并返回检测结果。
     */
    CheckResult checkAndEnqueueImage(
            String imageId, String businessType, String businessId, String imageUrl);

    /** 视频/文件上传后同步检测并入队。逻辑同 checkAndEnqueueImage。 */
    CheckResult checkAndEnqueueFile(
            String fileId, String businessType, String businessId, String fileUrl);

    /** 仅同步检测图片，blocked 时抛 BusinessException，不入审核队列。上传端点用。 */
    void checkImageOnly(String imageUrl);

    /** 仅同步检测文件/视频，blocked 时抛 BusinessException，不入审核队列。上传端点用。 */
    void checkFileOnly(String fileUrl);

    /** 超管改判审核结果。 */
    void overrideReview(String queueId, int newStatus, String reviewerId, String remark);

    /** 分页查询审核队列。 */
    Page<ImageReviewQueue> pageQueue(int page, int size, Integer status, String businessType);
}
