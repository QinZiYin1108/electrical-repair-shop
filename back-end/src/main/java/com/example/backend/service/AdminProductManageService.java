package com.example.backend.service;

import com.example.backend.model.admin.AdminProductCategoryResponse;
import com.example.backend.model.admin.AdminProductResponse;
import com.example.backend.model.admin.AdminProductSaveRequest;
import com.example.backend.model.admin.AdminProductUploadMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminProductManageService {

    List<AdminProductCategoryResponse> listProductCategories();

    List<AdminProductResponse> listProducts(Integer productType, String keyword, String categoryId, Integer status, String storeId);

    AdminProductResponse createProduct(Integer productType, AdminProductSaveRequest request, String storeId);

    AdminProductResponse updateProduct(Integer productType, String id, AdminProductSaveRequest request, String storeId);

    AdminProductUploadMediaResponse uploadProductMedia(String mediaType, MultipartFile file);

    void deleteProduct(Integer productType, String id, String storeId);
}
