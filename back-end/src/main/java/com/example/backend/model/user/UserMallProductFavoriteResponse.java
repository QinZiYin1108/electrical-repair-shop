package com.example.backend.model.user;

import lombok.Data;

@Data
public class UserMallProductFavoriteResponse {

    private Boolean isFavorite;

    private Integer favoriteCount;
}
