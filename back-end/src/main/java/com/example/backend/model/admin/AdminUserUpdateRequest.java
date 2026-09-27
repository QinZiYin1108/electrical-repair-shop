package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "管理员用户Update请求")
public class AdminUserUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "username")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "职业")
    private String profession;

    @Schema(description = "紧急联系")
    private String emergencyContact;

    @Schema(description = "紧急手机号")
    private String emergencyPhone;

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

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }
}
