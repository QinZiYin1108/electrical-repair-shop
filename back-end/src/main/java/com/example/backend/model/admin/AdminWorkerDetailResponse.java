package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理员师傅详情响应")
public class AdminWorkerDetailResponse {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "username")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "账号状态")
    private Integer accountStatus;

    @Schema(description = "工作状态")
    private Integer workStatus;

    @Schema(description = "评分")
    private BigDecimal rating;

    @Schema(description = "创建时间")
    private Long createdTime;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "完成率")
    private BigDecimal completionRate;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "IDCard")
    private String idCard;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "生日")
    private String birthday;

    @Schema(description = "workYears")
    private Integer workYears;

    @Schema(description = "education")
    private String education;

    @Schema(description = "introduction")
    private String introduction;

    @Schema(description = "响应时间")
    private Integer responseTime;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "服务AreaCenter")
    private AdminWorkerServiceAreaCenterResponse serviceAreaCenter;

    @Schema(description = "上门费用Policies")
    private List<AdminWorkerVisitFeePolicyResponse> visitFeePolicies;

    @Schema(description = "workTimes")
    private List<AdminWorkerWorkTimeResponse> workTimes;

    @Schema(description = "排序Stats")
    private AdminWorkerOrderStatsResponse orderStats;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(Integer accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Integer getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(Integer workStatus) {
        this.workStatus = workStatus;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(BigDecimal completionRate) {
        this.completionRate = completionRate;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public Integer getWorkYears() {
        return workYears;
    }

    public void setWorkYears(Integer workYears) {
        this.workYears = workYears;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public Integer getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Integer responseTime) {
        this.responseTime = responseTime;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public AdminWorkerServiceAreaCenterResponse getServiceAreaCenter() {
        return serviceAreaCenter;
    }

    public void setServiceAreaCenter(AdminWorkerServiceAreaCenterResponse serviceAreaCenter) {
        this.serviceAreaCenter = serviceAreaCenter;
    }

    public List<AdminWorkerVisitFeePolicyResponse> getVisitFeePolicies() {
        return visitFeePolicies;
    }

    public void setVisitFeePolicies(List<AdminWorkerVisitFeePolicyResponse> visitFeePolicies) {
        this.visitFeePolicies = visitFeePolicies;
    }

    public List<AdminWorkerWorkTimeResponse> getWorkTimes() {
        return workTimes;
    }

    public void setWorkTimes(List<AdminWorkerWorkTimeResponse> workTimes) {
        this.workTimes = workTimes;
    }

    public AdminWorkerOrderStatsResponse getOrderStats() {
        return orderStats;
    }

    public void setOrderStats(AdminWorkerOrderStatsResponse orderStats) {
        this.orderStats = orderStats;
    }
}
