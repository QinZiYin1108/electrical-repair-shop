package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户商城商品收藏请求")
@Data
public class UserMallProductFavoriteRequest {

    @Schema(description = "收藏量")
    private Boolean favorite;
}
