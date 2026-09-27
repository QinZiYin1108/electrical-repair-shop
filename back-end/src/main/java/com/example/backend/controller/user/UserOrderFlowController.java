package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserOrderFlowModel;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.service.CreditRecordsService;
import com.example.backend.service.UserOrderFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "用户端/订单流程")
@RequestMapping("/user/order-flow")
public class UserOrderFlowController {

    private final UserOrderFlowService userOrderFlowService;
    private final CreditRecordsService creditRecordsService;

    public UserOrderFlowController(
            UserOrderFlowService userOrderFlowService, CreditRecordsService creditRecordsService) {
        this.userOrderFlowService = userOrderFlowService;
        this.creditRecordsService = creditRecordsService;
    }

    @Operation(summary = "查询ServiceModes")
    @GetMapping("/service-modes")
    public Result<List<UserOrderFlowModel.ServiceModeItem>> listServiceModes() {
        return Result.success(userOrderFlowService.listServiceModes());
    }

    @Operation(summary = "查询CategoryTree")
    @GetMapping("/categories")
    public Result<List<UserOrderFlowModel.CategoryNode>> listCategoryTree(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(userOrderFlowService.listCategoryTree(keyword));
    }

    @Operation(summary = "查询Category详情")
    @GetMapping("/category-detail")
    public Result<UserOrderFlowModel.CategoryDetailResponse> getCategoryDetail(
            @RequestParam("categoryId") String categoryId) {
        return Result.success(userOrderFlowService.getCategoryDetail(categoryId));
    }

    @Operation(summary = "查询ServiceTypes")
    @GetMapping("/service-types")
    public Result<List<UserOrderFlowModel.ServiceTypeItem>> listServiceTypes(
            @RequestParam("serviceMode") Integer serviceMode,
            @RequestParam("categoryId") String categoryId) {
        return Result.success(userOrderFlowService.listServiceTypes(serviceMode, categoryId));
    }

    @Operation(summary = "查询SelectionContext")
    @GetMapping("/selection-context")
    public Result<UserOrderFlowModel.SelectionContextResponse> getSelectionContext(
            @RequestParam("serviceMode") Integer serviceMode,
            @RequestParam("serviceTypeId") String serviceTypeId,
            @RequestParam(value = "addressId", required = false) String addressId) {
        return Result.success(
                userOrderFlowService.getSelectionContext(serviceMode, serviceTypeId, addressId));
    }

    @Operation(summary = "查询AllTechnicians")
    @GetMapping("/all-technicians")
    public Result<UserOrderFlowModel.TechnicianBrowseResponse> listAllTechnicians(
            @RequestParam(value = "addressId", required = false) String addressId) {
        return Result.success(userOrderFlowService.listAllTechnicians(addressId));
    }

    @Operation(summary = "查询Technicians")
    @GetMapping("/technicians")
    public Result<List<UserOrderFlowModel.TechnicianItem>> listTechnicians(
            @RequestParam("serviceMode") Integer serviceMode,
            @RequestParam("serviceTypeId") String serviceTypeId,
            @RequestParam(value = "addressId", required = false) String addressId) {
        return Result.success(
                userOrderFlowService.listTechnicians(serviceMode, serviceTypeId, addressId));
    }

    @Operation(summary = "查询师傅详情")
    @GetMapping("/technician-detail")
    public Result<UserOrderFlowModel.TechnicianDetailResponse> getTechnicianDetail(
            @RequestParam("technicianId") String technicianId) {
        return Result.success(userOrderFlowService.getTechnicianDetail(technicianId));
    }

    @Operation(summary = "切换切换师傅Follow")
    @PostMapping("/technician-follow")
    public Result<UserOrderFlowModel.FollowTechnicianResponse> toggleTechnicianFollow(
            @RequestBody UserOrderFlowModel.FollowTechnicianRequest request) {
        return Result.success(userOrderFlowService.toggleTechnicianFollow(request));
    }

    @GetMapping("/fault-options")
    public Result<List<UserOrderFlowModel.FaultOptionItem>> listFaultOptions(
            @RequestParam("serviceTypeId") String serviceTypeId) {
        return Result.success(userOrderFlowService.listFaultOptions(serviceTypeId));
    }

    @Operation(summary = "查询AppointmentSlots")
    @GetMapping("/appointment-slots")
    public Result<UserOrderFlowModel.AppointmentSlotsResponse> listAppointmentSlots(
            @RequestParam("serviceMode") Integer serviceMode,
            @RequestParam("serviceTypeId") String serviceTypeId,
            @RequestParam("technicianId") String technicianId,
            @RequestParam(value = "addressId", required = false) String addressId,
            @RequestParam(value = "days", required = false) Integer days) {
        return Result.success(
                userOrderFlowService.listAppointmentSlots(
                        serviceMode, serviceTypeId, technicianId, addressId, days));
    }

    @Operation(summary = "查询FeePreview")
    @GetMapping("/fee-preview")
    public Result<UserOrderFlowModel.FeePreviewResponse> getFeePreview(
            @RequestParam("serviceMode") Integer serviceMode,
            @RequestParam("serviceTypeId") String serviceTypeId,
            @RequestParam("technicianId") String technicianId,
            @RequestParam(value = "addressId", required = false) String addressId) {
        return Result.success(
                userOrderFlowService.getFeePreview(
                        serviceMode, serviceTypeId, technicianId, addressId));
    }

    @Operation(summary = "上传上传故障Media")
    @PostMapping(value = "/upload-fault-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserOrderFlowModel.UploadMediaResponse> uploadFaultMedia(
            @RequestParam(value = "mediaType", required = false) String mediaType,
            @RequestPart("file") MultipartFile file) {
        return Result.success(userOrderFlowService.uploadFaultMedia(mediaType, file));
    }

    @Operation(summary = "提交提交Order")
    @PostMapping("/submit")
    public Result<UserOrderFlowModel.SubmitResponse> submitOrder(
            @RequestBody UserOrderFlowModel.SubmitRequest request) {
        creditRecordsService.checkCreditLimit(AuthUserContext.get().getAccountId(), 1, "提交维修订单");
        return Result.success(userOrderFlowService.submitOrder(request));
    }
}
