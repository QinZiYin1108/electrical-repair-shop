package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "师傅首页订单列表项")
public class WorkerHomeOrderItem {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "状态Text")
    private String statusText;

    @Schema(description = "nextActionText")
    private String nextActionText;

    @Schema(description = "服务类型ID")
    private String serviceTypeId;

    @Schema(description = "服务类型名称")
    private String serviceTypeName;

    @Schema(description = "服务分类ID")
    private String serviceCategoryId;

    @Schema(description = "服务分类名称")
    private String serviceCategoryName;

    @Schema(description = "服务分类路径")
    private String serviceCategoryPath;

    @Schema(description = "服务Mode")
    private Integer serviceMode;

    @Schema(description = "服务ModeText")
    private String serviceModeText;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "服务地址")
    private String serviceAddress;

    @Schema(description = "服务地址Short")
    private String serviceAddressShort;

    @Schema(description = "联系名称")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "电器品牌")
    private String applianceBrand;

    @Schema(description = "电器型号")
    private String applianceModel;

    @Schema(description = "故障Summary")
    private String faultSummary;

    @Schema(description = "总金额")
    private String totalAmount;

    @Schema(description = "paid金额")
    private String paidAmount;

    @Schema(description = "预约时间")
    private Long appointmentTime;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "更新时间")
    private Long updatedTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getNextActionText() {
        return nextActionText;
    }

    public void setNextActionText(String nextActionText) {
        this.nextActionText = nextActionText;
    }

    public String getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public String getServiceTypeName() {
        return serviceTypeName;
    }

    public void setServiceTypeName(String serviceTypeName) {
        this.serviceTypeName = serviceTypeName;
    }

    public String getServiceCategoryId() {
        return serviceCategoryId;
    }

    public void setServiceCategoryId(String serviceCategoryId) {
        this.serviceCategoryId = serviceCategoryId;
    }

    public String getServiceCategoryName() {
        return serviceCategoryName;
    }

    public void setServiceCategoryName(String serviceCategoryName) {
        this.serviceCategoryName = serviceCategoryName;
    }

    public String getServiceCategoryPath() {
        return serviceCategoryPath;
    }

    public void setServiceCategoryPath(String serviceCategoryPath) {
        this.serviceCategoryPath = serviceCategoryPath;
    }

    public Integer getServiceMode() {
        return serviceMode;
    }

    public void setServiceMode(Integer serviceMode) {
        this.serviceMode = serviceMode;
    }

    public String getServiceModeText() {
        return serviceModeText;
    }

    public void setServiceModeText(String serviceModeText) {
        this.serviceModeText = serviceModeText;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public String getServiceAddressShort() {
        return serviceAddressShort;
    }

    public void setServiceAddressShort(String serviceAddressShort) {
        this.serviceAddressShort = serviceAddressShort;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getApplianceBrand() {
        return applianceBrand;
    }

    public void setApplianceBrand(String applianceBrand) {
        this.applianceBrand = applianceBrand;
    }

    public String getApplianceModel() {
        return applianceModel;
    }

    public void setApplianceModel(String applianceModel) {
        this.applianceModel = applianceModel;
    }

    public String getFaultSummary() {
        return faultSummary;
    }

    public void setFaultSummary(String faultSummary) {
        this.faultSummary = faultSummary;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Long getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(Long appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }
}
