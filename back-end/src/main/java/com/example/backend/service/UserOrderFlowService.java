package com.example.backend.service;

import com.example.backend.model.user.UserOrderFlowModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserOrderFlowService {

    List<UserOrderFlowModel.ServiceModeItem> listServiceModes();

    List<UserOrderFlowModel.CategoryNode> listCategoryTree(String keyword);

    UserOrderFlowModel.CategoryDetailResponse getCategoryDetail(String categoryId);

    List<UserOrderFlowModel.ServiceTypeItem> listServiceTypes(Integer serviceMode, String categoryId);

    UserOrderFlowModel.SelectionContextResponse getSelectionContext(Integer serviceMode, String serviceTypeId, String addressId);

    UserOrderFlowModel.TechnicianBrowseResponse listAllTechnicians(String addressId);

    List<UserOrderFlowModel.FaultOptionItem> listFaultOptions(String serviceTypeId);

    List<UserOrderFlowModel.TechnicianItem> listTechnicians(Integer serviceMode, String serviceTypeId, String addressId);

    UserOrderFlowModel.TechnicianDetailResponse getTechnicianDetail(String technicianId);

    UserOrderFlowModel.FollowTechnicianResponse toggleTechnicianFollow(UserOrderFlowModel.FollowTechnicianRequest request);

    UserOrderFlowModel.AppointmentSlotsResponse listAppointmentSlots(
        Integer serviceMode,
        String serviceTypeId,
        String technicianId,
        String addressId,
        Integer days
    );

    UserOrderFlowModel.FeePreviewResponse getFeePreview(
        Integer serviceMode,
        String serviceTypeId,
        String technicianId,
        String addressId
    );

    UserOrderFlowModel.UploadMediaResponse uploadFaultMedia(String mediaType, MultipartFile file);

    UserOrderFlowModel.SubmitResponse submitOrder(UserOrderFlowModel.SubmitRequest request);

    void validateAppointmentTime(String technicianId, Long appointmentTimeMillis, String excludeOrderId);
}
