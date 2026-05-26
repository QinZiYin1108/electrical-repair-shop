package com.example.backend.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminServiceConfigService {

    List<AdminServiceCategoryResponse> listServiceCategories();

    AdminServiceCategoryResponse createServiceCategory(AdminServiceCategoryCreateRequest request);

    AdminServiceCategoryResponse updateServiceCategory(String id, AdminServiceCategoryUpdateRequest request);

    void deleteServiceCategory(String id);

    List<AdminServiceTypeResponse> listServiceTypes();

    AdminServiceTypeResponse createServiceType(AdminServiceTypeCreateRequest request);

    List<AdminServiceTypeResponse> copyServiceTypes(AdminServiceTypeBatchCopyRequest request);

    AdminServiceTypeResponse updateServiceType(String id, AdminServiceTypeUpdateRequest request);

    void deleteServiceType(String id);

    String uploadServiceCategoryIcon(String categoryId, MultipartFile file);

    List<AdminFaultPhenomenonResponse> listFaultPhenomena(String serviceTypeId);

    AdminFaultPhenomenonResponse createFaultPhenomenon(AdminFaultPhenomenonCreateRequest request);

    List<AdminFaultPhenomenonResponse> copyFaultPhenomena(AdminFaultPhenomenonBatchCopyRequest request);

    AdminFaultPhenomenonResponse updateFaultPhenomenon(String id, AdminFaultPhenomenonUpdateRequest request);

    void deleteFaultPhenomenon(String id);
}
