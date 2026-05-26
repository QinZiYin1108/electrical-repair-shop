package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.FaultPhenomena;
import com.example.backend.entity.Images;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.RepairOrderFaults;
import com.example.backend.entity.RepairOrderPayments;
import com.example.backend.entity.RepairOrders;
import com.example.backend.entity.ServiceCategories;
import com.example.backend.entity.ServiceTypes;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.entity.UserAccounts;
import com.example.backend.entity.UserAddresses;
import com.example.backend.entity.Videos;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminReserveOrderDetailResponse;
import com.example.backend.model.admin.AdminReserveOrderFaultItemResponse;
import com.example.backend.model.admin.AdminReserveOrderListItemResponse;
import com.example.backend.model.admin.AdminReserveOrderMediaItemResponse;
import com.example.backend.model.admin.AdminReserveOrderProgressItemResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.FaultPhenomenaService;
import com.example.backend.service.ImagesService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.RepairOrderFaultsService;
import com.example.backend.service.RepairOrderPaymentsService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.service.ServiceCategoriesService;
import com.example.backend.service.ServiceTypesService;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.service.UserAccountsService;
import com.example.backend.service.UserAddressesService;
import com.example.backend.service.VideosService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/orders/reserve")
public class AdminReserveOrderController {

    private static final String ORDER_FAULT_BUSINESS_TYPE = "REPAIR_ORDER_FAULT";
    private static final String ORDER_INSPECTION_BUSINESS_TYPE = "REPAIR_ORDER_INSPECTION";
    private static final String INSPECTION_PROGRESS_TYPE = "inspection";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderPaymentsService repairOrderPaymentsService;
    private final RepairOrderFaultsService repairOrderFaultsService;
    private final OrderProgressService orderProgressService;
    private final ServiceTypesService serviceTypesService;
    private final ServiceCategoriesService serviceCategoriesService;
    private final UserAccountsService userAccountsService;
    private final TechnicianAccountsService technicianAccountsService;
    private final UserAddressesService userAddressesService;
    private final FaultPhenomenaService faultPhenomenaService;
    private final ImagesService imagesService;
    private final VideosService videosService;

    public AdminReserveOrderController(
        RepairOrdersService repairOrdersService,
        RepairOrderPaymentsService repairOrderPaymentsService,
        RepairOrderFaultsService repairOrderFaultsService,
        OrderProgressService orderProgressService,
        ServiceTypesService serviceTypesService,
        ServiceCategoriesService serviceCategoriesService,
        UserAccountsService userAccountsService,
        TechnicianAccountsService technicianAccountsService,
        UserAddressesService userAddressesService,
        FaultPhenomenaService faultPhenomenaService,
        ImagesService imagesService,
        VideosService videosService
    ) {
        this.repairOrdersService = repairOrdersService;
        this.repairOrderPaymentsService = repairOrderPaymentsService;
        this.repairOrderFaultsService = repairOrderFaultsService;
        this.orderProgressService = orderProgressService;
        this.serviceTypesService = serviceTypesService;
        this.serviceCategoriesService = serviceCategoriesService;
        this.userAccountsService = userAccountsService;
        this.technicianAccountsService = technicianAccountsService;
        this.userAddressesService = userAddressesService;
        this.faultPhenomenaService = faultPhenomenaService;
        this.imagesService = imagesService;
        this.videosService = videosService;
    }

    @GetMapping
    public Result<Page<AdminReserveOrderListItemResponse>> listReserveOrders(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "paymentStatus", required = false) Integer paymentStatus,
        @RequestParam(value = "serviceMode", required = false) Integer serviceMode,
        @RequestParam(value = "appointmentStart", required = false) Long appointmentStart,
        @RequestParam(value = "appointmentEnd", required = false) Long appointmentEnd
    ) {
        requireAdmin();
        long currentPage = pageNum <= 0 ? 1 : pageNum;
        long currentSize = pageSize <= 0 ? 10 : pageSize;

        LambdaQueryWrapper<RepairOrders> wrapper = new LambdaQueryWrapper<RepairOrders>()
            .eq(RepairOrders::getIsDelete, 0);
        if (status != null) {
            wrapper.eq(RepairOrders::getStatus, status);
        }
        if (paymentStatus != null) {
            wrapper.eq(RepairOrders::getPaymentStatus, paymentStatus);
        }
        if (appointmentStart != null && appointmentStart > 0L) {
            wrapper.ge(RepairOrders::getAppointmentTime, appointmentStart);
        }
        if (appointmentEnd != null && appointmentEnd > 0L) {
            wrapper.le(RepairOrders::getAppointmentTime, appointmentEnd);
        }
        if (serviceMode != null) {
            Set<String> serviceTypeIds = findServiceTypeIdsByMode(serviceMode);
            if (serviceTypeIds.isEmpty()) {
                return Result.success(emptyPage(currentPage, currentSize));
            }
            wrapper.in(RepairOrders::getServiceTypeId, serviceTypeIds);
        }

        String normalizedKeyword = trimToNull(keyword);
        if (StringUtils.hasText(normalizedKeyword)) {
            applyKeywordFilter(wrapper, normalizedKeyword);
        }

        wrapper.orderByDesc(RepairOrders::getUpdatedTime)
            .orderByDesc(RepairOrders::getCreatedTime);

        Page<RepairOrders> page = repairOrdersService.page(new Page<>(currentPage, currentSize), wrapper);
        Page<AdminReserveOrderListItemResponse> responsePage = new Page<>(
            page.getCurrent(),
            page.getSize(),
            page.getTotal()
        );
        responsePage.setRecords(buildListItems(page.getRecords()));
        return Result.success(responsePage);
    }

    @GetMapping("/{id}")
    public Result<AdminReserveOrderDetailResponse> getReserveOrderDetail(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(buildDetailResponse(requireReserveOrder(id)));
    }

    private void applyKeywordFilter(LambdaQueryWrapper<RepairOrders> wrapper, String keyword) {
        Set<String> userIds = findUserIdsByKeyword(keyword);
        Set<String> technicianIds = findTechnicianIdsByKeyword(keyword);
        Set<String> serviceTypeIds = findServiceTypeIdsByKeyword(keyword);
        Set<String> addressIds = findAddressIdsByKeyword(keyword);

        wrapper.and(q -> {
            q.like(RepairOrders::getOrderNo, keyword);
            if (!userIds.isEmpty()) {
                q.or().in(RepairOrders::getAccountId, userIds);
            }
            if (!technicianIds.isEmpty()) {
                q.or().in(RepairOrders::getTechnicianAccountId, technicianIds);
            }
            if (!serviceTypeIds.isEmpty()) {
                q.or().in(RepairOrders::getServiceTypeId, serviceTypeIds);
            }
            if (!addressIds.isEmpty()) {
                q.or().in(RepairOrders::getServiceAddressId, addressIds);
            }
        });
    }

    private Page<AdminReserveOrderListItemResponse> emptyPage(long currentPage, long currentSize) {
        Page<AdminReserveOrderListItemResponse> page = new Page<>(currentPage, currentSize, 0);
        page.setRecords(Collections.emptyList());
        return page;
    }

    private List<AdminReserveOrderListItemResponse> buildListItems(List<RepairOrders> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);

        List<AdminReserveOrderListItemResponse> items = new ArrayList<>();
        for (RepairOrders order : orders) {
            items.add(buildListItem(
                order,
                serviceTypeMap.get(order.getServiceTypeId()),
                categoryMap,
                userMap.get(order.getAccountId()),
                technicianMap.get(order.getTechnicianAccountId()),
                addressMap.get(order.getServiceAddressId()),
                faultMap.get(order.getId()),
                paymentMap.get(order.getId())
            ));
        }
        return items;
    }

    private AdminReserveOrderListItemResponse buildListItem(
        RepairOrders order,
        ServiceTypes serviceType,
        Map<String, ServiceCategories> categoryMap,
        UserAccounts user,
        TechnicianAccounts technician,
        UserAddresses address,
        List<RepairOrderFaults> faults,
        RepairOrderPayments payment
    ) {
        AdminReserveOrderListItemResponse item = new AdminReserveOrderListItemResponse();
        item.setId(order.getId());
        item.setOrderNo(safe(order.getOrderNo()));
        item.setStatus(order.getStatus());
        item.setStatusText(getDisplayStatusText(order));
        item.setPaymentStatus(order.getPaymentStatus());
        item.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus(), payment));
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
        item.setUserPhone(user == null ? "" : safe(user.getPhone()));
        item.setTechnicianId(safe(order.getTechnicianAccountId()));
        item.setTechnicianName(technician == null ? "" : safe(technician.getUsername()));
        item.setTechnicianPhone(technician == null ? "" : safe(technician.getPhone()));
        item.setContactName(address == null ? "" : safe(address.getContactName()));
        item.setContactPhone(address == null ? "" : safe(address.getContactPhone()));
        item.setServiceAddress(buildFullAddress(address));
        item.setApplianceBrand(safe(order.getApplianceBrand()));
        item.setApplianceModel(safe(order.getApplianceModel()));
        item.setFaultSummary(buildFaultSummary(faults));
        item.setTotalAmount(formatMoney(payment == null ? null : payment.getTotalAmount()));
        item.setPaidAmount(formatMoney(resolvePaidAmount(order, payment)));
        item.setAppointmentTime(order.getAppointmentTime());
        item.setCreatedTime(order.getCreatedTime());
        item.setUpdatedTime(order.getUpdatedTime());
        return item;
    }

    private AdminReserveOrderDetailResponse buildDetailResponse(RepairOrders order) {
        List<RepairOrders> orders = Collections.singletonList(order);
        Map<String, ServiceTypes> serviceTypeMap = listServiceTypeMap(orders);
        Map<String, ServiceCategories> categoryMap = listCategoryMap(serviceTypeMap);
        Map<String, UserAccounts> userMap = listUserMap(orders);
        Map<String, TechnicianAccounts> technicianMap = listTechnicianMap(orders);
        Map<String, UserAddresses> addressMap = listAddressMap(orders);
        Map<String, List<RepairOrderFaults>> faultMap = listFaultMap(orders);
        Map<String, RepairOrderPayments> paymentMap = listPaymentMap(orders);
        List<OrderProgress> progressList = listOrderProgress(order.getId());
        OrderProgress latestInspectionProgress = findLatestInspectionProgress(progressList);
        InspectionProgressSnapshot inspectionSnapshot = parseInspectionProgress(latestInspectionProgress);

        ServiceTypes serviceType = serviceTypeMap.get(order.getServiceTypeId());
        UserAccounts user = userMap.get(order.getAccountId());
        TechnicianAccounts technician = technicianMap.get(order.getTechnicianAccountId());
        UserAddresses address = addressMap.get(order.getServiceAddressId());
        RepairOrderPayments payment = paymentMap.get(order.getId());

        AdminReserveOrderListItemResponse item = buildListItem(
            order,
            serviceType,
            categoryMap,
            user,
            technician,
            address,
            faultMap.get(order.getId()),
            payment
        );

        AdminReserveOrderDetailResponse response = new AdminReserveOrderDetailResponse();
        copyListFields(item, response);
        response.setUserEmail(user == null ? "" : safe(user.getEmail()));
        response.setTechnicianEmail(technician == null ? "" : safe(technician.getEmail()));
        response.setServiceAddressShort(buildShortAddress(address));
        response.setPurchaseDate(order.getPurchaseDate());
        response.setStartTime(order.getStartTime());
        response.setEndTime(order.getEndTime());
        response.setCompletionTime(order.getCompletionTime());
        response.setDoorFee(formatMoney(payment == null ? null : payment.getDoorFee()));
        response.setDistanceFee(formatMoney(payment == null ? null : payment.getDistanceFee()));
        response.setServiceFee(formatMoney(payment == null ? null : payment.getServiceFee()));
        response.setMaterialFee(formatMoney(payment == null ? null : payment.getMaterialFee()));
        response.setOvertimeFee(formatMoney(payment == null ? null : payment.getOvertimeFee()));
        response.setRemark(safe(order.getRemark()));
        response.setInspectionDiagnosis(inspectionSnapshot == null ? "" : inspectionSnapshot.getInspectionDiagnosis());
        response.setRepairPlan(inspectionSnapshot == null ? "" : inspectionSnapshot.getRepairPlan());
        response.setInspectionTime(latestInspectionProgress == null ? null : latestInspectionProgress.getCreatedTime());
        response.setCancelReason(safe(order.getCancelReason()));
        response.setCancelTime(order.getCancelTime());
        response.setRefundReason(safe(order.getRefundReason()));
        response.setRefundAmount(formatMoney(order.getRefundAmount()));
        response.setRefundTime(order.getRefundTime());
        response.setFaultList(buildFaultItems(faultMap.get(order.getId())));
        response.setInspectionImages(listInspectionImages(latestInspectionProgress == null ? null : latestInspectionProgress.getId()));
        response.setInspectionVideos(listInspectionVideos(latestInspectionProgress == null ? null : latestInspectionProgress.getId()));
        response.setProgressList(buildProgressItems(order, progressList));
        return response;
    }

    private void copyListFields(AdminReserveOrderListItemResponse item, AdminReserveOrderDetailResponse response) {
        response.setId(item.getId());
        response.setOrderNo(item.getOrderNo());
        response.setStatus(item.getStatus());
        response.setStatusText(item.getStatusText());
        response.setPaymentStatus(item.getPaymentStatus());
        response.setPaymentStatusText(item.getPaymentStatusText());
        response.setServiceTypeId(item.getServiceTypeId());
        response.setServiceTypeName(item.getServiceTypeName());
        response.setServiceCategoryId(item.getServiceCategoryId());
        response.setServiceCategoryName(item.getServiceCategoryName());
        response.setServiceCategoryPath(item.getServiceCategoryPath());
        response.setServiceMode(item.getServiceMode());
        response.setServiceModeText(item.getServiceModeText());
        response.setUserId(item.getUserId());
        response.setUserName(item.getUserName());
        response.setUserPhone(item.getUserPhone());
        response.setTechnicianId(item.getTechnicianId());
        response.setTechnicianName(item.getTechnicianName());
        response.setTechnicianPhone(item.getTechnicianPhone());
        response.setContactName(item.getContactName());
        response.setContactPhone(item.getContactPhone());
        response.setServiceAddress(item.getServiceAddress());
        response.setApplianceBrand(item.getApplianceBrand());
        response.setApplianceModel(item.getApplianceModel());
        response.setFaultSummary(item.getFaultSummary());
        response.setTotalAmount(item.getTotalAmount());
        response.setPaidAmount(item.getPaidAmount());
        response.setAppointmentTime(item.getAppointmentTime());
        response.setCreatedTime(item.getCreatedTime());
        response.setUpdatedTime(item.getUpdatedTime());
    }

    private List<AdminReserveOrderFaultItemResponse> buildFaultItems(List<RepairOrderFaults> faults) {
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

        List<AdminReserveOrderFaultItemResponse> items = new ArrayList<>();
        for (RepairOrderFaults fault : faults) {
            FaultPhenomena phenomenon = phenomenonMap.get(fault.getFaultPhenomenonId());
            AdminReserveOrderFaultItemResponse item = new AdminReserveOrderFaultItemResponse();
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

    private List<AdminReserveOrderMediaItemResponse> toImageItems(List<Images> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminReserveOrderMediaItemResponse> items = new ArrayList<>();
        for (Images image : images) {
            AdminReserveOrderMediaItemResponse item = new AdminReserveOrderMediaItemResponse();
            item.setId(image.getId());
            item.setName(StringUtils.hasText(image.getOriginalName()) ? image.getOriginalName() : safe(image.getFileName()));
            item.setUrl(safe(image.getFileUrl()));
            item.setThumbnailUrl(safe(image.getFileUrl()));
            item.setMimeType(safe(image.getMimeType()));
            item.setFileSize(image.getFileSize());
            item.setWidth(image.getWidth());
            item.setHeight(image.getHeight());
            items.add(item);
        }
        return items;
    }

    private List<AdminReserveOrderMediaItemResponse> toVideoItems(List<Videos> videos) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminReserveOrderMediaItemResponse> items = new ArrayList<>();
        for (Videos video : videos) {
            AdminReserveOrderMediaItemResponse item = new AdminReserveOrderMediaItemResponse();
            item.setId(video.getId());
            item.setName(StringUtils.hasText(video.getOriginalName()) ? video.getOriginalName() : safe(video.getFileName()));
            item.setUrl(safe(video.getFileUrl()));
            item.setThumbnailUrl(safe(video.getThumbnailUrl()));
            item.setMimeType(safe(video.getMimeType()));
            item.setFileSize(video.getFileSize());
            item.setWidth(video.getWidth());
            item.setHeight(video.getHeight());
            item.setDuration(video.getDuration());
            items.add(item);
        }
        return items;
    }

    private List<AdminReserveOrderMediaItemResponse> listInspectionImages(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        return toImageItems(imagesService.list(
            new LambdaQueryWrapper<Images>()
                .eq(Images::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Images::getBusinessId, progressId)
                .eq(Images::getIsDelete, 0)
                .orderByAsc(Images::getCreatedTime)
        ));
    }

    private List<AdminReserveOrderMediaItemResponse> listInspectionVideos(String progressId) {
        if (!StringUtils.hasText(progressId)) {
            return Collections.emptyList();
        }
        return toVideoItems(videosService.list(
            new LambdaQueryWrapper<Videos>()
                .eq(Videos::getBusinessType, ORDER_INSPECTION_BUSINESS_TYPE)
                .eq(Videos::getBusinessId, progressId)
                .eq(Videos::getIsDelete, 0)
                .orderByAsc(Videos::getCreatedTime)
        ));
    }

    private List<AdminReserveOrderProgressItemResponse> buildProgressItems(RepairOrders order, List<OrderProgress> progressList) {
        List<AdminReserveOrderProgressItemResponse> items = new ArrayList<>();

        AdminReserveOrderProgressItemResponse initial = new AdminReserveOrderProgressItemResponse();
        initial.setId("INIT-" + order.getId());
        initial.setStatus(1);
        initial.setStatusText(getStatusText(1));
        initial.setDescription("用户已提交订单，等待师傅接单");
        initial.setOperatorName("系统");
        initial.setOperatorType(4);
        initial.setCreatedTime(order.getCreatedTime());
        items.add(initial);

        for (OrderProgress progress : progressList) {
            AdminReserveOrderProgressItemResponse item = new AdminReserveOrderProgressItemResponse();
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

    private String resolveProgressDescription(OrderProgress progress) {
        InspectionProgressSnapshot snapshot = parseInspectionProgress(progress);
        if (snapshot != null) {
            return buildInspectionProgressSummary(snapshot);
        }
        return safe(progress == null ? null : progress.getDescription());
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

    private InspectionProgressSnapshot parseInspectionProgress(OrderProgress progress) {
        if (progress == null || !StringUtils.hasText(progress.getDescription())) {
            return null;
        }
        try {
            Map<String, Object> payload = OBJECT_MAPPER.readValue(
                progress.getDescription(),
                new TypeReference<Map<String, Object>>() {}
            );
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

    private RepairOrders requireReserveOrder(String id) {
        RepairOrders order = repairOrdersService.getOne(
            new LambdaQueryWrapper<RepairOrders>()
                .eq(RepairOrders::getId, id)
                .eq(RepairOrders::getIsDelete, 0)
                .last("limit 1"),
            false
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "预约订单不存在");
        }
        return order;
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

    private Map<String, TechnicianAccounts> listTechnicianMap(List<RepairOrders> orders) {
        Set<String> ids = orders.stream()
            .map(RepairOrders::getTechnicianAccountId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .in(TechnicianAccounts::getId, ids)
                .eq(TechnicianAccounts::getIsDelete, 0)
        ).stream().collect(Collectors.toMap(TechnicianAccounts::getId, item -> item, (a, b) -> a));
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

    private Set<String> findUserIdsByKeyword(String keyword) {
        return userAccountsService.list(
            new LambdaQueryWrapper<UserAccounts>()
                .eq(UserAccounts::getIsDelete, 0)
                .and(q -> q.like(UserAccounts::getUsername, keyword)
                    .or().like(UserAccounts::getPhone, keyword)
                    .or().like(UserAccounts::getEmail, keyword))
        ).stream().map(UserAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    private Set<String> findTechnicianIdsByKeyword(String keyword) {
        return technicianAccountsService.list(
            new LambdaQueryWrapper<TechnicianAccounts>()
                .eq(TechnicianAccounts::getIsDelete, 0)
                .and(q -> q.like(TechnicianAccounts::getUsername, keyword)
                    .or().like(TechnicianAccounts::getPhone, keyword)
                    .or().like(TechnicianAccounts::getEmail, keyword))
        ).stream().map(TechnicianAccounts::getId).filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    private Set<String> findServiceTypeIdsByKeyword(String keyword) {
        Set<String> categoryIds = serviceCategoriesService.list(
            new LambdaQueryWrapper<ServiceCategories>()
                .eq(ServiceCategories::getIsDelete, 0)
                .like(ServiceCategories::getName, keyword)
        ).stream().map(ServiceCategories::getId).filter(StringUtils::hasText).collect(Collectors.toSet());

        LambdaQueryWrapper<ServiceTypes> wrapper = new LambdaQueryWrapper<ServiceTypes>()
            .eq(ServiceTypes::getIsDelete, 0)
            .like(ServiceTypes::getName, keyword);
        if (!categoryIds.isEmpty()) {
            wrapper.or().in(ServiceTypes::getCategoryId, categoryIds);
        }
        return serviceTypesService.list(wrapper).stream()
            .map(ServiceTypes::getId)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
    }

    private Set<String> findAddressIdsByKeyword(String keyword) {
        return userAddressesService.list(
            new LambdaQueryWrapper<UserAddresses>()
                .eq(UserAddresses::getIsDelete, 0)
                .and(q -> q.like(UserAddresses::getContactName, keyword)
                    .or().like(UserAddresses::getContactPhone, keyword)
                    .or().like(UserAddresses::getProvince, keyword)
                    .or().like(UserAddresses::getCity, keyword)
                    .or().like(UserAddresses::getDistrict, keyword)
                    .or().like(UserAddresses::getStreet, keyword)
                    .or().like(UserAddresses::getDetailedAddress, keyword))
        ).stream().map(UserAddresses::getId).filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    private Set<String> findServiceTypeIdsByMode(Integer serviceMode) {
        return serviceTypesService.list(
            new LambdaQueryWrapper<ServiceTypes>()
                .eq(ServiceTypes::getType, serviceMode)
                .eq(ServiceTypes::getIsDelete, 0)
        ).stream().map(ServiceTypes::getId).filter(StringUtils::hasText).collect(Collectors.toSet());
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

    private boolean isWaitingUserConfirmation(RepairOrders order) {
        return order != null
            && safeInt(order.getStatus()) == 5
            && order.getEndTime() != null
            && order.getEndTime() > 0L
            && (order.getCompletionTime() == null || order.getCompletionTime() <= 0L);
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
        if (payment == null || payment.getTotalAmount() == null || payment.getActualAmount() == null) {
            return false;
        }
        return payment.getActualAmount().compareTo(payment.getTotalAmount()) >= 0;
    }

    private BigDecimal resolvePaidAmount(RepairOrders order, RepairOrderPayments payment) {
        if (payment == null) {
            return BigDecimal.ZERO;
        }
        int paymentStatus = safeInt(order == null ? null : order.getPaymentStatus());
        if (paymentStatus == 2 || paymentStatus == 3) {
            return defaultZero(payment.getActualAmount());
        }
        return BigDecimal.ZERO;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
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
        return String.join(" / ", parts);
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员预约订单");
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
}
