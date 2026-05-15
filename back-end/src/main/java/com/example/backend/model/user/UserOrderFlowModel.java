package com.example.backend.model.user;

import com.example.backend.model.review.ReviewItemResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class UserOrderFlowModel {

    @Data
    public static class ServiceModeItem {
        private Integer id;
        private String name;
        private String desc;
    }

    @Data
    public static class CategoryNode {
        private String id;
        private String name;
        private Integer level;
        private String parentId;
        private List<CategoryNode> children = new ArrayList<>();
    }

    @Data
    public static class CategoryDetailResponse {
        private String id;
        private String name;
        private String code;
        private String description;
        private Integer level;
        private String parentId;
        private String parentName;
        private String level1Id;
        private String level1Name;
        private String level2Id;
        private String level2Name;
        private String level3Id;
        private String level3Name;
        private String pathText;
        private String iconUrl;
    }

    @Data
    public static class ServiceTypeItem {
        private String id;
        private String name;
        private Integer type;
        private String categoryId;
        private String basePrice;
    }

    @Data
    public static class AddressItem {
        private String id;
        private String label;
        private String detail;
        private Integer isDefault;
        private String latitude;
        private String longitude;
    }

    @Data
    public static class TechnicianItem {
        private String id;
        private String name;
        private String rating;
        private Integer orderCount;
        private Integer accountStatus;
        private Integer workStatus;
        private String workStatusText;
        private String workStatusType;
        private String distanceText;
        private String maxDistanceText;
        private String recommendScore;
        private Boolean isRecommend;
        private String avatarUrl;
        private Boolean isFollowed;
    }

    @Data
    public static class TechnicianBrowseResponse {
        private String referenceAddressId;
        private String referenceAddressDetail;
        private List<TechnicianItem> technicians = new ArrayList<>();
    }

    @Data
    public static class TechnicianDetailResponse {
        private String id;
        private String name;
        private String rating;
        private Integer orderCount;
        private Integer accountStatus;
        private Integer workStatus;
        private String workStatusText;
        private String workStatusType;
        private String avatarUrl;
        private Boolean isFollowed;
        private Integer workYears;
        private Long completedOrderCount;
        private String introduction;
        private String specialties;
        private String certificates;
        private String education;
        private String locationAddress;
        private String latitude;
        private String longitude;
        private List<ReviewItemResponse> reviews = new ArrayList<>();
    }

    @Data
    public static class FollowTechnicianRequest {
        private String technicianId;
        private Boolean follow;
    }

    @Data
    public static class FollowTechnicianResponse {
        private String technicianId;
        private Boolean isFollowed;
    }

    @Data
    public static class SelectionContextResponse {
        private Integer serviceMode;
        private String serviceModeName;
        private String serviceTypeId;
        private String serviceTypeName;
        private String categoryPath;
        private Boolean showAddressSection;
        private String selectedAddressId;
        private List<AddressItem> addresses = new ArrayList<>();
        private List<TechnicianItem> technicians = new ArrayList<>();
    }

    @Data
    public static class FaultOptionItem {
        private String id;
        private String name;
    }

    @Data
    public static class AppointmentSlotItem {
        private String id;
        private String label;
        private Long appointmentTime;
    }

    @Data
    public static class AppointmentWorkWindowItem {
        private Integer dayOfWeek;
        private String dayLabel;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class AppointmentSlotsResponse {
        private Integer serviceMode;
        private String serviceModeName;
        private String serviceTypeName;
        private String technicianName;
        private String addressDetail;
        private Integer minLeadMinutes;
        private Integer bookingDays;
        private String bookingStartDate;
        private String bookingEndDate;
        private List<AppointmentWorkWindowItem> workWindows = new ArrayList<>();
        private List<AppointmentSlotItem> appointmentSlots = new ArrayList<>();
    }

    @Data
    public static class FeePreviewResponse {
        private String distanceKm;
        private String doorFee;
        private String distanceFee;
        private String totalAmount;
        private String formula;
    }

    @Data
    public static class UploadMediaResponse {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer width;
        private Integer height;
        private Integer duration;
        private String thumbnailUrl;
    }

    @Data
    public static class SubmitRequest {
        private Integer serviceMode;
        private String categoryId;
        private String serviceTypeId;
        private String technicianId;
        private String serviceAddressId;
        private Long appointmentTime;
        private String applianceBrand;
        private String applianceModel;
        private String purchaseDate;
        private String couponId;
        private Integer paymentMethod;
        private List<SubmitFaultItem> faultList = new ArrayList<>();
    }

    @Data
    public static class SubmitFaultItem {
        private String faultId;
        private String faultName;
        private String faultDescription;
        private List<SubmitImageItem> images = new ArrayList<>();
        private SubmitVideoItem video;
    }

    @Data
    public static class SubmitImageItem {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer width;
        private Integer height;
    }

    @Data
    public static class SubmitVideoItem {
        private String url;
        private String name;
        private Long fileSize;
        private String mimeType;
        private Integer duration;
        private Integer width;
        private Integer height;
        private String thumbnailUrl;
    }

    @Data
    public static class SubmitResponse {
        private String orderId;
        private String orderNo;
        private Integer paymentStatus;
        private String totalAmount;
        private String paidAmount;
    }
}
