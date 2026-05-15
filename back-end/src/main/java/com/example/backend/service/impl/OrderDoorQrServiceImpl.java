package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderDoorQrCodes;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.user.UserOrderDoorQrResponse;
import com.example.backend.model.worker.WorkerDoorQrConsumeResult;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderDoorQrCodesService;
import com.example.backend.service.OrderDoorQrService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderDoorQrServiceImpl implements OrderDoorQrService {

    private static final String QR_BUSINESS_TYPE = "ORDER_DOOR_QR";
    private static final int QR_STATUS_UNUSED = 1;
    private static final int QR_STATUS_USED = 2;
    private static final int QR_STATUS_INVALID = 3;
    private static final DateTimeFormatter OBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderDoorQrCodesService orderDoorQrCodesService;
    private final RepairOrdersService repairOrdersService;
    private final ServiceTypesService serviceTypesService;
    private final ImagesService imagesService;
    private final OssUtil ossUtil;
    private final SystemConfigsService systemConfigsService;

    @Value("${app.door-qr.base-url:http://localhost:8080/api/pass/door-qr/scan}")
    private String doorQrBaseUrl;

    public OrderDoorQrServiceImpl(
        OrderDoorQrCodesService orderDoorQrCodesService,
        RepairOrdersService repairOrdersService,
        ServiceTypesService serviceTypesService,
        ImagesService imagesService,
        OssUtil ossUtil,
        SystemConfigsService systemConfigsService
    ) {
        this.orderDoorQrCodesService = orderDoorQrCodesService;
        this.repairOrdersService = repairOrdersService;
        this.serviceTypesService = serviceTypesService;
        this.imagesService = imagesService;
        this.ossUtil = ossUtil;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForAcceptedOrder(RepairOrders order, ServiceTypes serviceType) {
        if (order == null || serviceType == null) {
            return;
        }
        Integer serviceMode = serviceType.getType();
        if (serviceMode == null || (serviceMode != 1 && serviceMode != 2)) {
            return;
        }

        long now = System.currentTimeMillis();
        invalidateCurrentCodes(order.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        byte[] imageBytes = generateQrCodeBytes(buildQrContent(token));
        String objectName = buildObjectName(order.getId());
        String imageUrl = ossUtil.upload(objectName, new ByteArrayInputStream(imageBytes));

        Images image = new Images();
        image.setId(SnowflakeIdUtil.nextImageId());
        image.setOriginalName("door-qr-" + order.getOrderNo() + ".png");
        image.setFileName(objectName.substring(objectName.lastIndexOf('/') + 1));
        image.setFilePath(objectName);
        image.setFileUrl(imageUrl);
        image.setFileSize((long) imageBytes.length);
        image.setMimeType("image/png");
        image.setWidth(360);
        image.setHeight(360);
        image.setUploaderId(order.getTechnicianAccountId());
        image.setUploaderType(2);
        image.setBusinessType(QR_BUSINESS_TYPE);
        image.setBusinessId(order.getId());
        image.setCreatedTime(now);
        image.setIsDelete(0);
        if (!imagesService.save(image)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存上门二维码图片失败");
        }

        OrderDoorQrCodes qrCode = new OrderDoorQrCodes();
        qrCode.setId(SnowflakeIdUtil.nextOrderDoorQrCodeId());
        qrCode.setRepairOrderId(order.getId());
        qrCode.setTechnicianAccountId(order.getTechnicianAccountId());
        qrCode.setAccountId(order.getAccountId());
        qrCode.setToken(token);
        qrCode.setStatus(QR_STATUS_UNUSED);
        qrCode.setExpireTime(resolveExpireTime(order, now));
        qrCode.setImageId(image.getId());
        qrCode.setCreatedTime(now);
        qrCode.setUpdatedTime(now);
        qrCode.setIsDelete(0);
        if (!orderDoorQrCodesService.save(qrCode)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存上门二维码失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidateCurrentCodes(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        List<OrderDoorQrCodes> codeList = orderDoorQrCodesService.list(
            new LambdaQueryWrapper<OrderDoorQrCodes>()
                .eq(OrderDoorQrCodes::getRepairOrderId, orderId)
                .eq(OrderDoorQrCodes::getIsDelete, 0)
                .eq(OrderDoorQrCodes::getStatus, QR_STATUS_UNUSED)
        );
        if (codeList.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (OrderDoorQrCodes item : codeList) {
            item.setStatus(QR_STATUS_INVALID);
            item.setUpdatedTime(now);
        }
        orderDoorQrCodesService.updateBatchById(codeList);
    }

    @Override
    public Map<String, OrderDoorQrCodes> getActiveCodeMap(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new HashMap<>();
        }
        long now = System.currentTimeMillis();
        List<OrderDoorQrCodes> codeList = orderDoorQrCodesService.list(
            new LambdaQueryWrapper<OrderDoorQrCodes>()
                .in(OrderDoorQrCodes::getRepairOrderId, orderIds)
                .eq(OrderDoorQrCodes::getIsDelete, 0)
                .eq(OrderDoorQrCodes::getStatus, QR_STATUS_UNUSED)
                .gt(OrderDoorQrCodes::getExpireTime, now)
                .orderByDesc(OrderDoorQrCodes::getCreatedTime)
        );
        Map<String, OrderDoorQrCodes> map = new HashMap<>();
        for (OrderDoorQrCodes item : codeList) {
            map.putIfAbsent(item.getRepairOrderId(), item);
        }
        return map;
    }

    @Override
    public UserOrderDoorQrResponse getUserDoorQr(String orderId, String accountId) {
        if (!StringUtils.hasText(orderId) || !StringUtils.hasText(accountId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单参数错误");
        }
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        OrderDoorQrCodes code = getLatestCodeByOrderId(orderId);
        if (code == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无上门二维码");
        }
        return toResponse(code);
    }

    @Override
    public UserOrderDoorQrResponse getDoorQrByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "二维码令牌不能为空");
        }
        OrderDoorQrCodes code = orderDoorQrCodesService.getOne(
            new LambdaQueryWrapper<OrderDoorQrCodes>()
                .eq(OrderDoorQrCodes::getToken, token.trim())
                .eq(OrderDoorQrCodes::getIsDelete, 0)
                .orderByDesc(OrderDoorQrCodes::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (code == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "二维码不存在");
        }
        return toResponse(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkerDoorQrConsumeResult consumeForTechnician(String token, String technicianAccountId) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上门码令牌不能为空");
        }
        if (!StringUtils.hasText(technicianAccountId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先登录");
        }

        OrderDoorQrCodes qrCode = orderDoorQrCodesService.getOne(
            new LambdaQueryWrapper<OrderDoorQrCodes>()
                .eq(OrderDoorQrCodes::getToken, token.trim())
                .eq(OrderDoorQrCodes::getIsDelete, 0)
                .orderByDesc(OrderDoorQrCodes::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (qrCode == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上门码不存在或已失效");
        }
        int qrStatus = safeInt(qrCode.getStatus());
        if (qrStatus == QR_STATUS_USED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上门码已使用");
        }
        if (qrStatus == QR_STATUS_INVALID) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上门码已失效");
        }
        if (!technicianAccountId.equals(qrCode.getTechnicianAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权核销该上门码");
        }

        long now = System.currentTimeMillis();
        if (qrCode.getExpireTime() != null && now > qrCode.getExpireTime()) {
            qrCode.setStatus(QR_STATUS_INVALID);
            qrCode.setUpdatedTime(now);
            orderDoorQrCodesService.updateById(qrCode);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "上门码已过期");
        }

        RepairOrders order = repairOrdersService.getById(qrCode.getRepairOrderId());
        if (order == null || safeInt(order.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不存在");
        }
        if (!technicianAccountId.equals(order.getTechnicianAccountId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权核销该订单上门码");
        }

        ServiceTypes serviceType = serviceTypesService.getById(order.getServiceTypeId());
        if (serviceType == null || safeInt(serviceType.getIsDelete()) != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在");
        }
        Integer serviceMode = serviceType.getType();
        if (serviceMode == null || (serviceMode != 1 && serviceMode != 2)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单不支持上门扫码");
        }

        int fromStatus = safeInt(order.getStatus());
        if (fromStatus != 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不在待上门状态，无法扫码");
        }

        int targetStatus;
        if (serviceMode == 1) {
            // onsite repair: scan -> waiting check
            targetStatus = 3;
        } else {
            // onsite install: scan -> start install
            if (safeInt(order.getPaymentStatus()) != 2) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户尚未支付安装费用");
            }
            targetStatus = 5;
        }

        order.setStatus(targetStatus);
        order.setUpdatedTime(now);
        if (targetStatus == 5 && order.getStartTime() == null) {
            order.setStartTime(now);
        }
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单状态更新失败");
        }

        qrCode.setStatus(QR_STATUS_USED);
        qrCode.setUsedTime(now);
        qrCode.setUsedBy(technicianAccountId);
        qrCode.setUpdatedTime(now);
        if (!orderDoorQrCodesService.updateById(qrCode)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上门码核销失败");
        }

        WorkerDoorQrConsumeResult result = new WorkerDoorQrConsumeResult();
        result.setOrderId(order.getId());
        result.setServiceMode(serviceMode);
        result.setFromStatus(fromStatus);
        result.setTargetStatus(targetStatus);
        return result;
    }

    private OrderDoorQrCodes getLatestCodeByOrderId(String orderId) {
        return orderDoorQrCodesService.getOne(
            new LambdaQueryWrapper<OrderDoorQrCodes>()
                .eq(OrderDoorQrCodes::getRepairOrderId, orderId)
                .eq(OrderDoorQrCodes::getIsDelete, 0)
                .orderByDesc(OrderDoorQrCodes::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private UserOrderDoorQrResponse toResponse(OrderDoorQrCodes code) {
        UserOrderDoorQrResponse response = new UserOrderDoorQrResponse();
        response.setOrderId(code.getRepairOrderId());
        response.setStatus(resolveVisibleStatus(code));
        response.setStatusText(getStatusText(code));
        response.setQrImageUrl(resolveImageUrl(code.getImageId()));
        response.setExpireTime(code.getExpireTime());
        return response;
    }

    private String resolveImageUrl(String imageId) {
        if (!StringUtils.hasText(imageId)) {
            return "";
        }
        Images image = imagesService.getOne(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getId, imageId)
                .eq(Images::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        return image == null ? "" : defaultText(image.getFileUrl());
    }

    private Integer resolveVisibleStatus(OrderDoorQrCodes code) {
        if (code == null) {
            return QR_STATUS_INVALID;
        }
        if (code.getStatus() != null && code.getStatus() == QR_STATUS_UNUSED && isExpired(code.getExpireTime())) {
            return QR_STATUS_INVALID;
        }
        return code.getStatus();
    }

    private String getStatusText(OrderDoorQrCodes code) {
        int value = resolveVisibleStatus(code) == null ? QR_STATUS_INVALID : resolveVisibleStatus(code);
        if (value == QR_STATUS_UNUSED) {
            return "待扫码";
        }
        if (value == QR_STATUS_USED) {
            return "已核销";
        }
        return "已失效";
    }

    private boolean isExpired(Long expireTime) {
        return expireTime != null && expireTime > 0 && expireTime < System.currentTimeMillis();
    }

    private String buildQrContent(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return doorQrBaseUrl + "?token=" + encodedToken;
    }

    private String buildObjectName(String orderId) {
        return "door-qr/" + orderId + "/" + LocalDateTime.now().format(OBJECT_DATE_FORMAT) + "_"
            + UUID.randomUUID().toString().replace("-", "") + ".png";
    }

    private Long resolveExpireTime(RepairOrders order, long now) {
        long fallback = now + getFallbackExpireHours() * 60L * 60L * 1000L;
        Long appointmentTime = order.getAppointmentTime();
        if (appointmentTime == null || appointmentTime <= 0) {
            return fallback;
        }
        long appointmentExpire = appointmentTime + getAfterAppointmentExpireHours() * 60L * 60L * 1000L;
        return Math.max(appointmentExpire, now + getMinValidHours() * 60L * 60L * 1000L);
    }

    private byte[] generateQrCodeBytes(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 360, 360, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成上门二维码失败");
        }
    }

    private long getFallbackExpireHours() {
        Long value = systemConfigsService.getLongConfig("door_qr.fallback_expire_hours", 24L);
        return value == null || value <= 0L ? 24L : value;
    }

    private long getAfterAppointmentExpireHours() {
        Long value = systemConfigsService.getLongConfig("door_qr.after_appointment_expire_hours", 2L);
        return value == null || value <= 0L ? 2L : value;
    }

    private long getMinValidHours() {
        Long value = systemConfigsService.getLongConfig("door_qr.min_valid_hours", 2L);
        return value == null || value <= 0L ? 2L : value;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
