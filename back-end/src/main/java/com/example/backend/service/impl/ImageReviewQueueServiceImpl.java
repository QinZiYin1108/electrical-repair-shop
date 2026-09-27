package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.ImageReviewQueue;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ImageReviewQueueMapper;
import com.example.backend.service.ImageReviewQueueService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.contentcheck.AliyunGreenClient;
import com.example.backend.service.contentcheck.CheckResult;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 图片/视频审核队列 Service 实现。 上传后同步调用阿里云检测，blocked 直接拒绝（由调用方清理 OSS），pass/watch 入队。 */
@Service
public class ImageReviewQueueServiceImpl
        extends ServiceImpl<ImageReviewQueueMapper, ImageReviewQueue>
        implements ImageReviewQueueService {

    /** 审核状态常量 */
    public static final int REVIEW_STATUS_APPROVED = 2;

    public static final int REVIEW_STATUS_REJECTED = 3;

    private static final Logger log = LoggerFactory.getLogger(ImageReviewQueueServiceImpl.class);

    private final AliyunGreenClient aliyunGreenClient;
    private final ImagesService imagesService;

    public ImageReviewQueueServiceImpl(
            AliyunGreenClient aliyunGreenClient, ImagesService imagesService) {
        this.aliyunGreenClient = aliyunGreenClient;
        this.imagesService = imagesService;
    }

    @Override
    public CheckResult checkAndEnqueueImage(
            String imageId, String businessType, String businessId, String imageUrl) {
        CheckResult result = aliyunGreenClient.checkImage(imageUrl);
        if (result.isBlocked()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "图片包含违规内容："
                            + (result.getLabelDesc() != null
                                    ? result.getLabelDesc()
                                    : result.getLabel())
                            + "，请更换后重新上传");
        }
        ImageReviewQueue queue = createQueueRecord(imageId, businessType, businessId);
        fillReviewResult(queue, result);
        save(queue);
        log.info(
                "图片审核入队: imageId={}, bizType={}, action={}",
                imageId,
                businessType,
                result.getAction());
        return result;
    }

    @Override
    public CheckResult checkAndEnqueueFile(
            String fileId, String businessType, String businessId, String fileUrl) {
        CheckResult result = aliyunGreenClient.checkFile(fileUrl);
        if (result.isBlocked()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "文件包含违规内容："
                            + (result.getLabelDesc() != null
                                    ? result.getLabelDesc()
                                    : result.getLabel())
                            + "，请更换后重新上传");
        }
        ImageReviewQueue queue = createQueueRecord(fileId, businessType, businessId);
        fillReviewResult(queue, result);
        save(queue);
        log.info(
                "文件审核入队: fileId={}, bizType={}, action={}",
                fileId,
                businessType,
                result.getAction());
        return result;
    }

    @Override
    public void checkImageOnly(String imageUrl) {
        CheckResult result = aliyunGreenClient.checkImage(imageUrl);
        if (result.isBlocked()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "图片包含违规内容："
                            + (result.getLabelDesc() != null
                                    ? result.getLabelDesc()
                                    : result.getLabel())
                            + "，请更换后重新上传");
        }
    }

    @Override
    public void checkFileOnly(String fileUrl) {
        CheckResult result = aliyunGreenClient.checkFile(fileUrl);
        if (result.isBlocked()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "文件包含违规内容："
                            + (result.getLabelDesc() != null
                                    ? result.getLabelDesc()
                                    : result.getLabel())
                            + "，请更换后重新上传");
        }
    }

    @Override
    public void overrideReview(String queueId, int newStatus, String reviewerId, String remark) {
        ImageReviewQueue queue = getById(queueId);
        if (queue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审核记录不存在");
        }
        long now = System.currentTimeMillis();
        queue.setStatus(newStatus);
        queue.setReviewerId(reviewerId);
        queue.setRejectReason(remark);
        queue.setReviewTime(now);
        queue.setUpdatedTime(now);
        updateById(queue);

        // 同步更新 images.review_status
        if (queue.getImageId() != null) {
            Images image = imagesService.getById(queue.getImageId());
            if (image != null) {
                image.setReviewStatus(
                        newStatus == STATUS_PASS ? REVIEW_STATUS_APPROVED : REVIEW_STATUS_REJECTED);
                imagesService.updateById(image);
                log.info(
                        "图片审核状态同步: imageId={}, reviewStatus={}",
                        image.getId(),
                        image.getReviewStatus());
            }
        }

        log.info("超管改判审核结果: queueId={}, newStatus={}, reviewer={}", queueId, newStatus, reviewerId);
    }

    @Override
    public Page<ImageReviewQueue> pageQueue(
            int page, int size, Integer status, String businessType) {
        LambdaQueryWrapper<ImageReviewQueue> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ImageReviewQueue::getStatus, status);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(ImageReviewQueue::getBusinessType, businessType);
        }
        wrapper.orderByDesc(ImageReviewQueue::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }

    // ==================== 内部方法 ====================

    private ImageReviewQueue createQueueRecord(
            String fileId, String businessType, String businessId) {
        long now = System.currentTimeMillis();
        ImageReviewQueue queue = new ImageReviewQueue();
        queue.setId(SnowflakeIdUtil.nextImageReviewQueueId());
        queue.setImageId(fileId);
        queue.setBusinessType(businessType);
        queue.setBusinessId(businessId);
        queue.setStatus(STATUS_PENDING);
        queue.setCreatedTime(now);
        queue.setUpdatedTime(now);
        queue.setVersion(1);
        queue.setIsDelete(0);
        return queue;
    }

    private void fillReviewResult(ImageReviewQueue queue, CheckResult result) {
        long now = System.currentTimeMillis();
        if (result.isWatch()) {
            queue.setStatus(STATUS_WATCH);
            queue.setRejectReason(result.getLabelDesc());
        } else {
            queue.setStatus(STATUS_PASS);
        }
        queue.setReviewTime(now);
        queue.setUpdatedTime(now);
    }
}
