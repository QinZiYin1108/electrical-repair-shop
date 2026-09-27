package com.example.backend.model.admin;

import com.example.backend.model.user.UserAddressModel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理员用户收货地址Update请求")
public class AdminUserAddressUpdateRequest extends UserAddressModel.SaveRequest {}
