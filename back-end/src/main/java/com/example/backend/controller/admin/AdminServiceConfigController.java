package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminFaultPhenomenonBatchCopyRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonCreateRequest;
import com.example.backend.model.admin.AdminFaultPhenomenonResponse;
import com.example.backend.model.admin.AdminFaultPhenomenonUpdateRequest;
import com.example.backend.model.admin.AdminServiceCategoryCreateRequest;
import com.example.backend.model.admin.AdminServiceCategoryResponse;
import com.example.backend.model.admin.AdminServiceCategoryUpdateRequest;
import com.example.backend.model.admin.AdminServiceTypeBatchCopyRequest;
import com.example.backend.model.admin.AdminServiceTypeCreateRequest;
import com.example.backend.model.admin.AdminServiceTypeResponse;
import com.example.backend.model.admin.AdminServiceTypeUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "管理员端/服务配置")
@RestController
@RequestMapping("/admin/config/services")
public class AdminServiceConfigController {

    private final AdminServiceConfigService adminServiceConfigService;

    /** 服务配置仅超级管理员可用 */
    private void requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可操作服务配置");
        }
    }

    public AdminServiceConfigController(AdminServiceConfigService adminServiceConfigService) {
        this.adminServiceConfigService = adminServiceConfigService;
    }

    @Operation(summary = "查询分类列表")
    @GetMapping("/categories")
    public Result<List<AdminServiceCategoryResponse>> listCategories() {
        return Result.success(adminServiceConfigService.listServiceCategories());
    }

    @Operation(summary = "创建创建Category")
    @PostMapping("/categories/create")
    public Result<AdminServiceCategoryResponse> createCategory(
            @Valid @RequestBody AdminServiceCategoryCreateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.createServiceCategory(request));
    }

    @Operation(summary = "修改编辑Category")
    @PostMapping("/categories/{id}/update")
    public Result<AdminServiceCategoryResponse> updateCategory(
            @PathVariable("id") String id,
            @Valid @RequestBody AdminServiceCategoryUpdateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.updateServiceCategory(id, request));
    }

    @Operation(summary = "删除删除Category")
    @PostMapping("/categories/{id}/delete")
    public Result<Void> deleteCategory(@PathVariable("id") String id) {
        requireSuperAdmin();
        adminServiceConfigService.deleteServiceCategory(id);
        return Result.success();
    }

    @Operation(summary = "上传上传CategoryIcon")
    @PostMapping(value = "/categories/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadCategoryIcon(
            @PathVariable("id") String id, @RequestPart("file") MultipartFile file) {
        return Result.success(adminServiceConfigService.uploadServiceCategoryIcon(id, file));
    }

    @Operation(summary = "查询Types")
    @GetMapping("/types")
    public Result<List<AdminServiceTypeResponse>> listTypes() {
        return Result.success(adminServiceConfigService.listServiceTypes());
    }

    @Operation(summary = "创建创建Type")
    @PostMapping("/types/create")
    public Result<AdminServiceTypeResponse> createType(
            @Valid @RequestBody AdminServiceTypeCreateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.createServiceType(request));
    }

    @Operation(summary = "提交复制Types")
    @PostMapping("/types/copy")
    public Result<List<AdminServiceTypeResponse>> copyTypes(
            @Valid @RequestBody AdminServiceTypeBatchCopyRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.copyServiceTypes(request));
    }

    @Operation(summary = "修改编辑Type")
    @PostMapping("/types/{id}/update")
    public Result<AdminServiceTypeResponse> updateType(
            @PathVariable("id") String id,
            @Valid @RequestBody AdminServiceTypeUpdateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.updateServiceType(id, request));
    }

    @Operation(summary = "删除删除Type")
    @PostMapping("/types/{id}/delete")
    public Result<Void> deleteType(@PathVariable("id") String id) {
        requireSuperAdmin();
        adminServiceConfigService.deleteServiceType(id);
        return Result.success();
    }

    @Operation(summary = "查询Faults")
    @GetMapping("/faults")
    public Result<List<AdminFaultPhenomenonResponse>> listFaults(
            @RequestParam(value = "serviceTypeId", required = false) String serviceTypeId) {
        return Result.success(adminServiceConfigService.listFaultPhenomena(serviceTypeId));
    }

    @Operation(summary = "创建创建故障")
    @PostMapping("/faults/create")
    public Result<AdminFaultPhenomenonResponse> createFault(
            @Valid @RequestBody AdminFaultPhenomenonCreateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.createFaultPhenomenon(request));
    }

    @Operation(summary = "提交复制Faults")
    @PostMapping("/faults/copy")
    public Result<List<AdminFaultPhenomenonResponse>> copyFaults(
            @Valid @RequestBody AdminFaultPhenomenonBatchCopyRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.copyFaultPhenomena(request));
    }

    @Operation(summary = "修改编辑故障")
    @PostMapping("/faults/{id}/update")
    public Result<AdminFaultPhenomenonResponse> updateFault(
            @PathVariable("id") String id,
            @Valid @RequestBody AdminFaultPhenomenonUpdateRequest request) {
        requireSuperAdmin();
        return Result.success(adminServiceConfigService.updateFaultPhenomenon(id, request));
    }

    @Operation(summary = "删除删除故障")
    @PostMapping("/faults/{id}/delete")
    public Result<Void> deleteFault(@PathVariable("id") String id) {
        requireSuperAdmin();
        adminServiceConfigService.deleteFaultPhenomenon(id);
        return Result.success();
    }
}
