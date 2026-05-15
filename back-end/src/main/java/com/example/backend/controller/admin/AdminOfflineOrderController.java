package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianSkills;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianSkillsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/orders/offline")
public class AdminOfflineOrderController {

    private static final int SERVICE_MODE_OFFLINE_REPAIR = 3;
    private static final int TECHNICIAN_ACCOUNT_ACTIVE = 1;
    private static final int TECHNICIAN_WORK_ONLINE = 1;
    private static final int TECHNICIAN_WORK_BUSY = 2;
    private static final int USER_STATUS_NORMAL = 1;
    private static final int ADMIN_UPLOADER_TYPE = 3;
    private static final BigDecimal BIG_DECIMAL_ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FAULT_MEDIA_BUSINESS_TYPE = "REPAIR_ORDER_FAULT";

    private final UserAccountsService userAccountsService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final ServiceTypesService serviceTypesService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final TechnicianAccountsService technicianAccountsService;
    private final TechnicianSkillsService technicianSkillsService;
    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final OssUtil ossUtil;

    public AdminOfflineOrderController(
        UserAccountsService userAccountsService,
        ServiceCategoriesService serviceCategoriesService,
        ServiceTypesService serviceTypesService,
        FaultPhenomenaService faultPhenomenaService,
        TechnicianAccountsService technicianAccountsService,
        TechnicianSkillsService technicianSkillsService,
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        RepairOrderFaultsService repairOrderFaultsService,
        ImagesService imagesService,
        VideosService videosService,
        OssUtil ossUtil
    ) {
        this.userAccountsService = userAccountsService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.serviceTypesService = serviceTypesService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.technicianAccountsService = technicianAccountsService;
        this.technicianSkillsService = technicianSkillsService;
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.ossUtil = ossUtil;
    }

    @GetMapping("/technicians")
    public Result<List<OfflineTechnicianOption>> listTechnicians(
        @RequestParam("serviceTypeId") String serviceTypeId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        requireAdmin();
        ServiceTypes serviceType = requireOfflineServiceType(serviceTypeId);

        List<TechnicianSkills> skillList = technicianSkillsService.list(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getServiceTypeId, serviceType.getId())
                .eq(TechnicianSkills::getIsActive, 1)
                .eq(TechnicianSkills::getIsDelete, 0)
        );
        if (skillList.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Set<String> technicianIds = skillList.stream()
            .map(TechnicianSkills::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (technicianIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        String normalizedKeyword = trimToNull(keyword);
        List<TechnicianAccounts> technicianList = technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, technicianIds)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .eq(TechnicianAccounts::getAccountStatus, TECHNICIAN_ACCOUNT_ACTIVE)
                .in(TechnicianAccounts::getWorkStatus, TECHNICIAN_WORK_ONLINE, TECHNICIAN_WORK_BUSY)
                .and(StringUtils.hasText(normalizedKeyword), wrapper -> wrapper
                    .like(TechnicianAccounts::getUsername, normalizedKeyword)
                    .or().like(TechnicianAccounts::getPhone, normalizedKeyword)
                    .or().like(TechnicianAccounts::getEmail, normalizedKeyword)
                )
                .orderByAsc(TechnicianAccounts::getWorkStatus)
                .orderByDesc(TechnicianAccounts::getRating)
                .orderByDesc(TechnicianAccounts::getOrderCount)
                .orderByDesc(TechnicianAccounts::getUpdatedTime)
        );
        if (technicianList.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, String> avatarMap = loadAvatarMap(
            technicianList.stream().map(TechnicianAccounts::getId).collect(Collectors.toList())
        );

        List<OfflineTechnicianOption> result = new ArrayList<>();
        for (TechnicianAccounts technician : technicianList) {
            OfflineTechnicianOption item = new OfflineTechnicianOption();
            item.setId(technician.getId());
            item.setName(safe(technician.getUsername()));
            item.setPhone(safe(technician.getPhone()));
            item.setEmail(safe(technician.getEmail()));
            item.setRating(formatDecimal(technician.getRating(), 1));
            item.setOrderCount(technician.getOrderCount() == null ? 0 : technician.getOrderCount());
            item.setWorkStatus(technician.getWorkStatus());
            item.setWorkStatusText(resolveWorkStatusText(technician.getWorkStatus()));
            item.setAvatarUrl(avatarMap.get(technician.getId()));
            result.add(item);
        }
        return Result.success(result);
    }

    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<OfflineUploadMediaResponse> uploadMedia(
        @RequestParam(value = "mediaType", required = false) String mediaType,
        @RequestPart("file") MultipartFile file
    ) {
        LoginUserInfo admin = requireAdmin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        String uploadType = resolveUploadMediaType(mediaType, file.getContentType(), file.getOriginalFilename());
        UploadLimitUtil.validateMediaSize(uploadType, file);
        String extension = resolveUploadExtension(file.getOriginalFilename(), file.getContentType(), uploadType);
        String objectName = "offline-orders/" + uploadType + "/" + UUID.randomUUID() + extension;

        String fileUrl;
        try (InputStream inputStream = file.getInputStream()) {
            fileUrl = ossUtil.upload(objectName, inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        }

        OfflineUploadMediaResponse response = new OfflineUploadMediaResponse();
        response.setUrl(fileUrl);
        response.setName(resolveMediaName(file.getOriginalFilename(), uploadType.equals("video") ? "fault-video.mp4" : "fault-image.jpg"));
        response.setFileSize(file.getSize());
        response.setMimeType(resolveUploadMimeType(file.getContentType(), uploadType));
        response.setUploaderId(admin.getAccountId());
        if ("image".equals(uploadType)) {
            fillImageSize(file, response);
        }
        return Result.success(response);
    }

    @PostMapping("/submit")
    @Transactional(rollbackFor = Exception.class)
    public Result<OfflineSubmitResponse> submitOfflineOrder(@RequestBody(required = false) OfflineSubmitRequest request) {
        LoginUserInfo admin = requireAdmin();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提交参数不能为空");
        }

        UserAccounts user = requireSelectableUser(request.getUserId());
        ServiceTypes serviceType = requireOfflineServiceType(request.getServiceTypeId());
        validateServiceCategory(serviceType, request.getCategoryId());
        TechnicianAccounts technician = requireSelectableTechnician(request.getTechnicianId());
        ensureTechnicianSkill(technician.getId(), serviceType.getId());

        long now = System.currentTimeMillis();
        String orderId = SnowflakeIdUtil.nextRepairOrderId();
        String orderNo = buildOrderNo(orderId);

        RepairOrders order = new RepairOrders();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setAccountId(user.getId());
        order.setTechnicianAccountId(technician.getId());
        order.setServiceTypeId(serviceType.getId());
        order.setApplianceBrand(trimToNull(request.getApplianceBrand()));
        order.setApplianceModel(trimToNull(request.getApplianceModel()));
        order.setPurchaseDate(parsePurchaseDate(request.getPurchaseDate()));
        order.setServiceAddressId(null);
        order.setAppointmentTime(now);
        order.setStatus(1);
        order.setPaymentStatus(1);
        order.setRemark(trimToNull(request.getRemark()));
        order.setCreatedTime(now);
        order.setUpdatedTime(now);
        order.setIsDelete(0);
        if (!repairOrdersService.save(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建线下订单失败");
        }

        RepairOrderPayments payment = new RepairOrderPayments();
        payment.setId(SnowflakeIdUtil.nextRepairOrderPaymentId());
        payment.setRepairOrderId(orderId);
        payment.setDoorFee(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setDistanceFee(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setServiceDistanceKm(BIG_DECIMAL_ZERO.setScale(3, RoundingMode.HALF_UP));
        payment.setBaseRadiusKmSnapshot(BIG_DECIMAL_ZERO.setScale(3, RoundingMode.HALF_UP));
        payment.setDistanceOverKm(BIG_DECIMAL_ZERO.setScale(3, RoundingMode.HALF_UP));
        payment.setMinVisitFeeSnapshot(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setExtraFeePerKmSnapshot(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setDistanceCalcTypeSnapshot(1);
        payment.setRoundingRuleSnapshot(1);
        payment.setPricingLockedTime(now);
        payment.setFeeRuleSnapshot("管理员代录线下订单");
        payment.setServiceFee(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setMaterialFee(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setOvertimeFee(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setTotalAmount(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setDiscountAmount(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setActualAmount(BIG_DECIMAL_ZERO.setScale(2, RoundingMode.HALF_UP));
        payment.setCouponId(null);
        payment.setPaymentMethod(null);
        payment.setPaymentTime(null);
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        payment.setIsDelete(0);
        if (!repairOrderPaymentsService.save(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建订单支付信息失败");
        }

        saveFaultDetails(orderId, serviceType.getId(), admin.getAccountId(), request.getFaultList(), now);

        OfflineSubmitResponse response = new OfflineSubmitResponse();
        response.setOrderId(orderId);
        response.setOrderNo(orderNo);
        response.setUserId(user.getId());
        response.setUserName(safe(user.getUsername()));
        response.setTechnicianId(technician.getId());
        response.setTechnicianName(safe(technician.getUsername()));
        response.setPaymentStatus(1);
        return Result.success(response);
    }

    private void validateServiceCategory(ServiceTypes serviceType, String categoryId) {
        String normalizedCategoryId = trimToNull(categoryId);
        if (!StringUtils.hasText(normalizedCategoryId)) {
            return;
        }
        List<ServiceCategories> activeCategories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getIsActive, 1)
                .eq(ServiceCategories::getIsDelete, 0)
        );
        Map<String, ServiceCategories> categoryMap = new LinkedHashMap<>();
        for (ServiceCategories category : activeCategories) {
            if (category != null && StringUtils.hasText(category.getId())) {
                categoryMap.put(category.getId(), category);
            }
        }
        String currentId = serviceType.getCategoryId();
        while (StringUtils.hasText(currentId)) {
            if (normalizedCategoryId.equals(currentId)) {
                return;
            }
            ServiceCategories current = categoryMap.get(currentId);
            if (current == null) {
                break;
            }
            currentId = trimToNull(current.getParentId());
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "服务分类与服务类型不匹配");
    }

    private UserAccounts requireSelectableUser(String userId) {
        String normalizedUserId = trimToNull(userId);
        if (!StringUtils.hasText(normalizedUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择申请用户");
        }
        UserAccounts user = userAccountsService.getOne(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getId, normalizedUserId)
                .eq(UserAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != USER_STATUS_NORMAL) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选用户账号不可用");
        }
        return user;
    }

    private ServiceTypes requireOfflineServiceType(String serviceTypeId) {
        String normalizedServiceTypeId = trimToNull(serviceTypeId);
        if (!StringUtils.hasText(normalizedServiceTypeId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择服务类型");
        }
        ServiceTypes serviceType = serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, normalizedServiceTypeId)
                .eq(ServiceTypes::getType, SERVICE_MODE_OFFLINE_REPAIR)
                .eq(ServiceTypes::getIsActive, 1)
                .eq(ServiceTypes::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "线下服务类型不存在或已禁用");
        }
        return serviceType;
    }

    private TechnicianAccounts requireSelectableTechnician(String technicianId) {
        String normalizedTechnicianId = trimToNull(technicianId);
        if (!StringUtils.hasText(normalizedTechnicianId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择服务师傅");
        }
        TechnicianAccounts technician = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, normalizedTechnicianId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选师傅不存在");
        }
        if (technician.getAccountStatus() == null || technician.getAccountStatus() != TECHNICIAN_ACCOUNT_ACTIVE) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选师傅账号不可接单");
        }
        Integer workStatus = technician.getWorkStatus();
        if (workStatus == null || (workStatus != TECHNICIAN_WORK_ONLINE && workStatus != TECHNICIAN_WORK_BUSY)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选师傅当前不可接单");
        }
        return technician;
    }

    private void ensureTechnicianSkill(String technicianId, String serviceTypeId) {
        TechnicianSkills skill = technicianSkillsService.getOne(
            new LambdaQueryWrapper<TechnicianSkills>()
                .eq(TechnicianSkills::getTechnicianAccountId, technicianId)
                .eq(TechnicianSkills::getServiceTypeId, serviceTypeId)
                .eq(TechnicianSkills::getIsActive, 1)
                .eq(TechnicianSkills::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (skill == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "所选师傅不支持当前服务类型");
        }
    }

    private void saveFaultDetails(
        String orderId,
        String serviceTypeId,
        String adminAccountId,
        List<OfflineSubmitFaultItem> faultList,
        long now
    ) {
        if (faultList == null || faultList.isEmpty()) {
            return;
        }

        List<FaultPhenomena> validFaults = faultPhenomenaService.list(
            new LambdaQueryWrapper<FaultPhenomena>()
                .eq(FaultPhenomena::getServiceTypeId, serviceTypeId)
                .eq(FaultPhenomena::getIsActive, 1)
                .eq(FaultPhenomena::getIsDelete, 0)
        );
        Map<String, FaultPhenomena> faultMap = new HashMap<>();
        for (FaultPhenomena fault : validFaults) {
            if (fault != null && StringUtils.hasText(fault.getId())) {
                faultMap.put(fault.getId(), fault);
            }
        }

        for (OfflineSubmitFaultItem faultItem : faultList) {
            if (faultItem == null) {
                continue;
            }
            String faultId = trimToNull(faultItem.getFaultId());
            FaultPhenomena phenomenon = null;
            if (StringUtils.hasText(faultId)) {
                phenomenon = faultMap.get(faultId);
                if (phenomenon == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的故障现象");
                }
            }

            List<OfflineSubmitImageItem> images = faultItem.getImages() == null
                ? Collections.emptyList()
                : faultItem.getImages();
            if (images.size() > 5) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "每个故障现象最多上传5张图片");
            }
            if (faultItem.getVideoList() != null && faultItem.getVideoList().size() > 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "每个故障现象最多上传1段视频");
            }
            OfflineSubmitVideoItem video = faultItem.getVideo();
            if (video == null && faultItem.getVideoList() != null && !faultItem.getVideoList().isEmpty()) {
                video = faultItem.getVideoList().get(0);
            }

            String description = trimToNull(faultItem.getFaultDescription());
            if (!StringUtils.hasText(description) && phenomenon != null) {
                description = trimToNull(phenomenon.getName());
            }

            RepairOrderFaults fault = new RepairOrderFaults();
            String faultRecordId = SnowflakeIdUtil.nextRepairOrderFaultId();
            fault.setId(faultRecordId);
            fault.setRepairOrderId(orderId);
            fault.setFaultPhenomenonId(faultId);
            fault.setFaultDescription(description);
            fault.setCreatedTime(now);
            fault.setUpdatedTime(now);
            fault.setIsDelete(0);
            if (!repairOrderFaultsService.save(fault)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障信息失败");
            }

            for (int i = 0; i < images.size(); i++) {
                saveFaultImage(images.get(i), faultRecordId, adminAccountId, now, i);
            }
            if (video != null) {
                saveFaultVideo(video, faultRecordId, adminAccountId, now);
            }
        }
    }

    private void saveFaultImage(
        OfflineSubmitImageItem image,
        String faultRecordId,
        String adminAccountId,
        long now,
        int index
    ) {
        if (image == null) {
            return;
        }
        String fileName = resolveMediaName(image.getName(), "fault-image-" + (index + 1) + ".jpg");
        String fileUrl = requireMediaUrl(image.getUrl());

        Images entity = new Images();
        entity.setId(SnowflakeIdUtil.nextImageId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(fileUrl);
        entity.setFileUrl(fileUrl);
        entity.setFileSize(image.getFileSize() == null ? 0L : image.getFileSize());
        entity.setMimeType(resolveUploadMimeType(image.getMimeType(), "image"));
        entity.setWidth(image.getWidth());
        entity.setHeight(image.getHeight());
        entity.setUploaderId(adminAccountId);
        entity.setUploaderType(ADMIN_UPLOADER_TYPE);
        entity.setBusinessType(FAULT_MEDIA_BUSINESS_TYPE);
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!imagesService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障图片失败");
        }
    }

    private void saveFaultVideo(
        OfflineSubmitVideoItem video,
        String faultRecordId,
        String adminAccountId,
        long now
    ) {
        String fileName = resolveMediaName(video.getName(), "fault-video.mp4");
        String fileUrl = requireMediaUrl(video.getUrl());

        Videos entity = new Videos();
        entity.setId(SnowflakeIdUtil.nextVideoId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(fileUrl);
        entity.setFileUrl(fileUrl);
        entity.setFileSize(video.getFileSize() == null ? 0L : video.getFileSize());
        entity.setMimeType(resolveUploadMimeType(video.getMimeType(), "video"));
        entity.setDuration(video.getDuration());
        entity.setWidth(video.getWidth());
        entity.setHeight(video.getHeight());
        entity.setThumbnailUrl(trimToNull(video.getThumbnailUrl()));
        entity.setUploaderId(adminAccountId);
        entity.setUploaderType(ADMIN_UPLOADER_TYPE);
        entity.setBusinessType(FAULT_MEDIA_BUSINESS_TYPE);
        entity.setBusinessId(faultRecordId);
        entity.setCreatedTime(now);
        entity.setIsDelete(0);
        if (!videosService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存故障视频失败");
        }
    }

    private Map<String, String> loadAvatarMap(List<String> businessIds) {
        if (businessIds == null || businessIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Images> imageList = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, "AVATAR")
                .in(Images::getBusinessId, businessIds)
                .eq(Images::getIsDelete, 0)
                .orderByDesc(Images::getCreatedTime)
        );
        Map<String, String> avatarMap = new LinkedHashMap<>();
        for (Images image : imageList) {
            if (image == null || !StringUtils.hasText(image.getBusinessId()) || !StringUtils.hasText(image.getFileUrl())) {
                continue;
            }
            avatarMap.putIfAbsent(image.getBusinessId(), image.getFileUrl());
        }
        return avatarMap;
    }

    private void fillImageSize(MultipartFile file, OfflineUploadMediaResponse response) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                response.setWidth(image.getWidth());
                response.setHeight(image.getHeight());
            }
        } catch (IOException ignored) {
            // ignore
        }
    }

    private Long parsePurchaseDate(String purchaseDate) {
        String normalized = trimToNull(purchaseDate);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(normalized, DATE_FORMATTER);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买日期格式应为 yyyy-MM-dd");
        }
    }

    private String resolveUploadMediaType(String mediaType, String mimeType, String originalFilename) {
        String normalizedType = trimToNull(mediaType);
        if (StringUtils.hasText(normalizedType)) {
            String lower = normalizedType.toLowerCase();
            if ("image".equals(lower) || "video".equals(lower)) {
                return lower;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "mediaType 仅支持 image 或 video");
        }
        String normalizedMime = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if (lowerMime.startsWith("image/")) {
                return "image";
            }
            if (lowerMime.startsWith("video/")) {
                return "video";
            }
        }
        String fileName = trimToNull(originalFilename);
        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".webp")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".bmp")) {
                return "image";
            }
            if (lowerName.endsWith(".mp4")
                || lowerName.endsWith(".mov")
                || lowerName.endsWith(".m4v")
                || lowerName.endsWith(".avi")
                || lowerName.endsWith(".mkv")
                || lowerName.endsWith(".webm")) {
                return "video";
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别上传文件类型");
    }

    private String resolveUploadExtension(String originalFilename, String mimeType, String mediaType) {
        String filename = trimToNull(originalFilename);
        if (StringUtils.hasText(filename)) {
            int index = filename.lastIndexOf('.');
            if (index >= 0 && index < filename.length() - 1) {
                String extension = filename.substring(index);
                if (extension.length() <= 10) {
                    return extension;
                }
            }
        }
        String normalizedMime = trimToNull(mimeType);
        if (StringUtils.hasText(normalizedMime)) {
            String lowerMime = normalizedMime.toLowerCase();
            if ("image/png".equals(lowerMime)) {
                return ".png";
            }
            if ("image/webp".equals(lowerMime)) {
                return ".webp";
            }
            if ("image/gif".equals(lowerMime)) {
                return ".gif";
            }
            if ("video/quicktime".equals(lowerMime)) {
                return ".mov";
            }
            if ("video/webm".equals(lowerMime)) {
                return ".webm";
            }
            if (lowerMime.startsWith("video/")) {
                return ".mp4";
            }
        }
        return "video".equals(mediaType) ? ".mp4" : ".jpg";
    }

    private String resolveUploadMimeType(String mimeType, String mediaType) {
        String normalized = trimToNull(mimeType);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "video".equals(mediaType) ? "video/mp4" : "image/jpeg";
    }

    private String requireMediaUrl(String value) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "媒体地址不能为空");
        }
        return normalized;
    }

    private String resolveMediaName(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String resolveWorkStatusText(Integer workStatus) {
        if (workStatus == null) {
            return "离线";
        }
        if (workStatus == TECHNICIAN_WORK_ONLINE) {
            return "在线";
        }
        if (workStatus == TECHNICIAN_WORK_BUSY) {
            return "忙碌";
        }
        if (workStatus == 3) {
            return "休息";
        }
        return "离线";
    }

    private String buildOrderNo(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return "NO" + System.currentTimeMillis();
        }
        return "NO" + orderId.replace("RO", "");
    }

    private String formatDecimal(BigDecimal value, int scale) {
        BigDecimal decimal = value == null ? BIG_DECIMAL_ZERO : value;
        return decimal.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问线下订单录入");
        }
        return user;
    }

    @Data
    public static final class OfflineSubmitRequest {
        private String userId;
        private String categoryId;
        private String serviceTypeId;
        private String technicianId;
        private String applianceBrand;
        private String applianceModel;
        private String purchaseDate;
        private String remark;
        private List<OfflineSubmitFaultItem> faultList = new ArrayList<>();
    }

    @Data
    public static final class OfflineSubmitFaultItem {
        private String faultId;
        private String faultDescription;
        private List<OfflineSubmitImageItem> images = new ArrayList<>();
        private OfflineSubmitVideoItem video;
        private List<OfflineSubmitVideoItem> videoList = new ArrayList<>();
    }

    @Data
    public static final class OfflineSubmitImageItem {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer width;
        private Integer height;
    }

    @Data
    public static final class OfflineSubmitVideoItem {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer duration;
        private Integer width;
        private Integer height;
        private String thumbnailUrl;
    }

    @Data
    public static final class OfflineTechnicianOption {
        private String id;
        private String name;
        private String phone;
        private String email;
        private String rating;
        private Integer orderCount;
        private Integer workStatus;
        private String workStatusText;
        private String avatarUrl;
    }

    @Data
    public static final class OfflineUploadMediaResponse {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer width;
        private Integer height;
        private Integer duration;
        private String thumbnailUrl;
        private String uploaderId;
    }

    @Data
    public static final class OfflineSubmitResponse {
        private String orderId;
        private String orderNo;
        private String userId;
        private String userName;
        private String technicianId;
        private String technicianName;
        private Integer paymentStatus;
    }
}
