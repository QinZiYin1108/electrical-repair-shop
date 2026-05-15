package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserOrderFlowModel;
import com.example.backend.service.UserOrderFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user/order-flow")
public class UserOrderFlowController {

    private final UserOrderFlowService userOrderFlowService;

    public UserOrderFlowController(UserOrderFlowService userOrderFlowService) {
        this.userOrderFlowService = userOrderFlowService;
    }

    @GetMapping("/service-modes")
    public Result<List<UserOrderFlowModel.ServiceModeItem>> listServiceModes() {
        return Result.success(userOrderFlowService.listServiceModes());
    }

    @GetMapping("/categories")
    public Result<List<UserOrderFlowModel.CategoryNode>> listCategoryTree(
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return Result.success(userOrderFlowService.listCategoryTree(keyword));
    }

    @GetMapping("/category-detail")
    public Result<UserOrderFlowModel.CategoryDetailResponse> getCategoryDetail(
        @RequestParam("categoryId") String categoryId
    ) {
        return Result.success(userOrderFlowService.getCategoryDetail(categoryId));
    }

    @GetMapping("/service-types")
    public Result<List<UserOrderFlowModel.ServiceTypeItem>> listServiceTypes(
        @RequestParam("serviceMode") Integer serviceMode,
        @RequestParam("categoryId") String categoryId
    ) {
        return Result.success(userOrderFlowService.listServiceTypes(serviceMode, categoryId));
    }

    @GetMapping("/selection-context")
    public Result<UserOrderFlowModel.SelectionContextResponse> getSelectionContext(
        @RequestParam("serviceMode") Integer serviceMode,
        @RequestParam("serviceTypeId") String serviceTypeId,
        @RequestParam(value = "addressId", required = false) String addressId
    ) {
        return Result.success(userOrderFlowService.getSelectionContext(serviceMode, serviceTypeId, addressId));
    }

    @GetMapping("/all-technicians")
    public Result<UserOrderFlowModel.TechnicianBrowseResponse> listAllTechnicians(
        @RequestParam(value = "addressId", required = false) String addressId
    ) {
        return Result.success(userOrderFlowService.listAllTechnicians(addressId));
    }

    @GetMapping("/technicians")
    public Result<List<UserOrderFlowModel.TechnicianItem>> listTechnicians(
        @RequestParam("serviceMode") Integer serviceMode,
        @RequestParam("serviceTypeId") String serviceTypeId,
        @RequestParam(value = "addressId", required = false) String addressId
    ) {
        return Result.success(userOrderFlowService.listTechnicians(serviceMode, serviceTypeId, addressId));
    }

    @GetMapping("/technician-detail")
    public Result<UserOrderFlowModel.TechnicianDetailResponse> getTechnicianDetail(
        @RequestParam("technicianId") String technicianId
    ) {
        return Result.success(userOrderFlowService.getTechnicianDetail(technicianId));
    }

    @PostMapping("/technician-follow")
    public Result<UserOrderFlowModel.FollowTechnicianResponse> toggleTechnicianFollow(
        @RequestBody UserOrderFlowModel.FollowTechnicianRequest request
    ) {
        return Result.success(userOrderFlowService.toggleTechnicianFollow(request));
    }

@GetMapping("/fault-options")
    public Result<List<UserOrderFlowModel.FaultOptionItem>> listFaultOptions(
        @RequestParam("serviceTypeId") String serviceTypeId
    ) {
        return Result.success(userOrderFlowService.listFaultOptions(serviceTypeId));
    }

    @GetMapping("/appointment-slots")
    public Result<UserOrderFlowModel.AppointmentSlotsResponse> listAppointmentSlots(
        @RequestParam("serviceMode") Integer serviceMode,
        @RequestParam("serviceTypeId") String serviceTypeId,
        @RequestParam("technicianId") String technicianId,
        @RequestParam(value = "addressId", required = false) String addressId,
        @RequestParam(value = "days", required = false) Integer days
    ) {
        return Result.success(
            userOrderFlowService.listAppointmentSlots(serviceMode, serviceTypeId, technicianId, addressId, days)
        );
    }

    @GetMapping("/fee-preview")
    public Result<UserOrderFlowModel.FeePreviewResponse> getFeePreview(
        @RequestParam("serviceMode") Integer serviceMode,
        @RequestParam("serviceTypeId") String serviceTypeId,
        @RequestParam("technicianId") String technicianId,
        @RequestParam(value = "addressId", required = false) String addressId
    ) {
        return Result.success(userOrderFlowService.getFeePreview(serviceMode, serviceTypeId, technicianId, addressId));
    }

    @PostMapping(value = "/upload-fault-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserOrderFlowModel.UploadMediaResponse> uploadFaultMedia(
        @RequestParam(value = "mediaType", required = false) String mediaType,
        @RequestPart("file") MultipartFile file
    ) {
        return Result.success(userOrderFlowService.uploadFaultMedia(mediaType, file));
    }

    @PostMapping("/submit")
    public Result<UserOrderFlowModel.SubmitResponse> submitOrder(@RequestBody UserOrderFlowModel.SubmitRequest request) {
        return Result.success(userOrderFlowService.submitOrder(request));
    }
}
