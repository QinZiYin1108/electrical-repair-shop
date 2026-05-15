package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminFaultPhenomenonCreateRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonBatchCopyRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonResponse;
import com.example.backend.model.admin.AdminFaultPhenomenonUpdateRequest;
import com.example.backend.model.admin.AdminServiceCategoryCreateRequest;
import com.example.backend.model.admin.AdminServiceCategoryResponse;
import com.example.backend.model.admin.AdminServiceCategoryUpdateRequest;
import com.example.backend.model.admin.AdminServiceTypeBatchCopyRequest;
import com.example.backend.model.admin.AdminServiceTypeCreateRequest;
import com.example.backend.model.admin.AdminServiceTypeResponse;
import com.example.backend.model.admin.AdminServiceTypeUpdateRequest;
import com.example.backend.service.AdminServiceConfigService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/admin/config/services")
public class AdminServiceConfigController {

    private final AdminServiceConfigService adminServiceConfigService;

    public AdminServiceConfigController(AdminServiceConfigService adminServiceConfigService) {
        this.adminServiceConfigService = adminServiceConfigService;
    }

    @GetMapping("/categories")
    public Result<List<AdminServiceCategoryResponse>> listCategories() {
        return Result.success(adminServiceConfigService.listServiceCategories());
    }

    @PostMapping("/categories/create")
    public Result<AdminServiceCategoryResponse> createCategory(@Valid @RequestBody AdminServiceCategoryCreateRequest request) {
        return Result.success(adminServiceConfigService.createServiceCategory(request));
    }

    @PostMapping("/categories/{id}/update")
    public Result<AdminServiceCategoryResponse> updateCategory(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminServiceCategoryUpdateRequest request
    ) {
        return Result.success(adminServiceConfigService.updateServiceCategory(id, request));
    }

    @PostMapping("/categories/{id}/delete")
    public Result<Void> deleteCategory(@PathVariable("id") String id) {
        adminServiceConfigService.deleteServiceCategory(id);
        return Result.success();
    }

    @PostMapping(value = "/categories/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadCategoryIcon(@PathVariable("id") String id, @RequestPart("file") MultipartFile file) {
        return Result.success(adminServiceConfigService.uploadServiceCategoryIcon(id, file));
    }

    @GetMapping("/types")
    public Result<List<AdminServiceTypeResponse>> listTypes() {
        return Result.success(adminServiceConfigService.listServiceTypes());
    }

    @PostMapping("/types/create")
    public Result<AdminServiceTypeResponse> createType(@Valid @RequestBody AdminServiceTypeCreateRequest request) {
        return Result.success(adminServiceConfigService.createServiceType(request));
    }

    @PostMapping("/types/copy")
    public Result<List<AdminServiceTypeResponse>> copyTypes(@Valid @RequestBody AdminServiceTypeBatchCopyRequest request) {
        return Result.success(adminServiceConfigService.copyServiceTypes(request));
    }

    @PostMapping("/types/{id}/update")
    public Result<AdminServiceTypeResponse> updateType(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminServiceTypeUpdateRequest request
    ) {
        return Result.success(adminServiceConfigService.updateServiceType(id, request));
    }

    @PostMapping("/types/{id}/delete")
    public Result<Void> deleteType(@PathVariable("id") String id) {
        adminServiceConfigService.deleteServiceType(id);
        return Result.success();
    }

    @GetMapping("/faults")
    public Result<List<AdminFaultPhenomenonResponse>> listFaults(@RequestParam(value = "serviceTypeId", required = false) String serviceTypeId) {
        return Result.success(adminServiceConfigService.listFaultPhenomena(serviceTypeId));
    }

    @PostMapping("/faults/create")
    public Result<AdminFaultPhenomenonResponse> createFault(@Valid @RequestBody AdminFaultPhenomenonCreateRequest request) {
        return Result.success(adminServiceConfigService.createFaultPhenomenon(request));
    }

    @PostMapping("/faults/copy")
    public Result<List<AdminFaultPhenomenonResponse>> copyFaults(@Valid @RequestBody AdminFaultPhenomenonBatchCopyRequest request) {
        return Result.success(adminServiceConfigService.copyFaultPhenomena(request));
    }

    @PostMapping("/faults/{id}/update")
    public Result<AdminFaultPhenomenonResponse> updateFault(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminFaultPhenomenonUpdateRequest request
    ) {
        return Result.success(adminServiceConfigService.updateFaultPhenomenon(id, request));
    }

    @PostMapping("/faults/{id}/delete")
    public Result<Void> deleteFault(@PathVariable("id") String id) {
        adminServiceConfigService.deleteFaultPhenomenon(id);
        return Result.success();
    }
}
