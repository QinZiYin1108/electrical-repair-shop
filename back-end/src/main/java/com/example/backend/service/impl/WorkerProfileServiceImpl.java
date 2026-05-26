package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Images;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianProfiles;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerCertificationRequest;
import com.example.backend.model.worker.WorkerProfileResponse;
import com.example.backend.model.worker.WorkerUpdateProfileRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ImagesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianProfilesService;
import com.example.backend.service.WorkerProfileService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class WorkerProfileServiceImpl implements WorkerProfileService {

    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "^[1-9]\\d{5}(19\\d{2}|20\\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$"
    );

    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianProfilesService technicianProfilesService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;

    public WorkerProfileServiceImpl(
        TechnicianAccountsService technicianAccountsService,
        TechnicianProfilesService technicianProfilesService,
        ImagesService imagesService,
        OssUtil ossUtil
    ) {
        this.technicianAccountsService = technicianAccountsService;
        this.technicianProfilesService = technicianProfilesService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
    }

    @Override
    public WorkerProfileResponse getCurrentWorkerProfile() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }

        TechnicianProfiles profile = technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, accountId)
                .eq(TechnicianProfiles::getIsDelete, 0),
            false
        );

        String avatarUrl = resolveAvatarUrl(accountId);

        WorkerProfileResponse resp = new WorkerProfileResponse();
        resp.setId(technician.getId());
        resp.setUsername(technician.getUsername());
        resp.setPhone(technician.getPhone());
        resp.setEmail(technician.getEmail());
        resp.setAccountStatus(technician.getAccountStatus());
        resp.setWorkStatus(technician.getWorkStatus());
        resp.setRating(technician.getRating());
        resp.setOrderCount(technician.getOrderCount());
        resp.setCompletionRate(technician.getCompletionRate());
        resp.setAvatarUrl(avatarUrl);

        if (profile != null) {
            resp.setRealName(profile.getRealName());
            resp.setIdCard(profile.getIdCard());
            resp.setGender(profile.getGender());
            if (profile.getBirthday() != null) {
                resp.setBirthday(profile.getBirthday());
            }
            resp.setWorkYears(profile.getWorkYears());
            resp.setEducation(profile.getEducation());
            resp.setIntroduction(profile.getIntroduction());
            resp.setResponseTime(profile.getResponseTime());
            resp.setLocationUpdateTime(profile.getLocationUpdateTime());
        }
        return resp;
    }

    @Override
    public void updateCurrentWorkerProfile(WorkerUpdateProfileRequest request) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }
        long now = System.currentTimeMillis();

        if (request != null) {
            if (StringUtils.hasText(request.getUsername())) {
                technician.setUsername(request.getUsername().trim());
            }
        }
        technician.setUpdatedTime(now);
        technicianAccountsService.updateById(technician);

        TechnicianProfiles profile = technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, accountId)
                .eq(TechnicianProfiles::getIsDelete, 0),
            false
        );
        boolean isNew = profile == null;
        if (isNew) {
            profile = new TechnicianProfiles();
            profile.setId(SnowflakeIdUtil.nextTechnicianProfileId());
            profile.setTechnicianAccountId(accountId);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }

        if (request != null) {
            if (request.getGender() != null) {
                profile.setGender(request.getGender());
            }
            if (request.getBirthday() != null) {
                profile.setBirthday(normalizeBirthday(request.getBirthday()));
            }
            if (request.getWorkYears() != null) {
                profile.setWorkYears(request.getWorkYears());
            }
            if (request.getEducation() != null) {
                profile.setEducation(request.getEducation());
            }
            if (request.getIntroduction() != null) {
                profile.setIntroduction(request.getIntroduction());
            }
            if (request.getResponseTime() != null) {
                profile.setResponseTime(request.getResponseTime());
            }
        }

        if (profile.getWorkYears() == null) {
            profile.setWorkYears(0);
        }
        if (profile.getResponseTime() == null) {
            profile.setResponseTime(0);
        }

        profile.setUpdatedTime(now);
        if (isNew) {
            technicianProfilesService.save(profile);
        } else {
            technicianProfilesService.updateById(profile);
        }
    }

    @Override
    public void submitCertification(WorkerCertificationRequest request) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        String realName = request.getRealName() == null ? "" : request.getRealName().trim();
        String idCard = request.getIdCard() == null ? "" : request.getIdCard().trim();
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号格式不正确");
        }
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "真实姓名不能为空");
        }
        if (!StringUtils.hasText(idCard)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "身份证号不能为空");
        }
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "身份证号格式不正确");
        }

        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }

        long now = System.currentTimeMillis();
        technician.setPhone(phone);
        technician.setAccountStatus(1);
        technician.setUpdatedTime(now);
        technicianAccountsService.updateById(technician);

        TechnicianProfiles profile = technicianProfilesService.getOne(
            new LambdaQueryWrapper<TechnicianProfiles>()
                .eq(TechnicianProfiles::getTechnicianAccountId, accountId)
                .eq(TechnicianProfiles::getIsDelete, 0),
            false
        );
        boolean isNew = profile == null;
        if (isNew) {
            profile = new TechnicianProfiles();
            profile.setId(SnowflakeIdUtil.nextTechnicianProfileId());
            profile.setTechnicianAccountId(accountId);
            profile.setCreatedTime(now);
            profile.setIsDelete(0);
        }
        profile.setRealName(realName);
        profile.setIdCard(idCard.toUpperCase());
        profile.setUpdatedTime(now);
        if (isNew) {
            technicianProfilesService.save(profile);
        } else {
            technicianProfilesService.updateById(profile);
        }
    }

    @Override
    public String uploadCurrentWorkerAvatar(MultipartFile file) {
        LoginUserInfo user = requireWorker();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        UploadLimitUtil.validateImageSize(file);
        String accountId = user.getAccountId();
        TechnicianAccounts technician = technicianAccountsService.getById(accountId);
        if (technician == null || (technician.getIsDelete() != null && technician.getIsDelete() != 0)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "师傅账号不存在");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = "avatar";
        }
        String ext = "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx >= 0 && idx < originalFilename.length() - 1) {
            ext = originalFilename.substring(idx);
        }
        String objectName = "avatars/" + accountId + "/" + UUID.randomUUID() + ext;
        String url;
        try (InputStream in = file.getInputStream()) {
            url = ossUtil.upload(objectName, in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传头像失败");
        }

        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName(originalFilename);
        image.setFileName(objectName);
        image.setFilePath(objectName);
        image.setFileUrl(url);
        image.setFileSize(file.getSize());
        String mimeType = file.getContentType();
        image.setMimeType(StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream");
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage != null) {
                image.setWidth(bufferedImage.getWidth());
                image.setHeight(bufferedImage.getHeight());
            }
        } catch (Exception ignored) {
        }
        image.setUploaderId(accountId);
        image.setUploaderType(2);
        image.setBusinessType("AVATAR");
        image.setBusinessId(accountId);
        long now = System.currentTimeMillis();
        image.setCreatedTime(now);
        image.setIsDelete(0);
        imagesService.save(image);
        return url;
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        if (!StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return user;
    }

    private String resolveAvatarUrl(String accountId) {
        Images avatarImage = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .eq(Images::getBusinessId, accountId)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
                .last("limit 1"),
            false
        );
        return avatarImage != null ? avatarImage.getFileUrl() : null;
    }

    private Long normalizeBirthday(Long birthdayMillis) {
        if (birthdayMillis == null) {
            return null;
        }
        if (birthdayMillis <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "生日时间戳不正确");
        }
        LocalDate localDate = Instant.ofEpochMilli(birthdayMillis).atZone(SHANGHAI_ZONE).toLocalDate();
        String formatted = localDate.format(BIRTHDAY_FORMATTER);
        LocalDate normalized = LocalDate.parse(formatted, BIRTHDAY_FORMATTER);
        long normalizedMillis = normalized.atStartOfDay(SHANGHAI_ZONE).toInstant().toEpochMilli();
        return normalizedMillis;
    }
}
