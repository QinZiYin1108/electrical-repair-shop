package com.example.backend.service;

import com.example.backend.model.admin.AdminProductCategoryCreateRequest;
import com.example.backend.model.admin.AdminProductCategoryResponse;
import com.example.backend.model.admin.AdminProductCategoryUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminProductCategoryManageService {

    List<AdminProductCategoryResponse> listCategories();

    AdminProductCategoryResponse createCategory(AdminProductCategoryCreateRequest request);

    AdminProductCategoryResponse updateCategory(String id, AdminProductCategoryUpdateRequest request);

    void deleteCategory(String id);

    String uploadCategoryIcon(String id, MultipartFile file);
}
