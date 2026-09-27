package com.example.backend.model.user;

import com.example.backend.model.review.ReviewItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public class UserOrderFlowModel {

    @Data
    public static class ServiceModeItem {
        @Schema(description = "ID")
        private Integer id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "描述")
        private String desc;
    }

    @Data
    public static class CategoryNode {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "级别")
        private Integer level;

        @Schema(description = "父级ID")
        private String parentId;

        private List<CategoryNode> children = new ArrayList<>();
    }

    @Data
    public static class CategoryDetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "编码")
        private String code;

        @Schema(description = "description")
        private String description;

        @Schema(description = "级别")
        private Integer level;

        @Schema(description = "父级ID")
        private String parentId;

        @Schema(description = "父级名称")
        private String parentName;

        @Schema(description = "级别1ID")
        private String level1Id;

        @Schema(description = "级别1名称")
        private String level1Name;

        @Schema(description = "级别2ID")
        private String level2Id;

        @Schema(description = "级别2名称")
        private String level2Name;

        @Schema(description = "级别3ID")
        private String level3Id;

        @Schema(description = "级别3名称")
        private String level3Name;

        @Schema(description = "路径Text")
        private String pathText;

        @Schema(description = "图标URL")
        private String iconUrl;
    }

    @Data
    public static class ServiceTypeItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "类型")
        private Integer type;

        @Schema(description = "分类ID")
        private String categoryId;

        @Schema(description = "base价格")
        private String basePrice;
    }

    @Data
    public static class AddressItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "详情")
        private String detail;

        @Schema(description = "isDefault")
        private Integer isDefault;

        @Schema(description = "纬度")
        private String latitude;

        @Schema(description = "经度")
        private String longitude;
    }

    @Data
    public static class TechnicianItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "评分")
        private String rating;

        @Schema(description = "订单数量")
        private Integer orderCount;

        @Schema(description = "账号状态")
        private Integer accountStatus;

        @Schema(description = "工作状态")
        private Integer workStatus;

        @Schema(description = "work状态Text")
        private String workStatusText;

        @Schema(description = "work状态类型")
        private String workStatusType;

        @Schema(description = "距离Text")
        private String distanceText;

        @Schema(description = "max距离Text")
        private String maxDistanceText;

        @Schema(description = "recommend分数")
        private String recommendScore;

        @Schema(description = "isRecommend")
        private Boolean isRecommend;

        @Schema(description = "头像URL")
        private String avatarUrl;

        @Schema(description = "isFollowed")
        private Boolean isFollowed;

        @Schema(description = "所属门店ID")
        private String storeId;

        @Schema(description = "所属门店名称")
        private String storeName;
    }

    @Data
    public static class TechnicianBrowseResponse {
        @Schema(description = "reference地址ID")
        private String referenceAddressId;

        @Schema(description = "reference地址详情")
        private String referenceAddressDetail;

        private List<TechnicianItem> technicians = new ArrayList<>();
    }

    @Data
    public static class TechnicianDetailResponse {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "评分")
        private String rating;

        @Schema(description = "订单数量")
        private Integer orderCount;

        @Schema(description = "账号状态")
        private Integer accountStatus;

        @Schema(description = "工作状态")
        private Integer workStatus;

        @Schema(description = "work状态Text")
        private String workStatusText;

        @Schema(description = "work状态类型")
        private String workStatusType;

        @Schema(description = "头像URL")
        private String avatarUrl;

        @Schema(description = "isFollowed")
        private Boolean isFollowed;

        @Schema(description = "workYears")
        private Integer workYears;

        @Schema(description = "completed排序数量")
        private Long completedOrderCount;

        @Schema(description = "introduction")
        private String introduction;

        @Schema(description = "specialties")
        private String specialties;

        @Schema(description = "certificates")
        private String certificates;

        @Schema(description = "education")
        private String education;

        @Schema(description = "位置地址")
        private String locationAddress;

        @Schema(description = "纬度")
        private String latitude;

        @Schema(description = "经度")
        private String longitude;

        @Schema(description = "所属门店ID")
        private String storeId;

        @Schema(description = "所属门店名称")
        private String storeName;

        private List<ReviewItemResponse> reviews = new ArrayList<>();
    }

    @Data
    public static class FollowTechnicianRequest {
        @Schema(description = "师傅ID")
        private String technicianId;

        @Schema(description = "follow")
        private Boolean follow;
    }

    @Data
    public static class FollowTechnicianResponse {
        @Schema(description = "师傅ID")
        private String technicianId;

        @Schema(description = "isFollowed")
        private Boolean isFollowed;
    }

    @Data
    public static class SelectionContextResponse {
        @Schema(description = "服务Mode")
        private Integer serviceMode;

        @Schema(description = "服务Mode名称")
        private String serviceModeName;

        @Schema(description = "服务类型ID")
        private String serviceTypeId;

        @Schema(description = "服务类型名称")
        private String serviceTypeName;

        @Schema(description = "分类路径")
        private String categoryPath;

        @Schema(description = "show地址Section")
        private Boolean showAddressSection;

        @Schema(description = "selected地址ID")
        private String selectedAddressId;

        private List<AddressItem> addresses = new ArrayList<>();
        private List<TechnicianItem> technicians = new ArrayList<>();
    }

    @Data
    public static class FaultOptionItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;
    }

    @Data
    public static class AppointmentSlotItem {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "标签")
        private String label;

        @Schema(description = "预约时间")
        private Long appointmentTime;
    }

    @Data
    public static class AppointmentWorkWindowItem {
        @Schema(description = "星期")
        private Integer dayOfWeek;

        @Schema(description = "日标签")
        private String dayLabel;

        @Schema(description = "开始时间")
        private String startTime;

        @Schema(description = "结束时间")
        private String endTime;
    }

    @Data
    public static class AppointmentSlotsResponse {
        @Schema(description = "服务Mode")
        private Integer serviceMode;

        @Schema(description = "服务Mode名称")
        private String serviceModeName;

        @Schema(description = "服务类型名称")
        private String serviceTypeName;

        @Schema(description = "师傅名称")
        private String technicianName;

        @Schema(description = "地址详情")
        private String addressDetail;

        @Schema(description = "minLeadMinutes")
        private Integer minLeadMinutes;

        @Schema(description = "bookingDays")
        private Integer bookingDays;

        @Schema(description = "booking开始Date")
        private String bookingStartDate;

        @Schema(description = "booking结束Date")
        private String bookingEndDate;

        private List<AppointmentWorkWindowItem> workWindows = new ArrayList<>();
        private List<AppointmentSlotItem> appointmentSlots = new ArrayList<>();
    }

    @Data
    public static class FeePreviewResponse {
        @Schema(description = "距离Km")
        private String distanceKm;

        @Schema(description = "上门费用")
        private String doorFee;

        @Schema(description = "距离费用")
        private String distanceFee;

        @Schema(description = "总金额")
        private String totalAmount;

        @Schema(description = "formula")
        private String formula;
    }

    @Data
    public static class UploadMediaResponse {
        @Schema(description = "URL")
        private String url;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "文件每页数量")
        private Long fileSize;

        @Schema(description = "MIME类型类型")
        private String mimeType;

        @Schema(description = "宽度")
        private Integer width;

        @Schema(description = "高度")
        private Integer height;

        @Schema(description = "时长")
        private Integer duration;

        @Schema(description = "thumbnailURL")
        private String thumbnailUrl;
    }

    @Data
    public static class SubmitRequest {
        @Schema(description = "服务Mode")
        private Integer serviceMode;

        @Schema(description = "分类ID")
        private String categoryId;

        @Schema(description = "服务类型ID")
        private String serviceTypeId;

        @Schema(description = "师傅ID")
        private String technicianId;

        @Schema(description = "服务地址ID")
        private String serviceAddressId;

        @Schema(description = "预约时间")
        private Long appointmentTime;

        @Schema(description = "电器品牌")
        private String applianceBrand;

        @Schema(description = "电器型号")
        private String applianceModel;

        @Schema(description = "购买Date")
        private String purchaseDate;

        @Schema(description = "优惠券ID")
        private String couponId;

        @Schema(description = "支付方式")
        private Integer paymentMethod;

        private List<SubmitFaultItem> faultList = new ArrayList<>();
    }

    @Data
    public static class SubmitFaultItem {
        @Schema(description = "故障ID")
        private String faultId;

        @Schema(description = "故障名称")
        private String faultName;

        @Schema(description = "故障Description")
        private String faultDescription;

        private List<SubmitImageItem> images = new ArrayList<>();

        @Schema(description = "视频")
        private SubmitVideoItem video;
    }

    @Data
    public static class SubmitImageItem {
        @Schema(description = "URL")
        private String url;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "文件每页数量")
        private Long fileSize;

        @Schema(description = "MIME类型类型")
        private String mimeType;

        @Schema(description = "宽度")
        private Integer width;

        @Schema(description = "高度")
        private Integer height;
    }

    @Data
    public static class SubmitVideoItem {
        @Schema(description = "URL")
        private String url;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "文件每页数量")
        private Long fileSize;

        @Schema(description = "MIME类型类型")
        private String mimeType;

        @Schema(description = "时长")
        private Integer duration;

        @Schema(description = "宽度")
        private Integer width;

        @Schema(description = "高度")
        private Integer height;

        @Schema(description = "thumbnailURL")
        private String thumbnailUrl;
    }

    @Data
    public static class SubmitResponse {
        @Schema(description = "排序ID")
        private String orderId;

        @Schema(description = "订单号")
        private String orderNo;

        @Schema(description = "支付状态")
        private Integer paymentStatus;

        @Schema(description = "总金额")
        private String totalAmount;

        @Schema(description = "paid金额")
        private String paidAmount;
    }
}
