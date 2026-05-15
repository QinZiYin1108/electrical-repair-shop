package com.example.backend.model.admin;

import java.math.BigDecimal;
import java.util.List;

public class AdminWorkerDetailResponse {

    private String id;
    private String username;
    private String phone;
    private String email;
    private Integer accountStatus;
    private Integer workStatus;
    private BigDecimal rating;
    private Long createdTime;
    private String address;
    private Integer orderCount;
    private BigDecimal completionRate;
    private String realName;
    private String idCard;
    private Integer gender;
    private String birthday;
    private Integer workYears;
    private String education;
    private String introduction;
    private Integer responseTime;
    private String avatarUrl;

    private AdminWorkerServiceAreaCenterResponse serviceAreaCenter;
    private List<AdminWorkerVisitFeePolicyResponse> visitFeePolicies;
    private List<AdminWorkerWorkTimeResponse> workTimes;
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
