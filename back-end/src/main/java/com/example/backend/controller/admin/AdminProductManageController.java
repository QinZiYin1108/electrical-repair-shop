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
@RequestMapping("/admin/products")
public class AdminProductManageController {

    private final AdminProductCategoryManageService adminProductCategoryManageService;
    private final AdminProductManageService adminProductManageService;

    public AdminProductManageController(
        AdminProductCategoryManageService adminProductCategoryManageService,
        AdminProductManageService adminProductManageService
    ) {
        this.adminProductCategoryManageService = adminProductCategoryManageService;
        this.adminProductManageService = adminProductManageService;
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

    @GetMapping("/categories")
    public Result<List<AdminProductCategoryResponse>> listCategories() {
        return Result.success(adminProductCategoryManageService.listCategories());
    }

    @PostMapping("/categories/create")
    public Result<AdminProductCategoryResponse> createCategory(@Valid @RequestBody AdminProductCategoryCreateRequest request) {
        return Result.success(adminProductCategoryManageService.createCategory(request));
    }

    @PostMapping("/categories/{id}/update")
    public Result<AdminProductCategoryResponse> updateCategory(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminProductCategoryUpdateRequest request
    ) {
        return Result.success(adminProductCategoryManageService.updateCategory(id, request));
    }

    @PostMapping("/categories/{id}/delete")
    public Result<Void> deleteCategory(@PathVariable("id") String id) {
        adminProductCategoryManageService.deleteCategory(id);
        return Result.success();
    }

    @PostMapping(value = "/categories/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadCategoryIcon(@PathVariable("id") String id, @RequestPart("file") MultipartFile file) {
        return Result.success(adminProductCategoryManageService.uploadCategoryIcon(id, file));
    }

    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminProductUploadMediaResponse> uploadProductMedia(
        @RequestParam(value = "mediaType", required = false) String mediaType,
        @RequestPart("file") MultipartFile file
    ) {
        return Result.success(adminProductManageService.uploadProductMedia(mediaType, file));
    }

    @GetMapping("/main")
    public Result<List<AdminProductResponse>> listMainProducts(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "categoryId", required = false) String categoryId,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(adminProductManageService.listProducts(1, keyword, categoryId, status, getStoreFilterId()));
    }

    @PostMapping("/main/create")
    public Result<AdminProductResponse> createMainProduct(@Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(adminProductManageService.createProduct(1, request, requireStoreIdForCreate()));
    }

    @PostMapping("/main/{id}/update")
    public Result<AdminProductResponse> updateMainProduct(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminProductSaveRequest request
    ) {
        return Result.success(adminProductManageService.updateProduct(1, id, request, getStoreFilterId()));
    }

    @PostMapping("/main/{id}/delete")
    public Result<Void> deleteMainProduct(@PathVariable("id") String id) {
        adminProductManageService.deleteProduct(1, id, getStoreFilterId());
        return Result.success();
    }

    @GetMapping("/second-hand")
    public Result<List<AdminProductResponse>> listSecondHandProducts(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "categoryId", required = false) String categoryId,
        @RequestParam(value = "status", required = false) Integer status
    ) {
        return Result.success(adminProductManageService.listProducts(2, keyword, categoryId, status, getStoreFilterId()));
    }

    @PostMapping("/second-hand/create")
    public Result<AdminProductResponse> createSecondHandProduct(@Valid @RequestBody AdminProductSaveRequest request) {
        return Result.success(adminProductManageService.createProduct(2, request, requireStoreIdForCreate()));
    }

    @PostMapping("/second-hand/{id}/update")
    public Result<AdminProductResponse> updateSecondHandProduct(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminProductSaveRequest request
    ) {
        return Result.success(adminProductManageService.updateProduct(2, id, request, getStoreFilterId()));
    }

    @PostMapping("/second-hand/{id}/delete")
    public Result<Void> deleteSecondHandProduct(@PathVariable("id") String id) {
        adminProductManageService.deleteProduct(2, id, getStoreFilterId());
        return Result.success();
    }
}
