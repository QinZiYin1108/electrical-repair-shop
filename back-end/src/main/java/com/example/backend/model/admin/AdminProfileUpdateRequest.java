package com.example.backend.model.admin;

import jakarta.validation.constraints.NotBlank;

public class AdminProfileUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String phone;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String department;

    private String position;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}

