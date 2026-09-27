package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminProductCategoryCreateRequest;
import com.example.backend.model.admin.AdminProductCategoryResponse;
import com.example.backend.model.admin.AdminProductCategoryUpdateRequest;
import com.example.backend.model.admin.AdminProductResponse;
import com.example.backend.model.admin.AdminProductSaveRequest;
import com.example.backend.model.admin.AdminProductUploadMediaResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AdminProductCategoryManageService;
import com.example.backend.service.AdminProductManageService;
import com.example.backend.service.ProductsService;
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

@RestController
@Tag(name = "管理员端/商品管理", description = "商品创建/编辑/删除、分类管理、素材上传、审核冻结")
@RequestMapping("/admin/products")
public class AdminProductManageController {

    private final AdminProductCategoryManageService adminProductCategoryManageService;
    private final AdminProductManageService adminProductManageService;
    private final ProductsService productsService;

    public AdminProductManageController(
            AdminProductCategoryManageService adminProductCategoryManageService,
            AdminProductManageService adminProductManageService,
            ProductsService productsService) {
        this.adminProductCategoryManageService = adminProductCategoryManageService;
        this.adminProductManageService = adminProductManageService;
        this.productsService = productsService;
    }

    /** 获取当前管理员的店铺ID（店铺管理员返回自己的storeId，超管返回null表示不过滤） */
    private String getStoreFilterId() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isStoreAdmin()) {
            return null; // 超管不过滤
        }
        return user.getStoreId();
    }

    /** 店铺管理员必须传storeId，超管可传可不传 */
    private LoginUserInfo requireSuperAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅超级管理员可执行此操作");
        }
        return user;
    }

    private String requireStoreIdForCreate() {
        LoginUserInfo user = AuthUserContext.get();
        if (user.isStoreAdmin()) {
            if (user.getStoreId() == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "店铺管理员未绑定门店");
            }
            return user.getStoreId();
        }
        return null;
    }

    @Operation(summary = "查询分类列表")
    @GetMapping("/categories")
    public Result<List<AdminProductCategoryResponse>> listCategories() {
        return Result.success(adminProductCategoryManageService.listCategories());
    }

    @Operation(summary = "创建创建Category")
    @PostMapping("/categories/create")
    public Result<AdminProductCategoryResponse> createCategory(
            @Valid @RequestBody AdminProductCategoryCreateRequest request) {
        return Result.success(adminProductCategoryManageService.createCategory(request));
    }

    @Operation(summary = "修改编辑Category")
    @PostMapping("/categories/{id}/update")
    public Result<AdminProductCategoryResponse> updateCategory(
            @PathVariable("id") String id,
            @Valid @RequestBody AdminProductCategoryUpdateRequest request) {
        return Result.success(adminProductCategoryManageService.updateCategory(id, request));
    }

    @Operation(summary = "删除删除Category")
    @PostMapping("/categories/{id}/delete")
    public Result<Void> deleteCategory(@PathVariable("id") String id) {
        adminProductCategoryManageService.deleteCategory(id);
        return Result.success();
    }

    @Operation(summary = "上传上传CategoryIcon")
    @PostMapping(value = "/categories/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadCategoryIcon(
            @PathVariable("id") String id, @RequestPart("file") MultipartFile file) {
        return Result.success(adminProductCategoryManageService.uploadCategoryIcon(id, file));
    }

    @Operation(summary = "上传上传ProductMedia")
    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminProductUploadMediaResponse> uploadProductMedia(
            @RequestParam(value = "mediaType", required = false) String mediaType,
            @RequestPart("file") MultipartFile file) {
        return Result.success(adminProductManageService.uploadProductMedia(mediaType, file));
    }

    @Operation(summary = "查询主商品商品列表")
    @GetMapping("/main")
    public Result<List<AdminProductResponse>> listMainProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "isFrozen", required = false) Integer isFrozen) {
        return Result.success(
                adminProductManageService.listProducts(
                        1, keyword, categoryId, status, getStoreFilterId(), auditStatus, isFrozen));
    }

    @Operation(summary = "创建创建主商品Product")
    @PostMapping("/main/create")
    public Result<AdminProductResponse> createMainProduct(
            @Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(
                adminProductManageService.createProduct(1, request, requireStoreIdForCreate()));
    }

    @Operation(summary = "修改编辑主商品Product")
    @PostMapping("/main/{id}/update")
    public Result<AdminProductResponse> updateMainProduct(
            @PathVariable("id") String id, @Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(
                adminProductManageService.updateProduct(1, id, request, getStoreFilterId()));
    }

    @Operation(summary = "提交freeze主商品Product")
    @PostMapping("/main/{id}/freeze")
    public Result<Void> freezeMainProduct(@PathVariable("id") String id) {
        LoginUserInfo user = requireSuperAdmin();
        productsService.freezeProduct(id, user.getAccountId());
        return Result.success();
    }

    @Operation(summary = "提交unfreeze主商品Product")
    @PostMapping("/main/{id}/unfreeze")
    public Result<Void> unfreezeMainProduct(@PathVariable("id") String id) {
        LoginUserInfo user = requireSuperAdmin();
        productsService.unfreezeProduct(id);
        return Result.success();
    }

    @Operation(summary = "审核审核主商品Product")
    @PostMapping("/main/{id}/audit")
    public Result<Void> auditMainProduct(
            @PathVariable("id") String id, @RequestBody java.util.Map<String, Object> body) {
        LoginUserInfo user = requireSuperAdmin();
        Integer auditStatus =
                body.get("auditStatus") instanceof Number
                        ? ((Number) body.get("auditStatus")).intValue()
                        : null;
        String remark = body.get("remark") instanceof String ? (String) body.get("remark") : null;
        if (auditStatus == null || (auditStatus != 2 && auditStatus != 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态仅支持2(通过)或3(拒绝)");
        }
        productsService.auditProduct(id, auditStatus, remark);
        return Result.success();
    }

    @Operation(summary = "查询二手商品二手商品列表")
    @GetMapping("/second-hand")
    public Result<List<AdminProductResponse>> listSecondHandProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus,
            @RequestParam(value = "isFrozen", required = false) Integer isFrozen) {
        return Result.success(
                adminProductManageService.listProducts(
                        2, keyword, categoryId, status, getStoreFilterId(), auditStatus, isFrozen));
    }

    @Operation(summary = "创建创建二手商品二手Product")
    @PostMapping("/second-hand/create")
    public Result<AdminProductResponse> createSecondHandProduct(
            @Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(
                adminProductManageService.createProduct(2, request, requireStoreIdForCreate()));
    }

    @Operation(summary = "修改编辑二手商品二手Product")
    @PostMapping("/second-hand/{id}/update")
    public Result<AdminProductResponse> updateSecondHandProduct(
            @PathVariable("id") String id, @Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(
                adminProductManageService.updateProduct(2, id, request, getStoreFilterId()));
    }

    @Operation(summary = "提交freeze二手商品二手Product")
    @PostMapping("/second-hand/{id}/freeze")
    public Result<Void> freezeSecondHandProduct(@PathVariable("id") String id) {
        LoginUserInfo user = requireSuperAdmin();
        productsService.freezeProduct(id, user.getAccountId());
        return Result.success();
    }

    @Operation(summary = "提交unfreeze二手商品二手Product")
    @PostMapping("/second-hand/{id}/unfreeze")
    public Result<Void> unfreezeSecondHandProduct(@PathVariable("id") String id) {
        LoginUserInfo user = requireSuperAdmin();
        productsService.unfreezeProduct(id);
        return Result.success();
    }

    @Operation(summary = "审核审核二手商品二手Product")
    @PostMapping("/second-hand/{id}/audit")
    public Result<Void> auditSecondHandProduct(
            @PathVariable("id") String id, @RequestBody java.util.Map<String, Object> body) {
        LoginUserInfo user = requireSuperAdmin();
        Integer auditStatus =
                body.get("auditStatus") instanceof Number
                        ? ((Number) body.get("auditStatus")).intValue()
                        : null;
        String remark = body.get("remark") instanceof String ? (String) body.get("remark") : null;
        if (auditStatus == null || (auditStatus != 2 && auditStatus != 3)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态仅支持2(通过)或3(拒绝)");
        }
        productsService.auditProduct(id, auditStatus, remark);
        return Result.success();
    }
}
