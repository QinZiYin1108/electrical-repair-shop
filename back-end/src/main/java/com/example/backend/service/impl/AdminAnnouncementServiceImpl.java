package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Announcements;
import com.example.backend.entity.Images;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminAnnouncementCreateRequest;
import com.example.backend.model.admin.AdminAnnouncementResponse;
import com.example.backend.model.admin.AdminAnnouncementUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminAnnouncementService;
import com.example.backend.service.AnnouncementsService;
import com.example.backend.service.ImagesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminAnnouncementServiceImpl implements AdminAnnouncementService {

    private static final int CHANNEL_BANNER = 1;
    private static final int CHANNEL_NOTICE = 2;
    private static final int CONTENT_TYPE_IMAGE = 1;
    private static final int CONTENT_TYPE_TEXT = 2;
    private static final String BUSINESS_TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    private static final int BANNER_RECOMMEND_WIDTH = 702;
    private static final int BANNER_RECOMMEND_HEIGHT = 250;
    private static final double BANNER_RECOMMEND_RATIO = BANNER_RECOMMEND_WIDTH * 1.0 / BANNER_RECOMMEND_HEIGHT;
    private static final double BANNER_RATIO_TOLERANCE = 0.03D;

    private final AnnouncementsService announcementsService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;

    public AdminAnnouncementServiceImpl(
        AnnouncementsService announcementsService,
        ImagesService imagesService,
        OssUtil ossUtil
    ) {
        this.announcementsService = announcementsService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public List<AdminAnnouncementResponse> listAnnouncements(Integer channel) {
        LambdaQueryWrapper<Announcements> wrapper = new LambdaQueryWrapper<Announcements>()
            .orderByAsc(Announcements::getSortOrder)
            .orderByDesc(Announcements::getCreatedTime);
        if (channel != null) {
            validateChannel(channel);
            wrapper.eq(Announcements::getChannel, channel);
        }

        List<Announcements> list = announcementsService.list(wrapper);
        Map<String, String> imageMap = loadLatestImageUrlMap(
            list.stream().map(Announcements::getId).collect(Collectors.toList())
        );

        List<AdminAnnouncementResponse> responses = new ArrayList<>();
        for (Announcements item : list) {
            responses.add(toResponse(item, imageMap.get(item.getId())));
        }
        return responses;
    }

    @Override
    public AdminAnnouncementResponse createAnnouncement(AdminAnnouncementCreateRequest request) {
        validateCreateOrUpdateRequest(request.getChannel(), request.getContentType(), request.getStartTime(), request.getEndTime());
        long now = System.currentTimeMillis();

        Announcements announcement = new Announcements();
        announcement.setId(SnowflakeIdUtil.nextAnnouncementId());
        fillAnnouncementByRequest(announcement, request, true);
        announcement.setCreatedTime(now);
        announcement.setUpdatedTime(now);
        announcement.setIsDelete(0);

        boolean ok = announcementsService.save(announcement);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建公告失败");
        }
        return toResponse(announcement, null);
    }

    @Override
    public AdminAnnouncementResponse updateAnnouncement(String id, AdminAnnouncementUpdateRequest request) {
        Announcements current = announcementsService.getById(id);
        if (current == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }

        validateCreateOrUpdateRequest(request.getChannel(), request.getContentType(), request.getStartTime(), request.getEndTime());
        fillAnnouncementByRequest(current, request, false);
        current.setUpdatedTime(System.currentTimeMillis());

        boolean ok = announcementsService.updateById(current);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新公告失败");
        }

        String imageUrl = loadLatestImageUrlMap(List.of(current.getId())).get(current.getId());
        return toResponse(current, imageUrl);
    }

    @Override
    public void deleteAnnouncement(String id) {
        Announcements current = announcementsService.getById(id);
        if (current == null) {
            return;
        }
        boolean ok = announcementsService.removeById(id);
        if (!ok) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除公告失败");
        }
    }

    @Override
    public String uploadAnnouncementImage(String id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先选择图片文件");
        }
        UploadLimitUtil.validateImageSize(file);
        Announcements announcement = announcementsService.getById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        if (!Objects.equals(announcement.getChannel(), CHANNEL_BANNER)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有轮播图公告支持上传图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取上传文件失败");
        }

        Integer width = null;
        Integer height = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                width = image.getWidth();
                height = image.getHeight();
            }
        } catch (Exception ignored) {
        }
        validateBannerImage(width, height);

        String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("announcement");
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot);
        }
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectName = "announcements/" + id + "/" + date + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        String url = ossUtil.upload(objectName, new ByteArrayInputStream(bytes));

        LoginUserInfo user = AuthUserContext.get();
        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName(originalName);
        image.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        image.setMimeType(file.getContentType());
        image.setWidth(width);
        image.setHeight(height);
        image.setUploaderId(user == null ? null : user.getAccountId());
        image.setUploaderType(3);
        image.setBusinessType(BUSINESS_TYPE_ANNOUNCEMENT);
        image.setBusinessId(id);
        image.setCreatedTime(System.currentTimeMillis());
        image.setIsDelete(0);

        boolean saveOk = imagesService.save(image);
        if (!saveOk) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存公告图片失败");
        }

        if (!Objects.equals(announcement.getContentType(), CONTENT_TYPE_IMAGE)) {
            announcement.setContentType(CONTENT_TYPE_IMAGE);
            announcement.setUpdatedTime(System.currentTimeMillis());
            announcementsService.updateById(announcement);
        }
        return url;
    }

    private void validateBannerImage(Integer width, Integer height) {
        if (width == null || height == null || width <= 0 || height <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请上传有效的图片文件");
        }
        if (width < BANNER_RECOMMEND_WIDTH || height < BANNER_RECOMMEND_HEIGHT) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "轮播图尺寸不能小于 " + BANNER_RECOMMEND_WIDTH + "×" + BANNER_RECOMMEND_HEIGHT
            );
        }
        double ratio = width * 1.0 / height;
        if (Math.abs(ratio - BANNER_RECOMMEND_RATIO) > BANNER_RATIO_TOLERANCE) {
            throw new BusinessException(
                ErrorCode.PARAM_ERROR,
                "轮播图图片比例需要接近 " + BANNER_RECOMMEND_WIDTH + ":" + BANNER_RECOMMEND_HEIGHT
            );
        }
    }

    private void fillAnnouncementByRequest(Announcements announcement, AdminAnnouncementCreateRequest request, boolean createMode) {
        announcement.setChannel(request.getChannel());
        if (Objects.equals(request.getChannel(), CHANNEL_NOTICE)) {
            announcement.setContentType(CONTENT_TYPE_TEXT);
        } else {
            announcement.setContentType(request.getContentType());
        }
        announcement.setTitle(trimToNull(request.getTitle()));
        announcement.setSubtitle(trimToNull(request.getSubtitle()));
        announcement.setContent(trimToNull(request.getContent()));
        announcement.setEmoji(trimToNull(request.getEmoji()));
        announcement.setIsActive(defaultIfNull(request.getIsActive(), 1));
        announcement.setSortOrder(defaultIfNull(request.getSortOrder(), 0));
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        if (createMode && announcement.getIsDelete() == null) {
            announcement.setIsDelete(0);
        }
    }

    private void fillAnnouncementByRequest(Announcements announcement, AdminAnnouncementUpdateRequest request, boolean createMode) {
        AdminAnnouncementCreateRequest payload = new AdminAnnouncementCreateRequest();
        payload.setChannel(request.getChannel());
        payload.setContentType(request.getContentType());
        payload.setTitle(request.getTitle());
        payload.setSubtitle(request.getSubtitle());
        payload.setContent(request.getContent());
        payload.setEmoji(request.getEmoji());
        payload.setIsActive(request.getIsActive());
        payload.setSortOrder(request.getSortOrder());
        payload.setStartTime(request.getStartTime());
        payload.setEndTime(request.getEndTime());
        fillAnnouncementByRequest(announcement, payload, createMode);
    }

    private void validateCreateOrUpdateRequest(Integer channel, Integer contentType, Long startTime, Long endTime) {
        validateChannel(channel);
        validateContentType(channel, contentType);
        if (startTime != null && startTime <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间不正确");
        }
        if (endTime != null && endTime <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间不正确");
        }
        if (startTime != null && endTime != null && endTime < startTime) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间不能早于开始时间");
        }
    }

    private void validateChannel(Integer channel) {
        if (!Objects.equals(channel, CHANNEL_BANNER) && !Objects.equals(channel, CHANNEL_NOTICE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告渠道参数错误");
        }
    }

    private void validateContentType(Integer channel, Integer contentType) {
        if (Objects.equals(channel, CHANNEL_NOTICE)) {
            return;
        }
        if (!Objects.equals(contentType, CONTENT_TYPE_IMAGE) && !Objects.equals(contentType, CONTENT_TYPE_TEXT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "内容类型参数错误");
        }
    }

    private Map<String, String> loadLatestImageUrlMap(List<String> announcementIds) {
        if (announcementIds == null || announcementIds.isEmpty()) {
            return Map.of();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, BUSINESS_TYPE_ANNOUNCEMENT)
                .in(Images::getBusinessId, announcementIds)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, String> result = new HashMap<>();
        for (Images image : images) {
            if (!result.containsKey(image.getBusinessId()) && StringUtils.hasText(image.getFileUrl())) {
                result.put(image.getBusinessId(), image.getFileUrl());
            }
        }
        return result;
    }

    private AdminAnnouncementResponse toResponse(Announcements item, String imageUrl) {
        AdminAnnouncementResponse response = new AdminAnnouncementResponse();
        response.setId(item.getId());
        response.setChannel(item.getChannel());
        response.setContentType(item.getContentType());
        response.setTitle(item.getTitle());
        response.setSubtitle(item.getSubtitle());
        response.setContent(item.getContent());
        response.setEmoji(item.getEmoji());
        response.setIsActive(item.getIsActive());
        response.setSortOrder(item.getSortOrder());
        response.setStartTime(item.getStartTime());
        response.setEndTime(item.getEndTime());
        response.setCreatedTime(item.getCreatedTime());
        response.setUpdatedTime(item.getUpdatedTime());
        response.setImageUrl(imageUrl);
        return response;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}


