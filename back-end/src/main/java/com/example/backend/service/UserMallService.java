package com.example.backend.service;

import com.example.backend.model.user.UserMallCategoryResponse;
import com.example.backend.model.user.UserMallFavoriteProductListItemResponse;
import com.example.backend.model.user.UserMallProductDetailResponse;
import com.example.backend.model.user.UserMallProductFavoriteResponse;
import com.example.backend.model.user.UserMallProductListItemResponse;

import java.math.BigDecimal;
import java.util.List;

public interface UserMallService {

    List<UserMallCategoryResponse> listCategories(Integer productType);

    List<UserMallProductListItemResponse> listProducts(
        Integer productType,
        String keyword,
        String categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean onlyInStock,
        Boolean onlyFreeShipping,
        String marketingTag,
        String sortBy
    );

    List<UserMallFavoriteProductListItemResponse> listFavoriteProducts();

    UserMallProductDetailResponse getProductDetail(String id);

    UserMallProductFavoriteResponse toggleProductFavorite(String id, Boolean favorite);
}
