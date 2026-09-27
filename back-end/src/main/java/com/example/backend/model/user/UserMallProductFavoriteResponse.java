package com.example.backend.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户商城商品收藏响应")
@Data
public class UserMallProductFavoriteResponse {

    @Schema(description = "is收藏量")
    private Boolean isFavorite;

    @Schema(description = "收藏量")
    private Integer favoriteCount;
}
