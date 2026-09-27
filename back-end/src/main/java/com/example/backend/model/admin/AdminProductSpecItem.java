package com.example.backend.model.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理员商品规格列表项")
@Data
public class AdminProductSpecItem {

    @Schema(description = "键")
    private String key;

    @Schema(description = "值")
    private String value;
}
