package com.example.backend.controller.worker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AfterSalesApplications;
import com.example.backend.entity.ConversationSessions;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.TechnicianServiceAreas;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerHomeOrderItem;
import com.example.backend.model.worker.WorkerHomeOrdersResponse;
import com.example.backend.model.worker.WorkerDoorQrConsumeRequest;
import com.example.backend.model.worker.WorkerDoorQrConsumeResult;
import com.example.backend.model.worker.WorkerOrderDetailResponse;
import com.example.backend.model.worker.WorkerOrderFeeUpdateRequest;
import com.example.backend.model.worker.WorkerOrderFaultItem;
import com.example.backend.model.worker.WorkerOrderInspectionSubmitRequest;
import com.example.backend.model.worker.WorkerOrderMediaItem;
import com.example.backend.model.worker.WorkerOrderProgressItem;
import com.example.backend.model.worker.WorkerOrderSubmitMediaItem;
import com.example.backend.model.worker.WorkerOrderUploadMediaResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.ConversationSessionsService;
import com.example.backend.service.AfterSalesApplicationsService;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.OrderDoorQrService;
import com.example.backend.service.RepairOrderFundService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.TechnicianServiceAreasService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.VideosService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import com.example.backend.utils.oss.OssUtil;
import com.example.backend.utils.upload.UploadLimitUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/worker/orders")
public class WorkerOrderController {

    private static final String ORDER_FAULT_BUSINESS_TYPE = "REPAIR_ORDER_FAULT";
    private static final String ORDER_INSPECTION_BUSINESS_TYPE = "REPAIR_ORDER_INSPECTION";
    private static final String INSPECTION_PROGRESS_TYPE = "inspection";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int WORKER_UPLOADER_TYPE = 2;
    private static final int MAX_INSPECTION_IMAGE_COUNT = 5;
    private static final int MAX_PROGRESS_DESCRIPTION_LENGTH = 500;
    private static final int ORDER_TYPE_REPAIR = 1;
    private static final int AFTER_SALES_STATUS_PENDING = 1;
    private static final int AFTER_SALES_STATUS_APPROVED = 2;
    private static final int AFTER_SALES_STATUS_PROCESSING = 4;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final RepairOrdersService repairOrdersService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final ServiceTypesService serviceTypesService;
    private final TechnicianServiceAreasService technicianServiceAreasService;
    private final TechnicianAccountsService technicianAccountsService;
    private final UserAccountsService userAccountsService;
    private final UserAddressesService userAddressesService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final ImagesService imagesService;
    private final VideosService videosService;
    private final OrderProgressService orderProgressService;
    private final ConversationSessionsService conversationSessionsService;
    private final OrderDoorQrService orderDoorQrService;
    private final RepairOrderFundService repairOrderFundService;
    private final AfterSalesApplicationsService afterSalesApplicationsService;
    private final OssUtil ossUtil;

    public WorkerOrderController(
        RepairOrdersService repairOrdersService,
        ServiceCategoriesService serviceCategoriesService,
        ServiceTypesService serviceTypesService,
        TechnicianServiceAreasService technicianServiceAreasService,
        TechnicianAccountsService technicianAccountsService,
        UserAccountsService userAccountsService,
        UserAddressesService userAddressesService,
        RepairOrderFaultsService repairOrderFaultsService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        FaultPhenomenaService faultPhenomenaService,
        ImagesService imagesService,
        VideosService videosService,
        OrderProgressService orderProgressService,
        ConversationSessionsService conversationSessionsService,
        OrderDoorQrService orderDoorQrService,
        RepairOrderFundService repairOrderFundService,
        AfterSalesApplicationsService afterSalesApplicationsService,
        OssUtil ossUtil
    ) {
        this.repairOrdersService = repairOrdersService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.serviceTypesService = serviceTypesService;
        this.technicianServiceAreasService = technicianServiceAreasService;
        this.technicianAccountsService = technicianAccountsService;
        this.userAccountsService = userAccountsService;
        this.userAddressesService = userAddressesService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.imagesService = imagesService;
        this.videosService = videosService;
        this.orderProgressService = orderProgressService;
        this.conversationSessionsService = conversationSessionsService;
        this.orderDoorQrService = orderDoorQrService;
        this.repairOrderFundService = repairOrderFundService;
        this.afterSalesApplicationsService = afterSalesApplicationsService;
        this.ossUtil = ossUtil;
    }

    @GetMapping("/home")
    public Result<WorkerHomeOrdersResponse> getHomeOrders() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianServiceAreas defaultArea = getDefaultArea(accountId);

        List<RepairOrders> orders = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getTechnicianAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 1, 2, 3, 4, 5)
                .orderByAsc(RepairOrders::getStatus)
                .orderByAsc(RepairOrders::getAppointmentTime)
                .orderByDesc(RepairOrders::getCreatedTime)
        );

        WorkerHomeOrdersResponse response = new WorkerHomeOrdersResponse();
        if (orders.isEmpty()) {
            response.setWaitingCount(0);
            response.setInProgressCount(0);
            response.setTotalActiveCount(0);
            return Result.success(response);
        }

        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);

        List<WorkerHomeOrderItem> waitingOrders = new ArrayList<>();
        List<WorkerHomeOrderItem> inProgressOrders = new ArrayList<>();
        for (RepairOrders order : orders) {
            WorkerHomeOrderItem item = buildOrderItem(
                order,
                serviceTypeMap.get(order.getServiceTypeId()),
                categoryMap,
                defaultArea,
                userMap.get(order.getAccountId()),
                addressMap.get(order.getServiceAddressId()),
                faultMap.get(order.getId()),
                paymentMap.get(order.getId())
            );
            if (order.getStatus() != null && order.getStatus() == 1) {
                waitingOrders.add(item);
            } else {
                inProgressOrders.add(item);
            }
        }

        waitingOrders.sort(Comparator.comparing(WorkerHomeOrderItem::getAppointmentTime, this::compareTimestamp));
        inProgressOrders.sort(this::compareHomeOrderItem);

        response.setWaitingOrders(waitingOrders);
        response.setInProgressOrders(inProgressOrders);
        response.setWaitingCount(waitingOrders.size());
        response.setInProgressCount(inProgressOrders.size());
        response.setTotalActiveCount(waitingOrders.size() + inProgressOrders.size());
        return Result.success(response);
    }

    @GetMapping("/history")
    public Result<List<WorkerHomeOrderItem>> getHistoryOrders() {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianServiceAreas defaultArea = getDefaultArea(accountId);

        List<RepairOrders> orders = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getTechnicianAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 2, 3, 4, 5, 6, 7, 8)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
        );

        if (orders.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);

        List<WorkerHomeOrderItem> historyOrders = new ArrayList<>();
        for (RepairOrders order : orders) {
            historyOrders.add(buildOrderItem(
                order,
                serviceTypeMap.get(order.getServiceTypeId()),
                categoryMap,
                defaultArea,
                userMap.get(order.getAccountId()),
                addressMap.get(order.getServiceAddressId()),
                faultMap.get(order.getId()),
                paymentMap.get(order.getId())
            ));
        }

        historyOrders.sort(this::compareHistoryOrderItem);
        return Result.success(historyOrders);
    }

    @GetMapping("/{orderId}")
    public Result<WorkerOrderDetailResponse> getOrderDetail(@PathVariable String orderId) {
        LoginUserInfo user = requireWorker();
        RepairOrders order = requireOwnedOrder(orderId, user.getAccountId());
        return Result.success(buildOrderDetail(order, user.getAccountId()));
    }

    @PostMapping(value = "/inspection/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<WorkerOrderUploadMediaResponse> uploadInspectionMedia(
        @RequestParam(value = "mediaType", required = false) String mediaType,
        @RequestPart("file") MultipartFile file
    ) {
        LoginUserInfo user = requireWorker();
        return Result.success(uploadWorkerMedia(user.getAccountId(), mediaType, file));
    }

    @PostMapping("/{orderId}/inspection")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerOrderDetailResponse> submitInspection(
        @PathVariable String orderId,
        @RequestBody WorkerOrderInspectionSubmitRequest request
    ) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        RepairOrders order = requireOwnedOrder(orderId, accountId);
        ServiceTypes serviceType = requireServiceType(order.getServiceTypeId());
        TechnicianAccounts technician = requireTechnician(accountId);
        RepairOrderPayments payment = requireLatestPayment(order.getId());
        submitInspectionInternal(order, serviceType, technician, payment, request);
        return Result.success(buildOrderDetail(reloadOrder(orderId), accountId));
    }

    @PostMapping("/{orderId}/inspection/fees")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerOrderDetailResponse> updateInspectionFees(
        @PathVariable String orderId,
        @RequestBody WorkerOrderFeeUpdateRequest request
    ) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        RepairOrders order = requireOwnedOrder(orderId, accountId);
        ServiceTypes serviceType = requireServiceType(order.getServiceTypeId());
        TechnicianAccounts technician = requireTechnician(accountId);
        RepairOrderPayments payment = requireLatestPayment(order.getId());
        updateInspectionFeeInternal(order, serviceType, technician, payment, request);
        return Result.success(buildOrderDetail(reloadOrder(orderId), accountId));
    }

    @PostMapping("/{orderId}/accept")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerOrderDetailResponse> acceptOrder(@PathVariable String orderId) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        RepairOrders order = requireOwnedOrder(orderId, accountId);
        if (safeInt(order.getStatus()) != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单当前不能接单");
        }

        ServiceTypes serviceType = requireServiceType(order.getServiceTypeId());
        TechnicianAccounts technician = requireTechnician(accountId);
        long now = System.currentTimeMillis();
        int targetStatus = resolveAcceptStatus(serviceType.getType());

        order.setStatus(targetStatus);
        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接单失败");
        }

        saveOrderProgress(order, technician, targetStatus, buildProgressDescription(serviceType.getType(), 1, targetStatus, false));
        orderDoorQrService.generateForAcceptedOrder(order, serviceType);
        ensureConversationSessionOpen(order, now);
        return Result.success(buildOrderDetail(reloadOrder(orderId), accountId));
    }

    @PostMapping("/{orderId}/next-status")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerOrderDetailResponse> advanceOrderStatus(@PathVariable String orderId) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        RepairOrders order = requireOwnedOrder(orderId, accountId);
        ServiceTypes serviceType = requireServiceType(order.getServiceTypeId());
        TechnicianAccounts technician = requireTechnician(accountId);
        RepairOrderPayments payment = requireLatestPayment(order.getId());

        int currentStatus = safeInt(order.getStatus());
        int targetStatus = resolveNextStatus(serviceType.getType(), currentStatus, payment);
        long now = System.currentTimeMillis();

        boolean waitingUserConfirmation = currentStatus == 5 && targetStatus == 5;
        order.setStatus(targetStatus);
        order.setUpdatedTime(now);
        if (targetStatus == 5 && order.getStartTime() == null) {
            order.setStartTime(now);
        }
        if (waitingUserConfirmation) {
            order.setEndTime(now);
        } else if (targetStatus == 6) {
            if (order.getStartTime() == null) {
                order.setStartTime(now);
            }
            order.setEndTime(now);
            order.setCompletionTime(now);
        }
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "状态更新失败");
        }

        saveOrderProgress(
            order,
            technician,
            targetStatus,
            buildProgressDescription(serviceType.getType(), currentStatus, targetStatus, waitingUserConfirmation)
        );
        if (targetStatus == 6) {
            if (payment != null) {
                repairOrderFundService.settleOnOrderCompleted(order, payment, now);
            }
            completeProcessingAfterSales(order.getId(), now);
            orderDoorQrService.invalidateCurrentCodes(order.getId());
            closeConversationSession(order, now);
        }
        return Result.success(buildOrderDetail(reloadOrder(orderId), accountId));
    }

    @PostMapping("/door-qr/consume")
    @Transactional(rollbackFor = Exception.class)
    public Result<WorkerOrderDetailResponse> consumeDoorQr(@RequestBody WorkerDoorQrConsumeRequest request) {
        LoginUserInfo user = requireWorker();
        String accountId = user.getAccountId();
        TechnicianAccounts technician = requireTechnician(accountId);

        String token = request == null ? null : request.getToken();
        WorkerDoorQrConsumeResult consumeResult = orderDoorQrService.consumeForTechnician(token, accountId);

        RepairOrders updatedOrder = reloadOrder(consumeResult.getOrderId());
        saveOrderProgress(
            updatedOrder,
            technician,
            consumeResult.getTargetStatus(),
            buildProgressDescription(consumeResult.getServiceMode(), consumeResult.getFromStatus(), consumeResult.getTargetStatus(), false)
        );
        return Result.success(buildOrderDetail(updatedOrder, accountId));
    }

    private WorkerOrderDetailResponse buildOrderDetail(RepairOrders order, String technicianAccountId) {
        TechnicianServiceAreas defaultArea = getDefaultArea(technicianAccountId);
        List<RepairOrders> orders = Collections.singletonList(order);

        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);
        List<OrderProgress> progressList = listOrderProgress(order.getId());
        OrderProgress latestInspectionProgress = findLatestInspectionProgress(progressList);
        InspectionProgressSnapshot inspectionSnapshot = parseInspectionProgress(latestInspectionProgress);

        ServiceTypes serviceType = serviceTypeMap.get(order.getServiceTypeId());
        RepairOrderPayments payment = paymentMap.get(order.getId());
        WorkerHomeOrderItem item = buildOrderItem(
            order,
            serviceType,
            categoryMap,
            defaultArea,
            userMap.get(order.getAccountId()),
            addressMap.get(order.getServiceAddressId()),
            faultMap.get(order.getId()),
            payment
        );
        OrderActionState actionState = buildOrderActionState(order, item.getServiceMode(), payment);
        List<WorkerOrderMediaItem> inspectionImages = listInspectionImages(latestInspectionProgress == null ? null : latestInspectionProgress.getId());
        List<WorkerOrderMediaItem> inspectionVideos = listInspectionVideos(latestInspectionProgress == null ? null : latestInspectionProgress.getId());

        WorkerOrderDetailResponse response = new WorkerOrderDetailResponse();
        copyHomeFields(item, response);
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus(), payment));
        response.setDoorFee(formatMoney(payment == null ? null : payment.getDoorFee()));
        response.setDistanceFee(formatMoney(payment == null ? null : payment.getDistanceFee()));
        response.setServiceFee(formatMoney(payment == null ? null : payment.getServiceFee()));
        response.setMaterialFee(formatMoney(payment == null ? null : payment.getMaterialFee()));
        response.setOvertimeFee(formatMoney(payment == null ? null : payment.getOvertimeFee()));
        response.setRemark(safe(order.getRemark()));
        response.setInspectionDiagnosis(inspectionSnapshot == null ? "" : inspectionSnapshot.getInspectionDiagnosis());
        response.setRepairPlan(inspectionSnapshot == null ? "" : inspectionSnapshot.getRepairPlan());
        response.setInspectionTime(latestInspectionProgress == null ? null : latestInspectionProgress.getCreatedTime());
        response.setActionAvailable(actionState.isAvailable());
        response.setPrimaryActionType(actionState.getActionType());
        response.setPrimaryActionText(actionState.getActionText());
        response.setActionHint(actionState.getActionHint());
        response.setFaultList(buildFaultItems(faultMap.get(order.getId())));
        response.setInspectionImages(inspectionImages);
        response.setInspectionVideos(inspectionVideos);
        response.setProgressList(listProgressItems(order, progressList));
        return response;
    }

    private void copyHomeFields(WorkerHomeOrderItem item, WorkerOrderDetailResponse response) {
        response.setId(item.getId());
        response.setOrderNo(item.getOrderNo());
        response.setStatus(item.getStatus());
        response.setStatusText(item.getStatusText());
        response.setNextActionText(item.getNextActionText());
        response.setServiceTypeId(item.getServiceTypeId());
        response.setServiceTypeName(item.getServiceTypeName());
        response.setServiceCategoryId(item.getServiceCategoryId());
        response.setServiceCategoryName(item.getServiceCategoryName());
        response.setServiceCategoryPath(item.getServiceCategoryPath());
        response.setServiceMode(item.getServiceMode());
        response.setServiceModeText(item.getServiceModeText());
        response.setUserId(item.getUserId());
        response.setUserName(item.getUserName());
        response.setServiceAddress(item.getServiceAddress());
        response.setServiceAddressShort(item.getServiceAddressShort());
        response.setContactName(item.getContactName());
        response.setContactPhone(item.getContactPhone());
        response.setApplianceBrand(item.getApplianceBrand());
        response.setApplianceModel(item.getApplianceModel());
        response.setFaultSummary(item.getFaultSummary());
        response.setTotalAmount(item.getTotalAmount());
        response.setPaidAmount(item.getPaidAmount());
        response.setAppointmentTime(item.getAppointmentTime());
        response.setCreatedTime(item.getCreatedTime());
        response.setUpdatedTime(item.getUpdatedTime());
    }

    private List<WorkerOrderFaultItem> buildFaultItems(List<RepairOrderFaults> faults) {
        if (faults == null || faults.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> phenomenonIds = faults.stream()
            .map(RepairOrderFaults::getFaultPhenomenonId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, FaultPhenomena> phenomenonMap = phenomenonIds.isEmpty()
            ? new HashMap<>()
            : faultPhenomenaService.list(
                new LambdaQueryWrapper<FaultPhenomena>()
                    .in(FaultPhenomena::getId, phenomenonIds)
                    .eq(FaultPhenomena::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(FaultPhenomena::getId, item -> item, (a, b) -> a));

        Set<String> faultIds = faults.stream()
            .map(RepairOrderFaults::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, List<Images>> imageMap = faultIds.isEmpty()
            ? new HashMap<>()
            : imagesService.list(
                new LambdaQueryWrapper<Images>()
                    .eq(Images::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                    .in(Images::getBusinessId, faultIds)
                    .eq(Images::getIsDelete, 0)
                    .orderByAsc(Images::getCreatedTime)
            ).stream().collect(Collectors.groupingBy(Images::getBusinessId));
        Map<String, List<Videos>> videoMap = faultIds.isEmpty()
            ? new HashMap<>()
            : videosService.list(
                new LambdaQueryWrapper<Videos>()
                    .eq(Videos::getBusinessType, ORDER_FAULT_BUSINESS_TYPE)
                    .in(Videos::getBusinessId, faultIds)
                    .eq(Videos::getIsDelete, 0)
                    .orderByAsc(Videos::getCreatedTime)
            ).stream().collect(Collectors.groupingBy(Videos::getBusinessId));

        List<WorkerOrderFaultItem> items = new ArrayList<>();
        for (RepairOrderFaults fault : faults) {
            FaultPhenomena phenomenon = phenomenonMap.get(fault.getFaultPhenomenonId());
            WorkerOrderFaultItem item = new WorkerOrderFaultItem();
            item.setId(fault.getId());
            item.setFaultPhenomenonId(fault.getFaultPhenomenonId());
            item.setFaultPhenomenonName(phenomenon == null ? "" : safe(phenomenon.getName()));
            item.setFaultPhenomenonDescription(phenomenon == null ? "" : safe(phenomenon.getDescription()));
            item.setFaultDescription(safe(fault.getFaultDescription()));
            item.setImages(toImageItems(imageMap.get(fault.getId())));
            item.setVideos(toVideoItems(videoMap.get(fault.getId())));
            items.add(item);
        }
        return items;
    }

    private List<WorkerOrderMediaItem> toImageItems(List<Images> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<WorkerOrderMediaItem> items = new ArrayList<>();
        for (Images image : images) {
            WorkerOrderMediaItem item = new WorkerOrderMediaItem();
            item.setId(image.getId());
            item.setUrl(safe(image.getFileUrl()));
            item.setThumbnailUrl(safe(image.getFileUrl()));
            item.setName(StringUtils.hasText(image.getOriginalName()) ? image.getOriginalName() : safe(image.getFileName()));
            item.setMimeType(safe(image.getMimeType()));
            items.add(item);
        }
        return items;
    }

    private List<WorkerOrderMediaItem> toVideoItems(List<Videos> videos) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        List<WorkerOrderMediaItem> items = new ArrayList<>();
        for (Videos video : videos) {
            WorkerOrderMediaItem item = new WorkerOrderMediaItem();
            item.setId(video.getId());
            item.setUrl(safe(video.getFileUrl()));
            item.setThumbnailUrl(safe(video.getThumbnailUrl()));
            item.setName(StringUtils.hasText(video.getOriginalName()) ? video.getOriginalName() : safe(video.getFileName()));
            item.setMimeType(safe(video.getMimeType()));
            item.setDuration(video.getDuration());
            items.add(item);
        }
        return items;
    }

    private List<WorkerOrderMediaItem> listInspectionImages(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        List<Images> images = imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Images::getBusinessId, progressId)
                .eq(Images::getIsDelete, 0)
                .orderByAsc(Images::getCreatedTime)
        );
        return toImageItems(images);
    }

    private List<WorkerOrderMediaItem> listInspectionVideos(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        List<Videos> videos = videosService.list(
            new LambdaQueryWrapper<Videos>()
                .eq(Videos::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Videos::getBusinessId, progressId)
                .eq(Videos::getIsDelete, 0)
                .orderByAsc(Videos::getCreatedTime)
        );
        return toVideoItems(videos);
    }

    private List<OrderProgress> listOrderProgress(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return Collections.emptyList();
        }
        return orderProgressService.list(
            new LambdaQueryWrapper<OrderProgress>()
                .eq(OrderProgress::getOrderId, orderId)
                .eq(OrderProgress::getIsDelete, 0)
                .orderByAsc(OrderProgress::getCreatedTime)
        );
    }

    private OrderProgress findLatestInspectionProgress(List<OrderProgress> progressList) {
        if (progressList == null || progressList.isEmpty()) {
            return null;
        }
        for (int index = progressList.size() - 1; index >= 0; index--) {
            OrderProgress progress = progressList.get(index);
            if (parseInspectionProgress(progress) != null) {
                return progress;
            }
        }
        return null;
    }

    private List<WorkerOrderProgressItem> listProgressItems(RepairOrders order, List<OrderProgress> progressList) {
        List<WorkerOrderProgressItem> items = new ArrayList<>();
        WorkerOrderProgressItem initial = new WorkerOrderProgressItem();
        initial.setId("INIT-" + order.getId());
        initial.setStatus(1);
        initial.setStatusText(getStatusText(1));
        initial.setDescription("用户已提交订单，等待师傅接单");
        initial.setOperatorName("系统");
        initial.setOperatorType(4);
        initial.setCreatedTime(order.getCreatedTime());
        items.add(initial);

        for (OrderProgress progress : progressList) {
            WorkerOrderProgressItem item = new WorkerOrderProgressItem();
            item.setId(progress.getId());
            item.setStatus(progress.getStatus());
            item.setStatusText(getStatusText(progress.getStatus()));
            item.setDescription(resolveProgressDescription(progress));
            item.setOperatorName(safe(progress.getOperatorName()));
            item.setOperatorType(progress.getOperatorType());
            item.setCreatedTime(progress.getCreatedTime());
            items.add(item);
        }
        return items;
    }

    private void submitInspectionInternal(
        RepairOrders order,
        ServiceTypes serviceType,
        TechnicianAccounts technician,
        RepairOrderPayments payment,
        WorkerOrderInspectionSubmitRequest request
    ) {
        int currentStatus = safeInt(order.getStatus());
        int serviceMode = safeInt(serviceType == null ? null : serviceType.getType());
        if (currentStatus != 3 || (serviceMode != 1 && serviceMode != 3)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不支持提交检查结果");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检查结果不能为空");
        }

        String inspectionDiagnosis = trimToNull(request.getInspectionDiagnosis());
        String repairPlan = trimToNull(request.getRepairPlan());
        if (!StringUtils.hasText(inspectionDiagnosis)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写检查发现的问题");
        }
        if (!StringUtils.hasText(repairPlan)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写维修方案说明");
        }

        List<WorkerOrderSubmitMediaItem> images = request.getImages() == null
            ? Collections.emptyList()
            : request.getImages().stream()
                .filter(item -> item != null && StringUtils.hasText(trimToNull(item.getUrl())))
                .collect(Collectors.toList());
        if (images.size() > MAX_INSPECTION_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "检查图片最多上传5张");
        }
        WorkerOrderSubmitMediaItem video = request.getVideo();
        if (video != null && !StringUtils.hasText(trimToNull(video.getUrl()))) {
            video = null;
        }
        if (images.isEmpty() && video == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请至少上传一份检查凭证");
        }

        BigDecimal serviceFee = normalizeFee(request.getServiceFee());
        BigDecimal materialFee = normalizeFee(request.getMaterialFee());
        BigDecimal overtimeFee = defaultZero(payment.getOvertimeFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal doorFee = defaultZero(payment.getDoorFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceFee = defaultZero(payment.getDistanceFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = defaultZero(payment.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = doorFee
            .add(distanceFee)
            .add(serviceFee)
            .add(materialFee)
            .add(overtimeFee)
            .subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(ZERO) < 0) {
            totalAmount = ZERO;
        }

        long now = System.currentTimeMillis();
        order.setStatus(4);
        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存检查结果失败");
        }

        payment.setServiceFee(serviceFee);
        payment.setMaterialFee(materialFee);
        payment.setTotalAmount(totalAmount);
        payment.setUpdatedTime(now);
        if (!repairOrderPaymentsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单费用失败");
        }

        OrderProgress inspectionProgress = saveOrderProgress(
            order,
            technician,
            4,
            buildInspectionProgressPayload(inspectionDiagnosis, repairPlan, serviceFee, materialFee),
            now
        );
        saveInspectionImages(images, inspectionProgress.getId(), technician.getId(), now);
        saveInspectionVideo(video, inspectionProgress.getId(), technician.getId(), now);
    }

    private void updateInspectionFeeInternal(
        RepairOrders order,
        ServiceTypes serviceType,
        TechnicianAccounts technician,
        RepairOrderPayments payment,
        WorkerOrderFeeUpdateRequest request
    ) {
        int currentStatus = safeInt(order.getStatus());
        int serviceMode = safeInt(serviceType == null ? null : serviceType.getType());
        if (currentStatus != 4 || (serviceMode != 1 && serviceMode != 3)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不支持修改费用");
        }
        if (isFullyPaid(payment)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户已支付，无法再修改费用");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "费用信息不能为空");
        }

        List<OrderProgress> progressList = listOrderProgress(order.getId());
        OrderProgress inspectionProgress = findLatestInspectionProgress(progressList);
        InspectionProgressSnapshot snapshot = parseInspectionProgress(inspectionProgress);
        if (inspectionProgress == null || snapshot == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单暂无可修改的检查费用");
        }

        BigDecimal serviceFee = normalizeFee(request.getServiceFee());
        BigDecimal materialFee = normalizeFee(request.getMaterialFee());
        BigDecimal totalAmount = resolveInspectionTotalAmount(payment, serviceFee, materialFee);
        long now = System.currentTimeMillis();

        order.setUpdatedTime(now);
        if (!repairOrdersService.updateById(order)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单失败");
        }

        payment.setServiceFee(serviceFee);
        payment.setMaterialFee(materialFee);
        payment.setTotalAmount(totalAmount);
        payment.setUpdatedTime(now);
        if (!repairOrderPaymentsService.updateById(payment)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新订单费用失败");
        }

        inspectionProgress.setDescription(
            buildInspectionProgressPayload(
                safe(snapshot.getInspectionDiagnosis()),
                safe(snapshot.getRepairPlan()),
                serviceFee,
                materialFee
            )
        );
        if (!orderProgressService.updateById(inspectionProgress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新检查结果失败");
        }

        saveOrderProgress(order, technician, 4, buildInspectionFeeUpdateDescription(serviceFee, materialFee), now);
    }

    private void saveInspectionImages(List<WorkerOrderSubmitMediaItem> images, String progressId, String accountId, long now) {
        for (int index = 0; index < images.size(); index++) {
            WorkerOrderSubmitMediaItem image = images.get(index);
            Images entity = new Images();
            String fileName = resolveMediaName(image.getName(), "inspection-image-" + (index + 1) + ".jpg");
            entity.setId(SnowflakeIdUtil.nextImageId());
            entity.setOriginalName(fileName);
            entity.setFileName(fileName);
            entity.setFilePath(trimToNull(image.getUrl()));
            entity.setFileUrl(trimToNull(image.getUrl()));
            entity.setFileSize(image.getFileSize());
            entity.setMimeType(resolveUploadMimeType(image.getMimeType(), "image"));
            entity.setWidth(image.getWidth());
            entity.setHeight(image.getHeight());
            entity.setUploaderId(accountId);
            entity.setUploaderType(WORKER_UPLOADER_TYPE);
            entity.setBusinessType(ORDER_INSPECTION_BUSINESS_TYPE);
            entity.setBusinessId(progressId);
            entity.setCreatedTime(now);
            entity.setVersion(0);
            entity.setIsDelete(0);
            if (!imagesService.save(entity)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存检查图片失败");
            }
        }
    }

    private void saveInspectionVideo(WorkerOrderSubmitMediaItem video, String progressId, String accountId, long now) {
        if (video == null || !StringUtils.hasText(trimToNull(video.getUrl()))) {
            return;
        }
        Videos entity = new Videos();
        String fileName = resolveMediaName(video.getName(), "inspection-video.mp4");
        entity.setId(SnowflakeIdUtil.nextVideoId());
        entity.setOriginalName(fileName);
        entity.setFileName(fileName);
        entity.setFilePath(trimToNull(video.getUrl()));
        entity.setFileUrl(trimToNull(video.getUrl()));
        entity.setFileSize(video.getFileSize());
        entity.setMimeType(resolveUploadMimeType(video.getMimeType(), "video"));
        entity.setDuration(video.getDuration());
        entity.setWidth(video.getWidth());
        entity.setHeight(video.getHeight());
        entity.setThumbnailUrl(trimToNull(video.getThumbnailUrl()));
        entity.setUploaderId(accountId);
        entity.setUploaderType(WORKER_UPLOADER_TYPE);
        entity.setBusinessType(ORDER_INSPECTION_BUSINESS_TYPE);
        entity.setBusinessId(progressId);
        entity.setCreatedTime(now);
        entity.setVersion(0);
        entity.setIsDelete(0);
        if (!videosService.save(entity)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存检查视频失败");
        }
    }

    private String buildInspectionProgressPayload(
        String inspectionDiagnosis,
        String repairPlan,
        BigDecimal serviceFee,
        BigDecimal materialFee
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", INSPECTION_PROGRESS_TYPE);
        payload.put("inspectionDiagnosis", inspectionDiagnosis);
        payload.put("repairPlan", repairPlan);
        payload.put("serviceFee", formatMoney(serviceFee));
        payload.put("materialFee", formatMoney(materialFee));
        try {
            String json = OBJECT_MAPPER.writeValueAsString(payload);
            if (json.length() > MAX_PROGRESS_DESCRIPTION_LENGTH) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "检查说明过长，请精简问题说明或维修方案");
            }
            return json;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "构建检查进度失败");
        }
    }

    private BigDecimal resolveInspectionTotalAmount(
        RepairOrderPayments payment,
        BigDecimal serviceFee,
        BigDecimal materialFee
    ) {
        BigDecimal overtimeFee = defaultZero(payment.getOvertimeFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal doorFee = defaultZero(payment.getDoorFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distanceFee = defaultZero(payment.getDistanceFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = defaultZero(payment.getDiscountAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = doorFee
            .add(distanceFee)
            .add(serviceFee)
            .add(materialFee)
            .add(overtimeFee)
            .subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return totalAmount;
    }

    private WorkerOrderUploadMediaResponse uploadWorkerMedia(String accountId, String mediaType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        String mediaKind = resolveUploadMediaType(mediaType, file.getContentType(), file.getOriginalFilename());
        UploadLimitUtil.validateMediaSize(mediaKind, file);
        String originalFilename = trimToNull(file.getOriginalFilename());
        String extension = resolveUploadExtension(originalFilename, file.getContentType(), mediaKind);
        String objectName = "repair-orders/" + accountId + "/inspection/" + mediaKind + "/" + UUID.randomUUID() + extension;

        String uploadUrl;
        try (InputStream in = file.getInputStream()) {
            uploadUrl = ossUtil.upload(objectName, in);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传检查附件失败");
        }

        WorkerOrderUploadMediaResponse response = new WorkerOrderUploadMediaResponse();
        response.setUrl(uploadUrl);
        response.setName(StringUtils.hasText(originalFilename) ? originalFilename : objectName);
        response.setFileSize(file.getSize());
        response.setMimeType(resolveUploadMimeType(file.getContentType(), mediaKind));
        if ("image".equals(mediaKind)) {
            try (InputStream in = file.getInputStream()) {
                BufferedImage bufferedImage = ImageIO.read(in);
                if (bufferedImage != null) {
                    response.setWidth(bufferedImage.getWidth());
                    response.setHeight(bufferedImage.getHeight());
                }
            } catch (Exception ignored) {
            }
        }
        return response;
    }

    private RepairOrderPayments requireLatestPayment(String orderId) {
        RepairOrderPayments payment = repairOrderPaymentsService.getOne(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .eq(RepairOrderPayments::getRepairOrderId, orderId)
                .eq(RepairOrderPayments::getIsDelete, 0)
                .orderByDesc(RepairOrderPayments::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (payment == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单费用信息不存在");
        }
        return payment;
    }

    private void completeProcessingAfterSales(String orderId, long now) {
        if (!StringUtils.hasText(orderId)) {
            return;
        }
        AfterSalesApplications application = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .eq(AfterSalesApplications::getOrderId, orderId)
                .eq(AfterSalesApplications::getOrderType, 1)
                .eq(AfterSalesApplications::getApplicationType, 4)
                .eq(AfterSalesApplications::getStatus, 4)
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (application == null) {
            return;
        }
        application.setStatus(5);
        application.setCompletedTime(now);
        application.setUpdatedTime(now);
        afterSalesApplicationsService.updateById(application);
    }

    private OrderProgress saveOrderProgress(RepairOrders order, TechnicianAccounts technician, Integer status, String description) {
        return saveOrderProgress(order, technician, status, description, System.currentTimeMillis());
    }

    private OrderProgress saveOrderProgress(RepairOrders order, TechnicianAccounts technician, Integer status, String description, long createdTime) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(status);
        progress.setStatusName(getStatusText(status));
        progress.setDescription(description);
        progress.setOperatorId(technician.getId());
        progress.setOperatorType(2);
        progress.setOperatorName(technician.getUsername());
        progress.setCreatedTime(createdTime);
        progress.setIsDelete(0);
        if (!orderProgressService.save(progress)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存订单进度失败");
        }
        return progress;
    }

    private String resolveProgressDescription(OrderProgress progress) {
        InspectionProgressSnapshot snapshot = parseInspectionProgress(progress);
        if (snapshot != null) {
            return buildInspectionProgressSummary(snapshot);
        }
        return safe(progress == null ? null : progress.getDescription());
    }

    private InspectionProgressSnapshot parseInspectionProgress(OrderProgress progress) {
        if (progress == null || !StringUtils.hasText(progress.getDescription())) {
            return null;
        }
        try {
            Map<String, Object> payload = OBJECT_MAPPER.readValue(progress.getDescription(), new TypeReference<Map<String, Object>>() {});
            if (!INSPECTION_PROGRESS_TYPE.equals(String.valueOf(payload.get("type")))) {
                return null;
            }
            InspectionProgressSnapshot snapshot = new InspectionProgressSnapshot();
            snapshot.setInspectionDiagnosis(stringValue(payload.get("inspectionDiagnosis")));
            snapshot.setRepairPlan(stringValue(payload.get("repairPlan")));
            snapshot.setServiceFee(stringValue(payload.get("serviceFee")));
            snapshot.setMaterialFee(stringValue(payload.get("materialFee")));
            return snapshot;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildInspectionProgressSummary(InspectionProgressSnapshot snapshot) {
        return "师傅已完成检查：问题=" + safe(snapshot.getInspectionDiagnosis())
            + "；维修建议=" + safe(snapshot.getRepairPlan())
            + "；服务费=" + safe(snapshot.getServiceFee())
            + "；材料费=" + safe(snapshot.getMaterialFee());
    }

    private String buildInspectionFeeUpdateDescription(BigDecimal serviceFee, BigDecimal materialFee) {
        return "师傅已修改费用：服务费=" + formatMoney(serviceFee) + "；材料费=" + formatMoney(materialFee);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, ServiceTypes> listServiceTypeMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getServiceTypeId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .in(ServiceTypes::getId, ids)
                .eq(ServiceTypes::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceTypes::getId, item -> item, (a, b) -> a));
    }

    private Map<String, ServiceCategories> listCategoryMap(Map<String, ServiceTypes> serviceTypeMap) {
        Set<String> directIds = serviceTypeMap.values().stream()
            .map(ServiceTypes::getCategoryId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (directIds.isEmpty()) {
            return new HashMap<>();
        }

        List<ServiceCategories> directCategories = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .in(ServiceCategories::getId, directIds)
                .eq(ServiceCategories::getIsDelete, 0)
        );
        if (directCategories.isEmpty()) {
            return new HashMap<>();
        }

        Set<String> allIds = new LinkedHashSet<>(directIds);
        for (ServiceCategories category : directCategories) {
            allIds.addAll(parsePathIds(category.getPath()));
        }
        return serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .in(ServiceCategories::getId, allIds)
                .eq(ServiceCategories::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(ServiceCategories::getId, item -> item, (a, b) -> a));
    }

    private Map<String, UserAccounts> listUserMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .in(UserAccounts::getId, ids)
                .eq(UserAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(UserAccounts::getId, item -> item, (a, b) -> a));
    }

    private Map<String, UserAddresses> listAddressMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getServiceAddressId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return userAddressesService.list(
            new LambdaQueryWrapper<UserAddresses>()
                .in(UserAddresses::getId, ids)
                .eq(UserAddresses::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(UserAddresses::getId, item -> item, (a, b) -> a));
    }

    private Map<String, List<RepairOrderFaults>> listFaultMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return repairOrderFaultsService.list(
            new LambdaQueryWrapper<RepairOrderFaults>()
                .in(RepairOrderFaults::getRepairOrderId, ids)
                .eq(RepairOrderFaults::getIsDelete, 0)
                .orderByAsc(RepairOrderFaults::getCreatedTime)
        ).stream().collect(Collectors.groupingBy(RepairOrderFaults::getRepairOrderId));
    }

    private Map<String, RepairOrderPayments> listPaymentMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return repairOrderPaymentsService.list(
            new LambdaQueryWrapper<RepairOrderPayments>()
                .in(RepairOrderPayments::getRepairOrderId, ids)
                .eq(RepairOrderPayments::getIsDelete, 0)
                .orderByDesc(RepairOrderPayments::getCreatedTime)
        ).stream().collect(Collectors.toMap(RepairOrderPayments::getRepairOrderId, item -> item, (a, b) -> a));
    }

    private WorkerHomeOrderItem buildOrderItem(
        RepairOrders order,
        ServiceTypes serviceType,
        Map<String, ServiceCategories> categoryMap,
        TechnicianServiceAreas defaultArea,
        UserAccounts user,
        UserAddresses address,
        List<RepairOrderFaults> faults,
        RepairOrderPayments payment
    ) {
        WorkerHomeOrderItem item = new WorkerHomeOrderItem();
        item.setId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setStatus(order.getStatus());
        item.setStatusText(getDisplayStatusText(order));
        item.setServiceTypeId(order.getServiceTypeId());
        item.setServiceTypeName(serviceType == null ? "" : safe(serviceType.getName()));

        ServiceCategories category = serviceType == null ? null : categoryMap.get(serviceType.getCategoryId());
        item.setServiceCategoryId(category == null ? "" : safe(category.getId()));
        item.setServiceCategoryName(category == null ? "" : safe(category.getName()));
        item.setServiceCategoryPath(buildCategoryPath(category, categoryMap));

        Integer serviceMode = serviceType == null ? null : serviceType.getType();
        item.setServiceMode(serviceMode);
        item.setServiceModeText(getServiceModeText(serviceMode));

        item.setUserId(order.getAccountId());
        item.setUserName(user == null ? "" : safe(user.getUsername()));
        item.setServiceAddress(buildFullAddress(address));
        item.setServiceAddressShort(buildShortAddress(address));
        item.setContactName(address == null ? "" : safe(address.getContactName()));
        item.setContactPhone(address == null ? "" : safe(address.getContactPhone()));
        item.setApplianceBrand(safe(order.getApplianceBrand()));
        item.setApplianceModel(safe(order.getApplianceModel()));
        item.setFaultSummary(buildFaultSummary(faults));
        item.setTotalAmount(formatMoney(payment == null ? null : payment.getTotalAmount()));
        item.setPaidAmount(formatMoney(resolvePaidAmount(order, payment)));
        item.setAppointmentTime(order.getAppointmentTime());
        item.setCreatedTime(order.getCreatedTime());
        item.setUpdatedTime(order.getUpdatedTime());

        OrderActionState actionState = buildOrderActionState(order, serviceMode, payment);
        item.setNextActionText(actionState.getActionText());
        return item;
    }

    private int compareHomeOrderItem(WorkerHomeOrderItem left, WorkerHomeOrderItem right) {
        int statusCompare = Integer.compare(safeInt(left == null ? null : left.getStatus()), safeInt(right == null ? null : right.getStatus()));
        if (statusCompare != 0) {
            return statusCompare;
        }
        int appointmentCompare = compareTimestamp(left == null ? null : left.getAppointmentTime(), right == null ? null : right.getAppointmentTime());
        if (appointmentCompare != 0) {
            return appointmentCompare;
        }
        return compareTimestamp(right == null ? null : right.getUpdatedTime(), left == null ? null : left.getUpdatedTime());
    }

    private int compareHistoryOrderItem(WorkerHomeOrderItem left, WorkerHomeOrderItem right) {
        int updatedCompare = compareTimestamp(right == null ? null : right.getUpdatedTime(), left == null ? null : left.getUpdatedTime());
        if (updatedCompare != 0) {
            return updatedCompare;
        }
        return compareTimestamp(right == null ? null : right.getCreatedTime(), left == null ? null : left.getCreatedTime());
    }

    private int compareTimestamp(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return Long.compare(left, right);
    }

    private RepairOrders requireOwnedOrder(String orderId, String accountId) {
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getTechnicianAccountId, accountId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private ServiceTypes requireServiceType(String serviceTypeId) {
        ServiceTypes serviceType = serviceTypesService.getOne(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getId, serviceTypeId)
                .eq(ServiceTypes::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (serviceType == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "服务类型不存在或已停用");
        }
        return serviceType;
    }

    private TechnicianAccounts requireTechnician(String accountId) {
        TechnicianAccounts technician = technicianAccountsService.getOne(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getId, accountId)
                .eq(TechnicianAccounts::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (technician == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "维修师傅不存在");
        }
        return technician;
    }

    private RepairOrders reloadOrder(String orderId) {
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, orderId)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private int resolveAcceptStatus(Integer serviceMode) {
        int mode = safeInt(serviceMode);
        if (mode == 3) {
            return 3;
        }
        return 2;
    }

    private int resolveNextStatus(Integer serviceMode, int currentStatus, RepairOrderPayments payment) {
        int mode = safeInt(serviceMode);
        if (currentStatus == 4) {
            if (!isFullyPaid(payment)) {
                throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    mode == 2 ? "用户尚未支付安装尾款" : "用户尚未支付维修尾款"
                );
            }
            return 5;
        }
        if (currentStatus == 5) {
            return 5;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不支持继续处理");
    }

    private String buildProgressDescription(Integer serviceMode, Integer fromStatus, Integer targetStatus, boolean waitingUserConfirmation) {
        int mode = safeInt(serviceMode);
        int to = safeInt(targetStatus);
        if (to == 2) {
            return "师傅已接单，等待上门";
        }
        if (to == 3) {
            if (safeInt(fromStatus) == 2) {
                return "上门码核销成功，开始检查";
            }
            return mode == 3
                ? "线下维修订单已接单，待到店检查"
                : "师傅已上门，开始检查";
        }
        if (to == 5) {
            if (waitingUserConfirmation) {
                return "师傅已提交服务完成，等待用户确认";
            }
            if (mode == 2 && safeInt(fromStatus) == 2) {
                return "上门码核销成功，开始安装";
            }
            if (safeInt(fromStatus) == 4) {
                return mode == 2
                    ? "用户已支付安装尾款，开始安装"
                    : "用户已支付维修尾款，开始维修";
            }
            return "已开始服务";
        }
        if (to == 6) {
            return "服务已完成";
        }
        return "订单状态已更新";
    }

    private void ensureConversationSessionOpen(RepairOrders order, long now) {
        if (order == null
            || !StringUtils.hasText(order.getId())
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return;
        }
        ConversationSessions session = findConversationSession(order);
        if (session == null) {
            session = new ConversationSessions();
            session.setId(SnowflakeIdUtil.nextConversationSessionId());
            session.setUserAccountId(order.getAccountId());
            session.setTechnicianAccountId(order.getTechnicianAccountId());
            session.setRepairOrderId(order.getId());
            session.setUserUnreadCount(0);
            session.setTechnicianUnreadCount(0);
            session.setStatus(1);
            session.setCreatedTime(now);
            session.setUpdatedTime(now);
            session.setVersion(0);
            session.setIsDelete(0);
            conversationSessionsService.save(session);
            return;
        }
        if (safeInt(session.getStatus()) != 1 || !order.getId().equals(session.getRepairOrderId())) {
            session.setUserAccountId(order.getAccountId());
            session.setTechnicianAccountId(order.getTechnicianAccountId());
            session.setRepairOrderId(order.getId());
            session.setStatus(1);
            session.setUpdatedTime(now);
            conversationSessionsService.updateById(session);
        }
    }

    private void closeConversationSession(RepairOrders order, long now) {
        ConversationSessions session = findConversationSession(order);
        if (session == null) {
            return;
        }
        String referenceOrderId = resolveConversationReferenceOrderId(order);
        session.setRepairOrderId(StringUtils.hasText(referenceOrderId) ? referenceOrderId : order.getId());
        session.setStatus(StringUtils.hasText(referenceOrderId) ? 1 : 2);
        session.setUpdatedTime(now);
        conversationSessionsService.updateById(session);
    }

    private ConversationSessions findConversationSession(RepairOrders order) {
        if (order == null
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return null;
        }
        return conversationSessionsService.getOne(
            new LambdaQueryWrapper<ConversationSessions>()
                .eq(ConversationSessions::getUserAccountId, order.getAccountId())
                .eq(ConversationSessions::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(ConversationSessions::getIsDelete, 0)
                .orderByDesc(ConversationSessions::getUpdatedTime)
                .orderByDesc(ConversationSessions::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private String resolveConversationReferenceOrderId(RepairOrders order) {
        if (order == null
            || !StringUtils.hasText(order.getAccountId())
            || !StringUtils.hasText(order.getTechnicianAccountId())) {
            return null;
        }
        RepairOrders activeOrder = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, order.getAccountId())
                .eq(RepairOrders::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(RepairOrders::getIsDelete, 0)
                .in(RepairOrders::getStatus, 2, 3, 4, 5)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
                .last("limit 1"),
            false
        );
        if (activeOrder != null) {
            return activeOrder.getId();
        }

        List<String> relatedOrderIds = repairOrdersService.list(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getAccountId, order.getAccountId())
                .eq(RepairOrders::getTechnicianAccountId, order.getTechnicianAccountId())
                .eq(RepairOrders::getIsDelete, 0)
                .orderByDesc(RepairOrders::getUpdatedTime)
                .orderByDesc(RepairOrders::getCreatedTime)
        ).stream().map(RepairOrders::getId).filter(StringUtils::hasText).collect(Collectors.toList());
        if (relatedOrderIds.isEmpty()) {
            return null;
        }

        AfterSalesApplications activeApplication = afterSalesApplicationsService.getOne(
            new LambdaQueryWrapper<AfterSalesApplications>()
                .in(AfterSalesApplications::getOrderId, relatedOrderIds)
                .eq(AfterSalesApplications::getOrderType, ORDER_TYPE_REPAIR)
                .in(
                    AfterSalesApplications::getStatus,
                    AFTER_SALES_STATUS_PENDING,
                    AFTER_SALES_STATUS_APPROVED,
                    AFTER_SALES_STATUS_PROCESSING
                )
                .eq(AfterSalesApplications::getIsDelete, 0)
                .orderByDesc(AfterSalesApplications::getUpdatedTime)
                .orderByDesc(AfterSalesApplications::getCreatedTime)
                .last("limit 1"),
            false
        );
        return activeApplication == null ? null : activeApplication.getOrderId();
    }

    private OrderActionState buildOrderActionState(RepairOrders order, Integer serviceMode, RepairOrderPayments payment) {
        int mode = safeInt(serviceMode);
        int currentStatus = safeInt(order == null ? null : order.getStatus());
        if (currentStatus == 1) {
            return new OrderActionState(true, "accept", "接单", "确认接单后进入下一处理阶段");
        }
        if (currentStatus == 2) {
            return new OrderActionState(
                true,
                "scanDoorQr",
                mode == 2 ? "扫码开始安装" : "扫码上门",
                "上门后扫描用户提供的上门码"
            );
        }
        if (currentStatus == 3) {
            return new OrderActionState(
                true,
                "submitInspection",
                "提交检查",
                "请上传检查凭证并填写问题说明与费用"
            );
        }
        if (currentStatus == 4) {
            if (isFullyPaid(payment)) {
                return new OrderActionState(
                    true,
                    "advance",
                    mode == 2 ? "开始安装" : "开始维修",
                    "用户已支付，可开始后续服务"
                );
            }
            if (canEditInspectionFees(order, mode, payment)) {
                return new OrderActionState(
                    true,
                    "editInspectionFee",
                    "修改费用",
                    "已提交费用，用户支付前可调整服务费和材料费"
                );
            }
            return new OrderActionState(false, "", "", "等待用户支付尾款");
        }
        if (currentStatus == 5) {
            if (isWaitingUserConfirmation(order)) {
                return new OrderActionState(false, "", "", "已提交完工，等待用户确认完成");
            }
            return new OrderActionState(true, "advance", "提交完工", "提交后需等待用户确认，订单才会结束");
        }
        return new OrderActionState(false, "", "", "暂无可执行操作");
    }

    private boolean isWaitingUserConfirmation(RepairOrders order) {
        return order != null
            && safeInt(order.getStatus()) == 5
            && order.getEndTime() != null
            && order.getEndTime() > 0L
            && (order.getCompletionTime() == null || order.getCompletionTime() <= 0L);
    }

    private boolean canEditInspectionFees(RepairOrders order, int serviceMode, RepairOrderPayments payment) {
        return order != null
            && safeInt(order.getStatus()) == 4
            && (serviceMode == 1 || serviceMode == 3)
            && payment != null
            && !isFullyPaid(payment);
    }

    private String getPaymentStatusText(Integer paymentStatus, RepairOrderPayments payment) {
        int value = safeInt(paymentStatus);
        if (value == 2) {
            if (isPrepaidOnly(payment)) {
                return "已预付";
            }
            if (isFullyPaid(payment)) {
                return "已支付";
            }
            return "待补尾款";
        }
        if (value == 1) {
            return "待支付";
        }
        if (value == 3) {
            return "已退款";
        }
        return "未知";
    }

    private String getStatusText(Integer status) {
        int value = safeInt(status);
        if (value == 1) {
            return "待接单";
        }
        if (value == 2) {
            return "待上门";
        }
        if (value == 3) {
            return "待检查";
        }
        if (value == 4) {
            return "待支付";
        }
        if (value == 5) {
            return "服务中";
        }
        if (value == 6) {
            return "已完成";
        }
        if (value == 7) {
            return "已取消";
        }
        if (value == 8) {
            return "已退款";
        }
        return "未知状态";
    }

    private String getDisplayStatusText(RepairOrders order) {
        if (isWaitingUserConfirmation(order)) {
            return "待用户确认";
        }
        return getStatusText(order == null ? null : order.getStatus());
    }

    private String getServiceModeText(Integer serviceMode) {
        int value = safeInt(serviceMode);
        if (value == 1) {
            return "上门维修";
        }
        if (value == 2) {
            return "上门安装";
        }
        if (value == 3) {
            return "线下维修";
        }
        return "未知服务";
    }

    private boolean isPrepaidOnly(RepairOrderPayments payment) {
        if (payment == null) {
            return false;
        }
        return isZero(payment.getServiceFee())
            && isZero(payment.getMaterialFee())
            && isZero(payment.getOvertimeFee());
    }

    private boolean isFullyPaid(RepairOrderPayments payment) {
        if (payment == null) {
            return false;
        }
        BigDecimal totalAmount = payment.getTotalAmount();
        BigDecimal actualAmount = payment.getActualAmount();
        if (totalAmount == null || actualAmount == null) {
            return false;
        }
        return actualAmount.compareTo(totalAmount) >= 0;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal resolvePaidAmount(RepairOrders order, RepairOrderPayments payment) {
        if (payment == null) {
            return BigDecimal.ZERO;
        }
        Integer paymentStatus = order.getPaymentStatus();
        if (paymentStatus != null && (paymentStatus == 2 || paymentStatus == 3)) {
            return payment.getActualAmount();
        }
        return BigDecimal.ZERO;
    }

    private String buildFaultSummary(List<RepairOrderFaults> faults) {
        if (faults == null || faults.isEmpty()) {
            return "";
        }
        Set<String> phenomenonIds = faults.stream()
            .map(RepairOrderFaults::getFaultPhenomenonId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        Map<String, FaultPhenomena> phenomenonMap = phenomenonIds.isEmpty()
            ? new HashMap<>()
            : faultPhenomenaService.list(
                new LambdaQueryWrapper<FaultPhenomena>()
                    .in(FaultPhenomena::getId, phenomenonIds)
                    .eq(FaultPhenomena::getIsDelete, 0)
            ).stream().collect(Collectors.toMap(FaultPhenomena::getId, item -> item, (a, b) -> a));

        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (RepairOrderFaults fault : faults) {
            FaultPhenomena phenomenon = phenomenonMap.get(fault.getFaultPhenomenonId());
            if (phenomenon != null && StringUtils.hasText(phenomenon.getName())) {
                parts.add(phenomenon.getName().trim());
            }
            if (StringUtils.hasText(fault.getFaultDescription())) {
                parts.add(fault.getFaultDescription().trim());
            }
        }
        return String.join("?", parts);
    }

    private String buildFullAddress(UserAddresses address) {
        if (address == null) {
            return "";
        }
        return safe(address.getProvince())
            + safe(address.getCity())
            + safe(address.getDistrict())
            + safe(address.getStreet())
            + safe(address.getDetailedAddress());
    }

    private String buildShortAddress(UserAddresses address) {
        if (address == null) {
            return "";
        }
        String shortAddress = safe(address.getStreet()) + safe(address.getDetailedAddress());
        return StringUtils.hasText(shortAddress) ? shortAddress : buildFullAddress(address);
    }

    private String buildCategoryPath(ServiceCategories category, Map<String, ServiceCategories> categoryMap) {
        if (category == null) {
            return "";
        }
        if (!StringUtils.hasText(category.getPath())) {
            return safe(category.getName());
        }
        List<String> names = new ArrayList<>();
        for (String id : parsePathIds(category.getPath())) {
            ServiceCategories current = categoryMap.get(id);
            if (current != null && StringUtils.hasText(current.getName())) {
                names.add(current.getName().trim());
            }
        }
        if (names.isEmpty()) {
            return safe(category.getName());
        }
        return String.join(" / ", names);
    }

    private List<String> parsePathIds(String path) {
        List<String> ids = new ArrayList<>();
        if (!StringUtils.hasText(path)) {
            return ids;
        }
        for (String part : path.split("/")) {
            if (StringUtils.hasText(part)) {
                ids.add(part.trim());
            }
        }
        return ids;
    }

    private String formatMoney(BigDecimal value) {
        return defaultZero(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalizeFee(BigDecimal value) {
        BigDecimal normalized = defaultZero(value).setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "费用不能小于 0");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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

    private String resolveMediaName(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private TechnicianServiceAreas getDefaultArea(String accountId) {
        return technicianServiceAreasService.getOne(
            new LambdaQueryWrapper<TechnicianServiceAreas>()
                .eq(TechnicianServiceAreas::getTechnicianAccountId, accountId)
                .eq(TechnicianServiceAreas::getIsDelete, 0)
                .orderByDesc(TechnicianServiceAreas::getIsDefault)
                .orderByDesc(TechnicianServiceAreas::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅订单");
        }
        return user;
    }


    private static final class InspectionProgressSnapshot {

        private String inspectionDiagnosis;
        private String repairPlan;
        private String serviceFee;
        private String materialFee;

        private String getInspectionDiagnosis() {
            return inspectionDiagnosis;
        }

        private void setInspectionDiagnosis(String inspectionDiagnosis) {
            this.inspectionDiagnosis = inspectionDiagnosis;
        }

        private String getRepairPlan() {
            return repairPlan;
        }

        private void setRepairPlan(String repairPlan) {
            this.repairPlan = repairPlan;
        }

        private String getServiceFee() {
            return serviceFee;
        }

        private void setServiceFee(String serviceFee) {
            this.serviceFee = serviceFee;
        }

        private String getMaterialFee() {
            return materialFee;
        }

        private void setMaterialFee(String materialFee) {
            this.materialFee = materialFee;
        }
    }

    private static final class OrderActionState {

        private final boolean available;
        private final String actionType;
        private final String actionText;
        private final String actionHint;

        private OrderActionState(boolean available, String actionType, String actionText, String actionHint) {
            this.available = available;
            this.actionType = actionType;
            this.actionText = actionText;
            this.actionHint = actionHint;
        }

        private boolean isAvailable() {
            return available;
        }

        private String getActionType() {
            return actionType;
        }

        private String getActionText() {
            return actionText;
        }

        private String getActionHint() {
            return actionHint;
        }
    }
}
