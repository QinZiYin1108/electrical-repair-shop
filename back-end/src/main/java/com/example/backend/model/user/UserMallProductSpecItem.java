package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户商城商品规格列表项")
@Data
public class UserMallProductSpecItem {

    @Schema(description = "键")
    private String key;

    @Schema(description = "值")
    private String value;
}
